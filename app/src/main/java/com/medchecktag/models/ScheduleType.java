package com.medchecktag.models;

/**
 * Enum representing schedule types for medication dosing.
 */
public enum ScheduleType {
    /**
     * Interval-based schedule: dose every X hours (e.g., every 8 hours).
     */
    INTERVAL,

    /**
     * Specific times schedule: dose at specific times of day (e.g., 8:00 AM, 2:00 PM, 8:00 PM).
     */
    SPECIFIC_TIMES
}
