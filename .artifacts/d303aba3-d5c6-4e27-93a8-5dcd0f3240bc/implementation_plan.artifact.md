# VIORA Foundation Implementation Plan

This plan outlines the steps to build the Android foundation and UI for the VIORA application, an AI-powered contextual safety assistant.

## User Review Required

> [!IMPORTANT]
> The current plan focuses on the foundation and UI placeholders as requested. No real AI or scanning logic will be implemented in this phase.

## Proposed Changes

### Build Configuration

#### [MODIFY] [libs.versions.toml](file:///C:/Users/abina/AndroidStudioProjects/Viora/gradle/libs.versions.toml)
- Add `androidx-navigation-compose` dependency.
- Update `lifecycleRuntimeKtx` to a more recent version if needed (currently 2.6.1, 2.8.7 is stable).

#### [MODIFY] [build.gradle.kts](file:///C:/Users/abina/AndroidStudioProjects/Viora/app/build.gradle.kts)
- Add `androidx-navigation-compose` to dependencies.

---

### Core Layer

#### [NEW] [NavRoute.kt](file:///C:/Users/abina/AndroidStudioProjects/Viora/app/src/main/java/com/viora/app/core/navigation/NavRoute.kt)
- Define navigation routes for Home, Scanner, Result, and History.

#### [NEW] [VioraNavigation.kt](file:///C:/Users/abina/AndroidStudioProjects/Viora/app/src/main/java/com/viora/app/core/navigation/VioraNavigation.kt)
- Set up the NavHost and composable destinations.

---

### Domain Layer

#### [NEW] [InputType.kt](file:///C:/Users/abina/AndroidStudioProjects/Viora/app/src/main/java/com/viora/app/domain/model/InputType.kt)
- Enum for input types: QR, URL, MESSAGE, SCREENSHOT, IMAGE, TEXT, CAMERA.

#### [NEW] [VioraContext.kt](file:///C:/Users/abina/AndroidStudioProjects/Viora/app/src/main/java/com/viora/app/domain/model/VioraContext.kt)
- Data class for contextual information.

#### [NEW] [RiskLevel.kt](file:///C:/Users/abina/AndroidStudioProjects/Viora/app/src/main/java/com/viora/app/domain/threat/RiskLevel.kt)
- Enum for risk levels: SAFE, VERIFY, SUSPICIOUS, DANGEROUS.

#### [NEW] [ThreatSignal.kt](file:///C:/Users/abina/AndroidStudioProjects/Viora/app/src/main/java/com/viora/app/domain/threat/ThreatSignal.kt)
- Data class for individual threat signals.

#### [NEW] [ThreatAssessment.kt](file:///C:/Users/abina/AndroidStudioProjects/Viora/app/src/main/java/com/viora/app/domain/threat/ThreatAssessment.kt)
- Data class for the overall threat assessment.

#### [NEW] [GuardianAI.kt](file:///C:/Users/abina/AndroidStudioProjects/Viora/app/src/main/java/com/viora/app/ai/GuardianAI.kt)
- Interface for the AI engine.

#### [NEW] [ThreatEngine.kt](file:///C:/Users/abina/AndroidStudioProjects/Viora/app/src/main/java/com/viora/app/domain/threat/ThreatEngine.kt)
- Basic implementation of the `GuardianAI` interface.

---

### Presentation Layer

#### [NEW] [VioraTheme.kt](file:///C:/Users/abina/AndroidStudioProjects/Viora/app/src/main/java/com/viora/app/presentation/components/VioraTheme.kt)
- Premium dark-first theme definition.

#### [NEW] [HomeScreen.kt](file:///C:/Users/abina/AndroidStudioProjects/Viora/app/src/main/java/com/viora/app/presentation/home/HomeScreen.kt)
- Main screen with primary and secondary actions.

#### [NEW] [ScannerScreen.kt](file:///C:/Users/abina/AndroidStudioProjects/Viora/app/src/main/java/com/viora/app/presentation/scanner/ScannerScreen.kt) [PLACEHOLDER]
- Placeholder for the scanner functionality.

#### [NEW] [ResultScreen.kt](file:///C:/Users/abina/AndroidStudioProjects/Viora/app/src/main/java/com/viora/app/presentation/result/ResultScreen.kt) [PLACEHOLDER]
- Placeholder for assessment results.

#### [NEW] [HistoryScreen.kt](file:///C:/Users/abina/AndroidStudioProjects/Viora/app/src/main/java/com/viora/app/presentation/history/HistoryScreen.kt) [PLACEHOLDER]
- Placeholder for check history.

---

### Main Entry Point

#### [MODIFY] [MainActivity.kt](file:///C:/Users/abina/AndroidStudioProjects/Viora/app/src/main/java/com/viora/app/MainActivity.kt)
- Initialize the theme and navigation.

## Verification Plan

### Automated Tests
- `gradlew assembleDebug` to ensure the project compiles.

### Manual Verification
- Deploy the app to a physical device or emulator.
- Verify the Home screen UI matches the requirements.
- Verify navigation between Home, Scanner, Result, and History placeholders.
