package com.medchecktag.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.medchecktag.models.Medication;
import com.medchecktag.models.MedicationCategory;

import java.util.List;

/**
 * Data Access Object for Medication entity.
 * Per contracts/MedicationRepositoryContract.md
 */
@Dao
public interface MedicationDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(Medication medication);
    
    @Update
    void update(Medication medication);
    
    @Delete
    void delete(Medication medication);
    
    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    LiveData<Medication> getById(String id);
    
    @Query("SELECT * FROM medications WHERE id = :id LIMIT 1")
    Medication getByIdSync(String id);
    
    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY category ASC, nickname ASC")
    LiveData<List<Medication>> getAllActive();
    
    @Query("SELECT * FROM medications WHERE isActive = 1 ORDER BY category ASC, nickname ASC")
    List<Medication> getAllActiveSync();
    
    @Query("SELECT * FROM medications ORDER BY isActive DESC, category ASC, nickname ASC")
    LiveData<List<Medication>> getAll();
    
    @Query("SELECT * FROM medications WHERE category = :category AND isActive = 1 ORDER BY nickname ASC")
    LiveData<List<Medication>> getByCategory(MedicationCategory category);
    
    @Query("SELECT * FROM medications WHERE remainingDoses <= refillThreshold2 AND isActive = 1")
    LiveData<List<Medication>> getCriticalRefillMedications();
    
    @Query("SELECT * FROM medications WHERE remainingDoses <= refillThreshold1 AND remainingDoses > refillThreshold2 AND isActive = 1")
    LiveData<List<Medication>> getWarningRefillMedications();
    
    @Query("UPDATE medications SET remainingDoses = :newCount WHERE id = :medicationId")
    void updateRemainingDoses(String medicationId, int newCount);
    
    @Query("UPDATE medications SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :medicationId")
    void updateActiveStatus(String medicationId, boolean isActive, long updatedAt);
    
    @Query("SELECT COUNT(*) FROM medications WHERE isActive = 1")
    LiveData<Integer> getActiveMedicationCount();
}
