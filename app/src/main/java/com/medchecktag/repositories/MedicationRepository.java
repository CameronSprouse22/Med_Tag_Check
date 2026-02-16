package com.medchecktag.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.medchecktag.database.AppDatabase;
import com.medchecktag.database.MedicationDao;
import com.medchecktag.models.Medication;
import com.medchecktag.models.MedicationCategory;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Repository for Medication data access.
 * Implements IMedicationRepository contract.
 */
public class MedicationRepository {
    
    private final MedicationDao medicationDao;
    private final Executor executor;
    
    public MedicationRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.medicationDao = database.medicationDao();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    // Query Operations
    
    public LiveData<List<Medication>> getAllMedications() {
        return medicationDao.getAll();
    }
    
    public LiveData<List<Medication>> getActiveMedications() {
        return medicationDao.getAllActive();
    }
    
    public LiveData<Medication> getMedicationById(String medicationId) {
        return medicationDao.getById(medicationId);
    }
    
    public LiveData<List<Medication>> getMedicationsByCategory(MedicationCategory category) {
        return medicationDao.getByCategory(category);
    }
    
    public LiveData<List<Medication>> getMedicationsNeedingRefill() {
        return medicationDao.getWarningRefillMedications();
    }
    
    public LiveData<List<Medication>> getCriticalRefillMedications() {
        return medicationDao.getCriticalRefillMedications();
    }
    
    public LiveData<Integer> getActiveMedicationCount() {
        return medicationDao.getActiveMedicationCount();
    }
    
    /**
     * Get medication by ID synchronously (for NFC operations)
     */
    public Medication getMedicationByIdSync(String medicationId) {
        return medicationDao.getByIdSync(medicationId);
    }
    
    // Mutation Operations
    
    public void insertMedication(Medication medication, OnResultCallback<Long> callback) {
        validateMedication(medication);
        executor.execute(() -> {
            try {
                medicationDao.insert(medication);
                if (callback != null) {
                    callback.onSuccess(1L);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    public void updateMedication(Medication medication, OnResultCallback<Integer> callback) {
        validateMedication(medication);
        executor.execute(() -> {
            try {
                medicationDao.update(medication);
                if (callback != null) {
                    callback.onSuccess(1);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    public void deleteMedication(String medicationId, OnResultCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                Medication medication = medicationDao.getByIdSync(medicationId);
                if (medication != null) {
                    medicationDao.delete(medication);
                    if (callback != null) {
                        callback.onSuccess(1);
                    }
                } else {
                    if (callback != null) {
                        callback.onSuccess(0);
                    }
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    public void markMedicationInactive(String medicationId, OnResultCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                medicationDao.updateActiveStatus(medicationId, false, System.currentTimeMillis());
                if (callback != null) {
                    callback.onSuccess(1);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    public void markMedicationActive(String medicationId, OnResultCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                medicationDao.updateActiveStatus(medicationId, true, System.currentTimeMillis());
                if (callback != null) {
                    callback.onSuccess(1);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    public void updateRemainingDoses(String medicationId, int newCount, OnResultCallback<Integer> callback) {
        if (newCount < 0) {
            if (callback != null) {
                callback.onError(new IllegalArgumentException("Remaining doses cannot be negative"));
            }
            return;
        }
        
        executor.execute(() -> {
            try {
                medicationDao.updateRemainingDoses(medicationId, newCount);
                if (callback != null) {
                    callback.onSuccess(1);
                }
            } catch (Exception e) {
                if (callback != null) {
                    callback.onError(e);
                }
            }
        });
    }
    
    // Validation
    
    private void validateMedication(Medication medication) {
        if (medication.id == null || medication.id.trim().isEmpty()) {
            throw new IllegalArgumentException("Medication ID cannot be empty");
        }
        
        try {
            UUID.fromString(medication.id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Medication ID must be valid UUID format");
        }
        
        if (medication.nickname == null || medication.nickname.trim().isEmpty()) {
            throw new IllegalArgumentException("Medication nickname cannot be empty");
        }
        
        if (medication.dose == null || medication.dose.trim().isEmpty()) {
            throw new IllegalArgumentException("Medication dose cannot be empty");
        }
        
        if (medication.maxDoseCount < 1) {
            throw new IllegalArgumentException("Max dose count must be at least 1");
        }
        
        if (medication.remainingDoses < 0 || medication.remainingDoses > medication.maxDoseCount) {
            throw new IllegalArgumentException("Remaining doses must be between 0 and maxDoseCount");
        }
        
        if (medication.refillThreshold1 <= medication.refillThreshold2 || medication.refillThreshold2 < 0) {
            throw new IllegalArgumentException("refillThreshold1 must be greater than refillThreshold2, both non-negative");
        }
        
        if (medication.schedule == null) {
            throw new IllegalArgumentException("Schedule cannot be null");
        }
    }
    
    // Callback interface
    
    public interface OnResultCallback<T> {
        void onSuccess(T result);
        void onError(Exception error);
    }
}
