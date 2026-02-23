package com.medchecktag.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.medchecktag.alarms.AlarmScheduler;
import com.medchecktag.models.AlarmConfiguration;
import com.medchecktag.models.Medication;
import com.medchecktag.models.MedicationCategory;
import com.medchecktag.models.Schedule;
import com.medchecktag.models.ScheduleType;
import com.medchecktag.repositories.MedicationRepository;
import com.medchecktag.utils.TimeUtils;

import java.util.List;
import java.util.UUID;

/**
 * ViewModel for Add Medication screen.
 * Manages medication creation state, validation, and repository calls.
 * Task T049 - User Story 2: Add Medication
 */
public class AddMedicationViewModel extends AndroidViewModel {
    
    private final MedicationRepository medicationRepository;
    
    // Form input state
    private final MutableLiveData<String> nickname = new MutableLiveData<>("");
    private final MutableLiveData<String> medicationInfo = new MutableLiveData<>("");
    private final MutableLiveData<String> dose = new MutableLiveData<>("");
    private final MutableLiveData<MedicationCategory> category = new MutableLiveData<>(MedicationCategory.BENEFICIAL);
    private final MutableLiveData<Integer> maxDoseCount = new MutableLiveData<>(30);
    private final MutableLiveData<Integer> refillThreshold1 = new MutableLiveData<>(7);
    private final MutableLiveData<Integer> refillThreshold2 = new MutableLiveData<>(3);
    
    // Schedule state
    private final MutableLiveData<ScheduleType> scheduleType = new MutableLiveData<>(ScheduleType.INTERVAL);
    private final MutableLiveData<Integer> intervalHours = new MutableLiveData<>(8);
    private final MutableLiveData<List<String>> specificTimes = new MutableLiveData<>();
    private final MutableLiveData<Long> endDate = new MutableLiveData<>();
    
    // Alarm configuration state
    private final MutableLiveData<Boolean> preAlarmEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> preAlarmMinutes = new MutableLiveData<>(15);
    private final MutableLiveData<Boolean> onTimeAlarmEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> postAlarmEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Integer> postAlarmMinutes = new MutableLiveData<>(30);
    
    // Operation result state
    private final MutableLiveData<SaveResult> saveResult = new MutableLiveData<>();
    private final MutableLiveData<String> validationError = new MutableLiveData<>();
    
    // Created medication ID (for NFC write)
    private String createdMedicationId;
    // Cached medication for alarm scheduling (T120)
    private Medication lastCreatedMedication;
    
    public AddMedicationViewModel(@NonNull Application application) {
        super(application);
        this.medicationRepository = new MedicationRepository(application);
    }
    
    // Getters for LiveData
    
    public LiveData<String> getNickname() {
        return nickname;
    }
    
    public LiveData<String> getMedicationInfo() {
        return medicationInfo;
    }
    
    public LiveData<String> getDose() {
        return dose;
    }
    
    public LiveData<MedicationCategory> getCategory() {
        return category;
    }
    
    public LiveData<Integer> getMaxDoseCount() {
        return maxDoseCount;
    }
    
    public LiveData<Integer> getRefillThreshold1() {
        return refillThreshold1;
    }
    
    public LiveData<Integer> getRefillThreshold2() {
        return refillThreshold2;
    }
    
    public LiveData<ScheduleType> getScheduleType() {
        return scheduleType;
    }
    
    public LiveData<Integer> getIntervalHours() {
        return intervalHours;
    }
    
    public LiveData<List<String>> getSpecificTimes() {
        return specificTimes;
    }
    
    public LiveData<Long> getEndDate() {
        return endDate;
    }
    
    public LiveData<Boolean> getPreAlarmEnabled() {
        return preAlarmEnabled;
    }
    
    public LiveData<Integer> getPreAlarmMinutes() {
        return preAlarmMinutes;
    }
    
    public LiveData<Boolean> getOnTimeAlarmEnabled() {
        return onTimeAlarmEnabled;
    }
    
    public LiveData<Boolean> getPostAlarmEnabled() {
        return postAlarmEnabled;
    }
    
    public LiveData<Integer> getPostAlarmMinutes() {
        return postAlarmMinutes;
    }
    
    public LiveData<SaveResult> getSaveResult() {
        return saveResult;
    }
    
    public LiveData<String> getValidationError() {
        return validationError;
    }
    
