# Data Model: NFC Medication Reminder System

**Date**: 2026-02-14  
**Feature**: [spec.md](spec.md)  
**Purpose**: Define data entities, relationships, validation rules, and state transitions

---

## Entity Relationship Overview

```
Medication 1──────* DoseRecord
    │
    │ 1
    │
    * 
Schedule (embedded)
    │
    │ 1
    │
    *
AlarmConfiguration (embedded)

Medication 1──────* NFCTag

Medication *──────* EmergencyContact (many-to-many)

AppSettings (singleton)
```

**Key Relationships**:
- One Medication has many DoseRecords (historical log)
- One Medication has one Schedule (embedded/composition)
- One Medication has one AlarmConfiguration (embedded/composition)
- One Medication can have multiple NFCTags (backup tags)
- Multiple Medications can share EmergencyContacts (e.g., caregiver for multiple meds)
- AppSettings is a singleton (one per app installation)

---

## Core Entities

### 1. Medication

**Purpose**: Represents a prescribed medication that user needs to track

**Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | String (UUID) | Primary Key, NOT NULL, UNIQUE | Unique identifier for medication |
| `nickname` | String | NOT NULL, LENGTH(1-50) | User-friendly name (e.g., "Blood Pressure Pill") |
| `medicationInfo` | String | NULLABLE, LENGTH(0-500) | Optional notes/instructions |
| `dose` | String | NOT NULL, LENGTH(1-50) | Dose amount (e.g., "2 tablets", "5 mL") |
| `category` | Enum | NOT NULL, IN('LIFE_DEPENDENT', 'VERY_IMPORTANT', 'BENEFICIAL') | Importance category |
| `maxDoseCount` | Integer | NOT NULL, >= 1 | Maximum doses when full (for refill) |
| `remainingDoses` | Integer | NOT NULL, >= 0, <= maxDoseCount | Current remaining doses |
| `refillThreshold1` | Integer | NOT NULL, > refillThreshold2 | First warning level (notification) |
| `refillThreshold2` | Integer | NOT NULL, >= 0, < refillThreshold1 | Critical level (alarm) |
| `isActive` | Boolean | NOT NULL, DEFAULT TRUE | Active vs inactive (archived) |
| `createdAt` | Timestamp | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |
| `updatedAt` | Timestamp | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Last update timestamp |

**Embedded Object**: `schedule` (see Schedule entity below)  
**Embedded Object**: `alarmConfig` (see AlarmConfiguration entity below)

**Validation Rules**:
- `nickname` must not be empty or whitespace-only
- `dose` must not be empty or whitespace-only
- `remainingDoses` <= `maxDoseCount` at all times
- `refillThreshold1` > `refillThreshold2` (first warning before critical)
- `refillThreshold2` >= 0 (cannot be negative)
- If `isActive` = false, all alarms must be cancelled

**State Transitions**:
```
NEW → ACTIVE (when created with isActive=true)
ACTIVE → INACTIVE (when user archives/deletes)
INACTIVE → ACTIVE (when user re-activates)
ACTIVE → DELETED (permanent deletion, cascades to DoseRecords)
```

**Indexes**:
- Primary index on `id`
- Index on `isActive` (for filtering active/inactive lists)
- Index on `remainingDoses` (for refill queries)

---

### 2. Schedule

**Purpose**: Defines when medication doses should be taken (embedded in Medication)

**Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `scheduleType` | Enum | NOT NULL, IN('INTERVAL', 'SPECIFIC_TIMES') | Schedule mode |
| `intervalHours` | Integer | NULLABLE, >= 1, <= 24 | Hours between doses (if INTERVAL type) |
| `specificTimes` | List<String> | NULLABLE, TIME FORMAT (HH:mm) | Times of day (if SPECIFIC_TIMES type) |
| `endDate` | Date | NULLABLE | Optional end date for schedule |
| `nextDoseTime` | Timestamp | NOT NULL | Calculated next dose time |
| `lastDoseTime` | Timestamp | NULLABLE | Last confirmed dose time |

**Validation Rules**:
- If `scheduleType` = INTERVAL, `intervalHours` must be NOT NULL, `specificTimes` must be NULL
- If `scheduleType` = SPECIFIC_TIMES, `specificTimes` must be NOT NULL and contain at least 1 time, `intervalHours` must be NULL
- `specificTimes` must be sorted in chronological order
- `specificTimes` entries must be valid 24-hour format (HH:mm), e.g., "08:00", "14:30", "20:00"
- `endDate` must be in the future (if provided)
- `nextDoseTime` must be calculated based on `scheduleType` and current time

