package com.viora.app.domain.fusion

import com.viora.app.domain.model.OcrResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContextFusionEngineTest {

    private val engine = ContextFusionEngine()

    private fun ocr(text: String) = OcrResult(
        rawText = text,
        lineCount = text.lines().size,
        detectedAtMs = 0L
    )

    // ---------- Single-source fusion ----------

    @Test
    fun `qr only input preserves parsed fields`() {
        val qr = FusionInput.Parsed(
            com.viora.app.domain.model.VioraContext(
                inputType = com.viora.app.domain.model.InputType.QR,
                rawContent = "upi://pay?pa=random@upi&am=850",
                upiId = "random@upi",
                amount = "850"
            )
        )

        val fused = engine.fuse(listOf(qr))

        assertEquals("random@upi", fused.upiId)
        assertEquals("850", fused.amount)
        assertNull(fused.visibleMerchant)
        assertNull(fused.displayedAmount)
        assertNull(fused.extractedText)
    }

    @Test
    fun `ocr only input extracts visible merchant and displayed amount`() {
        val fused = engine.fuse(
            listOf(
                FusionInput.SceneText(ocr("ABC RESTAURANT\n₹850\nScan to Pay"))
            )
        )

        assertEquals("ABC RESTAURANT", fused.visibleMerchant)
        assertEquals("850", fused.displayedAmount)
        assertNull(fused.upiId)
        assertNull(fused.amount)
    }

    // ---------- Combined sources (spec example) ----------

    @Test
    fun `qr plus ocr preserves both sources without overwriting`() {
        val qr = FusionInput.Parsed(
            com.viora.app.domain.model.VioraContext(
                inputType = com.viora.app.domain.model.InputType.QR,
                rawContent = "upi://pay?pa=random@upi&am=850",
                upiId = "random@upi",
                amount = "850"
            )
        )
        val scene = FusionInput.SceneText(ocr("ABC RESTAURANT\n₹850"))

        val fused = engine.fuse(listOf(qr, scene))

        // From the scene (OCR)
        assertEquals("ABC RESTAURANT", fused.visibleMerchant)
        assertEquals("850", fused.displayedAmount)
        // From the payload (QR)
        assertEquals("random@upi", fused.upiId)
        assertEquals("850", fused.amount)
    }

    @Test
    fun `discrepant amounts are kept separately not merged`() {
        val qr = FusionInput.Parsed(
            com.viora.app.domain.model.VioraContext(
                inputType = com.viora.app.domain.model.InputType.QR,
                rawContent = "upi://pay?pa=x@upi&am=500",
                upiId = "x@upi",
                amount = "500"
            )
        )
        val scene = FusionInput.SceneText(ocr("SUPER MART\nPay ₹999"))

        val fused = engine.fuse(listOf(qr, scene))

        assertEquals("500", fused.amount)          // what the payload asks for
        assertEquals("999", fused.displayedAmount) // what the screen shows
    }

    @Test
    fun `ocr does not overwrite qr merchant name`() {
        val qr = FusionInput.Parsed(
            com.viora.app.domain.model.VioraContext(
                inputType = com.viora.app.domain.model.InputType.QR,
                rawContent = "upi://pay?pa=a@b&pn=PayloadName",
                upiId = "a@b",
                merchantName = "PayloadName"
            )
        )
        val scene = FusionInput.SceneText(ocr("Some Shop\n₹100"))

        val fused = engine.fuse(listOf(qr, scene))

        assertEquals("PayloadName", fused.merchantName)
        assertEquals("Some Shop", fused.visibleMerchant)
    }

    // ---------- Robustness ----------

    @Test
    fun `empty ocr contribution is ignored`() {
        val qr = FusionInput.Parsed(
            com.viora.app.domain.model.VioraContext(
                inputType = com.viora.app.domain.model.InputType.QR,
                rawContent = "upi://pay?pa=a@b",
                upiId = "a@b"
            )
        )

        val fused = engine.fuse(listOf(qr, FusionInput.SceneText(ocr("   "))))

        assertEquals("a@b", fused.upiId)
        assertNull(fused.visibleMerchant)
        assertNull(fused.extractedText)
    }

    @Test
    fun `fusion of no inputs yields empty camera context`() {
        val fused = engine.fuse(emptyList())

        assertNull(fused.upiId)
        assertNull(fused.amount)
        assertNull(fused.visibleMerchant)
    }

    @Test
    fun `amount with comma separators is normalized`() {
        val fused = engine.fuse(listOf(FusionInput.SceneText(ocr("BIG STORES\nRs. 12,500"))))

        assertEquals("12500", fused.displayedAmount)
        assertEquals("BIG STORES", fused.visibleMerchant)
    }

    // ---------- Visible UPI ID (Screenshot QR cross-check fix) ----------

    @Test
    fun `ocr extracts a visible UPI ID separately from the QR payload's pa`() {
        val qr = FusionInput.Parsed(
            com.viora.app.domain.model.VioraContext(
                inputType = com.viora.app.domain.model.InputType.QR,
                rawContent = "upi://pay?pa=nied.foundation@ybl&pn=Dr%20DHANRAJ",
                upiId = "nied.foundation@ybl",
                merchantName = "Dr DHANRAJ"
            )
        )
        val scene = FusionInput.SceneText(
            ocr("ALL UPI ACCEPTED\nNEXTGEN INDIA\nEDUCATIONAL DEVELOPMENT COUNCIL\nnied.foundation@ybl")
        )

        val fused = engine.fuse(listOf(qr, scene))

        assertEquals("nied.foundation@ybl", fused.upiId) // from QR
        assertEquals("nied.foundation@ybl", fused.visibleUpiId) // from OCR — same value, independent field
        assertEquals("Dr DHANRAJ", fused.merchantName)
        assertEquals("NEXTGEN INDIA", fused.visibleMerchant)
    }

    @Test
    fun `discrepant visible UPI ID is kept separately not merged into QR pa`() {
        val qr = FusionInput.Parsed(
            com.viora.app.domain.model.VioraContext(
                inputType = com.viora.app.domain.model.InputType.QR,
                rawContent = "upi://pay?pa=attacker@upi",
                upiId = "attacker@upi"
            )
        )
        val scene = FusionInput.SceneText(ocr("Trusted Shop\nreal.shop@ybl"))

        val fused = engine.fuse(listOf(qr, scene))

        assertEquals("attacker@upi", fused.upiId)          // what the payload actually pays
        assertEquals("real.shop@ybl", fused.visibleUpiId)  // what the screen shows
    }

    @Test
    fun `no UPI-ID-shaped token in the scene leaves visibleUpiId null`() {
        val fused = engine.fuse(listOf(FusionInput.SceneText(ocr("ABC RESTAURANT\n₹850"))))

        assertNull(fused.visibleUpiId)
    }
}
