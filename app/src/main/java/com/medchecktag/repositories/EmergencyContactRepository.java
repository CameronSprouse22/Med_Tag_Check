package com.medchecktag.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.medchecktag.database.AppDatabase;
import com.medchecktag.database.EmergencyContactDao;
import com.medchecktag.models.EmergencyContact;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Repository for EmergencyContact data access.
 * Implements IEmergencyContactRepository contract.
 */
public class EmergencyContactRepository {
    
    private final EmergencyContactDao emergencyContactDao;
    private final Executor executor;
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    public EmergencyContactRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.emergencyContactDao = database.emergencyContactDao();
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    // Query Operations
    
    public LiveData<List<EmergencyContact>> getAllContacts() {
        return emergencyContactDao.getAll();
    }
    
    public LiveData<List<EmergencyContact>> getAllActiveContacts() {
        return emergencyContactDao.getAllActive();
    }
    
    public LiveData<EmergencyContact> getContactById(String contactId) {
        return emergencyContactDao.getById(contactId);
    }
    
    public LiveData<Integer> getActiveContactCount() {
        return emergencyContactDao.getActiveCount();
    }
    
    /**
     * Get all active contacts synchronously (for emergency notifications)
     */
    public List<EmergencyContact> getAllActiveContactsSync() {
        return emergencyContactDao.getAllActiveSync();
    }
    
    /**
     * Get contact by ID synchronously
     */
    public EmergencyContact getContactByIdSync(String contactId) {
        return emergencyContactDao.getByIdSync(contactId);
    }
    
    // Mutation Operations
    
    public void insertContact(EmergencyContact contact, MedicationRepository.OnResultCallback<Long> callback) {
        executor.execute(() -> {
            try {
                validateEmergencyContact(contact);
                emergencyContactDao.insert(contact);
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
    
    public void updateContact(EmergencyContact contact, MedicationRepository.OnResultCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                validateEmergencyContact(contact);
                emergencyContactDao.update(contact);
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
    
    public void deleteContact(String contactId, MedicationRepository.OnResultCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                EmergencyContact contact = emergencyContactDao.getByIdSync(contactId);
                if (contact != null) {
                    emergencyContactDao.delete(contact);
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
    
    public void setContactActive(String contactId, boolean isActive, MedicationRepository.OnResultCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                emergencyContactDao.updateActiveStatus(contactId, isActive, System.currentTimeMillis());
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
    
    private void validateEmergencyContact(EmergencyContact contact) {
        if (contact.id == null || contact.id.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact ID cannot be empty");
        }
        
        try {
            UUID.fromString(contact.id);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Contact ID must be valid UUID format");
        }
        
        if (contact.name == null || contact.name.trim().isEmpty()) {
            throw new IllegalArgumentException("Contact name cannot be empty");
        }
        
        if (contact.phoneNumber == null || contact.phoneNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Phone number is required");
        }
        
        // Validate email format if provided
        if (contact.email != null && !contact.email.trim().isEmpty()) {
            if (!EMAIL_PATTERN.matcher(contact.email).matches()) {
                throw new IllegalArgumentException("Invalid email format");
            }
        }
        
        // Validate trigger settings
        if (contact.triggerMissedDoses < 1 || contact.triggerMissedDoses > 10) {
            throw new IllegalArgumentException("Trigger missed doses must be between 1 and 10");
        }
    }
}
