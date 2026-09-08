# Viora — Live Demo Script (Phase 4)

Three reproducible scenarios that exercise the REAL pipeline end-to-end
(`VioraContext → CompositeGuardianAI(ThreatEngine, AccessibilityThreatAnalyzer) → ThreatAssessment`).
Nothing here is hard-coded into the app — every result below is also asserted by
`app/src/test/java/com/viora/app/ai/DemoScenarioIntegrationTest.kt`, which runs the
exact same wiring `ScannerViewModel`/`VioraAccessibilityService` use by default.

## Scenario A — Amount manipulation (camera: QR + OCR)

Print/display a UPI QR code encoding:

```
upi://pay?pa=merchant@upi&pn=Corner Store&am=50000&cu=INR
```

Next to it (visible in the same camera frame), display the text `Corner Store — Pay ₹500`.

1. Open Viora → **Scan Payment**.
2. Point the camera at the QR code + the price text together.
3. Viora scans the QR (amount ₹50,000) and OCRs the visible price (₹500).
4. Expected result: **AMOUNT_MISMATCH** signal, risk level VERIFY or higher, explanation
   quoting both amounts.

## Scenario B — Merchant mismatch (camera: QR + OCR)

Print/display a UPI QR code encoding:

```
upi://pay?pa=randomhandle123@upi&pn=Zenith Traders&am=1200&cu=INR
```

Next to it, display the text `Sunrise Bakery — Pay ₹1200` (same amount, different name).

1. Scan both together as above.
2. Expected result: **MERCHANT_MISMATCH** signal ("Zenith Traders" vs "Sunrise Bakery"),
   risk level VERIFY.

## Scenario C — Social engineering (Share Sheet: text)

From any app (Notes, Messages, browser), select and share this text to Viora:

```
KYC has expired. Pay Rs 999 immediately or your account will be blocked.
```

1. Share → **Viora**.
2. Viora is brought to the front and navigates straight to the Result screen.
3. Expected result: **ACCOUNT_THREAT**, **URGENT_ACTION**, **PAYMENT_REQUEST** signals,
   risk level **DANGEROUS**.

## Why scenarios A/B require the camera (not Share Sheet)

`AMOUNT_MISMATCH` and `MERCHANT_MISMATCH` compare a QR *payload* value against a
*visibly displayed* value — two independent sources that only the live camera path
(QR detector + OCR, fused by `ContextFusionEngine`) supplies simultaneously. A shared
screenshot only produces OCR (scene) data, with no separate payload to compare against,
so those two signals cannot fire from a share alone. This mirrors Phase 4's own primary
demo scenario description.

## Fallback if a physical QR print isn't available

Any QR-generator app/website can render the `upi://pay?...` strings above; scanning the
generator's screen with a second device works identically to scanning a printed code, as
long as the comparison text is visible in the same frame.