    public String getCreatedMedicationId() {
        return createdMedicationId;
    }
    
    // Setters for form inputs
    
    public void setNickname(String value) {
        nickname.setValue(value);
    }
    
    public void setMedicationInfo(String value) {
        medicationInfo.setValue(value);
    }
    
    public void setDose(String value) {
        dose.setValue(value);
    }
    
    public void setCategory(MedicationCategory value) {
        category.setValue(value);
    }
    
    public void setMaxDoseCount(int value) {
        maxDoseCount.setValue(value);
    }
    
    public void setRefillThreshold1(int value) {
        refillThreshold1.setValue(value);
    }
    
    public void setRefillThreshold2(int value) {
        refillThreshold2.setValue(value);
    }
    
    public void setScheduleType(ScheduleType value) {
        scheduleType.setValue(value);
    }
    
    public void setIntervalHours(int value) {
        intervalHours.setValue(value);
    }
    
    public void setSpecificTimes(List<String> value) {
        specificTimes.setValue(value);
    }
    
    public void setEndDate(Long value) {
        endDate.setValue(value);
    }
    
    public void setPreAlarmEnabled(boolean value) {
        preAlarmEnabled.setValue(value);
    }
    
    public void setPreAlarmMinutes(int value) {
        preAlarmMinutes.setValue(value);
    }
    
    public void setOnTimeAlarmEnabled(boolean value) {
        onTimeAlarmEnabled.setValue(value);
    }
    
    public void setPostAlarmEnabled(boolean value) {
        postAlarmEnabled.setValue(value);
    }
    
    public void setPostAlarmMinutes(int value) {
        postAlarmMinutes.setValue(value);
    }
    
    /**
     * Validate all form inputs
     * @return Validation error message, or null if valid
     */
    public String validateInputs() {
        // Nickname validation
        String nicknameValue = nickname.getValue();
        if (nicknameValue == null || nicknameValue.trim().isEmpty()) {
            return "Medication nickname is required";
        }
        if (nicknameValue.length() > 50) {
            return "Medication nickname must be 50 characters or less";
        }
        
        // Dose validation
        String doseValue = dose.getValue();
        if (doseValue == null || doseValue.trim().isEmpty()) {
            return "Dose amount is required";
        }
        if (doseValue.length() > 50) {
            return "Dose must be 50 characters or less";
        }
        
        // Medication info validation (optional)
        String infoValue = medicationInfo.getValue();
        if (infoValue != null && infoValue.length() > 500) {
            return "Medication info must be 500 characters or less";
        }
        
        // Category validation
        if (category.getValue() == null) {
            return "Medication category is required";
        }
        
        // Max dose count validation
        Integer maxCount = maxDoseCount.getValue();
        if (maxCount == null || maxCount < 1) {
            return "Max dose count must be at least 1";
        }
        
        // Refill threshold validation
        Integer threshold1 = refillThreshold1.getValue();
        Integer threshold2 = refillThreshold2.getValue();
        if (threshold1 == null || threshold2 == null) {
            return "Refill thresholds are required";
        }
        if (threshold2 < 0) {
            return "Refill threshold 2 cannot be negative";
        }
        if (threshold1 <= threshold2) {
            return "First refill threshold must be greater than second threshold";
        }
        if (threshold1 > maxCount) {
            return "First refill threshold cannot exceed max dose count";
        }
        
        // Schedule validation
        ScheduleType scheduleTypeValue = scheduleType.getValue();
        if (scheduleTypeValue == null) {
            return "Schedule type is required";
        }
        
        if (scheduleTypeValue == ScheduleType.INTERVAL) {
            Integer hours = intervalHours.getValue();
            if (hours == null || hours < 1 || hours > 24) {
                return "Interval hours must be between 1 and 24";
            }
        } else if (scheduleTypeValue == ScheduleType.SPECIFIC_TIMES) {
            List<String> times = specificTimes.getValue();
            if (times == null || times.isEmpty()) {
                return "At least one specific time is required";
            }
        }
        
        // Alarm validation
        Boolean preEnabled = preAlarmEnabled.getValue();
        if (preEnabled != null && preEnabled) {
            Integer preMinutes = preAlarmMinutes.getValue();
            if (preMinutes == null || preMinutes < 1 || preMinutes > 60) {
                return "Pre-alarm minutes must be between 1 and 60";
            }
        }
        
        Boolean postEnabled = postAlarmEnabled.getValue();
        if (postEnabled != null && postEnabled) {
            Integer postMinutes = postAlarmMinutes.getValue();
            if (postMinutes == null || postMinutes < 1 || postMinutes > 120) {
                return "Post-alarm minutes must be between 1 and 120";
            }
        }
        
        return null; // All validations passed
    }
    
