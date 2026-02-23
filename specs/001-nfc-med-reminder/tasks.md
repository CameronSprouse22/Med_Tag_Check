# Tasks: NFC Medication Reminder System

**Input**: Design documents from `/specs/001-nfc-med-reminder/`
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md)

**Tests**: Tests are OPTIONAL for this feature (not explicitly requested in specification). Tasks marked with ⚠️ represent optional test tasks.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `- [ ] [ID] [P?] [Story?] Description`

- **Checkbox**: `- [ ]` (markdown task checkbox)
- **[ID]**: Sequential task ID (T001, T002, T003...)
- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Path Conventions

Android application structure (from [plan.md](plan.md)):
- Models: `app/src/main/java/com/medchecktag/models/`
- Database: `app/src/main/java/com/medchecktag/database/`
- Repositories: `app/src/main/java/com/medchecktag/repositories/`
- ViewModels: `app/src/main/java/com/medchecktag/viewmodels/`
- UI: `app/src/main/java/com/medchecktag/ui/`
- Services: `app/src/main/java/com/medchecktag/services/`, `nfc/`, `alarms/`, `audio/`
- Resources: `app/src/main/res/layout/`, `values/`, `drawable/`
- Unit tests: `app/src/test/java/com/medchecktag/`
- Instrumented tests: `app/src/androidTest/java/com/medchecktag/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic Android application structure

- [X] T001 Create Android project structure with Gradle configuration per [plan.md](plan.md#project-structure)
- [X] T002 Add dependencies to app/build.gradle (AndroidX Room 2.5+, Lifecycle, Material Components 1.9+, WorkManager 2.8+, JUnit 4, Mockito 4.0+, Espresso 3.5+, Robolectric 4.10+)
- [X] T003 Configure Android SDK versions in app/build.gradle (minSdk 21, targetSdk 34, compileSdk 34)
- [X] T004 [P] Add NFC permissions to app/src/main/AndroidManifest.xml (android.permission.NFC, android.hardware.nfc feature requirement)
- [X] T005 [P] Add alarm permissions to app/src/main/AndroidManifest.xml (SCHEDULE_EXACT_ALARM, USE_EXACT_ALARM for API 31+)
- [X] T006 [P] Add notification permissions to app/src/main/AndroidManifest.xml (POST_NOTIFICATIONS for API 33+)
- [X] T007 [P] Create base package structure (models, database, repositories, viewmodels, ui, services, nfc, alarms, audio, utils)
- [X] T008 [P] Configure ProGuard rules in app/proguard-rules.pro (keep Room entities, NFC classes)
- [X] T009 [P] Add string resources to app/src/main/res/values/strings.xml (app name, category names, common messages)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### Database Infrastructure

- [X] T010 Create Medication entity in app/src/main/java/com/medchecktag/models/Medication.java per [data-model.md](data-model.md#1-medication)
- [X] T011 [P] Create Schedule embedded object in app/src/main/java/com/medchecktag/models/Schedule.java per [data-model.md](data-model.md#2-schedule)
- [X] T012 [P] Create AlarmConfiguration embedded object in app/src/main/java/com/medchecktag/models/AlarmConfiguration.java per [data-model.md](data-model.md#3-alarmconfiguration)
- [X] T013 [P] Create DoseRecord entity in app/src/main/java/com/medchecktag/models/DoseRecord.java per [data-model.md](data-model.md#4-doserecord)
- [X] T014 [P] Create NFCTag entity in app/src/main/java/com/medchecktag/models/NFCTag.java per [data-model.md](data-model.md#5-nfctag)
- [X] T015 [P] Create EmergencyContact entity in app/src/main/java/com/medchecktag/models/EmergencyContact.java per [data-model.md](data-model.md#6-emergencycontact)
- [X] T016 [P] Create AppSettings entity in app/src/main/java/com/medchecktag/models/AppSettings.java per [data-model.md](data-model.md#8-appsettings)
- [X] T017 [P] Create MedicationCategory enum in app/src/main/java/com/medchecktag/models/MedicationCategory.java (LIFE_DEPENDENT, VERY_IMPORTANT, BENEFICIAL)
- [X] T018 [P] Create ScheduleType enum in app/src/main/java/com/medchecktag/models/ScheduleType.java (INTERVAL, SPECIFIC_TIMES)
- [X] T019 [P] Create DoseStatus enum in app/src/main/java/com/medchecktag/models/DoseStatus.java (TAKEN, SKIPPED, MISSED)
- [X] T020 [P] Create ConfirmationMethod enum in app/src/main/java/com/medchecktag/models/ConfirmationMethod.java (NFC_SCAN, MANUAL_CONFIRM)
- [X] T021 [P] Create AlarmType enum in app/src/main/java/com/medchecktag/models/AlarmType.java (PRE_DOSE, ON_TIME, POST_DOSE, REFILL_REMINDER)

### Room Database Setup

- [X] T022 Create MedicationDao interface in app/src/main/java/com/medchecktag/database/MedicationDao.java with query methods per [repository-contract.md](contracts/repository-contract.md#imedicationrepository)
- [X] T023 [P] Create DoseRecordDao interface in app/src/main/java/com/medchecktag/database/DoseRecordDao.java with query methods per [repository-contract.md](contracts/repository-contract.md#idoserecordrepository)
- [X] T024 [P] Create NFCTagDao interface in app/src/main/java/com/medchecktag/database/NFCTagDao.java with query methods per [repository-contract.md](contracts/repository-contract.md#infctagrepository)
- [X] T025 [P] Create EmergencyContactDao interface in app/src/main/java/com/medchecktag/database/EmergencyContactDao.java with query methods
- [X] T026 [P] Create AppSettingsDao interface in app/src/main/java/com/medchecktag/database/AppSettingsDao.java with singleton access methods
- [X] T027 Create AppDatabase class in app/src/main/java/com/medchecktag/database/AppDatabase.java (Room database with all entities, version 1, singleton pattern)
- [X] T028 Create TypeConverters class in app/src/main/java/com/medchecktag/database/Converters.java (convert List<String> for specificTimes, Date/Timestamp conversions)

### Repository Layer

- [X] T029 Create MedicationRepository class in app/src/main/java/com/medchecktag/repositories/MedicationRepository.java implementing IMedicationRepository per [repository-contract.md](contracts/repository-contract.md#imedicationrepository)
- [X] T030 [P] Create DoseRecordRepository class in app/src/main/java/com/medchecktag/repositories/DoseRecordRepository.java implementing IDoseRecordRepository
- [X] T031 [P] Create NFCTagRepository class in app/src/main/java/com/medchecktag/repositories/NFCTagRepository.java implementing INFCTagRepository
- [X] T032 [P] Create EmergencyContactRepository class in app/src/main/java/com/medchecktag/repositories/EmergencyContactRepository.java implementing IEmergencyContactRepository
- [X] T033 [P] Create AppSettingsRepository class in app/src/main/java/com/medchecktag/repositories/AppSettingsRepository.java implementing IAppSettingsRepository

### Core Services

- [X] T034 Create NFCHandler class in app/src/main/java/com/medchecktag/nfc/NFCHandler.java implementing INFCHandler per [nfc-contract.md](contracts/nfc-contract.md)
- [X] T035 [P] Create AudioFeedbackService class in app/src/main/java/com/medchecktag/audio/AudioFeedbackService.java implementing IAudioFeedbackService per [audio-contract.md](contracts/audio-contract.md)
- [X] T036 [P] Create AlarmScheduler class in app/src/main/java/com/medchecktag/alarms/AlarmScheduler.java implementing IAlarmScheduler per [alarm-contract.md](contracts/alarm-contract.md)
- [X] T037 Create AlarmReceiver BroadcastReceiver in app/src/main/java/com/medchecktag/alarms/AlarmReceiver.java (handles alarm triggers, creates notifications)
- [X] T038 [P] Create BootReceiver BroadcastReceiver in app/src/main/java/com/medchecktag/alarms/BootReceiver.java (reschedules alarms after device reboot)
- [X] T039 Register AlarmReceiver and BootReceiver in app/src/main/AndroidManifest.xml with intent filters

### Utilities

- [X] T040 [P] Create ValidationUtils class in app/src/main/java/com/medchecktag/utils/ValidationUtils.java (UUID validation, input validation, time format validation)
- [X] T041 [P] Create TimeUtils class in app/src/main/java/com/medchecktag/utils/TimeUtils.java (calculate next dose time, format time strings, timezone handling)
- [X] T042 [P] Create NotificationUtils class in app/src/main/java/com/medchecktag/utils/NotificationUtils.java (create notification channels, build notifications)

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 2 - Add Medication with Basic Info (Priority: P1) 🎯 MVP Foundation

**Goal**: Users can add new medications with all required details (nickname, dose, category, schedule, refill thresholds) and write the medication ID to an NFC tag

**Independent Test**: Add a medication with complete details, write to NFC tag, verify medication appears in database and tag is linked correctly

**Why First**: Cannot use app without medications; this is the entry point for all functionality

### Tests for User Story 2 (OPTIONAL ⚠️)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [ ] T043 ⚠️ [P] [US2] Unit test for Medication model validation in app/src/test/java/com/medchecktag/models/MedicationTest.java (test field validation, refillThreshold1 > refillThreshold2)
- [ ] T044 ⚠️ [P] [US2] Unit test for Schedule calculation logic in app/src/test/java/com/medchecktag/models/ScheduleTest.java (test INTERVAL vs SPECIFIC_TIMES nextDoseTime calculation)
- [ ] T045 ⚠️ [P] [US2] Instrumented test for MedicationDao in app/src/androidTest/java/com/medchecktag/database/MedicationDaoTest.java (test insert, query, Room integration)
- [ ] T046 ⚠️ [P] [US2] Unit test for MedicationRepository in app/src/test/java/com/medchecktag/repositories/MedicationRepositoryTest.java (test validation logic, mock DAO)
- [ ] T047 ⚠️ [P] [US2] Unit test for AddMedicationViewModel in app/src/test/java/com/medchecktag/viewmodels/AddMedicationViewModelTest.java (test state management, validation)
- [ ] T048 ⚠️ [P] [US2] Instrumented UI test for Add Medication flow in app/src/androidTest/java/com/medchecktag/ui/AddMedicationFlowTest.java (Espresso test for complete user journey)

### Implementation for User Story 2

#### ViewModel Layer

- [X] T049 [US2] Create AddMedicationViewModel in app/src/main/java/com/medchecktag/viewmodels/AddMedicationViewModel.java (manage medication creation state, validation, repository calls)

#### UI Layer

- [X] T050 [US2] Create add_medication_layout.xml in app/src/main/res/layout/add_medication_layout.xml (form with nickname, dose, category spinner, schedule mode toggle, refill thresholds)
- [X] T051 [US2] Create schedule_interval_fragment.xml in app/src/main/res/layout/schedule_interval_fragment.xml (hours input field, end date toggle)
- [X] T052 [US2] Create schedule_specific_times_fragment.xml in app/src/main/res/layout/schedule_specific_times_fragment.xml (time picker list, add/remove time buttons)
- [X] T053 [US2] Create nfc_write_dialog.xml in app/src/main/res/layout/nfc_write_dialog.xml (fullscreen modal with "Hold phone near tag to write" message, cancel button)
- [X] T054 [US2] Create AddMedicationActivity in app/src/main/java/com/medchecktag/ui/medication/AddMedicationActivity.java (form input handling, schedule mode switching, validation)
- [X] T055 [US2] Create ScheduleIntervalFragment in app/src/main/java/com/medchecktag/ui/medication/ScheduleIntervalFragment.java (interval-based schedule input)
- [X] T056 [US2] Create ScheduleSpecificTimesFragment in app/src/main/java/com/medchecktag/ui/medication/ScheduleSpecificTimesFragment.java (specific times list management with RecyclerView)
- [X] T057 [US2] Create NFCWriteDialogFragment in app/src/main/java/com/medchecktag/ui/nfc/NFCWriteDialogFragment.java (handle NFC write mode, display status, handle success/failure)

#### Integration

- [X] T058 [US2] Implement medication save logic in AddMedicationActivity (validate inputs, create Medication object, call repository, trigger NFC write dialog)
- [X] T059 [US2] Integrate NFCHandler in NFCWriteDialogFragment (foreground dispatch setup, write medication ID to tag, link tag to medication in database)
- [X] T060 [US2] Add AddMedicationActivity entry to app/src/main/AndroidManifest.xml
- [X] T061 [US2] Create string resources for User Story 2 in app/src/main/res/values/strings.xml (form labels, validation messages, NFC prompts)
- [X] T062 [US2] Create dimension resources for User Story 2 in app/src/main/res/values/dimens.xml (form spacing, button sizes)

**Checkpoint**: At this point, users can add medications with full details and write NFC tags. Database is populated for subsequent stories.

---

## Phase 4: User Story 3 - Medication List View & Status (Priority: P1) 🎯 MVP Core UI

**Goal**: Users see main medication list sorted by next dose time, showing countdown timers, overdue status, medication details, and action buttons

**Independent Test**: Add multiple medications with different schedules, open app, verify list displays correct order (soonest first), countdown timers update, overdue medications show red highlighting

**Why Second**: This is the main screen users see when opening the app; provides essential overview of all medications

### Tests for User Story 3 (OPTIONAL ⚠️)

- [ ] T063 ⚠️ [P] [US3] Unit test for MainViewModel in app/src/test/java/com/medchecktag/viewmodels/MainViewModelTest.java (test medication list sorting, status calculations)
- [ ] T064 ⚠️ [P] [US3] Unit test for TimeUtils in app/src/test/java/com/medchecktag/utils/TimeUtilsTest.java (test countdown formatting, overdue detection)
- [ ] T065 ⚠️ [P] [US3] Instrumented UI test for main list in app/src/androidTest/java/com/medchecktag/ui/MainListTest.java (test list display, sorting, button interactions)

### Implementation for User Story 3

#### ViewModel Layer

- [X] T066 [US3] Create MainViewModel in app/src/main/java/com/medchecktag/viewmodels/MainViewModel.java (observe active medications LiveData, calculate countdown timers, handle refill button)

#### UI Layer

- [X] T067 [US3] Create activity_main.xml in app/src/main/res/layout/activity_main.xml (RecyclerView for medication list, bottom navigation with Settings/Read Tag/Add Medication buttons)
- [X] T068 [US3] Create medication_list_item.xml in app/src/main/res/layout/medication_list_item.xml (countdown timer TextView, nickname, dose, remaining doses, "Med Refilled" button, "Info" button, "Write Tag" button)
- [X] T069 [US3] Create MainActivity in app/src/main/java/com/medchecktag/ui/main/MainActivity.java (setup RecyclerView, observe MainViewModel LiveData, handle bottom navigation)
- [X] T070 [US3] Create MedicationListAdapter in app/src/main/java/com/medchecktag/ui/main/MedicationListAdapter.java (RecyclerView adapter with ViewHolder, bind medication data, handle button clicks)
- [X] T071 [US3] Create MedicationViewHolder in app/src/main/java/com/medchecktag/ui/main/MedicationViewHolder.java (bind medication to list item views, format countdown timer, apply overdue styling)

#### Countdown Timer Logic

- [X] T072 [US3] Implement countdown timer updates in MedicationListAdapter (use Handler/Runnable or CountDownTimer to update every minute)
- [X] T073 [US3] Implement overdue detection and styling in MedicationViewHolder (check if current time > nextDoseTime, apply red background, show "Overdue" text)
- [X] T074 [US3] Implement inactive medication styling in MedicationViewHolder (gray out inactive entries, move to bottom of list)

#### "Med Refilled" Button Logic

- [X] T075 [US3] Implement "Med Refilled" button handler in MainActivity (call repository.updateRemainingDoses() with maxDoseCount, clear refill alarms)

#### Navigation

- [X] T076 [US3] Implement "Info" button navigation in MedicationListAdapter (launch MedicationInfoActivity with medicationId extra)
- [X] T077 [US3] Implement "Write Tag" button navigation in MedicationListAdapter (launch NFCWriteDialogFragment with medicationId)
- [X] T078 [US3] Implement "Add Medication" button in MainActivity bottom nav (launch AddMedicationActivity)
- [X] T079 [US3] Set MainActivity as launcher activity in app/src/main/AndroidManifest.xml (MAIN action, LAUNCHER category)

#### Resources

- [X] T080 [US3] Create string resources for User Story 3 in app/src/main/res/values/strings.xml (button labels, overdue text, time format strings)
- [X] T081 [US3] Create color resources in app/src/main/res/values/colors.xml (overdue red, inactive gray, category colors)
- [X] T082 [US3] Create drawable for overdue indicator in app/src/main/res/drawable/overdue_background.xml (red rounded rectangle)

**Checkpoint**: At this point, users can view all medications in a sorted list with live countdown timers and quick actions. Main UI is functional.

---

## Phase 5: User Story 1 - NFC Scan & Dose Confirmation (Priority: P1) 🎯 MVP Core Feature

**Goal**: Users tap phone on NFC tag, app reads medication ID, provides audio confirmation (name, dose, next dose time), updates dose history, decrements remaining doses, resets timer

**Independent Test**: Add medication, write NFC tag, set alarm, scan tag, verify audio confirmation plays, dose history is logged, remaining doses decremented, timer reset to next dose

**Why Third**: This is the core safety feature; requires medications to exist (US2) and list UI to display updates (US3)

### Tests for User Story 1 (OPTIONAL ⚠️)

- [ ] T083 ⚠️ [P] [US1] Unit test for NFCHandler read operations in app/src/test/java/com/medchecktag/nfc/NFCHandlerTest.java (test UUID parsing, format validation)
- [ ] T084 ⚠️ [P] [US1] Unit test for AudioFeedbackService in app/src/test/java/com/medchecktag/audio/AudioFeedbackServiceTest.java (test speech text formatting, mock TTS)
- [ ] T085 ⚠️ [P] [US1] Unit test for DoseConfirmationViewModel in app/src/test/java/com/medchecktag/viewmodels/DoseConfirmationViewModelTest.java (test dose confirmation logic, next dose calculation)
- [ ] T086 ⚠️ [P] [US1] Instrumented test for NFC scan flow in app/src/androidTest/java/com/medchecktag/nfc/NFCScanFlowTest.java (test tag discovery intent, read operation, UI updates)

### Implementation for User Story 1

#### ViewModel Layer

- [X] T087 [US1] Create DoseConfirmationViewModel in app/src/main/java/com/medchecktag/viewmodels/DoseConfirmationViewModel.java (handle dose confirmation, create DoseRecord, update medication, trigger audio)

#### NFC Tag Discovery

- [X] T088 [US1] Update MainActivity to handle NFC tag discovery intents in onNewIntent() (TAG_DISCOVERED, NDEF_DISCOVERED actions)
- [X] T089 [US1] Enable foreground dispatch in MainActivity onResume() (NfcAdapter.enableForegroundDispatch)
- [X] T090 [US1] Disable foreground dispatch in MainActivity onPause() (NfcAdapter.disableForegroundDispatch)
- [X] T091 [US1] Add NFC intent filters to MainActivity in app/src/main/AndroidManifest.xml (TAG_DISCOVERED, NDEF_DISCOVERED actions)

#### NFC Read Logic

- [X] T092 [US1] Implement tag read handler in MainActivity (call NFCHandler.readMedicationIdWithRetry(), handle exceptions, display errors)
- [X] T093 [US1] Implement medication lookup by tag ID in MainActivity (call MedicationRepository.getMedicationByTagId())
- [X] T094 [US1] Handle unrecognized tag error in MainActivity (display "Tag not recognized" message, speak error via AudioFeedbackService)
- [X] T095 [US1] Handle tag read failure in MainActivity (display retry prompt, speak error message)

#### Dose Confirmation Logic

- [X] T096 [US1] Check if dose is due in DoseConfirmationViewModel (compare current time vs nextDoseTime, warn if too early per FR-020)
- [X] T097 [US1] Create DoseRecord in DoseConfirmationViewModel (status=TAKEN, confirmationMethod=NFC_SCAN, actualTime=now)
- [X] T098 [US1] Decrement remaining doses in DoseConfirmationViewModel (call MedicationRepository.updateRemainingDoses())
- [X] T099 [US1] Calculate next dose time in DoseConfirmationViewModel (use TimeUtils based on schedule type, update medication)
- [X] T100 [US1] Update medication in database with new nextDoseTime and remainingDoses

#### Audio Confirmation

- [X] T101 [US1] Format audio confirmation message in DoseConfirmationViewModel (use template: "[Medication], [Dose]. Next dose in [X hours/minutes]")
- [X] T102 [US1] Trigger audio feedback in MainActivity after successful scan (call AudioFeedbackService.speakMedicationConfirmation())
- [X] T103 [US1] Implement number-to-text conversion in AudioFeedbackService (convert 120 → "one hundred twenty minutes" or "two hours")
- [X] T104 [US1] Handle TTS initialization delay in AudioFeedbackService (queue speech if not ready, speak when initialized)

#### UI Feedback

- [X] T105 [US1] Create nfc_scan_success_dialog.xml in app/src/main/res/layout/nfc_scan_success_dialog.xml (show medication name, dose, next dose time, dismiss button)
- [X] T106 [US1] Create NFCScanSuccessDialogFragment in app/src/main/java/com/medchecktag/ui/nfc/NFCScanSuccessDialogFragment.java (display confirmation details, auto-dismiss after 3 seconds)
- [X] T107 [US1] Show success dialog in MainActivity after scan (launch NFCScanSuccessDialogFragment)
- [X] T108 [US1] Update medication list in MainActivity after scan (RecyclerView auto-updates via LiveData observation)

#### Resources

- [X] T109 [US1] Create string resources for User Story 1 in app/src/main/res/values/strings.xml (audio templates, error messages, confirmation text)
- [X] T110 [US1] Create NFC error drawable in app/src/main/res/drawable/ic_nfc_error.xml (vector icon for error states)

**Checkpoint**: At this point, users can scan NFC tags to confirm doses with audio feedback. Core medication tracking is fully functional. **MVP COMPLETE** - Stories 1, 2, 3 deliver minimum viable product.

---

## Phase 6: User Story 4 - Scheduled Alarms & Notifications (Priority: P2)

**Goal**: Users configure alarm preferences (pre-dose, on-time, post-dose) per medication with category-based sounds. Alarms trigger at configured times, display notifications, support NFC scan or manual confirmation

**Independent Test**: Add medication with alarm configuration, wait for alarm time, verify alarm triggers with correct sound, notification appears, can confirm via NFC scan or manual buttons

### Tests for User Story 4 (OPTIONAL ⚠️)

- [ ] T111 ⚠️ [P] [US4] Unit test for AlarmScheduler in app/src/test/java/com/medchecktag/alarms/AlarmSchedulerTest.java (test alarm time calculations, request code generation, mock AlarmManager)
- [ ] T112 ⚠️ [P] [US4] Unit test for AlarmReceiver in app/src/test/java/com/medchecktag/alarms/AlarmReceiverTest.java (test alarm handling logic, notification creation)
- [ ] T113 ⚠️ [P] [US4] Instrumented test for alarm scheduling in app/src/androidTest/java/com/medchecktag/alarms/AlarmSchedulingTest.java (test AlarmManager integration, verify alarms set)

### Implementation for User Story 4

#### Alarm Configuration UI

- [ ] T114 [US4] Create alarm_configuration_fragment.xml in app/src/main/res/layout/alarm_configuration_fragment.xml (toggles for pre/on-time/post alarms, minute inputs, sound selector)
- [ ] T115 [US4] Create AlarmConfigurationFragment in app/src/main/java/com/medchecktag/ui/medication/AlarmConfigurationFragment.java (embed in AddMedicationActivity, bind to AlarmConfiguration object)
- [ ] T116 [US4] Update AddMedicationActivity to include AlarmConfigurationFragment (integrate alarm settings into medication creation flow)

#### Alarm Scheduling Logic

- [X] T117 [US4] Implement scheduleAlarmsForMedication() in AlarmScheduler (calculate trigger times for pre/on-time/post alarms based on nextDoseTime)
- [X] T118 [US4] Generate unique request codes in AlarmScheduler (hash medicationId + alarmType to ensure uniqueness per alarm)
- [X] T119 [US4] Set exact alarms using AlarmManager.setExactAndAllowWhileIdle() in AlarmScheduler (handle API 31+ SCHEDULE_EXACT_ALARM permission)
- [X] T120 [US4] Schedule alarms after medication creation in AddMedicationViewModel (call AlarmScheduler.scheduleAlarmsForMedication())
- [X] T121 [US4] Reschedule alarms after dose confirmation in DoseConfirmationViewModel (call AlarmScheduler after updating nextDoseTime)

#### Alarm Receiver Logic

- [X] T122 [US4] Implement onReceive() in AlarmReceiver (extract medicationId and alarmType from intent extras, query medication)
- [X] T123 [US4] Determine alarm action in AlarmReceiver (check alarmType: PRE_DOSE → notification; ON_TIME → notification + audio; POST_DOSE → persistent notification)
- [X] T124 [US4] Create notification channels in AlarmReceiver (three channels: PRE_DOSE, ON_TIME, POST_DOSE with different priorities)
- [X] T125 [US4] Build notification in AlarmReceiver using NotificationUtils (title: medication name, text: dose info, actions: Open App, Confirm, Skip)
- [X] T126 [US4] Add notification actions in AlarmReceiver (PendingIntents for "Confirm med taken" and "Medication skipped" buttons)
- [X] T127 [US4] Play alarm sound in AlarmReceiver (MediaPlayer for category-based sound, respect volume settings)
- [X] T128 [US4] Handle manual confirmation from notification in AlarmReceiver (create DoseRecord with MANUAL_CONFIRM, update medication, cancel alarm)
- [X] T129 [US4] Handle "Medication skipped" from notification in AlarmReceiver (create DoseRecord with SKIPPED status, reset timer, cancel alarm)

#### Alarm Queueing

- [X] T130 [US4] Implement simultaneous alarm queueing in AlarmReceiver (detect multiple active alarms, show one notification at a time, queue others)
- [X] T131 [US4] Prioritize alarms by category in AlarmReceiver (LIFE_DEPENDENT > VERY_IMPORTANT > BENEFICIAL when queueing)

#### Boot Receiver

- [X] T132 [US4] Implement onReceive() in BootReceiver (query all active medications, reschedule alarms for each)
- [X] T133 [US4] Request RECEIVE_BOOT_COMPLETED permission in app/src/main/AndroidManifest.xml

#### Settings Integration

- [X] T134 [US4] Load default alarm configuration from AppSettings when creating new medication in AddMedicationActivity

#### Resources

- [X] T135 [US4] Add default alarm sounds to app/src/main/res/raw/ (life_dependent_alarm.mp3, very_important_alarm.mp3, beneficial_alarm.mp3)
- [X] T136 [US4] Create string resources for User Story 4 in app/src/main/res/values/strings.xml (notification titles, action labels, alarm descriptions)
- [ ] T137 [US4] Create notification icon in app/src/main/res/drawable/ic_notification_pill.xml (vector icon for notifications)

**Checkpoint**: At this point, scheduled alarms trigger at correct times with notifications. Users can confirm doses via NFC or manual buttons.

---

## Phase 7: User Story 5 - Low Dose Notifications & Refill Reminders (Priority: P2)

**Goal**: Users configure two refill thresholds per medication. When remaining doses drop below threshold1, send notification. When below threshold2, schedule alarm at configured time.

**Independent Test**: Add medication with refillThreshold1=7, refillThreshold2=3. Confirm doses until remainingDoses=7, verify notification sent. Continue to 3 doses, verify refill alarm scheduled for configured time.

### Tests for User Story 5 (OPTIONAL ⚠️)

- [ ] T138 ⚠️ [P] [US5] Unit test for refill threshold detection in app/src/test/java/com/medchecktag/viewmodels/DoseConfirmationViewModelTest.java (test threshold comparison logic)
- [ ] T139 ⚠️ [P] [US5] Unit test for refill alarm scheduling in app/src/test/java/com/medchecktag/alarms/AlarmSchedulerTest.java (test scheduleRefillAlarm() logic)
- [ ] T140 ⚠️ [P] [US5] Instrumented test for refill notifications in app/src/androidTest/java/com/medchecktag/alarms/RefillReminderTest.java (test notification delivery, alarm scheduling)

### Implementation for User Story 5

#### Refill Threshold Detection

- [X] T141 [US5] Check refillThreshold1 in DoseConfirmationViewModel after decrementing doses (if remainingDoses == refillThreshold1, trigger notification)
- [X] T142 [US5] Check refillThreshold2 in DoseConfirmationViewModel after decrementing doses (if remainingDoses == refillThreshold2, schedule refill alarm)
- [X] T143 [US5] Send notification for threshold1 in DoseConfirmationViewModel (use NotificationUtils to create low-dose notification)

#### Refill Alarm Scheduling

- [X] T144 [US5] Implement scheduleRefillAlarm() in AlarmScheduler (calculate next configured refill reminder time, e.g., 9 AM next day)
- [X] T145 [US5] Handle REFILL_REMINDER alarm type in AlarmReceiver (create persistent notification, speak refill reminder via AudioFeedbackService)
- [X] T146 [US5] Create refill notification in AlarmReceiver (title: "Time to refill [medication]", text: "Only [X] doses remaining", actions: Open App, Mark Refilled)

#### "Med Refilled" Button Logic

- [X] T147 [US5] Clear refill notifications and alarms in MainActivity when "Med Refilled" button pressed (call AlarmScheduler.cancelAlarm(medicationId, REFILL_REMINDER))
- [X] T148 [US5] Reset tracking flags in MedicationRepository after refill (clear any "refill alarm triggered" state)

#### Settings Integration

- [X] T149 [US5] Load default refill thresholds from AppSettings in AddMedicationActivity (populate refillThreshold1 and refillThreshold2 based on category defaults)

#### UI for Refill Thresholds

- [X] T150 [US5] Add refill threshold inputs to add_medication_layout.xml (two number input fields: "Notification at X doses" and "Alarm at X doses")
- [X] T151 [US5] Validate refillThreshold1 > refillThreshold2 in AddMedicationViewModel (add validation rule, show error if invalid)

#### Resources

- [X] T152 [US5] Create string resources for User Story 5 in app/src/main/res/values/strings.xml (notification texts, threshold labels, refill confirmation)
- [X] T153 [US5] Create refill icon in app/src/main/res/drawable/ic_refill.xml (vector icon for refill notifications)

**Checkpoint**: At this point, users receive proactive refill reminders at two threshold levels. Medication stockouts are prevented.

---

## Phase 8: User Story 10 - Read Tag Quick Access (Priority: P2)

**Goal**: Users tap "Read Tag" button from main menu, scan any medication tag, navigate directly to that medication's Info screen

**Independent Test**: Tap "Read Tag" from main menu, scan tag, verify navigation to correct medication Info screen with all details displayed

### Tests for User Story 10 (OPTIONAL ⚠️)

- [ ] T154 ⚠️ [P] [US10] Instrumented UI test for Read Tag flow in app/src/androidTest/java/com/medchecktag/ui/ReadTagFlowTest.java (test button click, modal display, scan, navigation)

### Implementation for User Story 10

#### Read Tag Modal

- [X] T155 [US10] Create nfc_read_dialog.xml in app/src/main/res/layout/nfc_read_dialog.xml (fullscreen modal with "Scan tag now" text, cancel button)
- [X] T156 [US10] Create NFCReadDialogFragment in app/src/main/java/com/medchecktag/ui/nfc/NFCReadDialogFragment.java (handle NFC read mode, display status, navigate on success)

#### Read Tag Button

- [X] T157 [US10] Implement "Read Tag" button in MainActivity bottom navigation (launch NFCReadDialogFragment)
- [X] T158 [US10] Enable foreground dispatch in NFCReadDialogFragment onResume() (capture NFC intents)
- [X] T159 [US10] Handle tag scan in NFCReadDialogFragment onNewIntent() (call NFCHandler.readMedicationId(), query medication, navigate to Info screen)
- [X] T160 [US10] Handle unrecognized tag in NFCReadDialogFragment (show "Tag not found in system" error, keep modal open for retry)
- [X] T161 [US10] Implement Cancel button in NFCReadDialogFragment (dismiss modal, return to main menu)

#### Navigation

- [X] T162 [US10] Create MedicationInfoActivity in app/src/main/java/com/medchecktag/ui/medication/MedicationInfoActivity.java (display full medication details, accept medicationId as intent extra)
- [X] T163 [US10] Create activity_medication_info.xml in app/src/main/res/layout/activity_medication_info.xml (display nickname, dose, category, schedule, remaining doses, next dose time, Edit/Delete/Write Tag buttons)
- [X] T164 [US10] Create MedicationInfoViewModel in app/src/main/java/com/medchecktag/viewmodels/MedicationInfoViewModel.java (load medication by ID, observe LiveData)
- [X] T165 [US10] Navigate to MedicationInfoActivity from NFCReadDialogFragment on successful scan (pass medicationId as intent extra)
- [X] T166 [US10] Add MedicationInfoActivity entry to app/src/main/AndroidManifest.xml

#### Resources

- [X] T167 [US10] Create string resources for User Story 10 in app/src/main/res/values/strings.xml (modal text, error messages)

**Checkpoint**: At this point, users can quickly access medication details by scanning any tag from the main menu.

---

## Phase 9: User Story 7 - Write to NFC Tag (Priority: P3)

**Goal**: Users can write medication ID to new NFC tags from medication Info screen (for replacing lost/damaged tags or creating backups)

**Independent Test**: Create medication, navigate to Info screen, tap "Write Tag", write to multiple blank tags, verify all tags correctly identify the same medication when scanned

### Tests for User Story 7 (OPTIONAL ⚠️)

- [ ] T168 ⚠️ [P] [US7] Unit test for NFCHandler write operations in app/src/test/java/com/medchecktag/nfc/NFCHandlerTest.java (test NDEF write, verification, overwrite detection)
- [ ] T169 ⚠️ [P] [US7] Instrumented test for NFC write flow in app/src/androidTest/java/com/medchecktag/nfc/NFCWriteFlowTest.java (test write operation, tag linking, verification)

### Implementation for User Story 7

#### Write Tag Button

- [X] T170 [US7] Add "Write Tag" button to activity_medication_info.xml (FloatingActionButton or menu item)
- [X] T171 [US7] Implement "Write Tag" button handler in MedicationInfoActivity (launch NFCWriteDialogFragment with medicationId)

#### Write Tag Logic (Already Implemented in Phase 3, Enhance Here)

- [X] T172 [US7] Detect tag already in use in NFCWriteDialogFragment (query NFCTagRepository before writing)
- [X] T173 [US7] Show overwrite warning in NFCWriteDialogFragment (dialog: "Tag already assigned to [other medication]. Overwrite?")
- [X] T174 [US7] Handle overwrite confirmation in NFCWriteDialogFragment (if confirmed, unlink tag from old medication, write new ID, link to current medication)
- [X] T175 [US7] Handle write failure in NFCWriteDialogFragment (show "Write failed, please try again or use different tag" with retry button)
- [X] T176 [US7] Verify write integrity in NFCWriteDialogFragment (read back tag after write, compare UUID, show error if mismatch)

#### Multi-Tag Support

- [X] T177 [US7] Link multiple tags to same medication in NFCWriteDialogFragment (create NFCTag entry with medicationId foreign key)
- [X] T178 [US7] Display linked tags in MedicationInfoActivity (RecyclerView showing all tags for this medication, option to unlink)
- [X] T179 [US7] Implement tag unlinking in MedicationInfoActivity (delete NFCTag entry from database, confirmation dialog)

#### Resources

- [X] T180 [US7] Create string resources for User Story 7 in app/src/main/res/values/strings.xml (write prompts, overwrite warnings, success messages)

**Checkpoint**: At this point, users can write to multiple NFC tags per medication and manage tag associations.

---

## Phase 10: User Story 9 - Edit & Delete Medications (Priority: P3)

**Goal**: Users can edit medication details from Info screen and delete medications (with option to save as inactive for history preservation)

**Independent Test**: Create medication, edit its nickname and schedule, verify changes persist. Delete medication, choose "save as inactive", verify it appears grayed out at bottom of list with alarms disabled.

### Tests for User Story 9 (OPTIONAL ⚠️)

- [ ] T181 ⚠️ [P] [US9] Unit test for medication update logic in app/src/test/java/com/medchecktag/repositories/MedicationRepositoryTest.java (test updateMedication() validation)
- [ ] T182 ⚠️ [P] [US9] Unit test for inactive medication logic in app/src/test/java/com/medchecktag/viewmodels/MainViewModelTest.java (test isActive filtering, alarm cancellation)
- [ ] T183 ⚠️ [P] [US9] Instrumented UI test for edit flow in app/src/androidTest/java/com/medchecktag/ui/EditMedicationFlowTest.java (test edit screen, save changes, verify updates)
- [ ] T184 ⚠️ [P] [US9] Instrumented UI test for delete flow in app/src/androidTest/java/com/medchecktag/ui/DeleteMedicationFlowTest.java (test delete dialog, inactive option, verification)

### Implementation for User Story 9

#### Edit Functionality

- [X] T185 [US9] Create EditMedicationActivity in app/src/main/java/com/medchecktag/ui/medication/EditMedicationActivity.java (reuse AddMedicationActivity layout, pre-fill fields)
- [X] T186 [US9] Create EditMedicationViewModel in app/src/main/java/com/medchecktag/viewmodels/EditMedicationViewModel.java (load medication by ID, handle updates, validation)
- [X] T187 [US9] Implement "Edit" button in MedicationInfoActivity (launch EditMedicationActivity with medicationId)
- [X] T188 [US9] Pre-fill form fields in EditMedicationActivity (load medication from ViewModel, populate all inputs)
- [X] T189 [US9] Handle schedule type changes in EditMedicationActivity (allow switching between INTERVAL and SPECIFIC_TIMES)
- [X] T190 [US9] Save edited medication in EditMedicationViewModel (validate all fields, call MedicationRepository.updateMedication())
- [X] T191 [US9] Reschedule alarms after edit in EditMedicationViewModel (cancel old alarms, schedule new ones with updated schedule)
- [X] T192 [US9] Add EditMedicationActivity entry to app/src/main/AndroidManifest.xml

#### Delete Functionality

- [X] T193 [US9] Implement "Delete" button in MedicationInfoActivity (show confirmation dialog)
- [X] T194 [US9] Create delete confirmation dialog (AlertDialog: "Delete [medication]?" with Confirm/Cancel buttons)
- [X] T195 [US9] Show inactive option dialog after delete confirmation (AlertDialog: "Save as inactive? (Keeps history but stops alarms)" with Yes/No buttons)
- [X] T196 [US9] Handle "Save as inactive" in MedicationInfoViewModel (call MedicationRepository.markMedicationInactive(), cancel all alarms)
- [X] T197 [US9] Handle "Permanent delete" in MedicationInfoViewModel (call MedicationRepository.deleteMedication(), cascade delete DoseRecords and NFCTags)
- [X] T198 [US9] Cancel alarms for deleted medication in MedicationInfoViewModel (call AlarmScheduler.cancelAllAlarmsForMedication())
- [X] T199 [US9] Navigate back to MainActivity after delete (finish MedicationInfoActivity)

#### Inactive Medication Display

- [X] T200 [US9] Filter inactive medications in MainViewModel (query getActiveMedicationsSortedByNextDose() for main list, separate query for inactive)
- [X] T201 [US9] Display inactive medications at bottom of list in MedicationListAdapter (append to end, apply gray styling)
- [X] T202 [US9] Apply inactive styling in MedicationViewHolder (reduce opacity, gray text, show "Inactive" label)

#### Resources

- [X] T203 [US9] Create string resources for User Story 9 in app/src/main/res/values/strings.xml (confirmation dialogs, inactive labels)

**Checkpoint**: At this point, users can edit medication details and delete medications with history preservation option.

---

## Phase 11: User Story 8 - Settings & Customization (Priority: P3)

**Goal**: Users access Settings screen to configure global preferences: alarm sounds per category, master volume, emergency contacts, refill threshold defaults, manual confirmation toggle

**Independent Test**: Open Settings, change Life Dependent alarm sound, adjust volume, set default refill thresholds, enable manual confirmation. Create new medication, verify it uses new defaults. Trigger alarm, verify it plays new sound at new volume.

### Tests for User Story 8 (OPTIONAL ⚠️)

- [ ] T204 ⚠️ [P] [US8] Unit test for AppSettingsRepository in app/src/test/java/com/medchecktag/repositories/AppSettingsRepositoryTest.java (test singleton pattern, default values)
- [ ] T205 ⚠️ [P] [US8] Instrumented test for Settings screen in app/src/androidTest/java/com/medchecktag/ui/SettingsActivityTest.java (test preference changes, persistence)

### Implementation for User Story 8

#### Settings UI

- [X] T206 [US8] Create SettingsActivity in app/src/main/java/com/medchecktag/ui/settings/SettingsActivity.java (host PreferenceFragmentCompat)
- [X] T207 [US8] Create preferences.xml in app/src/main/res/xml/preferences.xml (define all settings: alarm sounds, volume, refill thresholds, manual confirmation, emergency contacts)
- [X] T208 [US8] Create SettingsFragment in app/src/main/java/com/medchecktag/ui/settings/SettingsFragment.java (extend PreferenceFragmentCompat, bind to AppSettings)
- [X] T209 [US8] Create SettingsViewModel in app/src/main/java/com/medchecktag/viewmodels/SettingsViewModel.java (load and save AppSettings singleton)

#### Settings Categories

- [X] T210 [US8] Add master volume slider in preferences.xml (SeekBarPreference, range 0-100, default 80)
- [X] T211 [US8] Add alarm sound pickers in preferences.xml (three RingtonePreference entries for LIFE_DEPENDENT, VERY_IMPORTANT, BENEFICIAL categories)
- [X] T212 [US8] Add refill threshold defaults in preferences.xml (six number inputs: threshold1 and threshold2 for each category)
- [X] T213 [US8] Add manual confirmation toggle in preferences.xml (SwitchPreference, default OFF)
- [X] T214 [US8] Add emergency contact fields in preferences.xml (EditTextPreference for email, phone)

#### Settings Persistence

- [X] T215 [US8] Initialize AppSettings singleton on first launch in MainActivity (create default AppSettings entry if none exists)
- [X] T216 [US8] Save setting changes in SettingsFragment (observe preference changes, update AppSettings in database via repository)
- [X] T217 [US8] Load current settings in SettingsFragment onCreate() (query AppSettings from repository, update preference UI)

#### Apply Settings

- [X] T218 [US8] Apply volume setting in AlarmReceiver (read AppSettings.masterVolume, apply to MediaPlayer)
- [X] T219 [US8] Apply alarm sound setting in AlarmReceiver (read category-specific sound from AppSettings, fallback to default)
- [X] T220 [US8] Apply manual confirmation setting in AlarmReceiver (if enabled, show manual buttons; if disabled, hide buttons, require NFC)
- [X] T221 [US8] Apply refill threshold defaults in AddMedicationActivity (pre-fill refillThreshold1 and refillThreshold2 based on selected category)

#### Navigation

- [X] T222 [US8] Implement "Settings" button in MainActivity bottom navigation (launch SettingsActivity)
- [X] T223 [US8] Add SettingsActivity entry to app/src/main/AndroidManifest.xml (parentActivityName = MainActivity for up navigation)

#### Resources

- [X] T224 [US8] Create string resources for User Story 8 in app/src/main/res/values/strings.xml (setting titles, descriptions, category names)
- [X] T225 [US8] Create settings icon in app/src/main/res/drawable/ic_settings.xml (vector icon for settings button)

**Checkpoint**: At this point, users can customize alarm sounds, volume, refill defaults, and confirmation mode. Settings persist across app restarts.

---

## Phase 12: User Story 6 - Emergency Contact Notifications (Priority: P3)

**Goal**: Users configure emergency contacts (email/SMS/call) per medication or category. When critical medication is missed/skipped, send notification to contact with medication name, status, GPS location, missed dose count.

**Independent Test**: Configure emergency contact for Life Dependent category, add Life Dependent medication, simulate missed dose, verify email/SMS sent with correct information (medication name, missed status, location, count).

**Note**: This story has dependencies on external services (email/SMS/call) which require additional setup and permissions.

### Tests for User Story 6 (OPTIONAL ⚠️)

- [ ] T226 ⚠️ [P] [US6] Unit test for emergency notification logic in app/src/test/java/com/medchecktag/services/EmergencyNotificationServiceTest.java (test threshold detection, contact selection, message formatting)
- [ ] T227 ⚠️ [P] [US6] Unit test for missed dose tracking in app/src/test/java/com/medchecktag/repositories/DoseRecordRepositoryTest.java (test consecutive missed dose count)

### Implementation for User Story 6

#### Emergency Contact Model & Repository (Already in Phase 2)

- [X] T228 [US6] Add methods to EmergencyContactRepository for querying contacts by category/medication (implement in EmergencyContactRepository.java)

#### Emergency Notification Service

- [X] T229 [US6] Create EmergencyNotificationService in app/src/main/java/com/medchecktag/services/EmergencyNotificationService.java (handle email/SMS/call notifications)
- [X] T230 [US6] Implement email notification in EmergencyNotificationService (use JavaMail or Android Intent.ACTION_SENDTO for email)
- [X] T231 [US6] Implement SMS notification in EmergencyNotificationService (use SmsManager.sendTextMessage() with SEND_SMS permission)
- [X] T232 [US6] Implement call notification in EmergencyNotificationService (use Intent.ACTION_CALL with CALL_PHONE permission)

#### Missed Dose Detection

- [X] T233 [US6] Create background worker for missed dose detection (WorkManager PeriodicWorkRequest, runs every 15 minutes)
- [X] T234 [US6] Create MissedDoseWorker in app/src/main/java/com/medchecktag/services/MissedDoseWorker.java (query medications with nextDoseTime < currentTime && no recent DoseRecord)
- [X] T235 [US6] Create missed DoseRecord entries in MissedDoseWorker (status=MISSED, scheduledTime=nextDoseTime, actualTime=null)
- [X] T236 [US6] Count consecutive missed doses in MissedDoseWorker (query DoseRecords for medication, count recent MISSED entries)

#### Emergency Trigger Logic

- [X] T237 [US6] Check emergency threshold in MissedDoseWorker (query EmergencyContact for medication/category, compare missed count to threshold)
- [X] T238 [US6] Trigger emergency notification in MissedDoseWorker (call EmergencyNotificationService if threshold exceeded)
- [X] T239 [US6] Get GPS location in MissedDoseWorker (use LocationManager, require ACCESS_FINE_LOCATION permission, handle permission denial gracefully)
- [X] T240 [US6] Format emergency message in EmergencyNotificationService (template: "[Medication] missed. [Count] missed doses. Location: [GPS or 'unavailable']")

#### Emergency Contact UI

- [X] T241 [US6] Create emergency_contact_fragment.xml in app/src/main/res/layout/emergency_contact_fragment.xml (contact name, email, phone, notification methods checkboxes, trigger threshold)
- [X] T242 [US6] Create EmergencyContactFragment in app/src/main/java/com/medchecktag/ui/medication/EmergencyContactFragment.java (embed in AddMedicationActivity or SettingsActivity)
- [X] T243 [US6] Add emergency contact management to SettingsActivity (list of emergency contacts with add/edit/delete)
- [X] T244 [US6] Link emergency contacts to medications in AddMedicationActivity (select contacts from list, create many-to-many associations)

#### Permissions

- [X] T245 [US6] Add SMS permissions to app/src/main/AndroidManifest.xml (SEND_SMS)
- [X] T246 [US6] Add call permissions to app/src/main/AndroidManifest.xml (CALL_PHONE)
- [X] T247 [US6] Add location permissions to app/src/main/AndroidManifest.xml (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
- [X] T248 [US6] Request runtime permissions in MainActivity (ActivityCompat.requestPermissions for SMS, CALL_PHONE, LOCATION)
- [X] T249 [US6] Handle permission denial gracefully in EmergencyNotificationService (show user notification, log failure, retry later)

#### Resources

- [X] T250 [US6] Create string resources for User Story 6 in app/src/main/res/values/strings.xml (emergency message templates, permission rationales)

**Checkpoint**: At this point, emergency contacts receive notifications when critical medications are missed. Provides caregiver safety net.

---

## Phase 13: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories, final testing, documentation

- [X] T251 [P] Add app icon in app/src/main/res/mipmap/ (launcher icon with pill/NFC imagery, multiple densities)
- [X] T252 [P] Create splash screen in app/src/main/res/drawable/splash_background.xml (Material Design 12+ splash screen API)
- [X] T253 [P] Add animations to app/src/main/res/anim/ (fade transitions, slide animations for dialogs)
- [X] T254 [P] Add haptic feedback to NFC operations (vibrate on successful scan/write using Vibrator service)
- [X] T255 [P] Implement dark theme support in app/src/main/res/values-night/ (colors.xml, styles.xml)
- [X] T256 [P] Add accessibility improvements (content descriptions for all ImageViews, TalkBack support, high contrast mode)
- [X] T257 [P] Optimize database queries (add missing indexes per data-model.md, analyze query performance)
- [X] T258 [P] Implement error logging framework (Timber or custom logger, no sensitive medication data in logs per constitution)
- [X] T259 [P] Add ProGuard optimization for release builds (R8 full mode, test APK size < 10MB target)
- [X] T260 [P] Create user documentation in docs/ (setup guide, troubleshooting, FAQ)
- [ ] T261 [P] ⚠️ Add additional unit tests for edge cases (test validation boundary conditions, timezone handling, alarm queueing)
- [ ] T262 [P] ⚠️ Add end-to-end integration tests (test complete user journeys: add med → scan → confirm → alarm)
- [ ] T263 [P] Run [quickstart.md](quickstart.md) validation (follow quickstart guide, verify all steps work for new developer)
- [X] T264 Perform code review and refactoring (extract common logic, reduce duplication, improve naming)
- [ ] T265 Test on multiple devices (test on API 21, 28, 34; test with/without NFC hardware)
- [ ] T266 Performance profiling (use Android Profiler, ensure <2s startup, <100MB memory, 60 fps UI)
- [X] T267 Security audit (verify no medication data in logs, validate NFC UUID-only storage, check permission usage)
- [ ] T268 Prepare release build (sign APK, generate release notes, test on clean device)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 2 (Phase 3)**: Depends on Foundational completion - Must come first (provides medications for all other stories)
- **User Story 3 (Phase 4)**: Depends on US2 completion - Displays medications created in US2
- **User Story 1 (Phase 5)**: Depends on US2 and US3 completion - Requires medications to exist and list UI to display updates
- **User Story 4 (Phase 6)**: Depends on US1/US2/US3 completion - Alarms need medication tracking to be functional
- **User Story 5 (Phase 7)**: Depends on US1/US2/US3/US4 completion - Builds on dose tracking and alarm infrastructure
- **User Story 10 (Phase 8)**: Depends on US2 completion and MedicationInfoActivity (Phase 10+) - Requires Info screen to navigate to
- **User Story 7 (Phase 9)**: Depends on US2 completion - Reuses NFC write infrastructure from Add Medication
- **User Story 9 (Phase 10)**: Depends on US2/US3 completion - Requires medication CRUD infrastructure
- **User Story 8 (Phase 11)**: Depends on all core infrastructure (Phase 2) - Settings affect all user stories
- **User Story 6 (Phase 12)**: Depends on US4/US5 completion - Builds on alarm and dose tracking
- **Polish (Phase 13)**: Depends on all desired user stories being complete

### User Story Dependencies

**MVP (P1 Stories)**:
1. User Story 2 (Add Medication) → foundational for all other stories
2. User Story 3 (Medication List) → requires US2
3. User Story 1 (NFC Scan) → requires US2 and US3

**MVP COMPLETE** after Phase 5 - delivers core medication tracking with NFC confirmation

**P2 Stories** (can proceed in any order after MVP):
- User Story 4 (Alarms) → independent after MVP
- User Story 5 (Refill Reminders) → builds on US4
- User Story 10 (Read Tag) → independent after MVP (but needs MedicationInfoActivity from US9)

**P3 Stories** (can proceed in any order after P1/P2):
- User Story 7 (Write Tag) → independent after US2
- User Story 9 (Edit/Delete) → independent after US2/US3
- User Story 8 (Settings) → independent after Phase 2
- User Story 6 (Emergency Contacts) → builds on US4/US5

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Models before DAOs before repositories
- Repositories before ViewModels
- ViewModels before UI
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

**Setup Phase (Phase 1)**:
- T004-T009 (all permissions, configurations, resources) can run in parallel

**Foundational Phase (Phase 2)**:
- T011-T021 (all entity models and enums) can run in parallel
- T023-T026 (all DAO interfaces except MedicationDao) can run in parallel after T010-T021
- T030-T033 (all repositories except MedicationRepository) can run in parallel after T029
- T035-T036 (AudioFeedbackService, AlarmScheduler) can run in parallel
- T040-T042 (all utility classes) can run in parallel

**User Story Tests**:
- All tests within a story marked [P] can run in parallel (different test files)

**User Story Implementation**:
- Model creation tasks marked [P] can run in parallel (different entity files)
- Layout XML creation tasks marked [P] can run in parallel (different layout files)
- Utility and service tasks marked [P] can run in parallel (different service files)

**Multi-Story Parallelism**:
Once MVP complete (US1/US2/US3), these can proceed in parallel with different developers:
- Developer A: US4 (Alarms)
- Developer B: US7 + US9 (Write Tag, Edit/Delete)
- Developer C: US10 (Read Tag) → requires MedicationInfoActivity from US9, so coordinate
- Developer D: US8 (Settings)

After P2 stories complete:
- Developer A: US6 (Emergency Contacts)
- Developer B: Polish tasks (T251-T268 most can run in parallel)

---

## Parallel Example: Foundational Phase

```bash
# Launch all entity models together (T011-T021):
Task: "Create Schedule embedded object in app/src/main/java/com/medchecktag/models/Schedule.java"
Task: "Create AlarmConfiguration embedded object in app/src/main/java/com/medchecktag/models/AlarmConfiguration.java"
Task: "Create DoseRecord entity in app/src/main/java/com/medchecktag/models/DoseRecord.java"
Task: "Create NFCTag entity in app/src/main/java/com/medchecktag/models/NFCTag.java"
Task: "Create EmergencyContact entity in app/src/main/java/com/medchecktag/models/EmergencyContact.java"
Task: "Create AppSettings entity in app/src/main/java/com/medchecktag/models/AppSettings.java"
# All enums T017-T021 can also run in parallel

