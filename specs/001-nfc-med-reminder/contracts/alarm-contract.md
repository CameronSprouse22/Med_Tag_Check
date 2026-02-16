# Alarm Scheduling Contract

**Purpose**: Define interface for scheduling and managing medication alarms

**Package**: `com.medchecktag.alarms`

---

## IAlarmScheduler

**Responsibility**: Schedule, update, and cancel medication reminder alarms using Android AlarmManager

### Methods

#### Scheduling Operations

```java
/**
 * Schedule all alarms for a medication based on its configuration
 * 
 * @param medication Medication object with schedule and alarm configuration
 * @return true if all alarms scheduled successfully, false if any failed
 */
boolean scheduleAlarmsForMedication(Medication medication);

/**
 * Schedule pre-dose alarm (X minutes before dose time)
 * 
 * @param medicationId Medication UUID
 * @param triggerTimeMillis When alarm should trigger
 * @return true if scheduled successfully
 */
boolean schedulePreAlarm(String medicationId, long triggerTimeMillis);

/**
 * Schedule on-time alarm (at exact dose time)
 * 
 * @param medicationId Medication UUID
 * @param triggerTimeMillis When alarm should trigger
 * @return true if scheduled successfully
 */
boolean scheduleOnTimeAlarm(String medicationId, long triggerTimeMillis);

/**
 * Schedule post-dose alarm (X minutes after dose time if not taken)
 * 
 * @param medicationId Medication UUID
 * @param triggerTimeMillis When alarm should trigger
 * @return true if scheduled successfully
 */
boolean schedulePostAlarm(String medicationId, long triggerTimeMillis);

/**
 * Schedule refill reminder alarm
 * 
 * @param medicationId Medication UUID
 * @param triggerTimeMillis When alarm should trigger
 * @param remainingDoses Number of doses remaining (for notification text)
 * @return true if scheduled successfully
 */
boolean scheduleRefillAlarm(String medicationId, long triggerTimeMillis, int remainingDoses);
```

**Scheduling Behavior**:
- Uses `AlarmManager.setExactAndAllowWhileIdle()` for precise timing
- Alarms wake device from Doze mode (critical for medication safety)
- Each alarm has unique request code based on: medicationId hash + alarm type enum
- PendingIntent includes medication ID and alarm type in extras
- Alarms persist across device reboots (with boot receiver)

---

#### Cancellation Operations

```java
/**
 * Cancel all alarms for a medication
 * 
 * @param medicationId Medication UUID
 */
void cancelAllAlarmsForMedication(String medicationId);

/**
 * Cancel specific alarm type for a medication
 * 
 * @param medicationId Medication UUID
 * @param alarmType Type of alarm to cancel (PRE, ON_TIME, POST, REFILL)
 */
void cancelAlarm(String medicationId, AlarmType alarmType);

/**
 * Cancel all alarms in the system (for testing or app reset)
 */
void cancelAllAlarms();
```

