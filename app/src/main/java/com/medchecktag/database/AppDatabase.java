package com.medchecktag.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

import com.medchecktag.models.AppSettings;
import com.medchecktag.models.DoseRecord;
import com.medchecktag.models.EmergencyContact;
import com.medchecktag.models.Medication;
import com.medchecktag.models.NFCTag;

/**
 * Room database for Med Check Tag application.
 * Local-first storage with offline support.
 */
@Database(
    entities = {
        Medication.class,
        DoseRecord.class,
        NFCTag.class,
        EmergencyContact.class,
        AppSettings.class
    },
    version = 1,
    exportSchema = true
)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    
    private static final String DATABASE_NAME = "med_check_tag.db";
    private static volatile AppDatabase INSTANCE;
    
    // DAO getters
    public abstract MedicationDao medicationDao();
    public abstract DoseRecordDao doseRecordDao();
    public abstract NFCTagDao nfcTagDao();
    public abstract EmergencyContactDao emergencyContactDao();
    public abstract AppSettingsDao appSettingsDao();
    
    /**
     * Get database singleton instance
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                        context.getApplicationContext(),
                        AppDatabase.class,
                        DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration() // For development
                    .build();
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * For testing purposes only
     */
    public static AppDatabase getInMemoryDatabase(Context context) {
        return Room.inMemoryDatabaseBuilder(
            context.getApplicationContext(),
            AppDatabase.class
        ).allowMainThreadQueries().build();
    }
}
