# Viora

Viora is an Android app that helps keep UPI payments safe. It scans QR codes and payment screens in real time, analyzes them for fraud indicators with an on-device threat engine ("Guardian AI"), and warns the user with an overlay before money leaves their account.

## Features

- **QR code scanner** — live camera scanning of QR codes using CameraX and ML Kit barcode detection.
- **UPI parser** — extracts and validates payee details (VPA, amount, etc.) from scanned codes.
- **OCR analysis** — reads text from the screen and shared screenshots using ML Kit text recognition to catch payment details that aren't in a QR code.
- **Threat engine (Guardian AI)** — scores inputs for scam/fraud risk signals and classifies the threat level.
- **Accessibility-based protection** — an optional accessibility service watches other apps' screens so Viora can warn during payments made outside the app.
- **Warning overlay** — draws system-level alerts over risky payment flows (`SYSTEM_ALERT_WINDOW`).
- **Share Sheet support** — share any text, URL, or screenshot from another app directly into Viora for analysis.

## Tech stack

- Kotlin, Jetpack Compose (Material 3), Navigation Compose
- CameraX + ML Kit (barcode scanning & text recognition)
- Android accessibility services
- Gradle with version catalogs, AGP 9.x
- JUnit unit tests

## Requirements

- Android Studio (latest stable)
- JDK 11+
- Android SDK 37 (compile/target), min SDK 29 (Android 10+)

## Building

```bash
# Build the debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test
```

Then open the project in Android Studio or install the APK on a device/emulator:

```bash
./gradlew installDebug
```

## Project structure

```
app/src/main/java/com/viora/app/
├── ai/            # Threat engine + accessibility threat analyzer (Guardian AI)
├── core/
│   ├── camera/    # CameraX controller and QR frame analyzers
│   ├── ocr/       # Text recognition and shared-image processing
│   ├── accessibility/ # Accessibility service and screen snapshot store
│   ├── overlay/   # System warning overlay service/view
│   └── navigation/# App navigation graph
├── domain/
│   ├── parser/    # UPI string/intent parsing
│   ├── fusion/    # Context fusion engine (combines signals into a verdict)
│   └── processing/# Shared-input processing pipeline
└── presentation/  # Compose UI: home, scanner, history, result screens
```

## Permissions

| Permission | Purpose |
|---|---|
| `CAMERA` | Live QR code scanning |
| `SYSTEM_ALERT_WINDOW` | Drawing fraud warnings over other apps |
| Accessibility service (optional) | Detecting risky payment screens in other apps |

## License

All rights reserved. Add a license before distributing.
