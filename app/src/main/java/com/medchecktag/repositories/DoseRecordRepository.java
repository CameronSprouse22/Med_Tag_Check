package com.medchecktag.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.medchecktag.database.AppDatabase;
import com.medchecktag.database.DoseRecordDao;
import com.medchecktag.models.DoseRecord;
import com.medchecktag.models.DoseStatus;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Repository for DoseRecord data access.
 * Implements IDoseRecordRepository contract.
 */
public class DoseRecordRepository {
    
    private final DoseRecordDao doseRecordDao;
    private final Executor executor;
    
    public DoseRecordRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.doseRecordDao = database.doseRecordDao();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    // Query Operations
    
    public LiveData<List<DoseRecord>> getDoseRecordsByMedication(String medicationId) {
        return doseRecordDao.getByMedicationId(medicationId);
    }
    
    public LiveData<List<DoseRecord>> getRecentDoseRecords(String medicationId, int limit) {
        return doseRecordDao.getRecentByMedicationId(medicationId, limit);
    }
    
    public LiveData<List<DoseRecord>> getDoseRecordsInRange(String medicationId, long startMillis, long endMillis) {
        return doseRecordDao.getByMedicationIdInRange(medicationId, startMillis, endMillis);
    }
    
    public LiveData<List<DoseRecord>> getDoseRecordsByStatus(DoseStatus status) {
        return doseRecordDao.getByStatus(status);
    }
    
    public LiveData<List<DoseRecord>> getRecentDoseRecords(int limit) {
        return doseRecordDao.getRecent(limit);
    }
    
    public LiveData<Integer> getMissedDoseCount(String medicationId) {
        return doseRecordDao.getMissedDoseCount(medicationId);
    }
    
    /**
     * Get missed dose count synchronously
     */
    public int getMissedDoseCountSync(String medicationId) {
        return doseRecordDao.getMissedDoseCountSync(medicationId);
    }
    
    /**
     * Get most recent dose record synchronously
     */
    public DoseRecord getLastDoseRecord(String medicationId) {
        return doseRecordDao.getByIdSync(medicationId);
    }
    
    // Mutation Operations
    
    public void insertDoseRecord(DoseRecord record, MedicationRepository.OnResultCallback<Long> callback) {
        validateDoseRecord(record);
        executor.execute(() -> {
            try {
                doseRecordDao.insert(record);
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
    
    public void deleteDoseRecordsByMedication(String medicationId, MedicationRepository.OnResultCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                doseRecordDao.deleteByMedicationId(medicationId);
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
    
    private void validateDoseRecord(DoseRecord record) {
        if (record.id == null || record.id.trim().isEmpty()) {
            throw new IllegalArgumentException("DoseRecord ID cannot be empty");
        }
        
        try {
            UUID.fromString(record.id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("DoseRecord ID must be valid UUID format");
        }
        
        if (record.medicationId == null || record.medicationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Medication ID cannot be empty");
        }
        
        if (record.scheduledTime <= 0) {
            throw new IllegalArgumentException("Scheduled time must be positive");
        }
        
        if (record.status == null) {
            throw new IllegalArgumentException("Status cannot be null");
        }
        
        if (record.confirmationMethod == null) {
            throw new IllegalArgumentException("Confirmation method cannot be null");
        }
        
        // If status is TAKEN, takenTime should be set
        if (record.status == DoseStatus.TAKEN && record.takenTime == null) {
            throw new IllegalArgumentException("Taken time must be set when status is TAKEN");
        }
    }
}
