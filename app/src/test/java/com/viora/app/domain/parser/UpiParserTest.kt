package com.viora.app.domain.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpiParserTest {

    private val parser = UpiParser()

    // ---------- Valid UPI QR ----------

    @Test
    fun `full UPI URI extracts all fields`() {
        val result = parser.parse(
            "upi://pay?pa=merchant@upi&pn=Merchant&am=500&cu=INR&tn=Payment"
        )

        assertTrue(result is UpiParseResult.UpiPayment)
        val ctx = (result as UpiParseResult.UpiPayment).context
        assertEquals("merchant@upi", ctx.upiId)
        assertEquals("Merchant", ctx.merchantName)
        assertEquals("500", ctx.amount)
        assertEquals("INR", ctx.currency)
        assertEquals("Payment", ctx.note)
        assertEquals(
            "upi://pay?pa=merchant@upi&pn=Merchant&am=500&cu=INR&tn=Payment",
            ctx.rawContent
        )
    }

    @Test
    fun `parameter order does not matter`() {
        val result = parser.parse(
            "upi://pay?am=99.50&cu=INR&pa=user@oksbi&tn=Order+42"
        ) as UpiParseResult.UpiPayment

        assertEquals("user@oksbi", result.context.upiId)
        assertEquals("99.50", result.context.amount)
        assertEquals("Order 42", result.context.note)
    }

    // ---------- Missing optional fields ----------

    @Test
    fun `missing amount yields null amount`() {
        val ctx = (
            parser.parse("upi://pay?pa=shop@ybl&pn=Shop&cu=INR")
                as UpiParseResult.UpiPayment
            ).context

        assertNull(ctx.amount)
        assertNotNull(ctx.upiId)
    }

    @Test
    fun `missing merchant name yields null merchantName`() {
        val ctx = (
            parser.parse("upi://pay?pa=person@upi&am=100&cu=INR")
                as UpiParseResult.UpiPayment
            ).context

        assertNull(ctx.merchantName)
    }

    @Test
    fun `missing currency yields null currency`() {
        val ctx = (
            parser.parse("upi://pay?pa=person@upi&am=100")
                as UpiParseResult.UpiPayment
            ).context

        assertNull(ctx.currency)
    }

    @Test
    fun `bare upi uri with no query still parses as UPI payment`() {
        val result = parser.parse("upi://pay")

        assertTrue(result is UpiParseResult.UpiPayment)
        val ctx = (result as UpiParseResult.UpiPayment).context
        assertNull(ctx.upiId)
        assertNull(ctx.amount)
    }

    // ---------- Malformed URI / encoding ----------

    @Test
    fun `malformed percent encoding returns MalformedUpi`() {
        val result = parser.parse("upi://pay?pa=bad%2%@upi")

        assertTrue(result is UpiParseResult.MalformedUpi)
        assertEquals("upi://pay?pa=bad%2%@upi", (result as UpiParseResult.MalformedUpi).rawContent)
    }

    @Test
    fun `pair without value is kept as empty string not crash`() {
        val result = parser.parse("upi://pay?pa=a@b&flag&am=") as UpiParseResult.UpiPayment

        assertEquals("", result.context.amount)
        assertEquals("a@b", result.context.upiId)
    }

    // ---------- URL encoding ----------

    @Test
    fun `percent-encoded merchant name and note are decoded`() {
        val result = parser.parse(
            "upi://pay?pa=m%40upi&pn=Sharma%20Store&tn=Coffee%202%20go"
        ) as UpiParseResult.UpiPayment

        assertEquals("m@upi", result.context.upiId)
        assertEquals("Sharma Store", result.context.merchantName)
        assertEquals("Coffee 2 go", result.context.note)
    }

    @Test
    fun `plus sign decodes to space in merchant name`() {
        val result = parser.parse(
            "upi://pay?pa=x@upi&pn=Chai+Point"
        ) as UpiParseResult.UpiPayment

        assertEquals("Chai Point", result.context.merchantName)
    }

    // ---------- Non-UPI content ----------

    @Test
    fun `ordinary https url is classified as Url with detectedUrls populated`() {
        val raw = "https://example.com/pay?token=abc123"
        val result = parser.parse(raw) as UpiParseResult.Url

        assertEquals(listOf(raw), result.context.detectedUrls)
        assertNull(result.context.upiId)
    }

    @Test
    fun `www shorthand is classified as Url`() {
        val result = parser.parse("www.example.com/offers") as UpiParseResult.Url

        assertEquals("www.example.com/offers", result.context.detectedUrls.first())
    }

    @Test
    fun `plain text qr is classified as Text with no extracted fields`() {
        val result = parser.parse("HELLO THIS IS JUST A SIGN 42")

        assertTrue(result is UpiParseResult.Text)
        val ctx = (result as UpiParseResult.Text).context
        assertNull(ctx.upiId)
        assertTrue(ctx.detectedUrls.isEmpty())
        assertEquals("HELLO THIS IS JUST A SIGN 42", ctx.rawContent)
    }

    @Test
    fun `empty content is classified as Text and does not crash`() {
        val result = parser.parse("   ")

        assertTrue(result is UpiParseResult.Text)
    }

    // ---------- Safety neutrality guard ----------

    @Test
    fun `parser never assigns risk information`() {
        val result = parser.parse("upi://pay?pa=scammy@upi&am=999999") as UpiParseResult.UpiPayment

        // Extraction only: the context carries facts, no verdicts.
        assertEquals("scammy@upi", result.context.upiId)
        assertEquals("999999", result.context.amount)
    }
}
