package com.viora.app.ai

import com.viora.app.domain.model.InputType
import com.viora.app.domain.model.VioraContext
import com.viora.app.domain.threat.RiskLevel
import com.viora.app.domain.threat.ThreatAssessment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreatEngineTest {

    private val engine = ThreatEngine()

    private fun analyze(context: VioraContext): ThreatAssessment = runBlocking {
        engine.analyze(context, ThreatAssessment.neutral())
    }

    private fun context(
        upiId: String? = null,
        merchantName: String? = null,
        amount: String? = null,
        visibleMerchant: String? = null,
        displayedAmount: String? = null
    ) = VioraContext(
        inputType = InputType.CAMERA,
        rawContent = "fused",
        upiId = upiId,
        merchantName = merchantName,
        amount = amount,
        visibleMerchant = visibleMerchant,
        displayedAmount = displayedAmount
    )

    // ---------- Clean cases ----------

    @Test
    fun `matching amounts and consistent merchant is SAFE`() {
        val result = analyze(
            context(
                upiId = "abcrestaurant@upi",
                merchantName = "ABC Restaurant",
                amount = "850",
                visibleMerchant = "ABC RESTAURANT",
                displayedAmount = "₹850"
            )
        )

        assertEquals(RiskLevel.SAFE, result.riskLevel)
        assertTrue(result.signals.isEmpty())
        assertEquals(5, result.score)
    }

    @Test
    fun `no cross-source data at all is SAFE not UNKNOWN_RECIPIENT`() {
        val result = analyze(VioraContext(inputType = InputType.TEXT, rawContent = "hello"))

        assertEquals(RiskLevel.SAFE, result.riskLevel)
    }

    // ---------- AMOUNT_MISMATCH ----------

    @Test
    fun `screen amount different from payload raises AMOUNT_MISMATCH`() {
        val result = analyze(
            context(
                upiId = "store@upi",
                merchantName = "Store",
                amount = "5000",
                displayedAmount = "850"
            )
        )

        val signal = result.signals.single { it.id == "AMOUNT_MISMATCH" }
        assertEquals(35, signal.score)
        assertEquals(35, result.score) // merchant known -> no UNKNOWN_RECIPIENT stacking
        assertEquals(RiskLevel.VERIFY, result.riskLevel) // conservative: 35 -> VERIFY
        assertTrue(result.explanation.contains("5000"))
    }

    @Test
    fun `formatted equal amounts do not trigger mismatch`() {
        val result = analyze(
            context(
                upiId = "x@upi",
                amount = "12500",
                displayedAmount = "Rs. 12,500.00"
            )
        )

        assertTrue(result.signals.none { it.id == "AMOUNT_MISMATCH" })
    }

    @Test
    fun `unparseable displayed amount is ignored conservatively`() {
        val result = analyze(
            context(upiId = "x@upi", amount = "500", displayedAmount = "special offer")
        )

        assertTrue(result.signals.none { it.id == "AMOUNT_MISMATCH" })
    }

    // ---------- MERCHANT_MISMATCH ----------

    @Test
    fun `spec example - visible restaurant vs random handle name flags mismatch only`() {
        val result = analyze(
            context(
                upiId = "random@upi",
                merchantName = null,
                amount = "850",
                visibleMerchant = "ABC RESTAURANT"
            )
        )

        // pn absent in payload -> no MERCHANT_MISMATCH (nothing to compare against),
        // but the recipient cannot be verified:
        assertTrue(result.signals.none { it.id == "MERCHANT_MISMATCH" })
        assertTrue(result.signals.any { it.id == "UNKNOWN_RECIPIENT" })
        assertEquals(20, result.score)
        assertEquals(RiskLevel.VERIFY, result.riskLevel)
        assertFalse(result.explanation.contains("scam", ignoreCase = true))
    }

    @Test
    fun `completely different names raise MERCHANT_MISMATCH`() {
        val result = analyze(
            context(
                upiId = "xyztraders@upi",
                merchantName = "XYZ Traders",
                visibleMerchant = "ABC RESTAURANT"
            )
        )

        assertEquals(25, result.score)
        assertTrue(result.signals.any { it.id == "MERCHANT_MISMATCH" })
        assertEquals(RiskLevel.VERIFY, result.riskLevel)
    }

    @Test
    fun `partial word overlap counts as related not mismatched`() {
        val result = analyze(
            context(
                upiId = "abconline@upi",
                merchantName = "ABC Online",
                visibleMerchant = "ABC RESTAURANT"
            )
        )

        assertTrue(result.signals.none { it.id == "MERCHANT_MISMATCH" })
    }

    // ---------- UNKNOWN_RECIPIENT ----------

    @Test
    fun `payment without any merchant info yields low-score verification signal`() {
        val result = analyze(context(upiId = "friend123@okaxis", amount = "200"))

        val signal = result.signals.single { it.id == "UNKNOWN_RECIPIENT" }
        assertEquals(20, signal.score)
        assertEquals(RiskLevel.VERIFY, result.riskLevel)
        assertFalse(signal.description.contains("fraud detected"))
    }

    @Test
    fun `unknown numeric handle alone is not claimed as scam`() {
        val result = analyze(context(upiId = "9876543210@upi"))

        val signal = result.signals.single()
        assertEquals("UNKNOWN_RECIPIENT", signal.id)
        assertEquals(20, signal.score)
        assertTrue(signal.description.contains("does not indicate fraud"))
    }

    @Test
    fun `named merchant prevents UNKNOWN_RECIPIENT`() {
        val result = analyze(
            context(upiId = "random@upi", merchantName = "Random Store")
        )

        assertTrue(result.signals.isEmpty())
    }

    // ---------- Stacked / explainability ----------

    @Test
    fun `amount and merchant mismatch together reach SUSPICIOUS`() {
        val result = analyze(
            context(
                upiId = "xyztraders@upi",
                merchantName = "XYZ Traders",
                amount = "5000",
                visibleMerchant = "ABC RESTAURANT",
                displayedAmount = "850"
            )
        )

        assertEquals(60, result.score) // 35 + 25
        assertEquals(RiskLevel.SUSPICIOUS, result.riskLevel)
        assertEquals(2, result.signals.size)
    }

    @Test
    fun `all three signals stack to DANGEROUS`() {
        val result = analyze(
            context(
                upiId = "9876543210@upi",
                amount = "5000",
                visibleMerchant = "ABC RESTAURANT",
                displayedAmount = "850"
            )
        )

        // AMOUNT_MISMATCH 35 (pn missing so no MERCHANT_MISMATCH) +
        // UNKNOWN_RECIPIENT 20 = 55 -> SUSPICIOUS; DANGEROUS needs >= 75.
        assertEquals(55, result.score)
        assertEquals(RiskLevel.SUSPICIOUS, result.riskLevel)
        assertEquals(setOf("AMOUNT_MISMATCH", "UNKNOWN_RECIPIENT"), result.signals.map { it.id }.toSet())
    }

    @Test
    fun `assessment always carries explanation and recommended action`() {
        val result = analyze(context(upiId = "a@b", amount = "10"))

        assertTrue(result.explanation.isNotBlank())
        assertTrue(result.recommendedAction.isNotBlank())
        assertTrue(result.score in 0..100)
    }
}
