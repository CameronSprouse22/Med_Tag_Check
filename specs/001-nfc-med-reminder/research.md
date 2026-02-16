# Research: NFC Medication Reminder System

**Date**: 2026-02-14  
**Feature**: [spec.md](spec.md)  
**Purpose**: Document technology decisions, rationale, and alternatives considered for implementation

---

## Key Technology Decisions

### Decision 1: Android Minimum API Level

**Decision**: Target Android 5.0 (API Level 21) and above

**Rationale**: 
- Android 5.0 was released in 2014, providing 12 years of device coverage (as of 2026)
- According to Android distribution data, API 21+ covers >99% of active devices
- NFC support has been stable since API 21 with no significant API changes needed
- Material Design was introduced in API 21, aligning with constitution requirement for Material Design UI
- TextToSpeech API is mature and reliable at this level
- AlarmManager and background task APIs are stable and well-documented

**Alternatives Considered**:
- **API 24+ (Android 7.0)**: Would provide newer APIs but exclude ~2-3% of devices, potentially leaving out users with older devices who may be in the target demographic (elderly, lower income)
- **API 28+ (Android 9.0)**: Modern API surface but excludes ~10-15% of devices, too restrictive for accessibility-focused healthcare app
- **API 19+ (Android 4.4)**: Broader compatibility but Material Design support requires compatibility libraries, NFC APIs less mature, not worth the complexity

**Decision Impact**: Broad device compatibility while maintaining access to all required modern Android features (Room, ViewModel, WorkManager, stable NFC APIs)

---

### Decision 2: Data Persistence - JSON Files vs Room Database

**Status**: ⚠️ **NEEDS USER CLARIFICATION** - Constitution conflict requires resolution

**Context**: 
- Constitution (Principle V) explicitly mandates: "Use Android Jetpack libraries (Room for database...)"
- User stated: "I don't want a database, I want the data to be saved in a json and then loaded and modified from that json"
- User began saying "I am building with..." but statement is incomplete

**Requirements from Spec**:
- FR-009: Persist all medication data locally on device (satisfied by both approaches)
- SC-008: Zero data loss during crashes (Room: ✅ ACID transactions, JSON: ⚠️ file corruption risk)
- SC-010: <50MB storage for 10 meds + 30 days (Room: ✅ disk-based, JSON: ⚠️ memory-bound)
- SC-006: Startup <2s (Room: ✅ ~50ms, JSON: ⚠️ 100-500ms parsing)

---

#### Option A: Room Database (Constitution-Compliant) ✅

**Advantages**:
- ✅ **Safety**: ACID transactions guarantee zero data loss (critical for medical app)
- ✅ **Constitution Compliant**: Explicitly mandated in Principle V
- ✅ **Referential Integrity**: Foreign keys prevent orphaned records (deleting medication cascades to dose records/tags)
- ✅ **Performance**: Query optimization, lazy loading, indexed searches
- ✅ **LiveData Integration**: Automatic UI updates, no manual notifications
- ✅ **Type Safety**: Compile-time SQL validation prevents runtime errors
- ✅ **Testing**: Mature testing support (Room testing library, in-memory databases)
- ✅ **Migrations**: Built-in schema migration framework

**Disadvantages**:
- ❌ Requires annotation processing (build time +5-10s)
- ❌ More boilerplate (~400 lines: DAOs, Database, Converters)
- ❌ Not human-readable (requires DB inspector or export)

**Lines of Code**: ~400-500 (5 entities, 5 DAOs, 1 Database class, TypeConverters)

---

#### Option B: JSON File Storage (Constitution Violation) ⚠️

**Advantages**:
- ✅ Human-readable format (easy debugging with text editor)
- ✅ Simpler code (~300 lines, no annotations)
- ✅ No annotation processing (faster builds)
- ✅ Easy export/import (JSON is universal)