# After entities complete, launch DAOs together (T023-T026):
Task: "Create DoseRecordDao interface in app/src/main/java/com/medchecktag/database/DoseRecordDao.java"
Task: "Create NFCTagDao interface in app/src/main/java/com/medchecktag/database/NFCTagDao.java"
Task: "Create EmergencyContactDao interface in app/src/main/java/com/medchecktag/database/EmergencyContactDao.java"
Task: "Create AppSettingsDao interface in app/src/main/java/com/medchecktag/database/AppSettingsDao.java"
```

---

## Parallel Example: User Story 2

```bash
# Launch all optional test tasks together (T043-T048):
Task: "Unit test for Medication model validation in app/src/test/java/com/medchecktag/models/MedicationTest.java"
Task: "Unit test for Schedule calculation logic in app/src/test/java/com/medchecktag/models/ScheduleTest.java"
Task: "Instrumented test for MedicationDao in app/src/androidTest/java/com/medchecktag/database/MedicationDaoTest.java"
Task: "Unit test for MedicationRepository in app/src/test/java/com/medchecktag/repositories/MedicationRepositoryTest.java"
Task: "Unit test for AddMedicationViewModel in app/src/test/java/com/medchecktag/viewmodels/AddMedicationViewModelTest.java"
Task: "Instrumented UI test for Add Medication flow in app/src/androidTest/java/com/medchecktag/ui/AddMedicationFlowTest.java"

