# Repository Contract

**Purpose**: Define the data access layer interface between ViewModels and data sources (Room database)

**Package**: `com.medchecktag.repositories`

---

## IMedicationRepository

**Responsibility**: Manage medication CRUD operations and queries

### Methods

#### Query Operations

```java
// Get all medications (active and inactive)
LiveData<List<Medication>> getAllMedications();

// Get only active medications, sorted by next dose time
LiveData<List<Medication>> getActiveMedicationsSortedByNextDose();

// Get single medication by ID
LiveData<Medication> getMedicationById(String medicationId);

// Get medications by category
LiveData<List<Medication>> getMedicationsByCategory(MedicationCategory category);

// Get medications with low doses (below threshold1)
LiveData<List<Medication>> getMedicationsNeedingRefill();

// Get medication by NFC tag ID
Medication getMedicationByTagId(String nfcTagId); // Synchronous for NFC scan
```

**Return Types**:
- `LiveData<T>` for reactive UI updates (Room integration)
- Direct return for NFC operations (must be synchronous)

**Null Safety**:
- Methods returning `LiveData` never return null (may emit empty list)
- Direct returns may be null if medication not found

---

#### Mutation Operations

```java
// Create new medication
long insertMedication(Medication medication);

// Update existing medication
int updateMedication(Medication medication);

// Delete medication permanently
int deleteMedication(String medicationId);

// Mark medication as inactive (soft delete)
int markMedicationInactive(String medicationId);

// Mark medication as active
int markMedicationActive(String medicationId);

// Update remaining doses (after confirmation or refill)
int updateRemainingDoses(String medicationId, int newCount);

// Update next dose time (after schedule change or confirmation)
int updateNextDoseTime(String medicationId, long timestampMillis);
```

**Return Types**:
- `long`: New row ID (for insert)
- `int`: Number of rows affected (for update/delete)

**Thread Safety**:
- All mutations must execute on background thread (Room enforces this)
- Use Executors or Kotlin coroutines for async execution

---

#### Validation Rules

**Pre-Insert Validations**:
1. `medicationId` must be valid UUID v4 format
2. `nickname` must not be empty/whitespace
3. `dose` must not be empty/whitespace
4. `maxDoseCount` >= 1
5. `remainingDoses` <= `maxDoseCount`
6. `refillThreshold1` > `refillThreshold2` >= 0
7. `schedule.intervalHours` XOR `schedule.specificTimes` must be set (not both)
8. If `schedule.specificTimes` provided, must contain at least 1 valid time (HH:mm format)

**Pre-Update Validations**:
- Same as insert, plus:
- Medication with given ID must exist
- Cannot decrease `remainingDoses` below 0

**Error Handling**:
- Validation failures throw `IllegalArgumentException` with descriptive message
- Database errors throw `SQLiteException` (handled by ViewModels)

---

## IDoseRecordRepository

**Responsibility**: Manage dose history logging

### Methods

```java
// Insert new dose record
long insertDoseRecord(DoseRecord record);

// Get all dose records for a medication
LiveData<List<DoseRecord>> getDoseRecordsByMedication(String medicationId);

// Get dose records within date range
LiveData<List<DoseRecord>> getDoseRecordsInRange(String medicationId, long startMillis, long endMillis);

// Get count of consecutive missed doses
int getConsecutiveMissedDoseCount(String medicationId);

// Get most recent dose record for medication
DoseRecord getLastDoseRecord(String medicationId);

// Delete all dose records for medication (cascade on medication deletion)
int deleteDoseRecordsByMedication(String medicationId);
```

**Business Rules**:
- `DoseRecord` is immutable after creation (no updates, only inserts/deletes)
- When dose is TAKEN, parent `Medication.remainingDoses` must decrement atomically
- When dose is MISSED (auto-created), does not affect `remainingDoses`

---

## INFCTagRepository

**Responsibility**: Manage NFC tag associations

### Methods

```java
// Insert new tag
long insertNFCTag(NFCTag tag);

// Get all tags for a medication
LiveData<List<NFCTag>> getTagsByMedication(String medicationId);

// Get medication ID for tag (fast lookup during scan)
String getMedicationIdByTagId(String tagId); // Synchronous

// Check if tag already assigned
boolean isTagAlreadyAssigned(String tagId);

// Update last scanned timestamp
int updateLastScannedTime(String tagId, long timestampMillis);

// Delete tag
int deleteNFCTag(long tagInternalId);

// Delete all tags for medication
int deleteTagsByMedication(String medicationId);
```

**Validation Rules**:
- `tagId` must be valid UUID v4 format
- `tagId` must be unique (query before insert to prevent duplicates)
- Before assigning tag to medication, check `isTagAlreadyAssigned()`

---

## IEmergencyContactRepository

**Responsibility**: Manage emergency contacts

### Methods

```java
// Get all emergency contacts
LiveData<List<EmergencyContact>> getAllContacts();

// Get contacts for a specific medication
LiveData<List<EmergencyContact>> getContactsForMedication(String medicationId);

// Insert new contact
long insertContact(EmergencyContact contact);

// Update contact
int updateContact(EmergencyContact contact);

// Delete contact
int deleteContact(long contactId);

// Link contact to medication with threshold
int linkContactToMedication(long contactId, String medicationId, int triggerAfterMissedDoses);

// Unlink contact from medication
int unlinkContactFromMedication(long contactId, String medicationId);
```

**Validation Rules**:
- `name` must not be empty
- At least one of `email` or `phoneNumber` must be provided
- If `notifyViaEmail` = true, `email` must be valid format
- If `notifyViaSms` or `notifyViaCall` = true, `phoneNumber` must be valid format

---

## IAppSettingsRepository

**Responsibility**: Manage global app settings (singleton)

### Methods

```java
// Get app settings (always returns non-null, creates default if not exists)
LiveData<AppSettings> getSettings();

// Get settings synchronously (for immediate use)
AppSettings getSettingsSync();

// Update settings (upsert - update if exists, insert if not)
int updateSettings(AppSettings settings);

// Reset settings to defaults
int resetToDefaults();
```

**Singleton Behavior**:
- Only one `AppSettings` row exists (ID = 1)
- First call to `getSettings()` creates default settings if not exists
- Update operation is always an UPDATE (never creates duplicate rows)

---

## Contract Implementation Notes

**Technology**: Room Database with LiveData/Flow

**Threading**:
- Query operations returning `LiveData` execute on background thread automatically (Room handles this)
- Synchronous operations (NFC lookups) must be called from background thread by caller
- Mutation operations must be called from background thread (Room enforces, throws exception if called on UI thread)

**Error Handling**:
- Room throws `SQLiteException` for database errors (unique constraint violations, foreign key errors, etc.)
- ViewModels catch exceptions and convert to user-friendly error messages
- Validation errors throw `IllegalArgumentException` with specific field violations

**Testing**:
- Interfaces allow mocking in ViewModel unit tests (Mockito)
- Instrumented tests use real Room in-memory database
- Contract defines behavior, not implementation (allows swapping storage if needed)
