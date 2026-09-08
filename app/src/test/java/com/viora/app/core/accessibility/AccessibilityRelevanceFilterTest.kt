package com.viora.app.core.accessibility

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityRelevanceFilterTest {

    private val self = "com.viora.app"

    @Test
    fun `ignores Viora's own package`() {
        assertFalse(AccessibilityRelevanceFilter.isRelevant(self, self))
    }

    @Test
    fun `ignores system UI and launcher packages`() {
        assertFalse(AccessibilityRelevanceFilter.isRelevant("com.android.systemui", self))
        assertFalse(AccessibilityRelevanceFilter.isRelevant("com.android.launcher3", self))
        assertFalse(AccessibilityRelevanceFilter.isRelevant("com.google.android.apps.nexuslauncher", self))
    }

    @Test
    fun `allows known UPI and payment apps`() {
        assertTrue(AccessibilityRelevanceFilter.isRelevant("com.google.android.apps.nbu.paisa.user", self))
        assertTrue(AccessibilityRelevanceFilter.isRelevant("com.phonepe.app", self))
        assertTrue(AccessibilityRelevanceFilter.isRelevant("net.one97.paytm", self))
    }

    @Test
    fun `allows known messaging apps as social-engineering vectors`() {
        assertTrue(AccessibilityRelevanceFilter.isRelevant("com.whatsapp", self))
        assertTrue(AccessibilityRelevanceFilter.isRelevant("com.google.android.apps.messaging", self))
    }

    @Test
    fun `allows unlisted apps whose package name suggests banking`() {
        assertTrue(AccessibilityRelevanceFilter.isRelevant("com.csam.icici.bank.imobile", self))
        assertTrue(AccessibilityRelevanceFilter.isRelevant("com.sbi.upi", self))
        assertTrue(AccessibilityRelevanceFilter.isRelevant("com.axis.mobile.wallet", self))
    }

    @Test
    fun `ignores an unrelated app with no payment or messaging signal`() {
        assertFalse(AccessibilityRelevanceFilter.isRelevant("com.spotify.music", self))
        assertFalse(AccessibilityRelevanceFilter.isRelevant("com.example.puzzlegame", self))
    }
}