**Calculation Logic**:
- **INTERVAL mode**: `nextDoseTime` = `lastDoseTime` + `intervalHours` (if lastDoseTime exists), otherwise current time + intervalHours
- **SPECIFIC_TIMES mode**: `nextDoseTime` = next occurring time from `specificTimes` list after current time; if all times passed today, use first time tomorrow

**State Transitions**:
```
When dose confirmed → lastDoseTime = current time, nextDoseTime recalculated
When reaching endDate → schedule becomes inactive (alarms cancelled)
When user edits schedule → nextDoseTime recalculated immediately
```

---

### 3. AlarmConfiguration

**Purpose**: Defines alarm settings for a medication (embedded in Medication)

**Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `preAlarmEnabled` | Boolean | NOT NULL, DEFAULT FALSE | Enable alarm before dose time |
| `preAlarmMinutes` | Integer | NULLABLE, >= 1, <= 60 | Minutes before dose (if pre-alarm enabled) |
| `onTimeAlarmEnabled` | Boolean | NOT NULL, DEFAULT TRUE | Enable alarm at exact dose time |
| `postAlarmEnabled` | Boolean | NOT NULL, DEFAULT FALSE | Enable alarm after dose time if not taken |
| `postAlarmMinutes` | Integer | NULLABLE, >= 1, <= 120 | Minutes after dose (if post-alarm enabled) |
| `alarmSoundUri` | String | NULLABLE | URI to custom alarm sound (or use category default) |
| `volumeLevel` | Float | >= 0.0, <= 1.0, DEFAULT 0.8 | Volume level (0.0-1.0) |

**Validation Rules**:
- If `preAlarmEnabled` = true, `preAlarmMinutes` must be NOT NULL
- If `postAlarmEnabled` = true, `postAlarmMinutes` must be NOT NULL
- At least one alarm type must be enabled (pre, onTime, or post)
- `volumeLevel` must be between 0.0 (silent) and 1.0 (max volume)

**Default Behavior**:
- New medications default to `onTimeAlarmEnabled` = true, others false
- Volume defaults to 0.8 (80% of max)
- Sound defaults to NULL (use category-specific sound from AppSettings)

---

### 4. DoseRecord

**Purpose**: Historical log of dose events (taken, skipped, missed)

**Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | Primary Key, AUTO_INCREMENT | Unique record ID |
| `medicationId` | String (UUID) | Foreign Key → Medication.id, NOT NULL | Reference to medication |
| `scheduledTime` | Timestamp | NOT NULL | When dose was scheduled |
| `actualTime` | Timestamp | NULLABLE | When dose was actually taken/skipped |
| `status` | Enum | NOT NULL, IN('TAKEN', 'SKIPPED', 'MISSED') | Dose status |
| `confirmationMethod` | Enum | NULLABLE, IN('NFC_SCAN', 'MANUAL_CONFIRM') | How dose was confirmed (if taken) |
| `remainingDosesAfter` | Integer | NOT NULL, >= 0 | Remaining doses after this event |
| `createdAt` | Timestamp | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Record creation time |

**Validation Rules**:
- If `status` = TAKEN, `actualTime` and `confirmationMethod` must be NOT NULL
- If `status` = SKIPPED, `actualTime` must be NOT NULL, `confirmationMethod` is NULLABLE
- If `status` = MISSED, `actualTime` is NULL (auto-created when dose time passes)
- `scheduledTime` must be in the past (cannot log future doses)
- `remainingDosesAfter` must match parent Medication's `remainingDoses` at time of record

**Status Definitions**:
- **TAKEN**: User confirmed dose via NFC scan or manual button
- **SKIPPED**: User explicitly marked dose as skipped (not taking it)
- **MISSED**: Dose time passed without confirmation (system-generated)

**Cascade Rules**:
- When Medication is deleted permanently, all DoseRecords are deleted (CASCADE)
- When Medication is marked inactive, DoseRecords are retained (historical data)

**Indexes**:
- Primary index on `id`
- Foreign key index on `medicationId`
- Index on `scheduledTime` (for time-based queries)
- Index on `status` (for filtering missed doses)

---

### 5. NFCTag