# Launch all layout XML tasks together (T050-T053):
Task: "Create add_medication_layout.xml"
Task: "Create schedule_interval_fragment.xml"
Task: "Create schedule_specific_times_fragment.xml"
Task: "Create nfc_write_dialog.xml"
```

---

## Implementation Strategy

### MVP First (User Stories 1, 2, 3 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 2 (Add Medication)
4. Complete Phase 4: User Story 3 (Medication List)
5. Complete Phase 5: User Story 1 (NFC Scan)
6. **STOP and VALIDATE**: Test MVP independently (add medication → view list → scan tag → confirm dose)
7. Deploy/demo MVP if ready

**MVP delivers**: Users can add medications with schedules, view medication list with countdown timers, scan NFC tags for audio confirmation, track doses automatically. Core safety feature is functional.

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US2 → Can add medications (but no UI yet)
3. Add US3 → Can view medications (main UI works)
4. Add US1 → **MVP COMPLETE** (can scan tags, confirm doses, track medication intake)
5. Add US4 → Alarms work (scheduled reminders)
6. Add US5 → Refill reminders work
7. Add US10 → Quick tag access works
8. Add US7 → Backup tags work
9. Add US9 → Edit/delete works
10. Add US8 → Settings customization works
11. Add US6 → Emergency notifications work
12. Polish → Performance, documentation, release-ready

Each story adds value without breaking previous stories.

### Parallel Team Strategy

With multiple developers (4-5 team members):

**Sprint 1: Foundation**
- Team completes Setup (Phase 1) together
- Team completes Foundational (Phase 2) together
  - Developer A: Models (T010-T021)
  - Developer B: DAOs (T022-T026)
  - Developer C: Repositories (T029-T033)
  - Developer D: Services (T034-T039)
  - Developer E: Utilities (T040-T042)

**Sprint 2: MVP**
- Developer A: US2 (Add Medication)
- Developer B: US3 (Medication List) - waits for US2 completion mid-sprint
- Developer C: US1 (NFC Scan) - waits for US2/US3 completion mid-sprint
- Developer D: Write tests for US2/US3/US1
- Developer E: Documentation and quickstart validation

**Sprint 3: P2 Features**
- Developer A: US4 (Alarms)
- Developer B: US5 (Refill Reminders) - builds on US4, starts mid-sprint
- Developer C: US10 (Read Tag) + US9 (Edit/Delete) for MedicationInfoActivity
- Developer D: Write tests
- Developer E: Settings UI prep

**Sprint 4: P3 Features**
- Developer A: US8 (Settings)
- Developer B: US7 (Write Tag)
- Developer C: US9 (Edit/Delete) completion
- Developer D: US6 (Emergency Contacts)
- Developer E: Polish tasks

**Sprint 5: Polish & Release**
- All developers: Polish tasks (T251-T268)
- Code review, testing, performance optimization
- Release preparation

---

## Task Counts & Story Breakdown

**Total Tasks**: 268 (including optional tests)

**Phase 1 (Setup)**: 9 tasks
**Phase 2 (Foundational)**: 33 tasks (T010-T042)
**Phase 3 (US2 - Add Medication)**: 20 tasks (T043-T062, including 6 optional tests)
**Phase 4 (US3 - Medication List)**: 20 tasks (T063-T082, including 3 optional tests)
**Phase 5 (US1 - NFC Scan)**: 28 tasks (T083-T110, including 4 optional tests)
**Phase 6 (US4 - Alarms)**: 27 tasks (T111-T137, including 3 optional tests)
**Phase 7 (US5 - Refill Reminders)**: 16 tasks (T138-T153, including 3 optional tests)
**Phase 8 (US10 - Read Tag)**: 14 tasks (T154-T167, including 1 optional test)
**Phase 9 (US7 - Write to NFC Tag)**: 13 tasks (T168-T180, including 2 optional tests)
**Phase 10 (US9 - Edit/Delete)**: 19 tasks (T181-T203, including 4 optional tests)
**Phase 11 (US8 - Settings)**: 20 tasks (T204-T225, including 2 optional tests)
**Phase 12 (US6 - Emergency Contacts)**: 25 tasks (T226-T250, including 2 optional tests)
**Phase 13 (Polish)**: 18 tasks (T251-T268, including 2 optional tests)

**Optional Test Tasks**: 32 (marked with ⚠️)
**Required Implementation Tasks**: 236

**Parallel Opportunities**: ~80 tasks marked [P] across all phases

**MVP Task Count** (US1 + US2 + US3 + Setup + Foundational): ~119 tasks (including MVP tests)

---

## Notes

- **[P]** marker indicates tasks that can run in parallel (different files, no sequential dependencies)
- **[Story]** label maps task to specific user story for traceability
- **⚠️** marker indicates optional test tasks (not required if TDD not requested, but recommended for quality)
- Each user story is independently completable and testable after Foundational phase
- Verify tests fail before implementing (TDD workflow)
- Commit after each task or logical group of tasks
- Stop at any checkpoint to validate story independently before proceeding
- Follow file path conventions from [plan.md](plan.md) project structure
- All tasks reference constitution principles: User Safety First, Privacy by Design, TDD, NFC Reliability, Android Standards
- Constitution compliance: UUID-only NFC tags (privacy), local storage (privacy), input validation (safety), retry logic (reliability), AndroidX libraries (standards)

---

## Quick Reference

**MVP Scope** (Minimum Viable Product):
- Phase 1: Setup
- Phase 2: Foundational
- Phase 3: User Story 2 (Add Medication)
- Phase 4: User Story 3 (Medication List)
- Phase 5: User Story 1 (NFC Scan & Dose Confirmation)

**Total MVP Tasks**: ~119 tasks
**Estimated MVP Effort**: 3-4 weeks for single developer, 1-2 weeks for team of 4

**Post-MVP Priorities**:
1. P2 Stories (US4, US5, US10): Alarms, refill reminders, quick access - adds critical scheduled reminders
2. P3 Stories (US7, US9, US8, US6): Write tags, edit/delete, settings, emergency contacts - adds flexibility and safety net

**Constitution Principles Mapped to Tasks**:
- **User Safety First**: T096 (dose timing check), T141-T143 (refill thresholds), T151 (validation), T237-T240 (emergency notifications)
- **Privacy by Design**: T034 (NFC UUID-only), T029 (local database), T258 (no sensitive logs)
- **Test-Driven Development**: All tasks marked ⚠️ (test tasks written first)
- **NFC Reliability**: T092 (retry logic), T172-T176 (write verification), T094-T095 (error handling)
- **Simplicity & Android Standards**: T002 (AndroidX libraries), T022-T027 (Room database), T049+ (MVVM ViewModels)
