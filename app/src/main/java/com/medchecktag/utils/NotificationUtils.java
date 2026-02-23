package com.medchecktag.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.medchecktag.R;
import com.medchecktag.models.MedicationCategory;

/**
 * NotificationUtils provides helper methods for creating and managing notifications.
 */
public class NotificationUtils {
    
    // Notification channel IDs
    public static final String CHANNEL_ID_DOSE_REMINDER = "dose_reminder";
    public static final String CHANNEL_ID_REFILL_WARNING = "refill_warning";
    public static final String CHANNEL_ID_REFILL_CRITICAL = "refill_critical";
    public static final String CHANNEL_ID_EMERGENCY = "emergency_alert";
    public static final String CHANNEL_ID_GENERAL = "general";
    
    // Notification IDs
    public static final int NOTIFICATION_ID_DOSE_BASE = 1000;
    public static final int NOTIFICATION_ID_REFILL_BASE = 2000;
    public static final int NOTIFICATION_ID_EMERGENCY = 9000;
    
    /**
     * Create all notification channels (Android O+)
     */
    public static void createNotificationChannels(Context context) {
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
            doseChannel.setDescription("Reminders to take your medication doses");
            doseChannel.enableVibration(true);
            doseChannel.enableLights(true);
            doseChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(doseChannel);
            
            // Refill warning channel (default priority)
            NotificationChannel refillWarningChannel = new NotificationChannel(
                CHANNEL_ID_REFILL_WARNING,
                "Refill Warnings",
                NotificationManager.IMPORTANCE_DEFAULT
            );
            refillWarningChannel.setDescription("Warnings when medication is running low");
            refillWarningChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(refillWarningChannel);
            
            // Refill critical channel (high priority)
            NotificationChannel refillCriticalChannel = new NotificationChannel(
                CHANNEL_ID_REFILL_CRITICAL,
                "Critical Refill Alerts",
                NotificationManager.IMPORTANCE_HIGH
            );
            refillCriticalChannel.setDescription("Critical alerts for very low medication");
            refillCriticalChannel.enableVibration(true);
            refillCriticalChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(refillCriticalChannel);
            
            // Emergency contact channel (urgent)
            NotificationChannel emergencyChannel = new NotificationChannel(
                CHANNEL_ID_EMERGENCY,
                "Emergency Alerts",
                NotificationManager.IMPORTANCE_HIGH
            );
            emergencyChannel.setDescription("Critical alerts for missed life-dependent medications");
            emergencyChannel.enableVibration(true);
            emergencyChannel.enableLights(true);
            emergencyChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(emergencyChannel);
            
            // General channel
            NotificationChannel generalChannel = new NotificationChannel(
                CHANNEL_ID_GENERAL,
                "General Notifications",
                NotificationManager.IMPORTANCE_LOW
            );
            generalChannel.setDescription("General app notifications");
            notificationManager.createNotificationChannel(generalChannel);
        }
    }
    
    /**
     * Build dose reminder notification
     */
    public static Notification buildDoseReminderNotification(
            Context context,
            String medicationNickname,
            String dose,
            MedicationCategory category,
            PendingIntent contentIntent) {
        
        String title = "Time to Take Medication";
        String message = String.format("%s - %s", medicationNickname, dose);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_DOSE_REMINDER)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: Replace with app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(getPriorityForCategory(category))
            .setAutoCancel(true)
            .setContentIntent(contentIntent);
        
        // Add category-specific styling
        if (category == MedicationCategory.LIFE_DEPENDENT) {
            builder.setColor(0xFFD32F2F); // Red
            builder.setCategory(NotificationCompat.CATEGORY_ALARM);
        } else if (category == MedicationCategory.VERY_IMPORTANT) {
            builder.setColor(0xFFFFA000); // Orange
        }
        
        return builder.build();
    }
    
    /**
     * Build refill reminder notification
     */
    public static Notification buildRefillReminderNotification(
            Context context,
            String medicationNickname,
            int remainingDoses,
            boolean isCritical,
            PendingIntent contentIntent) {
        
        String channelId = isCritical ? CHANNEL_ID_REFILL_CRITICAL : CHANNEL_ID_REFILL_WARNING;
        String title = isCritical ? "Critical: Refill Needed" : "Refill Reminder";
        String message = String.format("%s: %d doses remaining", medicationNickname, remainingDoses);
        
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(isCritical ? NotificationCompat.PRIORITY_HIGH : NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent);
        
        if (isCritical) {
            builder.setColor(0xFFD32F2F); // Red
        }
        
        return builder.build();
    }
    
    /**
     * Build emergency alert notification
     */
    public static Notification buildEmergencyAlertNotification(
            Context context,
            String medicationNickname,
            int missedDoseCount,
            PendingIntent contentIntent) {
        
        String title = "URGENT: Medication Missed";
        String message = String.format("You've missed %d doses of %s. Please take immediately!", 
            missedDoseCount, medicationNickname);
        
        return new NotificationCompat.Builder(context, CHANNEL_ID_EMERGENCY)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setColor(0xFFD32F2F) // Red
            .setContentIntent(contentIntent)
            .build();
    }
    
    /**
     * Build confirmation notification
     */
    public static Notification buildConfirmationNotification(
            Context context,
            String medicationNickname,
            String nextDoseTime) {
        
        String title = "Dose Confirmed";
        String message = String.format("%s confirmed. Next dose: %s", medicationNickname, nextDoseTime);
        
        return new NotificationCompat.Builder(context, CHANNEL_ID_GENERAL)
            .setSmallIcon(android.R.drawable.checkbox_on_background)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .setTimeoutAfter(5000) // Auto-dismiss after 5 seconds
            .build();
    }
    
    /**
     * Cancel notification
     */
    public static void cancelNotification(Context context, int notificationId) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.cancel(notificationId);
        }
    }
    
    /**
     * Cancel all notifications
     */
    public static void cancelAllNotifications(Context context) {
        NotificationManager notificationManager = 
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (notificationManager != null) {
            notificationManager.cancelAll();
        }
    }
    
    /**
     * Get notification priority based on medication category
     */
    private static int getPriorityForCategory(MedicationCategory category) {
        switch (category) {
            case LIFE_DEPENDENT:
                return NotificationCompat.PRIORITY_MAX;
            case VERY_IMPORTANT:
                return NotificationCompat.PRIORITY_HIGH;
            case BENEFICIAL:
            default:
                return NotificationCompat.PRIORITY_DEFAULT;
        }
    }
    
    /**
     * Generate notification ID for medication
     */
    public static int getNotificationIdForMedication(String medicationId, boolean isRefill) {
        int base = isRefill ? NOTIFICATION_ID_REFILL_BASE : NOTIFICATION_ID_DOSE_BASE;
        return base + Math.abs(medicationId.hashCode() % 1000);
    }

    /**
     * T143: Send a one-off refill warning notification when remainingDoses <= refillThreshold1.
     */
    public static void sendRefillWarningNotification(Context context, String nickname, int remainingDoses) {
        createNotificationChannels(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID_REFILL_WARNING)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(context.getString(R.string.refill_warning_title, nickname))
                .setContentText(context.getString(R.string.refill_warning_message, nickname, remainingDoses))
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        NotificationManagerCompat nm = NotificationManagerCompat.from(context);
        int id = NOTIFICATION_ID_REFILL_BASE + Math.abs(nickname.hashCode() % 1000);
        try {
            nm.notify(id, builder.build());
        } catch (SecurityException ignored) {
            // POST_NOTIFICATIONS permission not granted
        }
    }
}