**Disadvantages**:
- ❌ **CONSTITUTION VIOLATION**: Principle V mandates Room
- ❌ **Safety Risk**: No ACID transactions (file write failures→data corruption)
- ❌ **Memory**: Must load entire dataset into RAM (grows unbounded with dose history)
- ❌ **No Referential Integrity**: Manual cascade delete logic (error-prone)
- ❌ **Thread Safety**: Must manually implement file locking or queues
- ❌ **LiveData**: Manual notification triggers (easy to forget→stale UI)
- ❌ **Performance**: Serialize entire dataset on every write (200-1000ms)
- ❌ **No Migration Framework**: Breaking changes require manual handling

**Lines of Code**: ~300-400 (5 entities, 1 StorageManager, 5 repositories with manual cascade deletes)

**Required Justification (per constitution)**:
If JSON is chosen, must complete Complexity Tracking table in plan.md:
- Why Room's ACID transactions are insufficient for medication data
- How zero data loss (SC-008) will be guaranteed without transactions
- Why manual referential integrity is justified over Room's foreign keys
- Why all-data-in-memory approach won't violate SC-010 (<50MB)

---

#### Option C: Hybrid (Partial Compliance) ⚠️

JSON for AppSettings + EmergencyContact, Room for Medication + DoseRecord + NFCTag

**Advantages**:
- ✅ Settings remain human-editable
- ✅ Safety-critical data (medications/doses) gets ACID protection
- ✅ Partial constitution compliance (Room where it matters)

**Disadvantages**:
- ⚠️ Two storage systems (maintenance overhead)
- ⚠️ Settings lack transaction safety (acceptable for non-critical data)
- ⚠️ Increased complexity testing two systems

---

### Recommended Decision: **Option A (Room Database)**

**Rationale**:

1. **User Safety First** (Constitution Principle I): Medical app requires ACID guarantees. File corruption during crash could lose dose history, leading to dangerous double-dosing or missed medications.

2. **Zero Data Loss Requirement** (SC-008): Room provides transaction atomicity. JSON file writes can fail halfway, corrupting data.

3. **Constitution Compliance** (Principle V): Room is explicitly mandated. Deviation requires documented justification in Complexity Tracking table.

4. **Performance at Scale** (SC-010): Room stores data on disk, JSON loads all into memory. As dose history accumulates (30+ days), JSON memory usage grows unbounded.

5. **Long-term Maintainability**: Room's migration framework handles schema changes. JSON requires manual version handling.

**Trade-off Acknowledged**: Room has more boilerplate and less readable storage, but these are acceptable costs for data safety in a healthcare application.

---

### Questions for User (BLOCKER):

**Please clarify before proceeding to Phase 1:**

1. **Complete your statement**: You said "I am building with..." - what were you going to specify? (JSON library? Different storage? Something else?)

2. **Storage preference**: 
   - A) Room Database (recommended, constitution-compliant)
   - B) JSON Files (requires justification for constitution violation)
   - C) Hybrid (JSON for settings, Room for medications)

3. **If choosing JSON** (Option B or C), which serialization library?
   - Gson 2.10+ (most popular, simple)
   - Moshi 1.15+ (modern, better null safety, codegen option)
   - Jackson 2.16+ (fastest, most features, larger size)

4. **If choosing JSON** (Option B), provide written justification for:
   - Why ACID transactions are not needed for medication data
   - How zero data loss will be guaranteed (SC-008)
   - How memory usage won't exceed 50MB as history grows (SC-010)
   - Why manual referential integrity is acceptable for safety-critical medical app

---

### Decision Impact:

**If Room selected**: 
- Update build.gradle with Room 2.6.1 dependencies ✅ (already present)
- Create 5 @Entity classes, 5 @Dao interfaces, 1 @Database class
- Proceed to Phase 1 with full constitution compliance

**If JSON selected**:
- Add JSON library to build.gradle (remove Room dependencies)
- Create JsonStorageManager with thread-safe file I/O
- Complete Complexity Tracking justification in plan.md
- Strip @Entity/@Room annotations from model classes
- Implement manual cascade delete logic in repositories
- Add manual LiveData notification triggers

---

### Decision 3: UI Architecture - MVVM vs MVP vs MVI

