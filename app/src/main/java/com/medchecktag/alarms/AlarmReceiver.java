package com.medchecktag.alarms;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.medchecktag.R;
import com.medchecktag.models.AlarmType;

/**
 * AlarmReceiver handles alarm triggers and creates notifications.
 * Receives broadcast from AlarmManager when medication reminders trigger.
 */
public class AlarmReceiver extends BroadcastReceiver {
    
    private static final String TAG = "AlarmReceiver";
    private static final String CHANNEL_ID_DOSE_REMINDER = "dose_reminder";
    private static final String CHANNEL_ID_REFILL = "refill_reminder";
    private static final int NOTIFICATION_ID_BASE = 1000;
    
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarm received");
        
        String medicationId = intent.getStringExtra("medication_id");
        String alarmTypeStr = intent.getStringExtra("alarm_type");
        int remainingDoses = intent.getIntExtra("remaining_doses", 0);
        
        if (medicationId == null || alarmTypeStr == null) {
            Log.e(TAG, "Missing medication ID or alarm type");
            return;
        }
        
        AlarmType alarmType;
        try {
            alarmType = AlarmType.valueOf(alarmTypeStr);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Invalid alarm type: " + alarmTypeStr);
            return;
        }
        
        // Create notification channel if needed
        createNotificationChannels(context);
        
        // Show notification
        showNotification(context, medicationId, alarmType, remainingDoses);
    }
    
    /**
     * Create notification channels for Android O+
     */
    private void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            
            if (notificationManager == null) {
                return;
            }
            
            // Dose reminder channel (high priority)
            NotificationChannel doseChannel = new NotificationChannel(
                CHANNEL_ID_DOSE_REMINDER,
                "Medication Reminders",
                NotificationManager.IMPORTANCE_HIGH
            );
            doseChannel.setDescription("Reminders to take your medication");
            doseChannel.enableVibration(true);
            doseChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(doseChannel);
            
            // Refill reminder channel (default priority)
            NotificationChannel refillChannel = new NotificationChannel(
                CHANNEL_ID_REFILL,
                "Refill Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            refillChannel.setDescription("Reminders to refill your medication");
            notificationManager.createNotificationChannel(refillChannel);
        }
    }
    
    /**
     * Show notification for alarm
     */
    private void showNotification(Context context, String medicationId, AlarmType alarmType, int remainingDoses) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager == null) {
            return;
        }
        
        String channelId = alarmType == AlarmType.REFILL_REMINDER ? 
            CHANNEL_ID_REFILL : CHANNEL_ID_DOSE_REMINDER;
        
        String title = getNotificationTitle(alarmType);
        String message = getNotificationMessage(alarmType, medicationId, remainingDoses);
        
        // Create intent to open app (would open specific activity)
        Intent appIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (appIntent != null) {
            appIntent.putExtra("medication_id", medicationId);
            appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        }
        
        PendingIntent pendingIntent = PendingIntent.getActivity(
            context,
            medicationId.hashCode(),
            appIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Replace with custom icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(alarmType == AlarmType.REFILL_REMINDER ? 
                NotificationCompat.PRIORITY_DEFAULT : NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent);
        
        // Add action buttons for dose reminders
        if (alarmType != AlarmType.REFILL_REMINDER) {
            // TODO: Add "Take Now" and "Snooze" action buttons
        }
        
        int notificationId = NOTIFICATION_ID_BASE + medicationId.hashCode() + alarmType.ordinal();
        notificationManager.notify(notificationId, builder.build());
        
        Log.d(TAG, String.format("Notification shown for %s alarm, medication %s", alarmType, medicationId));
    }
    
    /**
     * Get notification title based on alarm type
     */
    private String getNotificationTitle(AlarmType alarmType) {
        switch (alarmType) {
            case PRE_DOSE:
                return "Medication Reminder Soon";
            case ON_TIME:
                return "Time to Take Medication";
            case POST_DOSE:
                return "Medication Overdue";
            case REFILL_REMINDER:
                return "Refill Reminder";
            default:
                return "Medication Reminder";
        }
    }
    
    /**
     * Get notification message
     */
    private String getNotificationMessage(AlarmType alarmType, String medicationId, int remainingDoses) {
        switch (alarmType) {
            case PRE_DOSE:
                return "Your medication dose is coming up soon. Get ready!";
            case ON_TIME:
                return "It's time to take your medication. Scan NFC tag to confirm.";
            case POST_DOSE:
                return "You haven't confirmed your medication dose yet. Please take it now.";
            case REFILL_REMINDER:
                return String.format("Running low on medication. %d doses remaining.", remainingDoses);
            default:
                return "Medication reminder";
        }
    }
}
