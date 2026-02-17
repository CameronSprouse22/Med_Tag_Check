package com.medchecktag.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * DoseRecord entity representing a single dose confirmation event.
 * Per data-model.md Section 2: DoseRecord
 */
@Entity(
    tableName = "dose_records",
    foreignKeys = @ForeignKey(
        entity = Medication.class,
        parentColumns = "id",
        childColumns = "medicationId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = "medicationId"),
        @Index(value = "scheduledTime"),
        @Index(value = "status")
    }
)
public class DoseRecord {
    @PrimaryKey
    @NonNull
    public String id; // UUID v4 format

    @NonNull
    public String medicationId; // Foreign key to Medication.id

    public long scheduledTime; // Scheduled dose time (milliseconds)

    public Long takenTime; // Actual confirmation time (milliseconds, nullable)

    @NonNull
    public DoseStatus status; // TAKEN, SKIPPED, MISSED

    @NonNull
    public ConfirmationMethod confirmationMethod; // NFC_SCAN, MANUAL_CONFIRM

    public String notes; // Optional user notes (0-200 chars)

    public long createdAt; // Record creation timestamp (milliseconds)

    /**
     * Default constructor for Room
     */
    public DoseRecord() {
    }

    /**
     * Constructor for creating new dose record
     */
    @Ignore
    public DoseRecord(@NonNull String id, @NonNull String medicationId,
                      long scheduledTime, @NonNull DoseStatus status,
                      @NonNull ConfirmationMethod confirmationMethod) {
        this.id = id;
        this.medicationId = medicationId;
        this.scheduledTime = scheduledTime;
        this.status = status;
        this.confirmationMethod = confirmationMethod;
        this.createdAt = System.currentTimeMillis();
        
        // Set takenTime only if status is TAKEN
        if (status == DoseStatus.TAKEN) {
            this.takenTime = System.currentTimeMillis();
        }
    }

    /**
     * Constructor with takenTime for recording past events
     */
    @Ignore
    public DoseRecord(@NonNull String id, @NonNull String medicationId,
                      long scheduledTime, Long takenTime, @NonNull DoseStatus status,
                      @NonNull ConfirmationMethod confirmationMethod, String notes) {
        this.id = id;
        this.medicationId = medicationId;
        this.scheduledTime = scheduledTime;
        this.takenTime = takenTime;
        this.status = status;
        this.confirmationMethod = confirmationMethod;
        this.notes = notes;
        this.createdAt = System.currentTimeMillis();
    }
}