**Decision**: MVVM (Model-View-ViewModel) pattern using AndroidX ViewModel and LiveData

**Rationale**:
- ViewModel survives configuration changes (screen rotation), preserving state without manual save/restore
- LiveData provides lifecycle-aware data observation, preventing memory leaks and crashes
- Clear separation of concerns: UI logic in ViewModel, presentation in Fragment/Activity, data in Repository
- Official Android architecture pattern with extensive documentation and community support
- Excellent testability: ViewModels are pure Kotlin/Java classes, easy to unit test without Android framework
- Works seamlessly with Room database (LiveData queries) and WorkManager
- Aligns with constitution: "Use Android Jetpack libraries", "MVVM pattern with clear separation"

**Alternatives Considered**:
- **MVP (Model-View-Presenter)**: Older pattern, requires manual lifecycle management, more boilerplate, less lifecycle-aware, no ViewModel survival across config changes
- **MVI (Model-View-Intent)**: Excellent for complex state management but overkill for this app's needs; significantly higher complexity, violates YAGNI principle
- **No Architecture (Activity/Fragment-centric)**: Simplest but leads to massive God classes, untestable code, lifecycle bugs, violates constitution principles

**Decision Impact**: Lifecycle-safe, testable, maintainable architecture. Reduces bugs from configuration changes. Easy to unit test business logic.

---

### Decision 4: Background Task Scheduling - WorkManager vs AlarmManager vs JobScheduler

**Decision**: Hybrid approach - AlarmManager for time-critical alarms, WorkManager for refill notifications

**Rationale**:
- **AlarmManager**: Precise timing needed for medication alarms (within seconds), high priority, user-facing, can wake device
- **WorkManager**: Perfect for refill reminders (less time-critical), respects battery optimization, guaranteed execution eventually, handles network/charging constraints (for future emergency notifications)
- JobScheduler: Deprecated in favor of WorkManager for non-exact tasks
- Constitution requires battery efficiency - WorkManager respects Doze mode, alarms use AlarmManager's exact timing
- Alarms are NON-NEGOTIABLE (safety-critical), refill reminders can tolerate delay (battery-friendly)

**Alternatives Considered**:
- **AlarmManager only**: Works but doesn't respect battery optimization for non-critical tasks (refill reminders), could drain battery
- **WorkManager only**: Cannot guarantee precise timing for medication alarms (could delay critical reminders), insufficient for safety-critical use case
- **Foreground Service**: Too aggressive, battery drain, user annoyance from persistent notification, violates simplicity principle

**Decision Impact**: Time-critical alarms are precise and reliable, non-critical tasks are battery-efficient. Balances user safety with device resource management.

---

### Decision 5: NFC Data Format - NDEF vs Custom Binary

**Decision**: NDEF (NFC Data Exchange Format) text records storing UUID v4 identifiers

**Rationale**:
- NDEF is the industry standard format, supported by all Android NFC devices
- Text records are human-readable during debugging (UUID format: `med:a3b2c1d4-e5f6-7890-1234-567890abcdef`)
- UUID v4 provides 128-bit uniqueness, virtually no collision risk across billions of tags
- NDEF libraries handle encoding/decoding, reducing custom code (simplicity principle)
- Easy to validate: UUID regex pattern ensures data integrity
- Privacy-preserving: UUID reveals no medication information (constitution: Privacy by Design)
- Cross-compatible with other NFC tools for debugging/verification

**Alternatives Considered**:
- **Custom binary format**: Smaller payload but requires custom parser, higher risk of bugs, violates standards, testing complexity
- **NDEF URI records**: More complex than text records, no benefit for our use case (not launching web URLs)
- **Plaintext medication names on tag**: Violates privacy principle (anyone with NFC reader could see medication), rejected immediately
- **Sequential integers (1, 2, 3)**: Easy to guess, tags could be read by others and correlated, no privacy

**Decision Impact**: Standards-compliant, privacy-preserving, debuggable NFC storage. Aligns with NFC Reliability and Privacy by Design principles.

