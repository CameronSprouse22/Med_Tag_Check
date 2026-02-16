package com.medchecktag.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.medchecktag.models.AppSettings;

/**
 * Data Access Object for AppSettings entity (singleton).
 * Per contracts/AppSettingsRepositoryContract.md
 */
@Dao
public interface AppSettingsDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppSettings appSettings);
    
    @Update
    void update(AppSettings appSettings);
    
    @Query("SELECT * FROM app_settings WHERE id = :id LIMIT 1")
    LiveData<AppSettings> getSettings(String id);
    
    @Query("SELECT * FROM app_settings WHERE id = :id LIMIT 1")
    AppSettings getSettingsSync(String id);
    
    @Query("SELECT * FROM app_settings LIMIT 1")
    LiveData<AppSettings> getSingleton();
    
    @Query("SELECT * FROM app_settings LIMIT 1")
    AppSettings getSingletonSync();
}
