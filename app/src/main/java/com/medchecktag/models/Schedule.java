package com.medchecktag.models;

import androidx.room.Embedded;

import java.util.List;

/**
 * Schedule embedded object defining when medication doses should be taken.
 * Embedded within Medication entity.
 * Per data-model.md Section 2: Schedule
 */
public class Schedule {
    /**
     * Schedule mode: INTERVAL (every X hours) or SPECIFIC_TIMES (specific times of day)
     */
    public ScheduleType scheduleType;

    /**
     * Hours between doses (for INTERVAL type). Null for SPECIFIC_TIMES.
     * Range: 1-24 hours
     */
    public Integer intervalHours;

    /**
     * List of specific times in HH:mm format (for SPECIFIC_TIMES type). Null for INTERVAL.
     * Example: ["08:00", "14:00", "20:00"]
     * Stored as comma-separated string in database, converted by TypeConverter
     */
    public List<String> specificTimes;

    /**
     * Optional end date for schedule (timestamp in milliseconds).
     * Alarms stop after this date. Null means no end date.
     */
    public Long endDate;

    /**
     * Calculated next dose time (timestamp in milliseconds).
     * Recalculated after each dose confirmation.
     */
    public long nextDoseTime;

    /**
     * Last confirmed dose time (timestamp in milliseconds).
     * Null if no doses taken yet.
     */
    public Long lastDoseTime;

    /**
     * Constructor for interval-based schedule
     */
    public Schedule(int intervalHours) {
        this.scheduleType = ScheduleType.INTERVAL;
        this.intervalHours = intervalHours;
        this.specificTimes = null;
    }

    /**
     * Constructor for specific times schedule
     */
    public Schedule(List<String> specificTimes) {
        this.scheduleType = ScheduleType.SPECIFIC_TIMES;
        this.intervalHours = null;
        this.specificTimes = specificTimes;
    }

    /**
     * Default constructor for Room
     */
    public Schedule() {
    }
}
