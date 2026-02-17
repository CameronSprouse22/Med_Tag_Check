package com.medchecktag.repositories;

import android.content.Context;

import androidx.lifecycle.LiveData;

import com.medchecktag.database.AppDatabase;
import com.medchecktag.database.AppSettingsDao;
import com.medchecktag.models.AppSettings;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Repository for AppSettings data access (singleton).
 * Implements IAppSettingsRepository contract.
 */
public class AppSettingsRepository {
    
    private final AppSettingsDao appSettingsDao;
    private final Executor executor;
    
    public AppSettingsRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        this.appSettingsDao = database.appSettingsDao();
        this.executor = Executors.newSingleThreadExecutor();
        
        // Ensure default settings exist
        initializeDefaultSettings();
    }
    
    // Query Operations
    
    public LiveData<AppSettings> getSettings() {
        return appSettingsDao.getSingleton();
    }
    
    /**
     * Get settings synchronously (for immediate use)
     */
    public AppSettings getSettingsSync() {
        AppSettings settings = appSettingsDao.getSingletonSync();
        if (settings == null) {
            // Create default settings if not exists
            settings = new AppSettings();
            appSettingsDao.insert(settings);
        }
        return settings;
    }
    
    // Mutation Operations
    
    public void updateSettings(AppSettings settings, MedicationRepository.OnResultCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                validateAppSettings(settings);
                // Ensure singleton ID
                settings.id = AppSettings.getSingletonId();
                settings.updatedAt = System.currentTimeMillis();
                
                appSettingsDao.update(settings);
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
    
    public void resetToDefaults(MedicationRepository.OnResultCallback<Integer> callback) {
        executor.execute(() -> {
            try {
                AppSettings defaults = new AppSettings();
                appSettingsDao.update(defaults);
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
    
    // Initialization
    
    private void initializeDefaultSettings() {
        executor.execute(() -> {
            AppSettings existing = appSettingsDao.getSingletonSync();
            if (existing == null) {
                AppSettings defaults = new AppSettings();
                appSettingsDao.insert(defaults);
            }
        });
    }
    
    // Validation
    
    private void validateAppSettings(AppSettings settings) {
        if (settings.defaultVolume < 0.0f || settings.defaultVolume > 1.0f) {
            throw new IllegalArgumentException("Default volume must be between 0.0 and 1.0");
        }
        
        if (settings.missedDoseThresholdMinutes < 1 || settings.missedDoseThresholdMinutes > 1440) {
            throw new IllegalArgumentException("Missed dose threshold must be between 1 and 1440 minutes (24 hours)");
        }
        
        if (settings.theme == null || settings.theme.trim().isEmpty()) {
            throw new IllegalArgumentException("Theme cannot be null");
        }
        
        if (!settings.theme.equals("LIGHT") && !settings.theme.equals("DARK") && !settings.theme.equals("SYSTEM")) {
            throw new IllegalArgumentException("Theme must be LIGHT, DARK, or SYSTEM");
        }
    }
}
