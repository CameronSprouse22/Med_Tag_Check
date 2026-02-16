package com.medchecktag.database;

import androidx.room.TypeConverter;

import com.medchecktag.models.AlarmType;
import com.medchecktag.models.ConfirmationMethod;
import com.medchecktag.models.DoseStatus;
import com.medchecktag.models.MedicationCategory;
import com.medchecktag.models.ScheduleType;

import java.util.ArrayList;
import java.util.List;

/**
 * Room TypeConverters for converting complex types to database-compatible types.
 */
public class Converters {
    
    // MedicationCategory enum converters
    @TypeConverter
    public static String fromMedicationCategory(MedicationCategory category) {
        return category == null ? null : category.name();
    }
    
    @TypeConverter
    public static MedicationCategory toMedicationCategory(String value) {
        return value == null ? null : MedicationCategory.valueOf(value);
    }
    
    // ScheduleType enum converters
    @TypeConverter
    public static String fromScheduleType(ScheduleType scheduleType) {
        return scheduleType == null ? null : scheduleType.name();
    }
    
    @TypeConverter
    public static ScheduleType toScheduleType(String value) {
        return value == null ? null : ScheduleType.valueOf(value);
    }
    
    // DoseStatus enum converters
    @TypeConverter
    public static String fromDoseStatus(DoseStatus status) {
        return status == null ? null : status.name();
    }
    
    @TypeConverter
    public static DoseStatus toDoseStatus(String value) {
        return value == null ? null : DoseStatus.valueOf(value);
    }
    
    // ConfirmationMethod enum converters
    @TypeConverter
    public static String fromConfirmationMethod(ConfirmationMethod method) {
        return method == null ? null : method.name();
    }
    
    @TypeConverter
    public static ConfirmationMethod toConfirmationMethod(String value) {
        return value == null ? null : ConfirmationMethod.valueOf(value);
    }
    
    // AlarmType enum converters
    @TypeConverter
    public static String fromAlarmType(AlarmType alarmType) {
        return alarmType == null ? null : alarmType.name();
    }
    
    @TypeConverter
    public static AlarmType toAlarmType(String value) {
        return value == null ? null : AlarmType.valueOf(value);
    }
    
    // List<String> converters for Schedule.specificTimes
    @TypeConverter
    public static String fromStringList(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        return String.join(",", list);
    }
    
    @TypeConverter
    public static List<String> toStringList(String value) {
        if (value == null || value.isEmpty()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (String item : value.split(",")) {
            result.add(item.trim());
        }
        return result;
    }
}
