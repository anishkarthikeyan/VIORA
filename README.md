# Viora

Context-aware UPI payment security that checks the payment against the
surrounding context before you pay.

Viora is a local-first Android app. It reads a payment QR code, a shared
message/link, or a shared screenshot; extracts what it can from each source;
and runs the combined result through a deterministic rule engine (plus one
small on-device ML signal) that explains — in plain language — why a payment
looks safe, worth a second look, or actively suspicious. It never claims to
block, cancel, or intercept a transaction; it warns, and the user decides.

## What Viora Does

Viora has **three manual input paths**, reached from the Home screen, and one
**background protection** feature that is separate from all three.

### Manual checking (user-initiated)

| Home button | What it does |
|---|---|
| **Scan Payment** | Opens the live camera QR scanner (CameraX + ML Kit barcode detection), with OCR running on the same camera feed. |
| **Link / Message** | Opens a text box (`LinkMessageScreen`) where you paste or type a payment link or a suspicious message to check it. |
| **Screenshot** | Opens Android's native photo picker; the selected image is run through OCR **and** QR/barcode detection. |

All three paths converge on the same pipeline (see below) and land on the
same **Result** screen.

### Background protection (proactive, opt-in)

`VioraAccessibilityService` is a separate, optional Android Accessibility
Service. Once the user explicitly enables it in Android Settings (Viora never
enables it silently — there is no code path that could), it watches the
visible text of a small, curated set of payment/banking/messaging apps (see
`AccessibilityRelevanceFilter`) and can show a system-level warning overlay
without the user opening Viora first.

**Status: implemented and unit-tested (event filtering, debouncing, duplicate
protection, warning policy). Not yet verified on physical hardware in this
repository's history** — see [Physical Device Validation](#physical-device-validation).

## Core Flow

```
Input (QR / Link-Message / Screenshot / Accessibility event)
    │
    ▼
Context extraction (OCR and/or QR decode, as applicable)
    │
    ▼
UPI parsing (QR/text payload → structured pa/pn/am/cu/tn, where present)
    │
    ▼
Context fusion (ContextFusionEngine — keeps "visible" and "payload" facts
                separate; never lets one silently overwrite the other)
    │
    ▼
Risk analysis (CompositeGuardianAI: ThreatEngine + AccessibilityThreatAnalyzer
               + MLGuardianAI, all real, deterministic-first)
    │
    ▼
Explainable result (score, risk level, named signals, plain-language reasons,
                     recommended action)
```

For **Screenshot** input specifically, both extraction paths run on the same
decoded bitmap before fusion:

```
Screenshot
├── OCR (ML Kit text recognition)         → visible merchant / amount / UPI ID
└── QR decode (ML Kit barcode scanning)    → raw QR payload (if any)
                                                   │
                                            UPI parser (pa/pn/am/cu/tn)
                                                   │
                                            Context fusion (QR side + visible side, kept separate)
                                                   │
                                            Risk analysis (CompositeGuardianAI)
```

If no QR is present in the screenshot, the OCR-only path runs exactly as
before — nothing is invented to fill the gap.

## Main Security Idea

**Don't just verify the payment. Verify the context.**

A QR code or a message can be technically valid and still be misleading. Viora
compares independent sources of the same fact instead of trusting either one
alone:

- visible recipient identity (OCR) vs. QR recipient identity (`pa`/`pn`)
- visible amount (OCR) vs. QR amount (`am`)
- message context (urgency, threats, payment/OTP requests) via the
  deterministic `AccessibilityThreatAnalyzer` signals and the additive
  on-device ML language signal

**A mismatch is a signal, not a verdict.** Viora's own scoring is
conservative by design: e.g. a merchant-name mismatch alone or a UPI-ID
mismatch alone lands at `VERIFY` risk, not an automatic "scam" label — see
`ThreatEngine.kt`'s own documentation and `ScreenshotQrContextIntegrationTest.kt`
for the exact behavior. Only multiple signals stacking, or a strong
deterministic signal like a threatening/urgent message combined with a
payment request, reaches `SUSPICIOUS`/`DANGEROUS`.

## Repository Structure

