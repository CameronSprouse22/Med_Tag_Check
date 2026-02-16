package com.medchecktag.models;

/**
 * Enum representing medication importance categories.
 * Used to determine alarm sounds, emergency notification thresholds, and default settings.
 */
public enum MedicationCategory {
    /**
     * Life-dependent medications (e.g., insulin, heart medication).
     * Highest priority for alarms and emergency notifications.
     */
    LIFE_DEPENDENT,

    /**
     * Very important medications (e.g., blood pressure, chronic condition management).
     * Medium priority for alarms and emergency notifications.
     */
    VERY_IMPORTANT,

    /**
     * Beneficial medications (e.g., vitamins, supplements).
     * Standard priority for alarms.
     */
    BENEFICIAL
}
