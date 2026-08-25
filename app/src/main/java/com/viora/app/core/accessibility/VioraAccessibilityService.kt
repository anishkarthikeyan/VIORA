package com.viora.app.core.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.viora.app.BuildConfig
import com.viora.app.ai.AccessibilityThreatAnalyzer
import com.viora.app.core.overlay.VioraOverlayService
import com.viora.app.core.overlay.VioraRiskLevel
import com.viora.app.core.overlay.VioraWarning
import com.viora.app.domain.perception.AccessibilitySnapshot
import com.viora.app.domain.threat.RiskLevel

/** Collects structured text from the active accessibility tree for the perception layer. */
class VioraAccessibilityService : AccessibilityService() {

    private val analyzer = AccessibilityThreatAnalyzer()
    private val handler = Handler(Looper.getMainLooper())
    private var lastSnapshotPackage: String? = null
    private var lastSnapshotHash: Int? = null
    private var pendingSnapshot: AccessibilitySnapshot? = null
    private var lastWarningKey: String? = null
    private val analyzePendingSnapshot = Runnable { analyzePendingSnapshot() }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val root = rootInActiveWindow ?: return
        val packageName = root.packageName?.toString()
            ?: event?.packageName?.toString()
            ?: return
        if (shouldIgnorePackage(packageName)) return

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
        val assessment = analyzer.analyze(snapshot)
        val confidence = confidenceFor(assessment)
        if (BuildConfig.DEBUG) {
            Log.d(
                TAG,
                "VIORA_ANALYSIS package=${snapshot.packageName} " +
                    "signals=${assessment.signals.map { it.id }} " +
                    "risk=${assessment.riskLevel} confidence=$confidence"
            )
        }

        if (assessment.riskLevel == RiskLevel.SAFE) {
            lastWarningKey = null
            return
        }
        val warningKey = "${snapshot.packageName}|${assessment.riskLevel}|" +
            assessment.signals.joinToString(",") { it.id }
        if (warningKey == lastWarningKey) return

        lastWarningKey = warningKey
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

    private fun shouldIgnorePackage(packageName: String): Boolean =
        packageName == packageNameOfApplication ||
            packageName == ANDROID_SYSTEM_UI ||
            packageName.startsWith(ANDROID_LAUNCHER_PREFIX) ||
            packageName.startsWith(GOOGLE_LAUNCHER_PREFIX)

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
        private const val ANDROID_SYSTEM_UI = "com.android.systemui"
        private const val ANDROID_LAUNCHER_PREFIX = "com.android.launcher"
        private const val GOOGLE_LAUNCHER_PREFIX = "com.google.android.apps.nexuslauncher"
        private val WHITESPACE_REGEX = Regex("\\s+")
    }

    private val packageNameOfApplication: String
        get() = applicationContext.packageName
}