```
app/src/main/java/com/viora/app/
├── ai/                    # GuardianAI, ThreatEngine, AccessibilityThreatAnalyzer,
│   │                      # CompositeGuardianAI (orchestrates the above + ML)
│   └── ml/                # MLGuardianAI, LocalThreatClassifier, ThreatPrediction
├── core/
│   ├── camera/            # CameraX controller, QR frame analyzer, shared barcode decoder
│   ├── ocr/                # ML Kit text recognizer, shared/gallery image processor
│   ├── accessibility/      # Accessibility service, relevance filter, warning policy
│   ├── overlay/            # System warning overlay service/view
│   └── navigation/         # App navigation graph (Home/Scanner/LinkMessage/Result/History)
├── data/history/           # Room entity/DAO/database + repository implementation
├── domain/
│   ├── model/              # VioraContext, InputType, OcrResult
│   ├── parser/              # UPI URI parser
│   ├── fusion/              # ContextFusionEngine — merges QR + OCR facts
│   ├── processing/          # Share-Sheet text/URL processing
│   ├── threat/              # ThreatAssessment, ThreatSignal, RiskLevel (domain models)
│   └── history/             # ThreatHistoryRepository interface + domain history model
└── presentation/            # Compose UI: home, scanner, link/message, history, result
```

`scripts/ml/` (outside the app module) holds the Python training script,
augmentation data, and reproduction instructions for the shipped ML model —
see [Model / Asset Requirements](#model--asset-requirements).

## Requirements

Versions actually pinned in this repository (`gradle/libs.versions.toml`,
`gradle/wrapper/gradle-wrapper.properties`, `app/build.gradle.kts`):

| | Version |
|---|---|
| Gradle (via wrapper) | 9.5.0 |
| Android Gradle Plugin | 9.3.2 |
| Kotlin | 2.1.0 |
| compileSdk / targetSdk | 37 |
| minSdk | 29 (Android 10+) |
| Java source/target compatibility | 11 |
| Room | 2.8.4 (KSP 2.3.11) |

**JDK to run Gradle itself**: this repository ships
`gradle/gradle-daemon-jvm.properties` with `toolchainVersion=25` — Gradle will
auto-provision a matching JDK 25 toolchain on first run (via the
`foojay-resolver-convention` plugin, which needs network access the first
time). This was verified working in this environment with Temurin JDK 26
(`java -version` → `Temurin-26.0.1`) as the launcher JVM. If you have any
JDK 17+ already installed and don't want auto-provisioning, Gradle 9.x can
still run with it as the launcher; the daemon-jvm criteria above is what
Gradle will otherwise fetch automatically.

**Android Studio**: not required to build (Gradle CLI is sufficient), but the
project is a standard Android Studio Gradle project — any recent stable
Android Studio release compatible with AGP 9.3.2 will open it. `.idea/` in
this repo only tracks the small set of project-level files Android Studio
itself recommends committing (code style, inspection profile, VCS mapping) —
no local machine paths or user-specific settings.

**Android SDK**: a local Android SDK with platform 37 installed, referenced
via `local.properties` → `sdk.dir` (this file is machine-specific and is
already `.gitignore`d — Android Studio regenerates it automatically on first
open, or run `./gradlew` once with `ANDROID_HOME` set).

## Build

```bash
./gradlew clean test
./gradlew assembleDebug
```

**Actual result of both commands in this environment**, run fresh as part of
this packaging pass:

- `./gradlew clean test` → **BUILD SUCCESSFUL**, 134 tests, 0 failures.
- `./gradlew assembleDebug` → **BUILD SUCCESSFUL**, produces
  `app/build/outputs/apk/debug/app-debug.apk`.

### Lint

```bash
./gradlew lint
```

`lint` **is** wired up (it's the default AGP `lint`/`lintDebug` task — no
custom lint configuration exists in this project). Run fresh in this
environment, it currently reports:

**4 errors, 30 warnings — the task fails as configured.** This is a real,
pre-existing state (present before this packaging pass; not introduced by it)
and is documented here rather than silently fixed, per this pass's own scope
(documentation only, no behavior changes). The 4 errors:

1. `UpiParser.kt:105` — `NewApi`: `URLDecoder.decode(String, Charset)`
   requires API 33; minSdk is 29. This is a genuine latent bug (would throw
   `NoSuchMethodError` on Android 10–12 devices) that predates this packaging
   pass and was not introduced or fixed by it.
2. `OcrFrameAnalyzer.kt:33` and `QrFrameAnalyzer.kt:33` —
   `UnsafeOptInUsageError`: the `@OptIn(ExperimentalGetImage::class)` markers
   don't suppress the check in this AGP/CameraX version combination (the same
   thing shows as a Kotlin compiler warning on every build — see the
   `ExperimentalGetImage`/`@RequiresOptIn` message in `assembleDebug` output).