**Cancellation Behavior**:
- Removes alarm from AlarmManager
- Removes any displayed notifications for that alarm
- Idempotent (safe to call even if alarm doesn't exist)

---

#### Update Operations

```java
/**
 * Reschedule alarms for medication after schedule change
 * 
 * @param medication Updated medication object
 * @return true if rescheduling successful
 */
boolean rescheduleAlarmsForMedication(Medication medication);

/**
 * Snooze alarm for specified duration
 * 
 * @param medicationId Medication UUID
 * @param alarmType Type of alarm to snooze
 * @param snoozeMinutes Minutes to snooze (5, 10, 15, 30)
 * @return true if snooze successful
 */
boolean snoozeAlarm(String medicationId, AlarmType alarmType, int snoozeMinutes);
```

**Update Behavior**:
- Reschedule cancels existing alarms and creates new ones
- Snooze cancels current alarm and creates new one at snooze time
- Validates snooze duration (must be positive, < 2 hours)

---

#### Query Operations

```java
/**
 * Check if medication has active alarms scheduled
 * 
 * @param medicationId Medication UUID
 * @return true if any alarms are currently scheduled
 */
boolean hasActiveAlarms(String medicationId);

/**
 * Get next alarm time for medication
 * 
 * @param medicationId Medication UUID
 * @return Timestamp of next alarm, or -1 if no alarms scheduled
 */
long getNextAlarmTime(String medicationId);

/**
 * Get all scheduled alarm times for medication
 * 
 * @param medicationId Medication UUID
 * @return Map of AlarmType to trigger timestamp
 */
Map<AlarmType, Long> getAllAlarmTimes(String medicationId);
```

---

## Alarm Types

```java
public enum AlarmType {
    PRE_DOSE,       // Alarm before dose time (e.g., 5 min warning)
    ON_TIME,        // Alarm at exact dose time
    POST_DOSE,      // Alarm after dose time if not taken (e.g., 30 min overdue)
    REFILL_REMINDER // Alarm to remind user to refill medication
}
```

**Request Code Calculation**:
```java
// Ensures unique request codes for each medication+alarm combination
public static int getRequestCode(String medicationId, AlarmType alarmType) {
    int medicationHash = medicationId.hashCode() & 0x00FFFFFF; // 24 bits
    int alarmTypeOrdinal = alarmType.ordinal() & 0x000000FF;   // 8 bits
    return (medicationHash << 8) | alarmTypeOrdinal;            // Combine to 32-bit int
}
```

---

## Alarm Receiver Contract

### IAlarmReceiver

**Responsibility**: Receive alarm broadcasts and trigger appropriate actions

```java
/**
 * Handle alarm trigger
 * 
 * @param context Android Context
 * @param medicationId Medication UUID from intent extras
 * @param alarmType Type of alarm that triggered
 */
void onAlarmTriggered(Context context, String medicationId, AlarmType alarmType);

/**
 * Handle boot completed (reschedule all alarms)
 * 
 * @param context Android Context
 */
void onBootCompleted(Context context);
```

**Alarm Trigger Actions**:
1. Query medication from database
2. Check if medication is still active (may have been deleted/deactivated)
3. Display notification with appropriate message
4. Play alarm sound based on medication category
5. If POST_DOSE alarm, check if dose was taken (query DoseRecord)
6. If refill alarm, check if medication was refilled
7. Schedule next alarm in sequence (e.g., after ON_TIME, schedule POST_DOSE)

---

## Notification Specifications

### Pre-Dose Notification

```
Title: "Medication Reminder"
Message: "[Medication Nickname] in [X minutes]"
Icon: Blue pill icon
Sound: Category-specific sound (brief)
Priority: HIGH
Actions: ["Open App"]
Dismiss: Auto-dismiss after tap or when dose taken
```

### On-Time Notification

```
Title: "Time for Medication"
Message: "[Medication Nickname] - [Dose Amount]"
Icon: Orange pill icon
Sound: Category-specific sound (persistent until dismissed)
Priority: MAX (heads-up notification)
Actions: ["Open App", "Snooze 10min"]
Dismiss: Manual dismiss or when dose confirmed
```

### Post-Dose Notification

```
Title: "⚠️ Missed Medication"
Message: "[Medication Nickname] was due [X minutes ago]"
Icon: Red pill icon (warning)
Sound: Category-specific sound (persistent)
Priority: MAX
Actions: ["Open App", "Mark as Skipped"]
Dismiss: Manual dismiss or when dose confirmed/skipped
```

### Refill Notification

```
Title: "Refill Medication"
Message: "[Medication Nickname] - [X doses remaining]"
Icon: Pharmacy icon
Sound: Default notification sound
Priority: HIGH
Actions: ["Open App", "Mark as Refilled"]
Dismiss: Auto-dismiss or manual
```

---

## Android Manifest Requirements

```xml
<!-- Permissions -->
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- Alarm Receiver -->
<receiver android:name=".alarms.AlarmReceiver"
          android:enabled="true"
          android:exported="false">
    <intent-filter>
        <action android:name="com.medchecktag.ALARM_TRIGGERED" />
    </intent-filter>
</receiver>

<!-- Boot Receiver -->
<receiver android:name=".alarms.BootReceiver"
          android:enabled="true"
          android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.QUICKBOOT_POWERON" />
    </intent-filter>
</receiver>
```

---

## Persistence Strategy

**Challenge**: AlarmManager alarms are cleared on device reboot

**Solution**: Boot Receiver + Database Persistence

```java
// On boot completed:
1. Query all active medications from database
2. For each medication:
   a. Calculate next alarm times based on schedule
   b. Re-schedule all enabled alarms
3. Log restoration completion
```

**Database Tracking**:
- Store next alarm time in `Schedule.nextDoseTime`
- Store alarm configuration in `AlarmConfiguration` (embedded in Medication)
- On boot, recalculate alarm times and reschedule

---

## Edge Case Handling

### Missed Alarms During Device Off

```
If device powered off during scheduled alarm time:
1. Boot receiver detects discrepancy (nextDoseTime < current time)
2. Immediately trigger overdue alarm
3. Create MISSED dose record if dose was not taken
4. Schedule next dose alarm
```

### Clock Changes (Timezone/DST)

```
System broadcasts time change events:
1. Listen for ACTION_TIME_CHANGED, ACTION_TIMEZONE_CHANGED
2. Recalculate all alarm times relative to new clock
3. Reschedule all alarms
```

### Doze Mode and App Standby

```
Use setExactAndAllowWhileIdle():
- Alarms trigger even in Doze mode
- App wakes up briefly to handle alarm
- Use WakeLock (hold for ~5 seconds max) to show notification
```

---

## Testing Strategy

### Unit Tests

```java
// Mock AlarmManager
AlarmManager mockAlarmManager = mock(AlarmManager.class);

// Test scheduling
alarmScheduler.scheduleOnTimeAlarm("test-id", triggerTime);
verify(mockAlarmManager).setExactAndAllowWhileIdle(
eq(AlarmManager.RTC_WAKEUP),
eq(triggerTime),
any(PendingIntent.class)
);
```

### Instrumented Tests

```java
// Use real AlarmManager with short time intervals
@Test
public void testAlarmTriggersWithin5Seconds() {
long triggerTime = System.currentTimeMillis() + 5000; // 5 sec from now
alarmScheduler.scheduleOnTimeAlarm("test-id", triggerTime);

// Wait for alarm
CountDownLatch latch = new CountDownLatch(1);
// Register broadcast receiver
// ...
assertTrue(latch.await(10, TimeUnit.SECONDS));
}
```

### Manual Testing

```
1. Schedule alarm for 1 minute in future
2. Lock device and wait
3. Verify alarm triggers and wakes device
4. Verify notification displays correctly
5. Test alarm sound plays
6. Test snooze functionality
7. Reboot device and verify alarms restored
```

---

## Implementation Notes

**Battery Optimization**:
- Exact alarms have battery impact (wake device)
- Justified for medication safety (health/fitness use case)
- Use WorkManager for non-critical refill reminders (battery-friendly)

**Permission Request**:
- Android 12+ requires `SCHEDULE_EXACT_ALARM` permission
- Show rationale dialog explaining why precise timing is needed
- Direct user to settings if permission denied

**Alarm Limits**:
- No system limit on number of alarms
- Practical limit: ~50 concurrent alarms (reasonable for 15 medications × 3 alarm types)