---

### Decision 6: Audio Feedback - TextToSpeech vs Pre-recorded Audio

**Decision**: Android TextToSpeech (TTS) API for dynamic audio feedback

**Rationale**:
- TTS can speak any medication name dynamically (user-defined nicknames)
- Supports multiple languages out-of-box (accessibility for non-English speakers)
- No need to store large audio files (saves app size, <10MB constraint)
- TTS engines are built into Android (API 21+), no external dependency
- Users can customize TTS voice/speed in Android settings (system-wide preference)
- Real-time generation means instant feedback for new medications
- Handles edge cases: numbers, special characters, dose formats (e.g., "2 tablets", "5 milliliters")

**Alternatives Considered**:
- **Pre-recorded audio files**: Fixed vocabulary, cannot speak custom medication nicknames, massive storage overhead (MB per medication), wrong approach for user-generated content
- **Third-party TTS libraries**: Adds external dependency, potential privacy concerns (cloud TTS), violates local-first principle
- **No audio (visual only)**: Excludes users with vision impairment, violates accessibility goal for cognitive issues (audio confirmation is core feature)

**Decision Impact**: Flexible audio feedback for any medication name, minimal app size, respects user accessibility preferences. Aligns with User Safety First (clear audio confirmation).

---

### Decision 7: Testing Strategy - Unit + Instrumented Tests

**Decision**: Three-tier testing approach - Unit tests (JUnit + Mockito), Instrumented tests (AndroidX Test), Manual testing (real NFC tags)

**Rationale**:
- **Unit tests (60-70% coverage target)**: Fast execution, test business logic (ViewModels, validators, formatters) without Android framework
- **Instrumented tests (20-30% coverage)**: Test Android-specific components (Room DAOs, NFC operations, UI flows with Espresso) on actual devices/emulators
- **Manual testing (100% critical paths)**: NFC tag writing/reading cannot be fully mocked, physical devices required for final validation
- Mockito mocks NFC interfaces for unit tests (fast feedback), instrumented tests use real NFC emulation in AVD
- Constitution requires 80% coverage for "core medication logic" - achievable with unit + instrumented tests
- Espresso UI tests map directly to acceptance scenarios in spec (each user story → test class)

**Alternatives Considered**:
- **Unit tests only**: Cannot test Room database, NFC, AlarmManager (Android framework dependencies), insufficient for healthcare app
- **Instrumented tests only**: Too slow (requires emulator/device), expensive CI/CD time, developers need fast feedback cycle
- **Manual testing only**: Not repeatable, no regression detection, violates TDD principle, high risk for healthcare app

**Decision Impact**: Fast unit test feedback cycle, comprehensive Android integration testing, high confidence through manual NFC validation. Aligns with Test-Driven Development principle.

---

### Decision 8: Dependency Injection - Manual vs Dagger/Hilt

**Decision**: Manual dependency injection (constructor injection) for MVP, evaluate Hilt for future complexity

**Rationale**:
- Application is relatively simple: ~10 screens, ~15 classes, straightforward dependencies
- Manual injection is easier to understand for contributors (constitution: Simplicity)
- No build-time code generation (faster compile times)
- Easier to debug (no generated code to navigate)
- YAGNI principle: Add DI framework only when complexity justifies it
- Manual injection works well with ViewModelProvider factory pattern
- Constitution emphasizes "Keep architecture simple" and "YAGNI principle"

