# Quickstart Guide: NFC Medication Reminder System

**Feature**: NFC Medication Reminder for users with cognitive issues  
**Date**: 2026-02-14  
**Target Audience**: Android developers joining the project

---

## Project Overview

**Purpose**: Help people with cognitive issues take medications correctly using NFC tags attached to medication bottles. When user taps phone on tag, app provides audio confirmation of medication name, dose, and timing.

**Technology Stack**:
- **Language**: Java (Android SDK API 21+)
- **Architecture**: MVVM (ViewModel + LiveData + Room)
- **Key Libraries**: AndroidX Jetpack, Room Database, Material Components
- **Build System**: Gradle
- **Min SDK**: 21 (Android 5.0)
- **Target SDK**: 34 (Android 14)

---

## Quick Start (5 Minutes)

### 1. Clone and Build

```bash
# Clone repository
git clone <repository-url>
cd Med_Check_Tag

# Switch to feature branch
git checkout 001-nfc-med-reminder

# Build project
./gradlew build

# Run on device/emulator (requires NFC for full functionality)
./gradlew installDebug
```

### 2. Run Tests

```bash
# Unit tests (fast, no device required)
./gradlew test

# Instrumented tests (requires Android device/emulator)
./gradlew connectedAndroidTest
```

### 3. Open in Android Studio

```
1. Open Android Studio
2. File → Open → Select Med_Check_Tag directory
3. Wait for Gradle sync
4. Run configuration: "app" → Select device → Run
```

---

## Project Structure

```
Med_Check_Tag/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/medchecktag/
│   │   │   │   ├── models/              # Data entities (Medication, DoseRecord, etc.)
│   │   │   │   ├── database/            # Room database, DAOs
│   │   │   │   ├── repositories/        # Data access layer
│   │   │   │   ├── viewmodels/          # UI state management
│   │   │   │   ├── ui/                  # Activities, Fragments, Adapters
│   │   │   │   │   ├── main/            # Main medication list
│   │   │   │   │   ├── medication/      # Add/Edit screens
│   │   │   │   │   ├── settings/        # Settings screen
│   │   │   │   │   └── nfc/             # NFC scan/write modals
│   │   │   │   ├── services/            # Background services
│   │   │   │   ├── nfc/                 # NFC read/write logic
│   │   │   │   ├── alarms/              # AlarmManager integration
│   │   │   │   ├── audio/               # TextToSpeech wrapper
│   │   │   │   └── utils/               # Helper classes
│   │   │   ├── res/                     # Android resources
│   │   │   └── AndroidManifest.xml
│   │   ├── androidTest/                 # Instrumented tests
│   │   └── test/                        # Unit tests
│   └── build.gradle
├── specs/                                # Feature documentation
│   └── 001-nfc-med-reminder/
│       ├── spec.md                       # Feature specification
│       ├── plan.md                       # This implementation plan
│       ├── research.md                   # Technology decisions
│       ├── data-model.md                 # Database schema
│       ├── contracts/                    # Interface contracts
│       └── quickstart.md                 # This guide
├── build.gradle
└── README.md
```

---

## Key Concepts

### 1. MVVM Architecture

```
View (Activity/Fragment)
    ↓ observes
ViewModel (Business Logic)
    ↓ uses
Repository (Data Access)
    ↓ queries
Room Database (Local Storage)
```

**Example Flow**: User taps NFC tag
1. `NFCActivity` receives NFC intent
2. Calls `MedicationViewModel.confirmDose(medicationId)`
3. ViewModel calls `MedicationRepository.updateDose()`
4. Repository updates Room database
5. ViewModel calculates next dose time
6. ViewModel calls `AudioService.speakConfirmation()`
7. View observes LiveData and updates UI

### 2. Room Database

**Entities**: Medication, DoseRecord, NFCTag, EmergencyContact, AppSettings

**Key Patterns**:
```java
// Define entity
@Entity(tableName = "medications")
public class Medication {
    @PrimaryKey @NonNull
    public String id;
    
    public String nickname;
    public String dose;
    // ... other fields
}

// Define DAO
@Dao
public interface MedicationDao {
    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY nextDoseTime")
    LiveData<List<Medication>> getActiveMedications();
    
    @Insert
    long insert(Medication medication);
    
    @Update
    int update(Medication medication);
}

// Use in Repository
public class MedicationRepository {
    private MedicationDao medicationDao;
    
    public LiveData<List<Medication>> getActiveMedications() {
        return medicationDao.getActiveMedications();
    }
}
```

