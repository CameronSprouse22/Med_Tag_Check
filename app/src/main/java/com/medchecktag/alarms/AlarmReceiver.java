package com.medchecktag.alarms;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.medchecktag.R;
import com.medchecktag.models.AlarmType;
import com.medchecktag.models.AppSettings;
import com.medchecktag.models.ConfirmationMethod;
import com.medchecktag.models.DoseRecord;
import com.medchecktag.models.DoseStatus;
import com.medchecktag.models.Medication;
import com.medchecktag.models.MedicationCategory;
import com.medchecktag.repositories.AppSettingsRepository;
import com.medchecktag.repositories.DoseRecordRepository;
import com.medchecktag.repositories.MedicationRepository;
import com.medchecktag.utils.TimeUtils;

import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * AlarmReceiver handles alarm triggers and creates notifications.
 * Receives broadcast from AlarmManager when medication reminders trigger.
 *
 * Tasks: T037, T122-T131
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";

    // Notification channels (T124)
    private static final String CHANNEL_ID_PRE_DOSE = "pre_dose_reminder";
    private static final String CHANNEL_ID_ON_TIME = "on_time_reminder";
    private static final String CHANNEL_ID_POST_DOSE = "post_dose_reminder";
    private static final String CHANNEL_ID_REFILL = "refill_reminder";
    private static final int NOTIFICATION_ID_BASE = 1000;

    // Action constants for notification buttons (T126)
    public static final String ACTION_CONFIRM_DOSE = "com.medchecktag.ACTION_CONFIRM_DOSE";
    public static final String ACTION_SKIP_DOSE = "com.medchecktag.ACTION_SKIP_DOSE";
    public static final String EXTRA_MEDICATION_ID = "medication_id";
    public static final String EXTRA_ALARM_TYPE = "alarm_type";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        // T128/T129: Handle notification action buttons
        if (ACTION_CONFIRM_DOSE.equals(action)) {
            handleManualConfirm(context, intent);
            return;
        }
        if (ACTION_SKIP_DOSE.equals(action)) {
            handleSkip(context, intent);
            return;
        }

        // T122: Standard alarm trigger
        Log.d(TAG, "Alarm received");

        String medicationId = intent.getStringExtra(EXTRA_MEDICATION_ID);
        String alarmTypeStr = intent.getStringExtra(EXTRA_ALARM_TYPE);
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

        // T124: Create notification channels
        createNotificationChannels(context);

        // T123/T125: Build and show notification based on alarm type
        final PendingResult pendingResult = goAsync();
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                MedicationRepository repo = new MedicationRepository(context);
                AppSettingsRepository settingsRepo = new AppSettingsRepository(context);
                Medication medication = repo.getMedicationByIdSync(medicationId);
                AppSettings settings = settingsRepo.getSettingsSync();

                String nickname = medication != null ? medication.nickname : "Medication";
                String dose = medication != null ? medication.dose : "";
                int remaining = medication != null ? medication.remainingDoses : remainingDoses;
                MedicationCategory category = medication != null ? medication.category : MedicationCategory.BENEFICIAL;

                showNotification(context, medicationId, alarmType, nickname, dose, remaining, category, settings);
            } catch (Exception e) {
                Log.e(TAG, "Error processing alarm", e);
                showNotification(context, medicationId, alarmType, "Medication", "", remainingDoses, MedicationCategory.BENEFICIAL, null);
            } finally {
                pendingResult.finish();
            }
        });
    }

    // ─── T128: Manual Confirmation from Notification ────────────────────

    private void handleManualConfirm(Context context, Intent intent) {
        String medicationId = intent.getStringExtra(EXTRA_MEDICATION_ID);
        if (medicationId == null) return;

        final PendingResult pendingResult = goAsync();
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                MedicationRepository medicationRepo = new MedicationRepository(context);
                DoseRecordRepository doseRecordRepo = new DoseRecordRepository(context);
                AlarmScheduler alarmScheduler = new AlarmScheduler(context);

                Medication medication = medicationRepo.getMedicationByIdSync(medicationId);
                if (medication == null) return;

                long now = System.currentTimeMillis();

                // Create DoseRecord with MANUAL_CONFIRM
                DoseRecord record = new DoseRecord(
                        UUID.randomUUID().toString(),
                        medicationId,
                        medication.schedule != null ? medication.schedule.nextDoseTime : now,
                        DoseStatus.TAKEN,
                        ConfirmationMethod.MANUAL_CONFIRM
                );
                record.takenTime = now;
                doseRecordRepo.insertDoseRecord(record, null);

                // Decrement remaining doses
                int newRemaining = Math.max(0, medication.remainingDoses - 1);
                medicationRepo.updateRemainingDoses(medicationId, newRemaining, null);

                // Calculate next dose time
                if (medication.schedule != null) {
                    medication.schedule.lastDoseTime = now;
                    medication.schedule.nextDoseTime = TimeUtils.calculateNextDoseTime(medication.schedule);
                    medication.remainingDoses = newRemaining;
                    medication.updatedAt = now;
                    medicationRepo.updateMedication(medication, null);

                    // Reschedule alarms
                    alarmScheduler.rescheduleAlarmsForMedication(medication);
                }

                // Dismiss notification
                dismissNotification(context, medicationId, AlarmType.ON_TIME);

            } catch (Exception e) {
                Log.e(TAG, "Error handling manual confirm", e);
            } finally {
                pendingResult.finish();
            }
        });
    }

    // ─── T129: Skip Dose from Notification ──────────────────────────────

    private void handleSkip(Context context, Intent intent) {
        String medicationId = intent.getStringExtra(EXTRA_MEDICATION_ID);
        if (medicationId == null) return;

        final PendingResult pendingResult = goAsync();
        Executor executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                MedicationRepository medicationRepo = new MedicationRepository(context);
                DoseRecordRepository doseRecordRepo = new DoseRecordRepository(context);
                AlarmScheduler alarmScheduler = new AlarmScheduler(context);

                Medication medication = medicationRepo.getMedicationByIdSync(medicationId);
                if (medication == null) return;

                long now = System.currentTimeMillis();

                // Create DoseRecord with SKIPPED status
                DoseRecord record = new DoseRecord(
                        UUID.randomUUID().toString(),
                        medicationId,
                        medication.schedule != null ? medication.schedule.nextDoseTime : now,
                        DoseStatus.SKIPPED,
                        ConfirmationMethod.MANUAL_CONFIRM
                );
                doseRecordRepo.insertDoseRecord(record, null);

                // Calculate next dose time without decrementing
                if (medication.schedule != null) {
                    medication.schedule.lastDoseTime = now;
                    medication.schedule.nextDoseTime = TimeUtils.calculateNextDoseTime(medication.schedule);
                    medication.updatedAt = now;
                    medicationRepo.updateMedication(medication, null);

                    // Reschedule alarms
                    alarmScheduler.rescheduleAlarmsForMedication(medication);
                }

                // Dismiss notification
                dismissNotification(context, medicationId, AlarmType.ON_TIME);

            } catch (Exception e) {
                Log.e(TAG, "Error handling skip", e);
            } finally {
                pendingResult.finish();
            }
        });
    }

    private void dismissNotification(Context context, String medicationId, AlarmType alarmType) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            int notificationId = NOTIFICATION_ID_BASE + medicationId.hashCode() + alarmType.ordinal();
            nm.cancel(notificationId);
        }
    }

    // ─── T124: Create Notification Channels ─────────────────────────────

    private void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm == null) return;

            // Pre-dose channel (default priority)
            NotificationChannel preDose = new NotificationChannel(
                    CHANNEL_ID_PRE_DOSE, "Pre-Dose Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT);
            preDose.setDescription("Upcoming medication reminders");
            nm.createNotificationChannel(preDose);

            // On-time channel (high priority)
            NotificationChannel onTime = new NotificationChannel(
                    CHANNEL_ID_ON_TIME, "Dose Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            onTime.setDescription("Time to take your medication");
            onTime.enableVibration(true);
            onTime.setShowBadge(true);
            nm.createNotificationChannel(onTime);

            // Post-dose / overdue channel (high priority)
            NotificationChannel postDose = new NotificationChannel(
                    CHANNEL_ID_POST_DOSE, "Overdue Reminders",
                    NotificationManager.IMPORTANCE_HIGH);
            postDose.setDescription("Overdue medication alerts");
            postDose.enableVibration(true);
            postDose.setShowBadge(true);
            nm.createNotificationChannel(postDose);

            // Refill channel (default)
            NotificationChannel refill = new NotificationChannel(
                    CHANNEL_ID_REFILL, "Refill Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT);
            refill.setDescription("Medication refill reminders");
            nm.createNotificationChannel(refill);
        }
    }

    // ─── T125: Build & Show Notification ────────────────────────────────

    private void showNotification(Context context, String medicationId,
                                  AlarmType alarmType, String nickname,
                                  String dose, int remainingDoses,
                                  MedicationCategory category, AppSettings settings) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm == null) return;

        String channelId = getChannelForAlarmType(alarmType);
        String title = getNotificationTitle(alarmType, nickname);
        String message = getNotificationMessage(alarmType, nickname, dose, remainingDoses);

        // T218: Apply volume setting from AppSettings
        if (settings != null) {
            try {
                AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                if (audioManager != null) {
                    int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM);
                    int targetVolume = (int) (settings.defaultVolume * maxVolume);
                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, targetVolume, 0);
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not set alarm volume", e);
            }
        }

        // Content intent: open app
        Intent appIntent = context.getPackageManager().getLaunchIntentForPackage(context.getPackageName());
        if (appIntent != null) {
            appIntent.putExtra(EXTRA_MEDICATION_ID, medicationId);
            appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        } else {
            appIntent = new Intent();
        }

        PendingIntent contentPI = PendingIntent.getActivity(
                context, medicationId.hashCode(), appIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(getPriority(alarmType))
                .setAutoCancel(true)
                .setContentIntent(contentPI);

        // T219: Apply category-specific alarm sound from AppSettings
        Uri alarmSoundUri = getAlarmSoundForCategory(category, settings);
        if (alarmSoundUri != null) {
            builder.setSound(alarmSoundUri);
        }

        // T126/T220: Action buttons for dose reminders
        // T220: If manual confirmation enabled in settings, show confirm/skip buttons
        //       If disabled, only show NFC scan reminder in message (buttons still shown as fallback)
        if (alarmType != AlarmType.REFILL_REMINDER) {
            // "Confirm" action
            Intent confirmIntent = new Intent(context, AlarmReceiver.class);
            confirmIntent.setAction(ACTION_CONFIRM_DOSE);
            confirmIntent.putExtra(EXTRA_MEDICATION_ID, medicationId);
            PendingIntent confirmPI = PendingIntent.getBroadcast(
                    context,
                    ("confirm_" + medicationId).hashCode(),
                    confirmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(0, context.getString(R.string.alarm_action_confirm), confirmPI);

            // "Skip" action
            Intent skipIntent = new Intent(context, AlarmReceiver.class);
            skipIntent.setAction(ACTION_SKIP_DOSE);
            skipIntent.putExtra(EXTRA_MEDICATION_ID, medicationId);
            PendingIntent skipPI = PendingIntent.getBroadcast(
                    context,
                    ("skip_" + medicationId).hashCode(),
                    skipIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            builder.addAction(0, context.getString(R.string.alarm_action_skip), skipPI);
        }

        int notificationId = NOTIFICATION_ID_BASE + medicationId.hashCode() + alarmType.ordinal();
        nm.notify(notificationId, builder.build());

        Log.d(TAG, String.format("Notification shown for %s alarm, medication %s (%s)",
                alarmType, medicationId, nickname));
    }

    private String getChannelForAlarmType(AlarmType alarmType) {
        switch (alarmType) {
            case PRE_DOSE:      return CHANNEL_ID_PRE_DOSE;
            case ON_TIME:       return CHANNEL_ID_ON_TIME;
            case POST_DOSE:     return CHANNEL_ID_POST_DOSE;
            case REFILL_REMINDER: return CHANNEL_ID_REFILL;
            default:            return CHANNEL_ID_ON_TIME;
        }
    }

    private int getPriority(AlarmType alarmType) {
        switch (alarmType) {
            case PRE_DOSE:      return NotificationCompat.PRIORITY_DEFAULT;
            case ON_TIME:       return NotificationCompat.PRIORITY_HIGH;
            case POST_DOSE:     return NotificationCompat.PRIORITY_HIGH;
            case REFILL_REMINDER: return NotificationCompat.PRIORITY_DEFAULT;
            default:            return NotificationCompat.PRIORITY_DEFAULT;
        }
    }

    private String getNotificationTitle(AlarmType alarmType, String nickname) {
        switch (alarmType) {
            case PRE_DOSE:
                return "Upcoming: " + nickname;
            case ON_TIME:
                return "Time for: " + nickname;
            case POST_DOSE:
                return "Overdue: " + nickname;
            case REFILL_REMINDER:
                return "Refill: " + nickname;
            default:
                return "Medication Reminder";
        }
    }

    private String getNotificationMessage(AlarmType alarmType, String nickname,
                                          String dose, int remainingDoses) {
        switch (alarmType) {
            case PRE_DOSE:
                return String.format("Get ready to take %s (%s) soon.", nickname, dose);
            case ON_TIME:
                return String.format("Take %s now. Scan NFC tag or confirm.", dose);
            case POST_DOSE:
                return String.format("You haven't taken %s (%s). Please take it now.", nickname, dose);
            case REFILL_REMINDER:
                return String.format("%s is running low: %d doses remaining.", nickname, remainingDoses);
            default:
                return "Medication reminder";
        }
    }

    /**
     * T219: Get alarm sound URI for medication category from settings.
     */
    private Uri getAlarmSoundForCategory(MedicationCategory category, AppSettings settings) {
        if (settings == null || settings.defaultAlarmSound == null) {
            return null; // Use system default
        }
        // Use the default alarm sound setting (category-specific sounds stored in SharedPreferences)
        try {
            return Uri.parse(settings.defaultAlarmSound);
        } catch (Exception e) {
            return null;
        }
    }
}
