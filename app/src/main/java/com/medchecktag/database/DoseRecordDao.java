package com.medchecktag.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.medchecktag.models.DoseRecord;
import com.medchecktag.models.DoseStatus;

import java.util.List;

/**
 * Data Access Object for DoseRecord entity.
 * Per contracts/DoseRecordRepositoryContract.md
 */
@Dao
public interface DoseRecordDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DoseRecord doseRecord);
    
    @Update
    void update(DoseRecord doseRecord);
    
    @Delete
    void delete(DoseRecord doseRecord);
    
    @Query("SELECT * FROM dose_records WHERE id = :id LIMIT 1")
    LiveData<DoseRecord> getById(String id);
    
    @Query("SELECT * FROM dose_records WHERE id = :id LIMIT 1")
    DoseRecord getByIdSync(String id);
    
    @Query("SELECT * FROM dose_records WHERE medicationId = :medicationId ORDER BY scheduledTime DESC LIMIT 1")
    DoseRecord getLastByMedicationIdSync(String medicationId);
    
    @Query("SELECT * FROM dose_records WHERE medicationId = :medicationId ORDER BY scheduledTime DESC")
    LiveData<List<DoseRecord>> getByMedicationId(String medicationId);
    
    @Query("SELECT * FROM dose_records WHERE medicationId = :medicationId ORDER BY scheduledTime DESC LIMIT :limit")
    LiveData<List<DoseRecord>> getRecentByMedicationId(String medicationId, int limit);
    
    @Query("SELECT * FROM dose_records WHERE medicationId = :medicationId AND scheduledTime >= :startTime AND scheduledTime <= :endTime ORDER BY scheduledTime DESC")
    LiveData<List<DoseRecord>> getByMedicationIdInRange(String medicationId, long startTime, long endTime);
    
    @Query("SELECT * FROM dose_records WHERE status = :status ORDER BY scheduledTime DESC")
    LiveData<List<DoseRecord>> getByStatus(DoseStatus status);
    
    @Query("SELECT * FROM dose_records WHERE medicationId = :medicationId AND status = :status ORDER BY scheduledTime DESC")
    LiveData<List<DoseRecord>> getByMedicationIdAndStatus(String medicationId, DoseStatus status);
    
    @Query("SELECT * FROM dose_records WHERE scheduledTime >= :startTime AND scheduledTime <= :endTime ORDER BY scheduledTime DESC")
    LiveData<List<DoseRecord>> getInTimeRange(long startTime, long endTime);
    
    @Query("SELECT * FROM dose_records ORDER BY scheduledTime DESC LIMIT :limit")
    LiveData<List<DoseRecord>> getRecent(int limit);
    
    @Query("SELECT COUNT(*) FROM dose_records WHERE medicationId = :medicationId AND status = 'MISSED'")
    LiveData<Integer> getMissedDoseCount(String medicationId);
    
    @Query("SELECT COUNT(*) FROM dose_records WHERE medicationId = :medicationId AND status = 'MISSED'")
    int getMissedDoseCountSync(String medicationId);
    
    @Query("SELECT * FROM dose_records WHERE medicationId = :medicationId ORDER BY scheduledTime DESC LIMIT :limit")
    List<DoseRecord> getRecentByMedicationIdSync(String medicationId, int limit);
    
    @Query("DELETE FROM dose_records WHERE medicationId = :medicationId")
    void deleteByMedicationId(String medicationId);
}
