package com.medchecktag.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

/**
 * EmergencyContact entity representing a contact to notify when critical medication is missed.
 * Per data-model.md Section 4: EmergencyContact
 */
@Entity(tableName = "emergency_contacts")
public class EmergencyContact {
    @PrimaryKey
    @NonNull
    public String id; // UUID v4 format

    @NonNull
    public String name; // Contact name (1-100 chars)

    @NonNull
    public String phoneNumber; // Phone number (E.164 format preferred)

    public String email; // Optional email address (valid email format)

    public boolean isActive = true; // Whether to notify this contact

    public int triggerMissedDoses = 2; // Missed dose count threshold (1-10)

    public boolean onlyLifeDependentMeds = true; // Only notify for LIFE_DEPENDENT medications

    public long createdAt; // Contact creation timestamp (milliseconds)

    public long updatedAt; // Last update timestamp (milliseconds)

    /**
     * Default constructor for Room
     */
    public EmergencyContact() {
    }

    /**
     * Constructor for creating new emergency contact
     */
    @Ignore
    public EmergencyContact(@NonNull String id, @NonNull String name,
                            @NonNull String phoneNumber, String email) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.isActive = true;
        this.triggerMissedDoses = 2;
        this.onlyLifeDependentMeds = true;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    /**
     * Constructor with custom trigger settings
     */
    @Ignore
    public EmergencyContact(@NonNull String id, @NonNull String name,
                            @NonNull String phoneNumber, String email,
                            int triggerMissedDoses, boolean onlyLifeDependentMeds) {
        this.id = id;
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.isActive = true;
        this.triggerMissedDoses = Math.max(1, Math.min(10, triggerMissedDoses));
        this.onlyLifeDependentMeds = onlyLifeDependentMeds;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }
}
