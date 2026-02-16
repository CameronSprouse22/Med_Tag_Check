package com.medchecktag.models;

import androidx.annotation.NonNull;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Medication entity representing a prescribed medication that user needs to track.
 * Per data-model.md Section 1: Medication
 */
@Entity(
    tableName = "medications",
    indices = {
        @Index(value = "isActive"),
        @Index(value = "remainingDoses")
    }
)
public class Medication {
    @PrimaryKey
    @NonNull
    public String id; // UUID v4 format

    @NonNull
    public String nickname; // User-friendly name (1-50 chars)

    public String medicationInfo; // Optional notes/instructions (0-500 chars)

    @NonNull
    public String dose; // Dose amount, e.g., "2 tablets", "5 mL" (1-50 chars)

    @NonNull
    public MedicationCategory category; // LIFE_DEPENDENT, VERY_IMPORTANT, BENEFICIAL

    public int maxDoseCount; // Maximum doses when full (for refill reset)

    public int remainingDoses; // Current remaining doses (0 to maxDoseCount)

    public int refillThreshold1; // First warning level (notification)

    public int refillThreshold2; // Critical level (alarm)

    public boolean isActive = true; // Active vs inactive (archived)

    public long createdAt; // Creation timestamp (milliseconds)

    public long updatedAt; // Last update timestamp (milliseconds)

    /**
     * Schedule configuration (embedded object)
     */
    @Embedded
    public Schedule schedule;

    /**
     * Alarm configuration (embedded object)
     */
    @Embedded(prefix = "alarm_")
    public AlarmConfiguration alarmConfig;

    /**
     * Default constructor for Room
     */
    public Medication() {
    }

    /**
     * Constructor for creating new medication
     */
    public Medication(@NonNull String id, @NonNull String nickname, @NonNull String dose,
                      @NonNull MedicationCategory category, int maxDoseCount,
                      int refillThreshold1, int refillThreshold2,
                      Schedule schedule, AlarmConfiguration alarmConfig) {
        this.id = id;
        this.nickname = nickname;
        this.dose = dose;
        this.category = category;
        this.maxDoseCount = maxDoseCount;
        this.remainingDoses = maxDoseCount; // Start with full count
        this.refillThreshold1 = refillThreshold1;
        this.refillThreshold2 = refillThreshold2;
        this.schedule = schedule;
        this.alarmConfig = alarmConfig != null ? alarmConfig : new AlarmConfiguration();
        this.isActive = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}