### 3. NFC Operations

**Reading NFC Tag**:
```java
// In Activity onNewIntent()
@Override
protected void onNewIntent(Intent intent) {
    if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        
        executor.execute(() -> {
            String medicationId = nfcHandler.readMedicationId(tag);
            viewModel.onTagScanned(medicationId);
        });
    }
}
```

**Writing NFC Tag**:
```java
// Enable foreground dispatch for write mode
nfcAdapter.enableForegroundDispatch(
    this,
    pendingIntent,
    null,
    null
);

// Write to tag
executor.execute(() -> {
    boolean success = nfcHandler.writeMedicationId(tag, medicationId);
    runOnUiThread(() -> showResult(success));
});
```

### 4. Alarm Scheduling

**Schedule medication alarm**:
```java
// Calculate trigger time
long triggerTime = medication.getNextDoseTime();

// Get PendingIntent
Intent intent = new Intent(context, AlarmReceiver.class);
intent.putExtra("medicationId", medication.getId());
PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, flags);

// Schedule exact alarm
AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
alarmManager.setExactAndAllowWhileIdle(
    AlarmManager.RTC_WAKEUP,
    triggerTime,
    pendingIntent
);
```

### 5. Text-to-Speech

**Speak medication confirmation**:
```java
// Initialize TTS
textToSpeech = new TextToSpeech(context, status -> {
    if (status == TextToSpeech.SUCCESS) {
        // TTS ready
    }
});

// Speak message
String message = medication.getNickname() + ", " + 
                medication.getDose() + ". " +
                "Next dose in " + formatMinutes(nextDoseMinutes);
                
textToSpeech.speak(message, TextToSpeech.QUEUE_ADD, null, utteranceId);
```

---

## Development Workflow

### Create New Feature

```bash
# Create feature branch from main
git checkout main
git pull origin main
git checkout -b 002-new-feature

# Write tests first (TDD)
# See tests/ directory for examples

# Implement feature

# Run tests
./gradlew test connectedAndroidTest

# Commit with descriptive message
git commit -m "feat: add medication export functionality"
```

### Code Review Checklist

- [ ] All unit tests pass
- [ ] All instrumented tests pass  
- [ ] Code follows Android style guide
- [ ] No medication data in logs (privacy)
- [ ] Validation for all user inputs
- [ ] Error handling with user-friendly messages
- [ ] Confirmation dialogs for destructive actions
- [ ] LiveData used for UI updates (no manual refresh)
- [ ] Background operations off UI thread
- [ ] TTS messages are clear and concise

---

## Testing Guide

### Unit Test Example

```java
// Test ViewModel logic
@Test
public void testDecrementDosesUpdatesLiveData() {
    // Given
    Medication medication = createTestMedication(10); // 10 doses
    medicationRepository.insert(medication);
    
    // When
    viewModel.confirmDose(medication.getId());
    
    // Then
    Medication updated = viewModel.getMedication(medication.getId()).getValue();
    assertEquals(9, updated.getRemainingDoses());
}
```

### Instrumented Test Example

```java
// Test Room database
@Test
public void testInsertAndRetrieveMedication() {
    // Given
    Medication medication = createTestMedication();
    
    // When
    long id = medicationDao.insert(medication);
    
    // Then
    Medication retrieved = medicationDao.getMedicationById(medication.getId()).getValue();
    assertNotNull(retrieved);
    assertEquals(medication.getNickname(), retrieved.getNickname());
}
```

### UI Test Example

```java
// Test add medication flow
@Test
public void testAddMedicationFlow() {
    // Click Add Medication button
    onView(withId(R.id.btn_add_medication)).perform(click());
    
    // Enter nickname
    onView(withId(R.id.edit_nickname)).perform(typeText("Test Med"));
    
    // Enter dose
    onView(withId(R.id.edit_dose)).perform(typeText("1 tablet"));
    
    // Save
    onView(withId(R.id.btn_save)).perform(click());
    
    // Verify appears in list
    onView(withText("Test Med")).check(matches(isDisplayed()));
}
```

---

## Common Tasks

### Add New Field to Medication

1. Update `Medication` entity in `models/Medication.java`
2. Increment database version in `AppDatabase.java`
3. Create migration in `database/Migrations.java`
4. Update DAOs if new queries needed
5. Update UI forms (add/edit screens)
6. Update ViewModels to handle new field
7. Write tests for new field validation
8. Update `data-model.md` documentation

