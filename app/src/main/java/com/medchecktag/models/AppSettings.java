package com.medchecktag.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * AppSettings entity representing global application settings (singleton).
 * Per data-model.md Section 5: AppSettings
 */
@Entity(tableName = "app_settings")
public class AppSettings {
    @PrimaryKey
    @NonNull
    public String id = "app_settings_singleton"; // Fixed ID for singleton pattern

    public boolean nfcEnabled = true; // Enable NFC scanning

    public boolean audioFeedbackEnabled = true; // Enable TTS audio feedback

    public boolean hapticFeedbackEnabled = true; // Vibration feedback

    public String defaultAlarmSound; // URI for default alarm sound (nullable = system default)

    public float defaultVolume = 0.8f; // Default alarm volume (0.0-1.0)

    public boolean autoMissedDoseDetection = true; // Auto-create MISSED records

    public int missedDoseThresholdMinutes = 30; // Minutes after scheduled time to mark as missed

    public boolean notificationsEnabled = true; // Enable system notifications

    public boolean emergencyContactAlerts = false; // Enable emergency contact notifications

    public String theme = "SYSTEM"; // Theme: LIGHT, DARK, SYSTEM

    public long createdAt; // Settings creation timestamp (milliseconds)

    public long updatedAt; // Last update timestamp (milliseconds)

    /**
     * Default constructor for Room
     */
    public AppSettings() {
        this.id = "app_settings_singleton";
        this.nfcEnabled = true;
        this.audioFeedbackEnabled = true;
        this.hapticFeedbackEnabled = true;
        this.defaultVolume = 0.8f;
        this.autoMissedDoseDetection = true;
        this.missedDoseThresholdMinutes = 30;
        this.notificationsEnabled = true;
        this.emergencyContactAlerts = false;
        this.theme = "SYSTEM";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Get singleton instance ID
     */
    public static String getSingletonId() {
        return "app_settings_singleton";
    }
}
