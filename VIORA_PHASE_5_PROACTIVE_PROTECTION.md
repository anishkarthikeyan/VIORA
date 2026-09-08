# VIORA PHASE 5 — PROACTIVE PROTECTION IMPLEMENTATION PLAN

## Objective

Make Viora's accessibility-based protection a credible proactive security layer.

The product should not require the user to manually open Viora for every payment.

Target concept:

Relevant payment/social screen
→ AccessibilityService
→ AccessibilitySnapshot
→ Viora analysis
→ CompositeGuardianAI
→ meaningful risk
→ warning overlay
→ user decides STOP or CONTINUE

## Existing architecture

Already available:
- VioraAccessibilityService
- AccessibilitySnapshot flow
- AccessibilityThreatAnalyzer
- CompositeGuardianAI
- GuardianAI
- ThreatAssessment
- overlay infrastructure

Do not rebuild these components.

## Tasks

### 1. Inspect accessibility implementation

Trace:
Android accessibility event
→ package/event filtering
→ visible text extraction
→ AccessibilitySnapshot
→ VioraContext
→ CompositeGuardianAI
→ ThreatAssessment
→ overlay

Understand current behavior before editing.

### 2. Relevant-context filtering

Ensure Viora does not indiscriminately analyze every application screen.

Use the existing implementation and Android package/event information to restrict processing to relevant contexts where appropriate.

Prioritize:
- payment apps
- banking apps
- relevant messaging/social-engineering contexts if already supported

Do not invent an enormous package database.

Use a maintainable approach.

### 3. Event handling and debouncing

Prevent repeated analysis of identical/rapid accessibility events where the existing architecture permits.

Preserve existing debouncing behavior if it is already correct.

Do not introduce complex background infrastructure.

### 4. Privacy/data minimization

Viora must not persist:
- full accessibility dumps
- OTPs
- PINs
- passwords
- credentials
- unnecessary screen content

Only use visible context required for analysis.

Do not expand data collection.

### 5. Guardian analysis

The service must use the existing CompositeGuardianAI.

Do not call AccessibilityThreatAnalyzer directly if the application architecture already migrated to CompositeGuardianAI.

Do not duplicate scoring.

### 6. Warning threshold

The warning overlay should appear only when the resulting assessment warrants meaningful intervention.

Use the existing RiskLevel/scoring semantics.

Do not arbitrarily invent new thresholds without strong justification.

The goal is to avoid:
- warning on every screen
- warning on harmless text
- alert fatigue

### 7. Warning content

Ensure the existing overlay can communicate:
- risk level
- important detected signals
- concise explanation
- recommended action

Use real ThreatAssessment data.

Do not hard-code a fake warning.

The user should be able to make the final decision.

### 8. STOP / CONTINUE semantics

Do not claim Viora intercepts NPCI/UPI network traffic or directly controls another app's payment API.

STOP should mean Viora warns the user and provides the protection decision supported by the existing Android architecture.

CONTINUE should return control appropriately.

Do not attempt unsupported transaction interception.

### 9. Accessibility enable/disable

Preserve explicit user control over enabling the accessibility service.

Do not silently enable it.

Check the existing permission/settings flow for obvious usability issues and make only necessary fixes.

### 10. Testing

Add focused tests where practical for:
- relevant package filtering
- event filtering
- debouncing
- threat triggering
- non-triggering low-risk cases

Do not add Robolectric or large test infrastructure unless absolutely necessary.

## Explicitly do NOT

Do not:
- modify ThreatEngine rules
- modify AccessibilityThreatAnalyzer detection rules
- modify CompositeGuardianAI scoring
- add cloud services
- add network interception
- claim actual UPI transaction blocking
- add ML
- redesign the whole UI
- add unrelated permissions
- collect/persist sensitive credentials
- refactor unrelated modules

## Validation

Run:
./gradlew test
./gradlew assembleDebug

Inspect:
git diff
git status

Do NOT commit or push.

## Final report

Report:
1. Accessibility flow.
2. Relevant package/context filtering.
3. Event/debounce behavior.
4. Privacy handling.
5. Warning trigger behavior.
6. STOP/CONTINUE behavior.
7. Files changed.
8. Tests.
9. Build result.
10. Limitations.

STOP after Phase 5.
