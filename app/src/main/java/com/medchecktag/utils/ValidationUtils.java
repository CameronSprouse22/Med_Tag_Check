package com.medchecktag.utils;

import android.text.TextUtils;

import java.util.UUID;
import java.util.regex.Pattern;

/**
 * ValidationUtils provides validation methods for user input and data formats.
 */
public class ValidationUtils {
    
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TIME_PATTERN_24H = Pattern.compile(
        "^([01]?[0-9]|2[0-3]):[0-5][0-9]$"
    );
    
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[0-9]{10,15}$"
    );
    
    /**
     * Validate UUID format
     */
    public static boolean isValidUUID(String uuid) {
        if (TextUtils.isEmpty(uuid)) {
            return false;
        }
        return UUID_PATTERN.matcher(uuid.trim()).matches();
    }
    
    /**
     * Generate new UUID v4
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Validate time format (HH:mm, 24-hour)
     */
    public static boolean isValidTimeFormat(String time) {
        if (TextUtils.isEmpty(time)) {
            return false;
        }
        return TIME_PATTERN_24H.matcher(time.trim()).matches();
    }
    
    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        if (TextUtils.isEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
    
    /**
     * Validate phone number format
     */
    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phoneNumber.trim()).matches();
    }
    
    /**
     * Validate medication nickname (1-50 chars, not empty)
     */
    public static boolean isValidMedicationNickname(String nickname) {
        if (TextUtils.isEmpty(nickname)) {
            return false;
        }
        String trimmed = nickname.trim();
        return trimmed.length() >= 1 && trimmed.length() <= 50;
    }
    
    /**
     * Validate dose string (1-50 chars, not empty)
     */
    public static boolean isValidDose(String dose) {
        if (TextUtils.isEmpty(dose)) {
            return false;
        }
        String trimmed = dose.trim();
        return trimmed.length() >= 1 && trimmed.length() <= 50;
    }
    
    /**
     * Validate medication info (0-500 chars)
     */
    public static boolean isValidMedicationInfo(String info) {
        if (info == null) {
            return true; // Optional field
        }
        return info.length() <= 500;
    }
    
    /**
     * Validate interval hours (1-24)
     */
    public static boolean isValidIntervalHours(int hours) {
        return hours >= 1 && hours <= 24;
    }
    
    /**
     * Validate dose count (>= 0)
     */
    public static boolean isValidDoseCount(int count) {
        return count >= 0;
    }
    
    /**
     * Validate refill thresholds (threshold1 > threshold2 >= 0)
     */
    public static boolean isValidRefillThresholds(int threshold1, int threshold2) {
        return threshold1 > threshold2 && threshold2 >= 0;
    }
    
    /**
     * Validate alarm minutes (1-120)
     */
    public static boolean isValidAlarmMinutes(int minutes) {
        return minutes >= 1 && minutes <= 120;
    }
    
    /**
     * Validate volume level (0.0-1.0)
     */
    public static boolean isValidVolumeLevel(float volume) {
        return volume >= 0.0f && volume <= 1.0f;
    }
    
    /**
     * Validate contact name (1-100 chars)
     */
    public static boolean isValidContactName(String name) {
        if (TextUtils.isEmpty(name)) {
            return false;
        }
        String trimmed = name.trim();
        return trimmed.length() >= 1 && trimmed.length() <= 100;
    }
    
    /**
     * Validate trigger missed doses (1-10)
     */
    public static boolean isValidTriggerMissedDoses(int count) {
        return count >= 1 && count <= 10;
    }
    
    /**
     * Sanitize string input (trim and limit length)
     */
    public static String sanitizeString(String input, int maxLength) {
        if (input == null) {
            return "";
        }
        String trimmed = input.trim();
        if (trimmed.length() > maxLength) {
            return trimmed.substring(0, maxLength);
        }
        return trimmed;
    }
}