3. `VioraNavigation.kt:42` — `ContextCastToActivity`: casts `LocalContext` to
   `ComponentActivity` instead of using the newer `LocalActivity` API.

The 30 warnings are mostly informational: 13 "newer dependency version
available" nudges, 7 unused default-template color resources
(`teal_200`, `purple_500`, ...) left over from project scaffolding, plus a
handful of minor style suggestions (`UseKtx`, a redundant manifest label, a
missing standard View constructor on a programmatically-only-constructed
overlay view). None are new risks introduced by this packaging pass.

No lint baseline was added, and none of the above was fixed — doing so would
mean editing production code, which this pass is explicitly scoped not to do.

## Install

```bash
adb devices              # confirm a device/emulator is attached and authorized
./gradlew installDebug   # builds (if needed) and installs app-debug.apk
```

Then launch it either from the device's app drawer ("Viora") or:

```bash
adb shell am start -n com.viora.app/.MainActivity
```

## Tests

All 134 tests under `app/src/test/` are **local JVM unit tests** (no
Robolectric, no emulator). They cover:

- `UpiParser`, `ContextFusionEngine`, `ThreatEngine`,
  `AccessibilityThreatAnalyzer`, `CompositeGuardianAI` — deterministic
  parsing/fusion/scoring logic, including the amount-mismatch,
  merchant-mismatch, recipient-ID-mismatch, and social-engineering scenarios
- `MLGuardianAI` / `LocalThreatClassifier` — confidence thresholding, failure
  safety (missing/invalid model, classifier exceptions), and a smoke test
  that loads the **actual shipped model asset** and checks it scores the
  canonical scam sentence higher than a benign one
- `ThreatHistoryRepository`/Room mapping — entity↔domain mapping, ordering,
  persistence-failure safety, using a fake DAO (no real SQLite)