### Add New Alarm Type

1. Add enum value to `AlarmType` in `alarms/AlarmType.java`
2. Update `AlarmScheduler.scheduleAlarmsForMedication()`
3. Update `AlarmReceiver.onAlarmTriggered()` to handle new type
4. Update `AlarmConfiguration` entity if needed
5. Update settings UI for alarm configuration
6. Write tests for new alarm behavior
7. Update `alarm-contract.md` documentation

### Add New Screen

1. Create Fragment/Activity in `ui/<feature>/`
2. Create ViewModel in `viewmodels/`
3. Create layout XML in `res/layout/`
4. Add navigation action in `res/navigation/nav_graph.xml`
5. Add menu item or button to navigate to screen
6. Write Espresso UI tests for screen
7. Update `spec.md` with new user story if applicable

---

## Debugging Tips

### Enable Debug Logging

```java
// In Application class or Activity onCreate
if (BuildConfig.DEBUG) {
    // Enable verbose Room logging
    Room.databaseBuilder(context, AppDatabase.class, "medchecktag.db")
        .setQueryCallback(new RoomDatabase.QueryCallback() {
            @Override
            public void onQuery(@NonNull String sqlQuery, @NonNull List<Object> bindArgs) {
                Log.d("RoomQuery", sqlQuery + " with args: " + bindArgs);
            }
        }, executor)
        .build();
}
```

### Test NFC Without Physical Tags

```java
//Use Android Debug Bridge (ADB) to emulate NFC
adb emu nfc touchType A
adb emu nfc addId <tag-id>
adb emu nfc read <ndef-message>
```

### Inspect Room Database

```bash
# Pull database from device
adb pull /data/data/com.medchecktag/databases/medchecktag.db

# Open with SQLite browser
sqlite3 medchecktag.db
.tables
SELECT * FROM medications;
```

### Debug Alarms

```java
// Log scheduled alarms
AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    AlarmManager.AlarmClockInfo nextAlarm = alarmManager.getNextAlarmClock();
    Log.d("Alarm", "Next alarm at: " + new Date(nextAlarm.getTriggerTime()));
}
```

---

## Resources

### Documentation
- [Feature Spec](spec.md) - User stories and requirements
- [Implementation Plan](plan.md) - Technical approach
- [Data Model](data-model.md) - Database schema
- [Contracts](contracts/) - Interface contracts

### Android Docs
- [Room Database](https://developer.android.com/training/data-storage/room)
- [ViewModel](https://developer.android.com/topic/libraries/architecture/viewmodel)
- [LiveData](https://developer.android.com/topic/libraries/architecture/livedata)
- [NFC Basics](https://developer.android.com/guide/topics/connectivity/nfc/nfc)
- [AlarmManager](https://developer.android.com/reference/android/app/AlarmManager)
- [TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech)

### Project Standards
- [Constitution](.specify/memory/constitution.md) - Project principles and governance
- [Android Code Style](https://source.android.com/setup/contribute/code-style)
- [Material Design Guidelines](https://material.io/design)

---

## Getting Help

### Stuck on Setup?
1. Check build.gradle dependencies are resolved
2. Verify Android Studio uses JDK 11+
3. Sync Gradle files (File → Sync Project with Gradle Files)
4. Clean build (`./gradlew clean build`)

### Stuck on Implementation?
1. Review contract files in `contracts/` directory
2. Look at similar existing implementations
3. Check test files for usage examples
4. Review feature spec for requirements clarity

### Found a Bug?
1. Check if it's addressed in constitution (safety requirements)
2. Write failing test that reproduces bug
3. Fix bug to make test pass
4. Commit with "fix:" prefix in message

---

## Next Steps

**For New Developers**:
1. ✅ Read this quickstart
2. ✅ Read [feature spec](spec.md) for user perspective
3. ✅ Set up development environment
4. ✅ Run and explore existing tests
5. ✅ Pick a task from [tasks.md](tasks.md) (once generated)
6. ✅ Write test, implement, submit PR

**For This Feature**:
1. ✅ Run `/speckit.tasks` to generate task breakdown
2. ✅ Implement Phase 1 tasks (MVP: Stories 1-3)
3. ✅ Run Phase 1 tests
4. ✅ Implement Phase 2 tasks (Stories 4-5)
5. ✅ Implement Phase 3 tasks (Stories 6-10)
6. ✅ Deploy beta for user testing

**Ready to Code!** 🚀
