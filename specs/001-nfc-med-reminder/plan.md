# Implementation Plan: NFC Medication Reminder System

**Branch**: `001-nfc-med-reminder` | **Date**: 2026-02-14 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-nfc-med-reminder/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

An Android application that helps users with cognitive issues track medication doses using NFC tags. Users attach NFC tags to medication bottles, scan them when taking doses, receive audio confirmation of medication details, and get timely alarms and refill reminders. The system prioritizes user safety through audio feedback, prevents missed doses with multi-level alarms, and maintains complete dose history locally on the device.

## Technical Context

**Language/Version**: Java 11 (Android SDK API 21-34, min Android 5.0)  
**Primary Dependencies**: AndroidX Core, Material Components 1.11.0, Lifecycle 2.7.0, WorkManager 2.9.0  
**Storage**: NEEDS CLARIFICATION - JSON file-based persistence requested (Gson/Jackson/Moshi library choice needed)  
**Testing**: JUnit 4.13.2, Mockito 5.8.0, Espresso 3.5.1, Robolectric 4.11.1, AndroidX Test  
**Target Platform**: Android 5.0+ (API 21+) with NFC hardware capability  
**Project Type**: Mobile (Android single-module application)  
**Performance Goals**: App startup <2s, NFC read/write <3s, alarm trigger within 30s of scheduled time  
**Constraints**: <10MB app size, <50MB storage (10 meds + 30 days history), <200ms UI response, offline-capable (local-only)  
**Scale/Scope**: Single-user device app, 5-15 medications typical, 10 user stories, ~8-12 screens (activities/fragments)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Principle I: User Safety First ✅
- ✅ All medication data validated before storage (FR-052)
- ✅ NFC operations include verification/checksums (FR-054)
- ✅ Clear, actionable error messages specified (FR-003)
- ✅ Confirmation dialogs for all medication actions (FR-051)
- ✅ Graceful failure handling without data loss (FR-008, SC-008)
- ✅ UI prevents accidental actions (FR-053 prevents double-confirmation)

**Status**: PASS - Spec comprehensively addresses safety requirements

### Principle II: Privacy by Design ✅
- ✅ Local-only storage, no cloud sync by default (FR-009)
- ✅ No external transmission without consent (FR-037)
- ✅ NFC tags use unique IDs, not medication names (FR-002)
- ✅ Minimal permissions: NFC, alarms, notifications, local storage only
- ✅ Logs must not contain sensitive data (implied by constitution)
- ✅ Privacy warnings for data export features (FR-037)

**Status**: PASS - Privacy-first design throughout

### Principle III: Test-Driven Development ✅
- ✅ TDD workflow enforced by constitution (tests before code)
- ✅ Unit tests required for NFC operations with mocks
- ✅ Instrumented tests required for database operations
- ✅ UI tests required for all user stories (10 stories identified)
- ✅ 80% code coverage minimum for core medication logic
- ✅ Tests must pass before merge

**Status**: PASS - Testing framework specified in dependencies, TDD process mandatory

### Principle IV: NFC Reliability ✅
- ✅ Retry logic with backoff required (max 3 attempts per constitution)
- ✅ NDEF format standardized (FR-001)
- ✅ Tag data integrity validation via checksums (FR-054)
- ✅ Clear feedback during NFC operations (FR-003, US1, US7)
- ✅ Graceful handling of partial read/writes (FR-054)
- ✅ Graceful degradation without NFC hardware (edge case documented)

**Status**: PASS - NFC reliability comprehensively addressed

### Principle V: Simplicity & Android Standards ⚠️
- ✅ Android Jetpack libraries specified (Lifecycle, ViewModel, WorkManager)
- ✅ Material Design guidelines (Material Components 1.11.0)
- ✅ Activity/Fragment lifecycle (implied by Android architecture)
- ✅ Android NFC APIs (android.nfc package specified)
- ✅ Java naming conventions (Java 11 confirmed)
- ⚠️ **NEEDS CLARIFICATION**: Storage mechanism - JSON files vs Room database
  - Constitution mandates AndroidX Room for database persistence
  - User requested JSON file-based storage instead
  - Requires research into trade-offs and constitution compliance

**Status**: CONDITIONAL PASS - Pending storage mechanism clarification in Phase 0

## Project Structure

### Documentation (this feature)

```text
specs/[###-feature]/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
app/
├── src/
│   ├── main/
│   │   ├── java/com/medchecktag/
│   │   │   ├── models/              # Entity models (Medication, DoseRecord, NFCTag, etc.)
│   │   │   ├── storage/             # JSON storage manager or database layer
│   │   │   ├── repositories/        # Data access with validation
│   │   │   ├── viewmodels/          # ViewModel classes for UI state
│   │   │   ├── ui/                  # Activities and Fragments
│   │   │   │   ├── main/            # Main medication list
│   │   │   │   ├── add/             # Add medication flow
│   │   │   │   ├── details/         # Medication info screen
│   │   │   │   ├── edit/            # Edit medication
│   │   │   │   └── settings/        # Settings screen
│   │   │   ├── nfc/                 # NFC read/write handlers
│   │   │   ├── alarms/              # AlarmManager integration, receivers
│   │   │   ├── audio/               # Text-to-speech feedback service
│   │   │   ├── services/            # Background services, WorkManager tasks
│   │   │   └── utils/               # Validation, time calculations, notifications
│   │   ├── res/                     # Android resources (layouts, strings, drawables)
│   │   └── AndroidManifest.xml      # App manifest with permissions
│   ├── test/                        # Unit tests (JUnit + Mockito + Robolectric)
│   │   └── java/com/medchecktag/
│   │       ├── models/
│   │       ├── repositories/
│   │       ├── nfc/
│   │       └── utils/
│   └── androidTest/                 # Instrumented tests (Espresso + AndroidX Test)
│       └── java/com/medchecktag/
│           ├── storage/             # Database/storage tests
│           ├── ui/                  # UI flow tests
│           └── nfc/                 # NFC integration tests
└── build.gradle                     # App module Gradle config

build.gradle                         # Project-level Gradle config
settings.gradle                      # Gradle settings
proguard-rules.pro                   # ProGuard obfuscation rules
```

**Structure Decision**: Standard Android single-module architecture with MVVM pattern. Package structure separates concerns clearly: models for data entities, storage for persistence (JSON or database), repositories for validated data access, viewmodels for UI state, ui packages organized by feature/screen, and separate utility packages for cross-cutting concerns (NFC, alarms, audio, validation). Testing mirrors main structure with unit tests (test/) and instrumented tests (androidTest/).

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