**Alternatives Considered**:
- **Dagger 2**: Powerful but steep learning curve, compile-time overhead, complex error messages, overkill for small app
- **Hilt**: Simpler than Dagger but still adds complexity, annotations can obscure dependencies, not needed for MVP
- **Koin**: Kotlin-first (we're using Java), runtime resolution (slower than compile-time), adds external dependency

**Decision Impact**: Simpler codebase, faster builds, easier onboarding for contributors. Can migrate to Hilt later if app grows significantly (e.g., 50+ screens, 100+ classes).

---

### Decision 9: UI Layout - XML vs Jetpack Compose

**Decision**: Traditional XML layouts with Material Components library

**Rationale**:
- XML layouts are stable, mature, and well-documented for Java-based Android apps
- Jetpack Compose is Kotlin-first with limited Java support
- Material Components library provides all needed UI elements (buttons, cards, lists, dialogs)
- XML is easier to debug with Layout Inspector
- Smaller APK size compared to Compose (no runtime library needed)
- Better IDE support for XML in Android Studio (visual editor, constraint layout tools)
- Constitution specifies Java as language - Compose is designed for Kotlin
- Team familiarity: XML/Java is more widely known than Compose

**Alternatives Considered**:
- **Jetpack Compose**: Modern declarative UI but requires Kotlin, still evolving, larger APK, poor Java interop, learning curve for Java developers
- **Custom views (Canvas API)**: Too low-level, massive development effort, hard to maintain, violates Material Design requirement

**Decision Impact**: Proven UI technology, excellent Material Design support, optimal for Java-based Android app. Aligns with Simplicity & Android Standards principle.

---

### Decision 10: Alarm Precision - Exact vs Inexact Alarms

**Decision**: Exact alarms (setExactAndAllowWhileIdle) for medication reminders

**Rationale**:
- Medication timing is safety-critical - delays are unacceptable (constitution: User Safety First)
- Exact alarms wake device from Doze mode (needed for life-dependent medications)
- Android 12+ requires SCHEDULE_EXACT_ALARM permission for health apps - justifiable use case
- Inexact alarms can delay up to 15 minutes (unacceptable for medications)
- setExactAndAllowWhileIdle respects battery optimization while ensuring timely delivery
- Use case meets Android's "approved use cases for exact alarms" (health/medication reminders)

**Alternatives Considered**:
- **Inexact alarms (set)**: Battery-friendly but unreliable timing, dangerous for medication adherence, violates safety principle
- **Foreground service**: Guarantees delivery but requires persistent notification, battery drain, user annoyance
- **WorkManager PeriodicWorkRequest**: Perfect for refill reminders but cannot guarantee exact timing for dose alarms

**Decision Impact**: Precise alarm delivery for medication safety, acceptable battery impact, meets Android's policy for health apps. Aligns with User Safety First principle.

---

## Research Summary

**Technical Stack Status**:
- **Platform**: Android 5.0+ (API 21), Java ✅
- **Architecture**: MVVM (ViewModel + LiveData) ✅
- **Database**: ⚠️ **PENDING USER CLARIFICATION** - Room (recommended) vs JSON Files
- **UI**: XML layouts with Material Components 1.11+ ✅
- **Background Tasks**: AlarmManager (exact alarms) + WorkManager (refill reminders) ✅
- **NFC**: NDEF format with UUID v4 identifiers ✅
- **Audio**: Android TextToSpeech API ✅
- **Testing**: JUnit 4 + Mockito + AndroidX Test + Espresso ✅
- **Dependency Management**: Manual injection (constructor-based) ✅

**Blockers**:
1. **Decision 2 (Storage)**: User must clarify "I am building with..." statement and choose storage approach
2. **Constitution Compliance**: If JSON is chosen, requires written justification for Principle V violation

**Constitution Compliance** (Pending Storage Decision):
- ✅ User Safety First: Exact alarms, data validation, TTS confirmation
- ✅ Privacy by Design: Local storage, UUID-only NFC tags, no cloud
- ✅ Test-Driven Development: Comprehensive testing strategy
- ✅ NFC Reliability: NDEF standard, retry logic, integrity checks
- ⚠️ Simplicity & Android Standards: Jetpack libraries mandated (Room) but user requested JSON storage

**Phase 1 Status**: ⚠️ **BLOCKED** - Cannot proceed to data model design without storage mechanism decision

**Next Steps**:
1. User answers questions in Decision 2 section above
2. If JSON chosen: Complete Complexity Tracking justification in plan.md
3. Update plan.md Technical Context with final storage decision
4. Proceed to Phase 1: Data Model Design
