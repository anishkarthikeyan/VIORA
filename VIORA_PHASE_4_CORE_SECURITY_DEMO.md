# VIORA PHASE 4 — CORE SECURITY DEMO IMPLEMENTATION PLAN

## Objective

Make Viora's core security-analysis flow fully reliable and demo-ready.

The primary demonstration must prove that Viora does more than scan a QR code: it combines payment data and visible context, detects inconsistencies, produces an explainable risk assessment, and presents a clear warning/result.

## Current completed architecture

Phase 1–3 are complete.

Existing components:
- GuardianAI
- ThreatEngine
- AccessibilityThreatAnalyzer
- CompositeGuardianAI
- VioraContext
- ContextFusionEngine
- ThreatAssessment
- ThreatSignal
- QR scanning/parsing
- OCR
- Share input processing
- Room-backed security history
- ScannerViewModel
- Result UI

Do NOT redo those phases.

## Primary demo scenario

The strongest demo is amount manipulation:

- Payment/QR payload contains one amount, e.g. ₹50,000.
- Visible payment context contains a different amount, e.g. ₹500.
- OCR extracts the displayed amount.
- ContextFusionEngine preserves both values.
- ThreatEngine detects AMOUNT_MISMATCH.
- CompositeGuardianAI produces the final ThreatAssessment.
- Result UI clearly explains the mismatch.

The implementation must use the real pipeline, not fake the final result.

## Tasks

### 1. Inspect the real scanner flow

Trace:

QR/camera
→ QR parser
→ OCR
→ FusionInput
→ ContextFusionEngine
→ VioraContext
→ CompositeGuardianAI
→ ThreatAssessment
→ ScannerViewModel state
→ Result UI

Identify any broken or incomplete links.

### 2. Fix end-to-end integration

Make the smallest necessary fixes so real inputs reach the GuardianAI pipeline correctly.

Preserve existing architecture.

Do not duplicate threat logic in ScannerViewModel or UI.

### 3. Verify all primary inputs

Verify:
- QR input
- OCR input
- shared text
- shared image

Each should reach the appropriate existing analysis path.

### 4. Verify three demo scenarios

Scenario A — Amount mismatch:
- QR/payment amount differs from displayed amount.
- AMOUNT_MISMATCH must appear.

Scenario B — Merchant mismatch:
- visible merchant differs from payment/QR merchant.
- MERCHANT_MISMATCH must appear when the existing conservative matching logic identifies the mismatch.

Scenario C — Social engineering:
Example context:
"KYC has expired. Pay ₹999 immediately or your account will be blocked."
Existing accessibility analyzer should identify applicable signals such as account threat, urgency, and payment request.

Do not invent new signals.

### 5. Result correctness

Ensure Result UI receives:
- score
- risk level
- signals
- explanation
- recommended action

Do not change the scoring thresholds or existing signal scores.

### 6. Error handling

Check:
- empty/invalid QR
- OCR unavailable
- incomplete context
- analysis exceptions

The app should fail gracefully rather than crash.

Do not add unrelated error infrastructure.

### 7. Demo reliability

If practical, add deterministic test/demo inputs ONLY through a clearly isolated test/demo mechanism.

Demo data must still pass through:
VioraContext → CompositeGuardianAI → ThreatAssessment.

Do not hard-code "DANGEROUS" into UI.

## Explicitly do NOT

Do not:
- add real ML
- add cloud AI
- add networking
- redesign the entire UI
- change ThreatEngine rules
- change AccessibilityThreatAnalyzer rules
- change CompositeGuardianAI scoring
- change Room architecture
- add backend
- add UPI API interception
- claim actual transaction blocking
- perform unrelated refactoring

## Validation

Run:
./gradlew test
./gradlew assembleDebug

Inspect:
git status
git diff

Preserve unrelated pre-existing working-tree changes.

Do NOT commit or push.

## Final report

Report:
1. End-to-end flow verified.
2. Files changed.
3. Fixes made.
4. Three demo scenarios and their actual detected signals/risk.
5. Error cases checked.
6. Tests.
7. Build result.
8. Any limitations.

STOP after Phase 4.
