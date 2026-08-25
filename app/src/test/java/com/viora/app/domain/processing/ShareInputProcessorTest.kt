package com.viora.app.domain.processing

import com.viora.app.domain.model.InputType
import com.viora.app.domain.parser.UpiParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareInputProcessorTest {

    private val processor = ShareInputProcessor(UpiParser())

    // ---------- Graceful handling of empty / malformed content ----------

    @Test
    fun `null text yields Empty`() {
        assertTrue(processor.process(null) is ShareProcessResult.Empty)
    }

    @Test
    fun `blank text yields Empty`() {
        assertTrue(processor.process("   \n\t ") is ShareProcessResult.Empty)
    }

    // ---------- Plain text ----------

    @Test
    fun `plain text without urls becomes TEXT input`() {
        val result = processor.process("Check out this amazing deal") as ShareProcessResult.Valid
        assertEquals(InputType.TEXT, result.context.inputType)
        assertEquals("Check out this amazing deal", result.context.rawContent)
        assertTrue(result.context.detectedUrls.isEmpty())
    }

    @Test
    fun `raw text is preserved in extractedText`() {
        val result = processor.process("hello world") as ShareProcessResult.Valid
        assertEquals("hello world", result.context.extractedText)
    }

    // ---------- URL detection ----------

    @Test
    fun `https url is detected and typed URL`() {
        val result = processor.process("https://example.com/pay?id=1") as ShareProcessResult.Valid
        assertEquals(InputType.URL, result.context.inputType)
        assertEquals(listOf("https://example.com/pay?id=1"), result.context.detectedUrls)
    }

    @Test
    fun `bare www url without scheme is detected`() {
        val result = processor.process("visit www.example.com now") as ShareProcessResult.Valid
        assertEquals(listOf("www.example.com"), result.context.detectedUrls)
        assertEquals(InputType.URL, result.context.inputType)
    }

    @Test
    fun `multiple distinct urls are all collected`() {
        val result = processor.process(
            "compare https://a.com and https://b.com"
        ) as ShareProcessResult.Valid
        assertEquals(listOf("https://a.com", "https://b.com"), result.context.detectedUrls)
    }

    @Test
    fun `duplicate urls are deduplicated`() {
        val result = processor.process(
            "https://a.com again https://a.com"
        ) as ShareProcessResult.Valid
        assertEquals(listOf("https://a.com"), result.context.detectedUrls)
    }

    @Test
    fun `trailing punctuation is not part of the url`() {
        val result = processor.process("look at https://example.com/x.") as ShareProcessResult.Valid
        assertEquals(listOf("https://example.com/x"), result.context.detectedUrls)
    }

    @Test
    fun `url inside a sentence is found`() {
        val result = processor.process(
            "Winner! claim at http://prize.tk/now or lose it"
        ) as ShareProcessResult.Valid
        assertEquals(listOf("http://prize.tk/now"), result.context.detectedUrls)
    }

    // ---------- UPI deep link shared as text ----------

    @Test
    fun `upi uri fills payment fields`() {
        val result = processor.process(
            "upi://pay?pa=merchant@upi&pn=Merchant&am=500&cu=INR&tn=Payment"
        ) as ShareProcessResult.Valid
        val ctx = result.context
        assertEquals("merchant@upi", ctx.upiId)
        assertEquals("Merchant", ctx.merchantName)
        assertEquals("500", ctx.amount)
        assertEquals("INR", ctx.currency)
        assertEquals("Payment", ctx.note)
    }

    @Test
    fun `malformed upi uri yields Empty`() {
        // Percent-encoding that cannot be decoded → unusable content.
        val result = processor.process("upi://pay?pa=%ZZ@upi")
        assertTrue(result is ShareProcessResult.Empty)
    }

    // ---------- Source application ----------

    @Test
    fun `source application is attached when provided`() {
        val result = processor.process("https://x.com", "com.whatsapp")
            as ShareProcessResult.Valid
        assertEquals("com.whatsapp", result.context.sourceApplication)
    }
}
