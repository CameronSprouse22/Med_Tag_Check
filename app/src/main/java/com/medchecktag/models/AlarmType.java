package com.medchecktag.models;

/**
 * Enum representing alarm types for medication reminders.
 */
public enum AlarmType {
    /**
     * Alarm triggered X minutes before dose time.
     */
    PRE_DOSE,

    /**
     * Alarm triggered at exact dose time.
     */
    ON_TIME,

    /**
     * Alarm triggered X minutes after dose time if not taken.
     */
    POST_DOSE,

    /**
     * Alarm triggered when medication doses fall below refill threshold.
     */
    REFILL_REMINDER
}
