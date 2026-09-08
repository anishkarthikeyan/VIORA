package com.viora.app.core.accessibility

/**
 * Decides whether a foreground app's screen is worth running Viora's analysis
 * pipeline on. Pure package-name logic (no Android framework dependency) so it is
 * directly unit-testable without Robolectric.
 *
 * Phase 5 goal: Viora must not indiscriminately analyze every application screen —
 * only contexts where payment fraud or social-engineering scams actually occur:
 * payment/banking apps, and common messaging/email apps (the usual delivery
 * channel for "KYC expired / pay now" style scams).
 *
 * Deliberately NOT an exhaustive bank/app database (unmaintainable) — a small,
 * curated allowlist of major India-relevant apps, plus a package-name keyword
 * heuristic ("bank", "pay", "upi", "wallet") that catches most banking apps
 * without hand-enumerating every one. This is an approximation, not a guarantee:
 * a bank app whose package name matches neither the allowlist nor a keyword will
 * be skipped. See VIORA_PHASE_5_PROACTIVE_PROTECTION.md limitations.
 */
object AccessibilityRelevanceFilter {

    /** Never relevant regardless of keywords: Viora's own UI, system UI, launchers. */
    private val ALWAYS_IGNORED_PREFIXES = listOf(
        "com.android.systemui",
        "com.android.launcher",
        "com.google.android.apps.nexuslauncher"
    )

    /** Curated, small allowlist of well-known payment/banking/messaging apps. */
    private val RELEVANT_PACKAGE_ALLOWLIST = setOf(
        // UPI / payment apps
        "com.google.android.apps.nbu.paisa.user", // Google Pay
        "com.phonepe.app",                        // PhonePe
        "net.one97.paytm",                        // Paytm
        "in.org.npci.upiapp",                     // BHIM
        "com.dreamplug.androidapp",                // CRED
        "com.freecharge.android",
        "com.mobikwik_new",
        "com.amazon.mShop.android.shopping",       // Amazon (Amazon Pay)
        // Messaging / email — common social-engineering delivery channels
        "com.whatsapp",
        "com.google.android.apps.messaging",       // Android Messages (SMS/RCS)
        "com.google.android.gm",                   // Gmail
        "com.facebook.orca",                       // Messenger
        "org.telegram.messenger",
        "com.samsung.android.messaging"
    )

    /** Package-name substrings suggesting a banking/payment/wallet app. */
    private val RELEVANT_PACKAGE_KEYWORDS = listOf("bank", "upi", "pay", "wallet", "netbanking")

    /**
     * True when [packageName] is worth analyzing: not Viora itself/system UI/launcher,
     * and either on the curated allowlist or matches a banking/payment keyword.
     */
    fun isRelevant(packageName: String, selfPackageName: String): Boolean {
        if (packageName == selfPackageName) return false
        if (ALWAYS_IGNORED_PREFIXES.any { packageName.startsWith(it) }) return false

        if (packageName in RELEVANT_PACKAGE_ALLOWLIST) return true
        val lower = packageName.lowercase()
        return RELEVANT_PACKAGE_KEYWORDS.any { lower.contains(it) }
    }
}
