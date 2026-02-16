package com.medchecktag.utils;

import com.medchecktag.models.Schedule;
import com.medchecktag.models.ScheduleType;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * TimeUtils provides time calculation and formatting utilities.
 */
public class TimeUtils {
    
    private static final SimpleDateFormat TIME_FORMAT_24H = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault());
    
    /**
     * Calculate next dose time based on schedule configuration
     */
    public static long calculateNextDoseTime(Schedule schedule) {
        if (schedule == null) {
            return System.currentTimeMillis();
        }
        
        Calendar now = Calendar.getInstance();
        long currentTime = now.getTimeInMillis();
        
        // Check if there's an end date and we've passed it
        if (schedule.endDate != null && currentTime > schedule.endDate) {
            return -1; // Schedule has ended
        }
        
        if (schedule.scheduleType == ScheduleType.INTERVAL) {
            return calculateNextIntervalDoseTime(schedule, currentTime);
        } else if (schedule.scheduleType == ScheduleType.SPECIFIC_TIMES) {
            return calculateNextSpecificTimeDoseTime(schedule, currentTime);
        }
        
        return currentTime;
    }
    
    /**
     * Calculate next dose time for interval-based schedule
     */
    private static long calculateNextIntervalDoseTime(Schedule schedule, long currentTime) {
        if (schedule.lastDoseTime == null || schedule.lastDoseTime == 0) {
            // No previous dose, start from now
            return currentTime;
        }
        
        long intervalMillis = schedule.intervalHours * 60L * 60L * 1000L;
        long nextTime = schedule.lastDoseTime + intervalMillis;
        
        // If next time is in the past, calculate the next future occurrence
        while (nextTime < currentTime) {
            nextTime += intervalMillis;
        }
        
        return nextTime;
    }
    
    /**
     * Calculate next dose time for specific-times schedule
     */
    private static long calculateNextSpecificTimeDoseTime(Schedule schedule, long currentTime) {
        if (schedule.specificTimes == null || schedule.specificTimes.isEmpty()) {
            return currentTime;
        }
        
        Calendar now = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        
        // Try times today
        for (String timeStr : schedule.specificTimes) {
            int[] timeParts = parseTime(timeStr);
            if (timeParts == null) continue;
            
            target.set(Calendar.HOUR_OF_DAY, timeParts[0]);
            target.set(Calendar.MINUTE, timeParts[1]);
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);
            
            if (target.getTimeInMillis() > currentTime) {
                return target.getTimeInMillis();
            }
        }
        
        // No more times today, use first time tomorrow
        target.add(Calendar.DAY_OF_MONTH, 1);
        String firstTime = schedule.specificTimes.get(0);
        int[] timeParts = parseTime(firstTime);
        if (timeParts != null) {
            target.set(Calendar.HOUR_OF_DAY, timeParts[0]);
            target.set(Calendar.MINUTE, timeParts[1]);
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);
            return target.getTimeInMillis();
        }
        
        return currentTime + (24 * 60 * 60 * 1000L); // Default to 24 hours from now
    }
    
    /**
     * Parse time string "HH:mm" to [hour, minute]
     */
    private static int[] parseTime(String timeStr) {
        if (timeStr == null || !timeStr.contains(":")) {
            return null;
        }
        
        try {
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0].trim());
            int minute = Integer.parseInt(parts[1].trim());
            
            if (hour >= 0 && hour < 24 && minute >= 0 && minute < 60) {
                return new int[]{hour, minute};
            }
        } catch (NumberFormatException e) {
            return null;
        }
        
        return null;
    }
    
    /**
     * Format timestamp to time string (HH:mm)
     */
    public static String formatTime(long timestampMillis) {
        return TIME_FORMAT_24H.format(new Date(timestampMillis));
    }
    
    /**
     * Format timestamp to date string (MMM d, yyyy)
     */
    public static String formatDate(long timestampMillis) {
        return DATE_FORMAT.format(new Date(timestampMillis));
    }
    
    /**
     * Format timestamp to date-time string (MMM d, yyyy HH:mm)
     */
    public static String formatDateTime(long timestampMillis) {
        return DATE_TIME_FORMAT.format(new Date(timestampMillis));
    }
    
    /**
     * Format relative time (e.g., "in 30 minutes", "2 hours ago")
     */
    public static String formatRelativeTime(long timestampMillis) {
        long now = System.currentTimeMillis();
        long diff = timestampMillis - now;
        long absDiff = Math.abs(diff);
        
        boolean future = diff > 0;
        
        long minutes = absDiff / (60 * 1000L);
        long hours = minutes / 60;
        long days = hours / 24;
        
        String timeStr;
        if (minutes < 1) {
            timeStr = "now";
        } else if (minutes < 60) {
            timeStr = minutes + (minutes == 1 ? " minute" : " minutes");
        } else if (hours < 24) {
            timeStr = hours + (hours == 1 ? " hour" : " hours");
        } else {
            timeStr = days + (days == 1 ? " day" : " days");
        }
        
        if (timeStr.equals("now")) {
            return "now";
        }
        
        return future ? "in " + timeStr : timeStr + " ago";
    }
    
    /**
     * Get start of day timestamp
     */
    public static long getStartOfDay(long timestampMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestampMillis);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
    
    /**
     * Get end of day timestamp
     */
    public static long getEndOfDay(long timestampMillis) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestampMillis);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }
    
    /**
     * Check if timestamp is today
     */
    public static boolean isToday(long timestampMillis) {
        long todayStart = getStartOfDay(System.currentTimeMillis());
        long todayEnd = getEndOfDay(System.currentTimeMillis());
        return timestampMillis >= todayStart && timestampMillis <= todayEnd;
    }
    
    /**
     * Check if timestamp is overdue (in the past)
     */
    public static boolean isOverdue(long timestampMillis) {
        return timestampMillis < System.currentTimeMillis();
    }
    
    /**
     * Get time zone offset in hours
     */
    public static int getTimeZoneOffsetHours() {
        TimeZone tz = TimeZone.getDefault();
        int offsetMillis = tz.getRawOffset();
        return offsetMillis / (60 * 60 * 1000);
    }
}
