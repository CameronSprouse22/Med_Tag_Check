package com.medchecktag.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.medchecktag.models.EmergencyContact;

import java.util.List;

/**
 * Data Access Object for EmergencyContact entity.
 * Per contracts/EmergencyContactRepositoryContract.md
 */
@Dao
public interface EmergencyContactDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EmergencyContact emergencyContact);
    
    @Update
    void update(EmergencyContact emergencyContact);
    
    @Delete
    void delete(EmergencyContact emergencyContact);
    
    @Query("SELECT * FROM emergency_contacts WHERE id = :id LIMIT 1")
    LiveData<EmergencyContact> getById(String id);
    
    @Query("SELECT * FROM emergency_contacts WHERE id = :id LIMIT 1")
    EmergencyContact getByIdSync(String id);
    
    @Query("SELECT * FROM emergency_contacts WHERE isActive = 1 ORDER BY name ASC")
    LiveData<List<EmergencyContact>> getAllActive();
    
    @Query("SELECT * FROM emergency_contacts WHERE isActive = 1 ORDER BY name ASC")
    List<EmergencyContact> getAllActiveSync();
    
    @Query("SELECT * FROM emergency_contacts ORDER BY isActive DESC, name ASC")
    LiveData<List<EmergencyContact>> getAll();
    
    @Query("UPDATE emergency_contacts SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :contactId")
    void updateActiveStatus(String contactId, boolean isActive, long updatedAt);
    
    @Query("SELECT COUNT(*) FROM emergency_contacts WHERE isActive = 1")
    LiveData<Integer> getActiveCount();
}
