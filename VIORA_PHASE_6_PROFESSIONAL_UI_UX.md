# VIORA PHASE 6 — PROFESSIONAL UI/UX IMPLEMENTATION PLAN

## Objective

Transform Viora's existing Compose UI from functional/acceptable into a polished, modern, professional security product suitable for a submission video and portfolio.

This phase has broad permission to modify presentation/UI code.

The goal is NOT a generic "AI dashboard."

Viora should visually communicate:
- security
- trust
- intelligence
- clarity
- calmness
- premium product quality

## Critical rule

You MAY substantially modify existing Compose presentation code when needed.

You MUST NOT modify:
- threat detection logic
- scoring
- GuardianAI
- ThreatEngine
- AccessibilityThreatAnalyzer
- CompositeGuardianAI
- ContextFusionEngine
- Room/data architecture

UI should consume existing real data.

Do not invent security results or statistics.

## 1. Audit every existing screen

Inspect:
- Home
- Scanner
- Result
- History
- reusable components
- navigation
- warning/overlay presentation where applicable
- loading/error/empty states

Identify:
- alignment problems
- inconsistent spacing
- inconsistent typography
- inconsistent colors
- oversized/undersized components
- weak hierarchy
- unnecessary visual noise
- inconsistent cards/buttons
- poor empty states
- awkward navigation
- poor risk-state communication

Do not assume existing UI is good enough.

## 2. Establish a coherent design system

Create/reuse centralized UI definitions where appropriate.

Define consistent:
- primary colors
- background
- surfaces
- primary/secondary text
- dividers
- safe state
- verify state
- suspicious state
- dangerous state
- typography hierarchy
- spacing scale
- corner radii
- component dimensions
- button hierarchy
- icon treatment

Avoid scattered magic colors and inconsistent dimensions.

## 3. Color philosophy

Security states must be immediately understandable.

Use:
- SAFE — reassuring
- VERIFY — caution
- SUSPICIOUS — warning
- DANGEROUS — strong danger

Do not make the whole application bright red/yellow.

Risk colors should be reserved primarily for risk communication.

Maintain strong contrast and readability.

## 4. Typography

Establish clear hierarchy for:
- screen titles
- major score
- risk level
- section headings
- body explanations
- metadata
- buttons
- timestamps

Do not use too many font sizes or weights.

Prioritize readability.

## 5. Spacing and alignment

Audit every screen for:
- consistent margins
- vertical rhythm
- horizontal alignment
- card padding
- button spacing
- icon/text alignment
- baseline alignment

The UI should feel deliberately designed rather than assembled.

## 6. Home screen

Make the Home screen immediately communicate:

VIORA
Payment Security Intelligence

Protection status

Useful real security/history information

Primary action:
Scan Payment

Secondary access:
History

Do not show fake counts.

If data is unavailable, use a useful empty state.

Avoid excessive cards.

## 7. Scanner screen

Make scanning feel like a security action, not a generic camera screen.

Clearly communicate:
- what Viora is scanning
- what the user should do
- scanning state
- processing state
- errors

Keep the camera experience clean.

Do not interfere with actual scanner functionality.

## 8. Result screen — highest UI priority

The result screen must be the strongest visual screen because it is the main demo payoff.

It should clearly show:

RISK LEVEL
Score

What Viora detected

Why it matters

Recommended action

Example structure:

HIGH RISK
87 / 100

Do not proceed

Amount mismatch
Displayed: ₹500
Requested: ₹50,000

Recommended action:
Stop and verify the recipient independently.

Use real ThreatAssessment data.

Do not hard-code example results.

Make dangerous/suspicious/verify/safe states visually distinct.

## 9. History screen

Use real Room-backed history.

Each item should clearly communicate:
- risk level
- relevant amount/merchant if available
- explanation
- timestamp
- important signal

Create a clean timeline/list hierarchy.

Keep a polished empty state when no history exists.

Do not add fake records.

## 10. Reusable components

Where duplication exists, create small reusable Compose components for:
- risk badge
- risk indicator
- security card
- section header
- primary/secondary buttons
- history item

Do not create an over-engineered design framework.

## 11. Loading/error/empty states

Polish all user-visible states.

Avoid:
- blank screens
- generic exceptions
- broken spacing
- placeholder-looking UI

Every state should feel intentional.

## 12. Animations

Use subtle animations only where they improve:
- risk reveal
- state transition
- loading
- navigation feedback

Do not add excessive animation.

The security result must remain fast and clear.

## 13. Accessibility and usability

Ensure:
- sufficient contrast
- readable text
- sensible touch targets
- content descriptions where appropriate
- no critical information conveyed by color alone

## 14. Responsive behavior

Check different screen sizes/aspect ratios conceptually and through previews/tests where available.

Avoid hard-coded absolute positioning.

Use Compose layout primitives properly.

## 15. Visual consistency audit

After implementation, inspect every screen as one product.

Ask:
- Do all screens feel like the same application?
- Are spacing and typography consistent?
- Are risk colors consistent?
- Are buttons consistent?
- Is the visual hierarchy obvious?
- Does the app look professional in a screen recording?
- Does the UI reinforce Viora's security identity?

Fix inconsistencies found.

## Explicitly do NOT

Do not:
- change security logic
- change risk scoring
- create fake data
- add unnecessary dependencies
- add cloud/backend functionality
- redesign architecture outside presentation
- replace working data flow
- add random gradients/neon effects just to look futuristic
- overcrowd the UI with cards
- copy another product's visual design

## Validation

Run:
./gradlew test
./gradlew assembleDebug

Also inspect all changed Compose screens for:
- compile errors
- alignment
- consistent colors
- typography
- spacing
- navigation
- real data binding

Do NOT commit or push.

## Final report

Report:
1. Screens redesigned.
2. Design-system changes.
3. Major visual improvements.
4. Result-screen improvements.
5. Home improvements.
6. History improvements.
7. Loading/error/empty states.
8. Reusable components created.
9. Tests.
10. Build result.
11. Any remaining visual concerns.

STOP after Phase 6.