**Purpose**: Represents an NFC tag linked to a medication

**Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | Primary Key, AUTO_INCREMENT | Internal database ID |
| `tagId` | String (UUID) | UNIQUE, NOT NULL | UUID written to physical NFC tag |
| `medicationId` | String (UUID) | Foreign Key → Medication.id, NOT NULL | Reference to medication |
| `createdAt` | Timestamp | NOT NULL, DEFAULT CURRENT_TIMESTAMP | When tag was written |
| `lastScannedAt` | Timestamp | NULLABLE | Last time tag was scanned |

**Validation Rules**:
- `tagId` must be valid UUID v4 format (e.g., "a3b2c1d4-e5f6-7890-1234-567890abcdef")
- `tagId` must be unique across all tags (cannot assign same UUID to multiple medications)
- One medication can have multiple tags (backup tags)

**NDEF Format**:
```
Record Type: Text (TNF_WELL_KNOWN, RTD_TEXT)
Payload: "med:<UUID>"
Example: "med:a3b2c1d4-e5f6-7890-1234-567890abcdef"
```

**Indexes**:
- Primary index on `id`
- Unique index on `tagId` (fast lookup during NFC scans)
- Foreign key index on `medicationId`

---

### 6. EmergencyContact

**Purpose**: Contact information for emergency notifications when critical doses missed

**Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Long | Primary Key, AUTO_INCREMENT | Unique contact ID |
| `name` | String | NOT NULL, LENGTH(1-100) | Contact name |
| `email` | String | NULLABLE, VALID_EMAIL | Email address |
| `phoneNumber` | String | NULLABLE, VALID_PHONE | Phone number |
| `notifyViaEmail` | Boolean | NOT NULL, DEFAULT TRUE | Send email notifications |
| `notifyViaSms` | Boolean | NOT NULL, DEFAULT FALSE | Send SMS notifications |
| `notifyViaCall` | Boolean | NOT NULL, DEFAULT FALSE | Initiate phone call |
| `createdAt` | Timestamp | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Creation timestamp |

**Validation Rules**:
- `name` must not be empty or whitespace-only
- At least one of `email` or `phoneNumber` must be provided
- If `notifyViaEmail` = true, `email` must be NOT NULL and valid
- If `notifyViaSms` or `notifyViaCall` = true, `phoneNumber` must be NOT NULL and valid
- Email format validation: basic regex (contains @ and domain)
- Phone format validation: 10-15 digits, optional country code

**Many-to-Many Relationship**:
- Join table: `MedicationEmergencyContact` with fields: `medicationId`, `contactId`, `triggerAfterMissedDoses`
- Each medication-contact link has a threshold (e.g., notify after 1 missed dose, 2 missed doses, etc.)

---

### 7. AppSettings

**Purpose**: Global application configuration (singleton - one row)

**Fields**:
| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| `id` | Integer | Primary Key, FIXED = 1 | Always 1 (singleton) |
| `masterVolume` | Float | >= 0.0, <= 1.0, DEFAULT 0.8 | Master volume for all sounds |
| `lifeDependentSound` | String | NULLABLE | URI for Life Dependent alarm sound |
| `veryImportantSound` | String | NULLABLE | URI for Very Important alarm sound |
| `beneficialSound` | String | NULLABLE | URI for Beneficial alarm sound |
| `defaultRefillThreshold1LifeDep` | Integer | >= 1, DEFAULT 10 | Default notification threshold |
| `defaultRefillThreshold1VeryImp` | Integer | >= 1, DEFAULT 7 | Default notification threshold |
| `defaultRefillThreshold1Beneficial` | Integer | >= 1, DEFAULT 5 | Default notification threshold |
| `defaultRefillThreshold2LifeDep` | Integer | >= 0, DEFAULT 5 | Default alarm threshold |
| `defaultRefillThreshold2VeryImp` | Integer | >= 0, DEFAULT 3 | Default alarm threshold |
| `defaultRefillThreshold2Beneficial` | Integer | >= 0, DEFAULT 2 | Default alarm threshold |
| `manualConfirmationEnabled` | Boolean | NOT NULL, DEFAULT TRUE | Allow manual dose confirmation |
| `gpsLocationEnabled` | Boolean | NOT NULL, DEFAULT FALSE | Include GPS in emergency notifications |
| `ttsVoiceSpeed` | Float | >= 0.5, <= 2.0, DEFAULT 1.0 | TextToSpeech speed (0.5=slow, 2.0=fast) |
| `updatedAt` | Timestamp | NOT NULL, DEFAULT CURRENT_TIMESTAMP | Last settings update |

