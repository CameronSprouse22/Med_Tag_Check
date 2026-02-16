package com.medchecktag.alarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.medchecktag.models.AlarmType;
import com.medchecktag.models.Medication;

/**
 * AlarmScheduler implementing IAlarmScheduler contract.
 * Manages medication reminder alarms using Android AlarmManager.
 */
public class AlarmScheduler {
    
    private static final String TAG = "AlarmScheduler";
    private static final String EXTRA_MEDICATION_ID = "medication_id";
    private static final String EXTRA_ALARM_TYPE = "alarm_type";
    private static final String EXTRA_REMAINING_DOSES = "remaining_doses";
    
    private final Context context;
    private final AlarmManager alarmManager;
    
    public AlarmScheduler(Context context) {
        this.context = context.getApplicationContext();
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    
    // Scheduling Operations
    
    /**
     * Schedule all alarms for a medication
     */
    public boolean scheduleAlarmsForMedication(Medication medication) {
        if (medication == null || medication.schedule == null || medication.alarmConfig == null) {
            Log.e(TAG, "Invalid medication or configuration");
            return false;
        }
        
        boolean allSuccess = true;
        
        // Schedule pre-dose alarm if enabled
        if (medication.alarmConfig.preAlarmEnabled && medication.alarmConfig.preAlarmMinutes != null) {
            long triggerTime = medication.schedule.nextDoseTime - (medication.alarmConfig.preAlarmMinutes * 60L * 1000L);
            if (triggerTime > System.currentTimeMillis()) {
                allSuccess &= schedulePreAlarm(medication.id, triggerTime);
            }
        }
        
        // Schedule on-time alarm if enabled
        if (medication.alarmConfig.onTimeAlarmEnabled && medication.schedule.nextDoseTime > System.currentTimeMillis()) {
            allSuccess &= scheduleOnTimeAlarm(medication.id, medication.schedule.nextDoseTime);
        }
        
        // Schedule post-dose alarm if enabled
        if (medication.alarmConfig.postAlarmEnabled && medication.alarmConfig.postAlarmMinutes != null) {
            long triggerTime = medication.schedule.nextDoseTime + (medication.alarmConfig.postAlarmMinutes * 60L * 1000L);
            allSuccess &= schedulePostAlarm(medication.id, triggerTime);
        }
        
        return allSuccess;
    }
    
    /**
     * Schedule pre-dose alarm
     */
    public boolean schedulePreAlarm(String medicationId, long triggerTimeMillis) {
        return scheduleAlarm(medicationId, AlarmType.PRE_DOSE, triggerTimeMillis, 0);
    }
    
    /**
     * Schedule on-time alarm
     */
    public boolean scheduleOnTimeAlarm(String medicationId, long triggerTimeMillis) {
        return scheduleAlarm(medicationId, AlarmType.ON_TIME, triggerTimeMillis, 0);
    }
    
    /**
     * Schedule post-dose alarm
     */
    public boolean schedulePostAlarm(String medicationId, long triggerTimeMillis) {
        return scheduleAlarm(medicationId, AlarmType.POST_DOSE, triggerTimeMillis, 0);
    }
    
    /**
     * Schedule refill reminder alarm
     */
    public boolean scheduleRefillAlarm(String medicationId, long triggerTimeMillis, int remainingDoses) {
        return scheduleAlarm(medicationId, AlarmType.REFILL_REMINDER, triggerTimeMillis, remainingDoses);
    }
    
    /**
     * Internal method to schedule an alarm
     */
    private boolean scheduleAlarm(String medicationId, AlarmType alarmType, long triggerTimeMillis, int remainingDoses) {
        if (alarmManager == null) {
            Log.e(TAG, "AlarmManager not available");
            return false;
        }
        
        try {
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra(EXTRA_MEDICATION_ID, medicationId);
            intent.putExtra(EXTRA_ALARM_TYPE, alarmType.name());
            if (alarmType == AlarmType.REFILL_REMINDER) {
                intent.putExtra(EXTRA_REMAINING_DOSES, remainingDoses);
            }
            
            int requestCode = getRequestCode(medicationId, alarmType);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            // Use setExactAndAllowWhileIdle for precise timing that works in Doze mode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTimeMillis, pendingIntent);
            }
            
            Log.d(TAG, String.format("Scheduled %s alarm for medication %s at %d", 
                alarmType, medicationId, triggerTimeMillis));
            return true;
            
        } catch (Exception e) {
            Log.e(TAG, "Failed to schedule alarm: " + e.getMessage());
            return false;
        }
    }
    
    // Cancellation Operations
    
    /**
     * Cancel all alarms for a medication
     */
    public void cancelAllAlarmsForMedication(String medicationId) {
        for (AlarmType alarmType : AlarmType.values()) {
            cancelAlarm(medicationId, alarmType);
        }
    }
    
    /**
     * Cancel specific alarm
     */
    public void cancelAlarm(String medicationId, AlarmType alarmType) {
        if (alarmManager == null) {
            return;
        }
        
        try {
            Intent intent = new Intent(context, AlarmReceiver.class);
            int requestCode = getRequestCode(medicationId, alarmType);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent);
                pendingIntent.cancel();
                Log.d(TAG, String.format("Cancelled %s alarm for medication %s", alarmType, medicationId));
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to cancel alarm: " + e.getMessage());
        }
    }
    
    /**
     * Cancel all alarms in the system
     */
    public void cancelAllAlarms() {
        // This would require tracking all medication IDs
        // For now, this is a placeholder that would be implemented with a repository query
        Log.w(TAG, "cancelAllAlarms not fully implemented");
    }
    
    // Update Operations
    
    /**
     * Reschedule alarms after medication update
     */
    public boolean rescheduleAlarmsForMedication(Medication medication) {
        cancelAllAlarmsForMedication(medication.id);
        return scheduleAlarmsForMedication(medication);
    }
    
    /**
     * Snooze alarm for specified duration
     */
    public boolean snoozeAlarm(String medicationId, AlarmType alarmType, int snoozeMinutes) {
        if (snoozeMinutes <= 0 || snoozeMinutes > 120) {
            Log.e(TAG, "Invalid snooze duration: " + snoozeMinutes);
            return false;
        }
        
        cancelAlarm(medicationId, alarmType);
        long snoozeTime = System.currentTimeMillis() + (snoozeMinutes * 60L * 1000L);
        return scheduleAlarm(medicationId, alarmType, snoozeTime, 0);
    }
    
    // Utility Methods
    
    /**
     * Generate unique request code for alarm
     */
    private int getRequestCode(String medicationId, AlarmType alarmType) {
        // Combine medication ID hash with alarm type ordinal for unique code
        int medicationHash = medicationId.hashCode();
        int typeCode = alarmType.ordinal();
        return (medicationHash & 0x00FFFFFF) | (typeCode << 24);
    }
    
    /**
     * Check if medication has active alarms
     */
    public boolean hasActiveAlarms(String medicationId) {
        for (AlarmType alarmType : AlarmType.values()) {
            Intent intent = new Intent(context, AlarmReceiver.class);
            int requestCode = getRequestCode(medicationId, alarmType);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE | PendingIntent.FLAG_IMMUTABLE
            );
            if (pendingIntent != null) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Get next alarm time for medication
     * Note: Android doesn't provide direct API to query scheduled alarm time
     * This would require maintaining separate tracking
     */
    public long getNextAlarmTime(String medicationId) {
        // This would require maintaining alarm schedule in database
        Log.w(TAG, "getNextAlarmTime not fully implemented");
        return -1;
    }
}
