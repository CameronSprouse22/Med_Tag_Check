package com.medchecktag.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * NFCTag entity representing an NFC tag linked to a specific medication.
 * Per data-model.md Section 3: NFCTag
 */
@Entity(
    tableName = "nfc_tags",
    foreignKeys = @ForeignKey(
        entity = Medication.class,
        parentColumns = "id",
        childColumns = "medicationId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = {
        @Index(value = "tagId", unique = true),
        @Index(value = "medicationId")
    }
)
public class NFCTag {
    @PrimaryKey
    @NonNull
    public String id; // UUID v4 format

    @NonNull
    public String tagId; // Physical NFC tag unique identifier (from tag serial number)

    @NonNull
    public String medicationId; // Foreign key to Medication.id

    public String tagLabel; // Optional label for tag (0-100 chars)

    public long createdAt; // Tag registration timestamp (milliseconds)

    public long lastScannedAt; // Last scan timestamp (milliseconds, 0 if never scanned)

    /**
     * Default constructor for Room
     */
    public NFCTag() {
    }

    /**
     * Constructor for creating new NFC tag
     */
    @Ignore
    public NFCTag(@NonNull String id, @NonNull String tagId,
                  @NonNull String medicationId, String tagLabel) {
        this.id = id;
        this.tagId = tagId;
        this.medicationId = medicationId;
        this.tagLabel = tagLabel;
        this.createdAt = System.currentTimeMillis();
        this.lastScannedAt = 0;
    }
}
