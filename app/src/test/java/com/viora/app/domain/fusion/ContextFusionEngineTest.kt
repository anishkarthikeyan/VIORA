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
}
