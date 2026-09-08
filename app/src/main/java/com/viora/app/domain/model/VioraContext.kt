package com.viora.app.domain.model

data class VioraContext(
    val inputType: InputType,
    val rawContent: String,
    val upiId: String? = null,
    val merchantName: String? = null,
    val amount: String? = null,
    val currency: String? = null,
    val note: String? = null,
    val extractedText: String? = null,
    val detectedUrls: List<String> = emptyList(),
    val sourceApplication: String? = null,
    /** Merchant name as visibly rendered in the scene (OCR). Kept separate from the QR's pn. */
    val visibleMerchant: String? = null,
    /** Amount as visibly displayed in the scene (OCR). Kept separate from the QR's am. */
    val displayedAmount: String? = null,
    /** UPI ID (payment address) as visibly rendered in the scene (OCR). Kept separate from the QR's pa. */
    val visibleUpiId: String? = null
)