- `ScreenshotQrContextIntegrationTest` — the canonical Screenshot QR+context
  scenario (see [Canonical Test Scenarios](#canonical-test-scenarios)), run
  through the real parser/fusion/composite pipeline

**These unit tests do not exercise**: the actual camera, actual ML Kit
inference on real image data, the actual Android AccessibilityService
receiving real events, the actual system overlay drawing, or the actual
Android photo picker/Share Sheet UI. `app/src/androidTest/` currently
contains only the default Android Studio template
(`ExampleInstrumentedTest.kt`) — there is no real instrumented test suite in
this repository yet.

## Physical Device Validation

| Capability | Status |
|---|---|
| QR scan → UPI parse → risk analysis (camera) | IMPLEMENTED, UNIT TESTED. Not physical-device verified in this repo's session history. |
| OCR (camera and shared image) | IMPLEMENTED, UNIT TESTED (via fused-context assertions). ML Kit inference itself not exercised outside a real device/emulator. |
| Screenshot QR decode + context fusion | IMPLEMENTED, UNIT TESTED (`ScreenshotQrContextIntegrationTest`). App installs and launches without crashing on a connected physical device (confirmed via `adb`/`logcat`, no `FATAL EXCEPTION`). The interactive picker → OCR → QR → Result flow was **not** interactively verified — the test device was locked and unlocking it was left to its owner. |
| Link/Message text input | IMPLEMENTED, UNIT TESTED (`onSharedText` pipeline). Not physical-device verified. |
| Background/Accessibility protection | IMPLEMENTED, UNIT TESTED (relevance filtering, debounce, warning policy, duplicate-history protection). **Not** physical-device verified — never confirmed firing from a real foreground app on a device in this repository's history. |
| Warning overlay | IMPLEMENTED. Not physical-device verified (system overlay permission + draw-over-apps behavior needs a real device). |
| Room-backed history | IMPLEMENTED, UNIT TESTED against a fake DAO. Real on-device SQLite persistence not separately verified. |
| App install/launch | PHYSICAL DEVICE VERIFIED — installed via `adb install` and launched via `adb shell am start` on a connected Android 13 device; confirmed no crash via `logcat`. |

**Nothing in this repository claims physical-device-verified behavior beyond
install/launch.** Everything else above "IMPLEMENTED, UNIT TESTED" is real,
exercised code with real assertions, but has not been confirmed running
end-to-end on Android hardware as part of this project's history.

## Permissions

Exactly what's declared in `app/src/main/AndroidManifest.xml` — nothing more:

| Permission | Purpose |
|---|---|
| `android.permission.CAMERA` | Live QR/OCR camera scanning (Scan Payment) |
| `android.permission.SYSTEM_ALERT_WINDOW` | Drawing the warning overlay over other apps |
| Accessibility service binding (`BIND_ACCESSIBILITY_SERVICE`, system-enforced) | `VioraAccessibilityService`, opt-in via Android Settings — Viora cannot enable this itself |

No storage/media permission is declared — the Screenshot flow uses Android's
system Photo Picker (`ActivityResultContracts.PickVisualMedia`), which does
not require a runtime permission grant.

**`android.permission.INTERNET` is not declared.** This is verified directly
from the manifest, not inferred.

## Offline / Privacy

- **No `INTERNET` permission is declared in the manifest** — Android enforces
  this at the OS level, so the installed app cannot make network requests
  regardless of what any library attempts to do internally.
- A repository-wide search found no HTTP client, no analytics/telemetry SDK,
  and no cloud AI call anywhere in `app/src/main/java`.
- The on-device ML model (`scam_language_classifier_v1.json`) is loaded from
  a bundled asset and run with plain arithmetic — no model download, no
  remote inference.
- Room history stores only structured fields (timestamp, input type,
  upiId/merchantName/amount/currency, score, risk level, explanation,
  recommended action, signals) — raw OCR text, raw QR payload strings, OTPs,
  and PINs are never persisted. See `ThreatHistoryEntity.kt`'s own
  documentation for the exact schema.
- This does **not** mean "completely secure" or "guaranteed private" — it
  means analysis is local and no network egress path exists in the shipped
  manifest, which is what the code actually proves.

## Demo

1. Launch Viora.
2. Pick one of: **Scan Payment** (camera), **Link / Message** (paste text),
   or **Screenshot** (pick an image).
3. Viora extracts context (QR/OCR/text), fuses it, and runs
   `CompositeGuardianAI`.
4. The **Result** screen shows the risk level, score, named signals, a
   plain-language explanation, and a recommended action.
5. If Background Protection has been enabled in Android Settings *and*
   verified working on your device, a warning overlay can also appear while
   using another app — this part is implemented but not yet
   physical-device-verified in this repository (see above).

For the **Screenshot** demo specifically:
- OCR extracts the visible context (organization name, visible amount,
  visible UPI ID if shown as text).
- The QR decoder independently extracts the QR payload from the same image.
- The UPI parser structures that payload (`pa`/`pn`/`am`/`cu`/`tn`).
- Context fusion keeps both sides separate so the risk engine can compare
  what's *shown* against what's *encoded*, rather than trusting either alone.

## Canonical Test Scenarios

These are real, existing files in this repository — not invented for this
README:

| Scenario | Where it's defined | What it proves |
|---|---|---|
| Legitimate / consistent payment | `ThreatEngineTest.kt` ("matching amounts and consistent merchant is SAFE"), `ScreenshotQrContextIntegrationTest.kt` ("screenshot with no QR...") | No signal fires when visible and payload facts agree (or there's nothing to compare) |
| Recipient/merchant mismatch | `ThreatEngineTest.kt` (`MERCHANT_MISMATCH`, `RECIPIENT_ID_MISMATCH` cases), `ScreenshotQrContextIntegrationTest.kt` (canonical NEXTGEN INDIA / Dr DHANRAJ case) | A differing visible identity vs. QR identity is flagged, cautiously (`VERIFY`, not an automatic scam label) |
| Amount mismatch | `ThreatEngineTest.kt` (`AMOUNT_MISMATCH`), `ScreenshotQrContextIntegrationTest.kt` ("high-value amount mismatch - visible 499 vs QR 4999") | A visible amount that differs from the QR's encoded amount is flagged |
| Suspicious cashback/KYC-style message | `AccessibilityThreatAnalyzerTest.kt`, `DemoScenarioIntegrationTest.kt` ("KYC has expired. Pay Rs 999 immediately or your account will be blocked.") | Urgency + account-threat + payment-request language reaches `DANGEROUS` |

See also `DEMO_SCRIPT.md` (three live-demo scripts for the camera Scan and
Share Sheet paths) and `scripts/ml/README.md` (the ML model's own held-out
test scenarios).

## Model / Asset Requirements

- **Model file**: `app/src/main/assets/ml/scam_language_classifier_v1.json`
  (14.6 KB). **This file is already committed** and required for the app to
  build and run — `LocalThreatClassifier` loads it from Android assets at
  runtime. Nothing needs to be downloaded or provisioned to build the app.
- **What it is**: a small bag-of-words logistic-regression classifier,
  genuinely trained (not hand-written rules) — see `scripts/ml/README.md` for
  the full data/methodology writeup and honestly-reported held-out metrics.
- **If the model is missing or fails to load**: `MLGuardianAI` degrades to
  contributing no signal (verified by `MLGuardianAITest`); the deterministic
  `ThreatEngine`/`AccessibilityThreatAnalyzer` signals are unaffected. The app
  does not crash and does not fabricate a result.
- **Retraining** (optional, not needed to build/run the app): the training
  script (`scripts/ml/train.py`) downloads a public dataset over the network
  on demand — this is a one-time, opt-in developer action, separate from the
  shipped app, and does not affect the app's own offline operation. See
  `scripts/ml/README.md` for exact steps.
- **No API keys, tokens, or other secrets** are used anywhere in this
  repository — verified by a repository-wide search for common
  key/token/credential patterns.

## Limitations

- **Heuristic limitations**: merchant-name matching, displayed-amount
  extraction, and visible-UPI-ID extraction are regex/heuristic-based OCR
  parsing (see `ContextFusionEngine.kt`) — they can miss or misread text
  depending on screenshot layout, font, or language. A UPI ID and an email
  address are syntactically indistinguishable to the extractor.
- **ML model limitations**: linear bag-of-words classifier, ~77% recall on
  its own held-out test set (tuned conservatively for few false positives,
  not high catch-rate), English/UK-SMS-corpus-skewed base data augmented
  with a small, disclosed set of India-specific examples. See
  `scripts/ml/README.md`.
- **Background Protection validation status**: implemented and unit-tested
  at the logic level; **not yet confirmed running on physical hardware** in
  this repository's history (no live device test of the accessibility
  service actually firing, the overlay actually drawing, or the permission
  flow actually working end-to-end).
- **Screenshot/QR hardware validation status**: build-verified (APK
  decompiled and confirmed to contain the QR-decoding code) and app-launch
  verified on a physical device; the interactive picker→analysis flow itself
  is still pending a live test on unlocked hardware.
- **Lint**: currently fails with 4 errors (see [Lint](#lint)) — pre-existing,
  not introduced by this packaging pass, not fixed by it.
- **No instrumented (`androidTest`) coverage** beyond the default template —
  anything requiring `Context`/`Activity`/real Android framework behavior is
  only unit-tested indirectly (via extracted pure logic) or not tested at
  all.
- No claim of "production ready," "works on all Android devices," or "100%"
  anything is made anywhere in this document.

## Quick Start

```bash
git clone <repo-url>
cd Viora

./gradlew clean test        # 134 tests, expect BUILD SUCCESSFUL
./gradlew lint               # currently reports 4 errors, 30 warnings — see Lint section
./gradlew assembleDebug      # produces app/build/outputs/apk/debug/app-debug.apk

adb devices                  # confirm a device/emulator is attached
./gradlew installDebug       # install onto it
```

Then:
- **Open the app** from the launcher, or `adb shell am start -n com.viora.app/.MainActivity`.
- **Grant Camera** when prompted, if you want to use **Scan Payment**.
- The three input modes are on the **Home** screen: **Scan Payment**,
  **Link / Message**, **Screenshot**.
- **Background Protection** is a separate, optional step: Home shows a
  "Protection" status card with an **Enable** link that opens Android's own
  Accessibility Settings — Viora never enables it silently.
- **Physical device**: a real device or emulator is required for camera,
  OCR, QR decoding, the accessibility service, and the overlay — none of
  these run in the JVM unit test suite. `minSdk` is 29 (Android 10+).
- **Model provisioning**: none needed — the ML model asset is already
  committed in the repository.

## License

All rights reserved. Add a license before distributing.
