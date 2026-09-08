package com.viora.app.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.viora.app.BuildConfig
import com.viora.app.ai.AccessibilityThreatAnalyzer
import com.viora.app.ai.CompositeGuardianAI
import com.viora.app.ai.GuardianAI
import com.viora.app.ai.MLGuardianAI
import com.viora.app.ai.ThreatEngine
import com.viora.app.ai.ml.LocalThreatClassifier
import com.viora.app.core.overlay.VioraOverlayService
import com.viora.app.core.overlay.VioraRiskLevel
import com.viora.app.core.overlay.VioraWarning
import com.viora.app.data.history.ThreatHistoryRepositoryImpl
import com.viora.app.data.history.VioraDatabase
import com.viora.app.domain.history.ThreatHistoryRepository
import com.viora.app.domain.model.InputType
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.perception.AccessibilitySnapshot
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import kotlinx.coroutines.runBlocking

/** Collects structured text from the active accessibility tree for the perception layer. */
class VioraAccessibilityService : AccessibilityService() {

    // Phase 8: adds MLGuardianAI (on-device text classifier) alongside the
    // deterministic engines. `by lazy` because MLGuardianAI needs
    // applicationContext to load its model asset, which isn't safely
    // available until the service is attached (same reason historyRepository
    // below is lazy).
    private val guardianAI: GuardianAI by lazy {
        CompositeGuardianAI(
            ThreatEngine(),
            AccessibilityThreatAnalyzer(),
            MLGuardianAI(LocalThreatClassifier(applicationContext))
        )
    }

    /** Same Room-backed history store ScannerViewModel uses — one shared record, no duplicate scoring. */
    private val historyRepository: ThreatHistoryRepository by lazy {
        ThreatHistoryRepositoryImpl(VioraDatabase.getInstance(applicationContext).threatHistoryDao())
    }

    private val handler = Handler(Looper.getMainLooper())
    private var lastSnapshotPackage: String? = null
    private var lastSnapshotHash: Int? = null
    private var pendingSnapshot: AccessibilitySnapshot? = null

    // Phase 7: identifies the last (package, risk, signals) combination we already
    // reacted to — gates BOTH history persistence and the overlay. The debounce
    // above re-triggers analysis on every visible-text change, which can happen
    // many times for one lingering screen (a ticking timestamp, a blinking
    // cursor); without this, each re-analysis would re-persist and previously
    // could re-consider showing the overlay for what is really one single event.
    private var lastEventKey: String? = null
    private val analyzePendingSnapshot = Runnable { analyzePendingSnapshot() }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        val packageName = root.packageName?.toString()
            ?: event?.packageName?.toString()
            ?: return
        // Phase 5: only run analysis on contexts where payment fraud/social-engineering
        // actually happens — payment/banking apps and common messaging/email apps.
        if (!AccessibilityRelevanceFilter.isRelevant(packageName, packageNameOfApplication)) return

        val visibleText = collectVisibleText(root)
        if (visibleText.isEmpty()) return

        val snapshot = AccessibilitySnapshot(
            packageName = packageName,
            timestamp = System.currentTimeMillis(),
            visibleText = visibleText
        )
        val snapshotHash = snapshot.visibleText.hashCode()
        if (snapshot.packageName == lastSnapshotPackage && snapshotHash == lastSnapshotHash) {
            return
        }

