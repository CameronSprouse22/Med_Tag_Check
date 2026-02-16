package com.medchecktag.models;

/**
 * AlarmConfiguration embedded object defining alarm settings for a medication.
 * Embedded within Medication entity.
 * Per data-model.md Section 3: AlarmConfiguration
 */
public class AlarmConfiguration {
    /**
     * Enable alarm before dose time
     */
    public boolean preAlarmEnabled = false;

    /**
     * Minutes before dose time to trigger pre-alarm (1-60).
     * Null if preAlarmEnabled = false.
     */
    public Integer preAlarmMinutes;

    /**
     * Enable alarm at exact dose time
     */
    public boolean onTimeAlarmEnabled = true;

    /**
     * Enable alarm after dose time if not taken
     */
    public boolean postAlarmEnabled = false;

    /**
     * Minutes after dose time to trigger post-alarm (1-120).
     * Null if postAlarmEnabled = false.
     */
    public Integer postAlarmMinutes;

    /**
     * URI to custom alarm sound. Null = use category default from AppSettings.
     */
    public String alarmSoundUri;

    /**
     * Volume level for alarm (0.0-1.0). Default = 0.8.
     */
    public float volumeLevel = 0.8f;

    /**
     * Default constructor - on-time alarm only
     */
    public AlarmConfiguration() {
        this.onTimeAlarmEnabled = true;
    }

    /**
     * Constructor with all alarm types
     */
    public AlarmConfiguration(boolean preAlarm, Integer preMinutes, 
                              boolean onTime, 
                              boolean postAlarm, Integer postMinutes) {
        this.preAlarmEnabled = preAlarm;
        this.preAlarmMinutes = preMinutes;
        this.onTimeAlarmEnabled = onTime;
        this.postAlarmEnabled = postAlarm;
        this.postAlarmMinutes = postMinutes;
    }
}