    /**
     * Save medication to database
     */
    public void saveMedication() {
        // Validate inputs
        String error = validateInputs();
        if (error != null) {
            validationError.setValue(error);
            return;
        }
        
        // Clear any previous errors
        validationError.setValue(null);
        
        // Create Schedule object
        Schedule schedule = createSchedule();
        
        // Create AlarmConfiguration object
        AlarmConfiguration alarmConfig = createAlarmConfiguration();
        
        // Generate unique ID
        String medicationId = UUID.randomUUID().toString();
        this.createdMedicationId = medicationId;
        
        // Create Medication object
        Medication medication = new Medication(
            medicationId,
            nickname.getValue().trim(),
            dose.getValue().trim(),
            category.getValue(),
            maxDoseCount.getValue(),
            refillThreshold1.getValue(),
            refillThreshold2.getValue(),
            schedule,
            alarmConfig
        );
        
        // Set optional medication info
        String infoValue = medicationInfo.getValue();
        if (infoValue != null && !infoValue.trim().isEmpty()) {
            medication.medicationInfo = infoValue.trim();
        }

        // Cache for alarm scheduling
        this.lastCreatedMedication = medication;
        
        // Save to repository
        medicationRepository.insertMedication(medication, new MedicationRepository.OnResultCallback<Long>() {
            @Override
            public void onSuccess(Long result) {
                saveResult.postValue(new SaveResult(true, "Medication saved successfully", medicationId));
            }
            
            @Override
            public void onError(Exception error) {
                saveResult.postValue(new SaveResult(false, "Error saving medication: " + error.getMessage(), null));
            }
        });
    }
    
    /**
     * Create Schedule object from current state
     */
    private Schedule createSchedule() {
        ScheduleType type = scheduleType.getValue();
        Schedule schedule;
        
        if (type == ScheduleType.INTERVAL) {
            schedule = new Schedule(intervalHours.getValue());
        } else {
            schedule = new Schedule(specificTimes.getValue());
        }
        
        // Set end date if provided
        Long endDateValue = endDate.getValue();
        if (endDateValue != null) {
            schedule.endDate = endDateValue;
        }
        
        // Calculate initial next dose time
        long now = System.currentTimeMillis();
        if (type == ScheduleType.INTERVAL) {
            schedule.nextDoseTime = now + (intervalHours.getValue() * 60 * 60 * 1000L);
        } else {
            // For specific times, calculate next occurrence using TimeUtils
            schedule.nextDoseTime = TimeUtils.calculateNextDoseTime(schedule);
        }
        
        return schedule;
    }
    
    /**
     * Create AlarmConfiguration object from current state
     */
    private AlarmConfiguration createAlarmConfiguration() {
        AlarmConfiguration config = new AlarmConfiguration();
        
        config.preAlarmEnabled = preAlarmEnabled.getValue() != null && preAlarmEnabled.getValue();
        config.preAlarmMinutes = preAlarmMinutes.getValue();
        
        config.onTimeAlarmEnabled = onTimeAlarmEnabled.getValue() != null && onTimeAlarmEnabled.getValue();
        
        config.postAlarmEnabled = postAlarmEnabled.getValue() != null && postAlarmEnabled.getValue();
        config.postAlarmMinutes = postAlarmMinutes.getValue();
        
        return config;
    }
    
    /**
     * T120: Schedule alarms for the most recently created medication.
     */
    public void scheduleAlarms(AlarmScheduler alarmScheduler) {
        if (lastCreatedMedication != null && alarmScheduler != null) {
            alarmScheduler.scheduleAlarmsForMedication(lastCreatedMedication);
        }
    }

    /**
     * Result class for save operation
     */
    public static class SaveResult {
        public final boolean success;
        public final String message;
        public final String medicationId;
        
        public SaveResult(boolean success, String message, String medicationId) {
            this.success = success;
            this.message = message;
            this.medicationId = medicationId;
        }
    }
}
