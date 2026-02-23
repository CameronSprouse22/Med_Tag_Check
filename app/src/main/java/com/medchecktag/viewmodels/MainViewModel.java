package com.medchecktag.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.medchecktag.alarms.AlarmScheduler;
import com.medchecktag.models.AlarmType;
import com.medchecktag.models.Medication;
import com.medchecktag.repositories.MedicationRepository;

import java.util.List;

/**
 * ViewModel for MainActivity.
 * Manages medication list display, countdown timers, and user actions.
 */
public class MainViewModel extends AndroidViewModel {
    
    private final MedicationRepository medicationRepository;
    private final AlarmScheduler alarmScheduler;
    private final LiveData<List<Medication>> activeMedications;
    private final LiveData<List<Medication>> allMedications;
    private final MutableLiveData<RefillResult> refillResult = new MutableLiveData<>();
    
    public MainViewModel(@NonNull Application application) {
        super(application);
        this.medicationRepository = new MedicationRepository(application);
        this.alarmScheduler = new AlarmScheduler(application);
        this.activeMedications = medicationRepository.getActiveMedications();
        this.allMedications = medicationRepository.getAllMedications();
    }
    
    /**
     * Get active medications sorted by next dose time (handled by DAO query).
     * Countdown timers and overdue detection are calculated in the UI layer.
     */
    public LiveData<List<Medication>> getActiveMedications() {
        return activeMedications;
    }
    
    /**
     * Get all medications (active first, then inactive) for full list display.
     * Used when showing inactive medications at the bottom of the list.
     */
    public LiveData<List<Medication>> getAllMedications() {
        return allMedications;
    }
    
    /**
     * Get refill operation result LiveData.
     */
    public LiveData<RefillResult> getRefillResult() {
        return refillResult;
    }
    
    /**
     * Handle "Med Refilled" button click.
     * Updates remaining doses to maxDoseCount.
     * 
     * @param medication The medication to refill
     */
    public void onMedRefilled(Medication medication) {
        if (medication == null) {
            refillResult.postValue(new RefillResult(false, "Invalid medication"));
            return;
        }
        
        medicationRepository.updateRemainingDoses(
            medication.id,
            medication.maxDoseCount,
            new MedicationRepository.OnResultCallback<Integer>() {
                @Override
                public void onSuccess(Integer result) {
                    // T147: Cancel refill alarms and notifications
                    alarmScheduler.cancelAlarm(medication.id, AlarmType.REFILL_REMINDER);

                    refillResult.postValue(new RefillResult(
                        true, 
                        "Medication refilled successfully"
                    ));
                }
                
                @Override
                public void onError(Exception error) {
                    refillResult.postValue(new RefillResult(
                        false, 
                        "Failed to update medication: " + error.getMessage()
                    ));
                }
            }
        );
    }
    
    /**
     * Result of refill operation.
     */
    public static class RefillResult {
        public final boolean success;
        public final String message;
        
        public RefillResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }
}