        lastSnapshotPackage = snapshot.packageName
        lastSnapshotHash = snapshotHash
        pendingSnapshot = snapshot
        handler.removeCallbacks(analyzePendingSnapshot)
        handler.postDelayed(analyzePendingSnapshot, ANALYSIS_DEBOUNCE_MS)
    }

    override fun onInterrupt() {
        handler.removeCallbacks(analyzePendingSnapshot)
        pendingSnapshot = null
    }

    override fun onDestroy() {
        handler.removeCallbacks(analyzePendingSnapshot)
        super.onDestroy()
    }

    private fun analyzePendingSnapshot() {
        val snapshot = pendingSnapshot ?: return
        pendingSnapshot = null
        val context = VioraContext(
            inputType = InputType.TEXT,
            rawContent = snapshot.visibleText,
            extractedText = snapshot.visibleText
        )
        val assessment = safeAnalyze(context) ?: return
        val confidence = confidenceFor(assessment)
        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "VIORA_ANALYSIS package=${snapshot.packageName} " +
                    "signals=${assessment.signals.map { it.id }} " +
                    "risk=${assessment.riskLevel} confidence=$confidence"
            )
        }

        if (!AccessibilityWarningPolicy.shouldRecord(assessment.riskLevel)) {
            lastEventKey = null
            return
        }

        val eventKey = accessibilityEventSignature(snapshot.packageName, assessment)
        if (eventKey == lastEventKey) return
        lastEventKey = eventKey

        // Anything above SAFE is worth keeping in history (nothing silently dropped),
        // even if it isn't strong enough to interrupt the user — see recordToHistory().
        recordToHistory(context, assessment)

        if (!AccessibilityWarningPolicy.shouldWarn(assessment.riskLevel)) return

        runCatching {
            VioraOverlayService.showWarning(
                this,
                VioraWarning(
                    title = titleFor(assessment.riskLevel),
                    explanation = assessment.explanation,
                    riskLevel = assessment.riskLevel.toOverlayRisk()
                )
            )
        }.onFailure {
            if (BuildConfig.DEBUG) Log.d(TAG, "Unable to show overlay: ${it.javaClass.simpleName}")
        }
    }

    /**
     * Runs GuardianAI defensively: every current engine is deterministic
     * regex/arithmetic with no expected failure mode, but an analysis failure must
     * never crash the accessibility service — that would silently disable
     * background protection until the user manually re-enables it, a far worse
     * outcome than skipping one screen's analysis.
     */
    private fun safeAnalyze(context: VioraContext): ThreatAssessment? =
        try {
            // GuardianAI.analyze is suspend, but every current engine is synchronous
            // (regex/arithmetic only, no real suspension point), so this call returns
            // immediately without blocking the handler thread.
            runBlocking { guardianAI.analyze(context, ThreatAssessment.neutral()) }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) Log.e(TAG, "Analysis failed: ${e.javaClass.simpleName}", e)
            null
        }

    /** Best-effort persistence — a database failure must never break protection. */
    private fun recordToHistory(context: VioraContext, assessment: ThreatAssessment) {
        runCatching {
            runBlocking { historyRepository.record(context, assessment) }
        }.onFailure {
            if (BuildConfig.DEBUG) Log.d(TAG, "Unable to record history: ${it.javaClass.simpleName}")
        }
    }

    private fun titleFor(level: RiskLevel) = when (level) {
        RiskLevel.VERIFY -> "Please Verify"
        RiskLevel.SUSPICIOUS -> "Potential Scam"
        RiskLevel.DANGEROUS -> "Dangerous Request"
        RiskLevel.SAFE -> "Viora"
    }

    private fun RiskLevel.toOverlayRisk() = when (this) {
        RiskLevel.SAFE -> VioraRiskLevel.SAFE
        RiskLevel.VERIFY -> VioraRiskLevel.VERIFY
        RiskLevel.SUSPICIOUS -> VioraRiskLevel.SUSPICIOUS
        RiskLevel.DANGEROUS -> VioraRiskLevel.DANGEROUS
    }

    private fun confidenceFor(assessment: com.viora.app.domain.threat.ThreatAssessment): String =
        (assessment.signals.size / SIGNALS_FOR_FULL_CONFIDENCE.toFloat()).coerceAtMost(1f)
            .let { "%.2f".format(java.util.Locale.US, it) }

    private fun collectVisibleText(root: AccessibilityNodeInfo): String {
        val values = LinkedHashSet<String>()
        val pending = ArrayDeque<AccessibilityNodeInfo>()
        pending.add(root)

        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            if (!node.isPassword) {
                addUsefulText(values, node.text?.toString())
                addUsefulText(values, node.contentDescription?.toString())
            }
            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(pending::addLast)
            }
        }

        return values.joinToString("\n")
    }

    private fun addUsefulText(values: LinkedHashSet<String>, value: String?) {
        val normalized = value?.replace(WHITESPACE_REGEX, " ")?.trim().orEmpty()
        if (normalized.isNotEmpty()) values.add(normalized)
    }

    companion object {
        private const val TAG = "VioraAccessibility"
        private const val ANALYSIS_DEBOUNCE_MS = 400L
        private const val SIGNALS_FOR_FULL_CONFIDENCE = 3
        private val WHITESPACE_REGEX = Regex("\\s+")
    }

    private val packageNameOfApplication: String
        get() = applicationContext.packageName
}