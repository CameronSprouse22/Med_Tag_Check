package com.medchecktag.models;

/**
 * Enum representing dose event status.
 */
public enum DoseStatus {
    /**
     * Dose was taken and confirmed (via NFC scan or manual confirmation).
     */
    TAKEN,

    /**
     * User explicitly marked dose as skipped.
     */
    SKIPPED,

    /**
     * Dose time passed without confirmation (system-generated).
     */
    MISSED
}
