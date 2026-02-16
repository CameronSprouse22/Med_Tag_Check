package com.medchecktag.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.medchecktag.models.NFCTag;

import java.util.List;

/**
 * Data Access Object for NFCTag entity.
 * Per contracts/NFCTagRepositoryContract.md
 */
@Dao
public interface NFCTagDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(NFCTag nfcTag);
    
    @Update
    void update(NFCTag nfcTag);
    
    @Delete
    void delete(NFCTag nfcTag);
    
    @Query("SELECT * FROM nfc_tags WHERE id = :id LIMIT 1")
    LiveData<NFCTag> getById(String id);
    
    @Query("SELECT * FROM nfc_tags WHERE id = :id LIMIT 1")
    NFCTag getByIdSync(String id);
    
    @Query("SELECT * FROM nfc_tags WHERE tagId = :tagId LIMIT 1")
    LiveData<NFCTag> getByTagId(String tagId);
    
    @Query("SELECT * FROM nfc_tags WHERE tagId = :tagId LIMIT 1")
    NFCTag getByTagIdSync(String tagId);
    
    @Query("SELECT * FROM nfc_tags WHERE medicationId = :medicationId")
    LiveData<List<NFCTag>> getByMedicationId(String medicationId);
    
    @Query("SELECT * FROM nfc_tags WHERE medicationId = :medicationId")
    List<NFCTag> getByMedicationIdSync(String medicationId);
    
    @Query("SELECT * FROM nfc_tags ORDER BY createdAt DESC")
    LiveData<List<NFCTag>> getAll();
    
    @Query("UPDATE nfc_tags SET lastScannedAt = :timestamp WHERE tagId = :tagId")
    void updateLastScanned(String tagId, long timestamp);
    
    @Query("DELETE FROM nfc_tags WHERE medicationId = :medicationId")
    void deleteByMedicationId(String medicationId);
    
    @Query("SELECT COUNT(*) FROM nfc_tags")
    LiveData<Integer> getCount();
}