**Validation Rules**:
- `id` must always be 1 (enforced at database level)
- Threshold1 > Threshold2 for each category
- Sound URIs must be valid file:// or content:// URIs (if provided)

**Singleton Pattern**:
- Only one row allowed in database (constraint or application logic)
- Created with defaults on first app launch
- Updated via Settings screen

---

## Data Integrity Rules

### Referential Integrity
- DoseRecord.medicationId → Medication.id (ON DELETE CASCADE)
- NFCTag.medicationId → Medication.id (ON DELETE CASCADE)
- MedicationEmergencyContact.medicationId → Medication.id (ON DELETE CASCADE)
- MedicationEmergencyContact.contactId → EmergencyContact.id (ON DELETE CASCADE)

### Business Rules
1. **Dose Tracking**: When dose is confirmed (TAKEN), `Medication.remainingDoses` decrements by 1, `DoseRecord` created with status TAKEN
2. **Refill Logic**: When `Medication.remainingDoses` reaches 0, all dose confirmations are blocked until user marks as refilled
3. **Alarm Scheduling**: When `Schedule.nextDoseTime` changes, all AlarmManager alarms for that medication must be rescheduled
4. **Inactive Medications**: When `Medication.isActive` = false, all scheduled alarms are cancelled, but historical DoseRecords are preserved
5. **Tag Uniqueness**: Before writing NFC tag, check if `NFCTag.tagId` already exists; if yes, prompt user to confirm overwrite
6. **Emergency Threshold**: Emergency notifications only triggered when consecutive missed doses >= configured threshold for that medication-contact pair

### Data Migration Strategy
- **Version 1**: Initial schema (all entities defined above)
- **Future versions**: Room migration classes with SQL ALTER TABLE statements
- **Backup strategy**: Export all data to JSON before major migrations

---

## Example Data Flow

### Scenario: User Takes Medication via NFC Scan

1. **User scans NFC tag**:
   - Read NDEF text record → extract UUID (e.g., "med:a3b2c1d4-...")
   - Query NFCTag table: `SELECT medicationId FROM NFCTag WHERE tagId = ?`
   - Query Medication table: `SELECT * FROM Medication WHERE id = ? AND isActive = true`

2. **Validate timing**:
   - Check `Schedule.nextDoseTime` vs current time
   - If too early (>30 min before), show warning: "Not time for [nickname] yet, next dose in [X]"
   - If on time or overdue, proceed

3. **Confirm dose**:
   - TextToSpeech speaks: "[nickname], [dose], next dose in [X hours/minutes]"
   - Decrement `Medication.remainingDoses` by 1
   - Update `Schedule.lastDoseTime` = current time
   - Recalculate `Schedule.nextDoseTime` based on scheduleType
   - Insert DoseRecord: status=TAKEN, confirmationMethod=NFC_SCAN, actualTime=now
   - Update `NFCTag.lastScannedAt` = current time
   - Reschedule AlarmManager alarms for new nextDoseTime

4. **Check refill thresholds**:
   - If `remainingDoses` == `refillThreshold1`, send notification
   - If `remainingDoses` == `refillThreshold2`, schedule refill alarm

5. **Return to main screen**:
   - Medication list updates via LiveData (Room query observer)
   - "Time Until Next Dose" shows countdown to nextDoseTime

---

## Database Schema Summary

**Tables**:
- `medications` (core entity)
- `dose_records` (historical log)
- `nfc_tags` (tag mappings)
- `emergency_contacts` (contact info)
- `medication_emergency_contacts` (many-to-many join table)
- `app_settings` (singleton config)

**Total Estimated Size**:
- 10 medications × ~1KB each = 10KB
- 10 medications × 30 days × 3 doses/day × 0.2KB = 180KB (dose history)
- Total: ~200KB for typical user (well under 50MB constraint)

**Room Implementation Notes**:
- `@Entity` annotations for tables
- `@Embedded` for Schedule and AlarmConfiguration
- `@Relation` for one-to-many relationships (Medication → DoseRecords)
- `@Dao` interfaces with LiveData queries for reactive UI
- `@TypeConverter` for enums, lists, timestamps, URIs
- Database version: 1 (initial release)
