package com.medchecktag.alarms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.medchecktag.database.AppDatabase;
import com.medchecktag.models.Medication;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * BootReceiver reschedules all medication alarms after device reboot.
 * Android cancels all alarms on reboot, so this receiver restores them.
 */
public class BootReceiver extends BroadcastReceiver {
    
    private static final String TAG = "BootReceiver";
    
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() == null) {
            return;
        }
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
            Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            
            Log.i(TAG, "Boot completed or package replaced, rescheduling alarms");
            rescheduleAllAlarms(context);
        }
    }
    
    /**
     * Reschedule all alarms for active medications
     */
    private void rescheduleAllAlarms(Context context) {
        Executor executor = Executors.newSingleThreadExecutor();
        AppDatabase database = AppDatabase.getInstance(context);
        AlarmScheduler alarmScheduler = new AlarmScheduler(context);
        
        executor.execute(() -> {
            try {
                // Get all active medications
                List<Medication> medications = database.medicationDao().getAllActiveSync();
                
                if (medications == null || medications.isEmpty()) {
                    Log.d(TAG, "No active medications to reschedule");
                    return;
                }
                
                int successCount = 0;
                for (Medication medication : medications) {
                    if (medication.schedule != null && 
                        medication.schedule.nextDoseTime > System.currentTimeMillis()) {
                        
                        boolean success = alarmScheduler.scheduleAlarmsForMedication(medication);
                        if (success) {
                            successCount++;
                        }
                    }
                }
                
                Log.i(TAG, String.format("Rescheduled alarms for %d of %d active medications", 
                    successCount, medications.size()));
                
            } catch (Exception e) {
                Log.e(TAG, "Failed to reschedule alarms: " + e.getMessage(), e);
            }
        });
    }
}
