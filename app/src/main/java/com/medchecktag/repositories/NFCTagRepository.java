package com.medchecktag.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.medchecktag.database.AppDatabase;
import com.medchecktag.database.NFCTagDao;
import com.medchecktag.models.NFCTag;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Repository for NFCTag data access.
 * Implements INFCTagRepository contract.
 */
public class NFCTagRepository {
    
    private final NFCTagDao nfcTagDao;
    private final Executor executor;
    
    public NFCTagRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.nfcTagDao = database.nfcTagDao();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    // Query Operations
    
    public LiveData<List<NFCTag>> getAllTags() {
        return nfcTagDao.getAll();
    }
    
    public LiveData<List<NFCTag>> getTagsByMedication(String medicationId) {
        return nfcTagDao.getByMedicationId(medicationId);
    }
    
    public LiveData<NFCTag> getTagByTagId(String tagId) {
        return nfcTagDao.getByTagId(tagId);
    }
    
    public LiveData<Integer> getTagCount() {
        return nfcTagDao.getCount();
    }
    
    /**
     * Get tag by tag ID synchronously (for NFC scan operations)
     */
    public NFCTag getTagByTagIdSync(String tagId) {
        return nfcTagDao.getByTagIdSync(tagId);
    }
    
    /**
     * Get medication ID for tag synchronously (for fast NFC lookup)
     */
    public String getMedicationIdByTagId(String tagId) {
        NFCTag tag = nfcTagDao.getByTagIdSync(tagId);
        return tag != null ? tag.medicationId : null;
    }
    
    /**
     * Check if tag is already assigned synchronously
     */
    public boolean isTagAlreadyAssigned(String tagId) {
        return nfcTagDao.getByTagIdSync(tagId) != null;
    }
    
    // Mutation Operations
    
    public void insertNFCTag(NFCTag tag, MedicationRepository.OnResultCallback<Long> callback) {
        validateNFCTag(tag);
        
        executor.execute(() -> {
            try {
                // Check if tag already assigned
                if (isTagAlreadyAssigned(tag.tagId)) {
                    if (callback != null) {
                        callback.onError(new IllegalArgumentException("NFC tag is already assigned to another medication"));
                    }
                    return;
                }
                
                nfcTagDao.insert(tag);
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
    
    public void updateNFCTag(NFCTag tag, MedicationRepository.OnResultCallback<Integer> callback) {
        validateNFCTag(tag);
        executor.execute(() -> {
            try {
                nfcTagDao.update(tag);
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
    
    public void updateLastScannedTime(String tagId, long timestamp, MedicationRepository.OnResultCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                nfcTagDao.updateLastScanned(tagId, timestamp);
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
    
    public void deleteNFCTag(String tagId, MedicationRepository.OnResultCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                NFCTag tag = nfcTagDao.getByTagIdSync(tagId);
                if (tag != null) {
                    nfcTagDao.delete(tag);
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
    
    public void deleteTagsByMedication(String medicationId, MedicationRepository.OnResultCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                nfcTagDao.deleteByMedicationId(medicationId);
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
    
    private void validateNFCTag(NFCTag tag) {
        if (tag.id == null || tag.id.trim().isEmpty()) {
            throw new IllegalArgumentException("NFCTag ID cannot be empty");
        }
        
        try {
            UUID.fromString(tag.id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("NFCTag ID must be valid UUID format");
        }
        
        if (tag.tagId == null || tag.tagId.trim().isEmpty()) {
            throw new IllegalArgumentException("Physical tag ID cannot be empty");
        }
        
        if (tag.medicationId == null || tag.medicationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Medication ID cannot be empty");
        }
    }
}
