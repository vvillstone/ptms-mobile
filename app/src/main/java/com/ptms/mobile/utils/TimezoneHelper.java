package com.ptms.mobile.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * ✅ TIMEZONE HELPER FOR INTERNATIONAL SUPPORT
 *
 * Handles timezone conversion between device timezone and server UTC
 *
 * USAGE:
 * - Always use Locale.US for ISO date formatters (avoid locale-specific issues)
 * - Store user's timezone preference in SharedPreferences
 * - Send timezone info with ALL API requests
 * - Convert times to UTC before sending to server
 *
 * @version 1.0
 * @date 2025-01-22
 */
public class TimezoneHelper {

    private static final String TAG = "TimezoneHelper";
    private static final String PREFS_NAME = "timezone_prefs";
    private static final String KEY_USER_TIMEZONE = "user_timezone";
    private static final String KEY_USE_DEVICE_TIMEZONE = "use_device_timezone";

    /**
     * ISO date format for API communication (ALWAYS use Locale.US for consistency)
     */
    public static final SimpleDateFormat ISO_DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.US);

    /**
     * ISO datetime format for API communication (ALWAYS use Locale.US)
     */
    public static final SimpleDateFormat ISO_DATETIME_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

    /**
     * Time format for API communication
     */
    public static final SimpleDateFormat ISO_TIME_FORMAT =
            new SimpleDateFormat("HH:mm:ss", Locale.US);

    /**
     * Display date format (can use locale-specific formatting)
     */
    public static final SimpleDateFormat DISPLAY_DATE_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE);

    /**
     * Display datetime format (can use locale-specific formatting)
     */
    public static final SimpleDateFormat DISPLAY_DATETIME_FORMAT =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRANCE);

    /**
     * Get device's current timezone ID
     */
    public static String getDeviceTimezoneId() {
        return TimeZone.getDefault().getID();
    }

    /**
     * Get user's preferred timezone (from settings or device default)
     */
    public static String getUserTimezone(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        boolean useDeviceTz = prefs.getBoolean(KEY_USE_DEVICE_TIMEZONE, true);
        if (useDeviceTz) {
            return getDeviceTimezoneId();
        }

        return prefs.getString(KEY_USER_TIMEZONE, getDeviceTimezoneId());
    }

    /**
     * Set user's preferred timezone
     */
    public static void setUserTimezone(Context context, String timezoneId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putString(KEY_USER_TIMEZONE, timezoneId)
                .putBoolean(KEY_USE_DEVICE_TIMEZONE, false)
                .apply();

        Log.d(TAG, "✅ User timezone set to: " + timezoneId);
    }

    /**
     * Enable device timezone (auto-detect)
     */
    public static void useDeviceTimezone(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit()
                .putBoolean(KEY_USE_DEVICE_TIMEZONE, true)
                .apply();

        Log.d(TAG, "✅ Using device timezone: " + getDeviceTimezoneId());
    }

    /**
     * Convert datetime from user timezone to UTC for API
     *
     * @param date Date string (yyyy-MM-dd)
     * @param time Time string (HH:mm:ss)
     * @param userTimezoneId User's timezone ID
     * @return Datetime in UTC (yyyy-MM-dd HH:mm:ss)
     */
    public static String toUTC(String date, String time, String userTimezoneId) {
        try {
            // Combine date and time
            String localDatetime = date + " " + time;

            // Parse in user's timezone
            SimpleDateFormat localFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            localFormat.setTimeZone(TimeZone.getTimeZone(userTimezoneId));
            Date localDate = localFormat.parse(localDatetime);

            if (localDate == null) {
                Log.e(TAG, "❌ Failed to parse datetime: " + localDatetime);
                return localDatetime; // Return as-is if parsing fails
            }

            // Format in UTC
            SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            String utcDatetime = utcFormat.format(localDate);

            Log.d(TAG, "🌍 Timezone conversion: " + localDatetime + " (" + userTimezoneId + ") → " + utcDatetime + " (UTC)");
            return utcDatetime;

        } catch (ParseException e) {
            Log.e(TAG, "❌ Error converting to UTC: " + e.getMessage());
            return date + " " + time; // Return as-is if conversion fails
        }
    }

    /**
     * Convert datetime from UTC to user timezone for display
     *
     * @param utcDatetime Datetime in UTC (yyyy-MM-dd HH:mm:ss)
     * @param userTimezoneId User's timezone ID
     * @return Datetime in user timezone (yyyy-MM-dd HH:mm:ss)
     */
    public static String fromUTC(String utcDatetime, String userTimezoneId) {
        try {
            // Parse UTC datetime
            SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date utcDate = utcFormat.parse(utcDatetime);

            if (utcDate == null) {
                Log.e(TAG, "❌ Failed to parse UTC datetime: " + utcDatetime);
                return utcDatetime;
            }

            // Format in user's timezone
            SimpleDateFormat localFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            localFormat.setTimeZone(TimeZone.getTimeZone(userTimezoneId));
            String localDatetime = localFormat.format(utcDate);

            Log.d(TAG, "🌍 Timezone conversion: " + utcDatetime + " (UTC) → " + localDatetime + " (" + userTimezoneId + ")");
            return localDatetime;

        } catch (ParseException e) {
            Log.e(TAG, "❌ Error converting from UTC: " + e.getMessage());
            return utcDatetime;
        }
    }

    /**
     * Get timezone offset from UTC in hours
     */
    public static float getOffsetHours(String timezoneId) {
        TimeZone tz = TimeZone.getTimeZone(timezoneId);
        int offsetMillis = tz.getRawOffset();
        return offsetMillis / (1000f * 60f * 60f); // Convert to hours
    }

    /**
     * Get formatted timezone offset string
     *
     * @param timezoneId Timezone ID
     * @return Formatted offset (e.g., "+01:00", "-05:00")
     */
    public static String getOffsetString(String timezoneId) {
        TimeZone tz = TimeZone.getTimeZone(timezoneId);
        int offsetMillis = tz.getRawOffset();
        int hours = offsetMillis / (1000 * 60 * 60);
        int minutes = Math.abs((offsetMillis / (1000 * 60)) % 60);

        return String.format(Locale.US, "%+03d:%02d", hours, minutes);
    }

    /**
     * Get current datetime in UTC
     */
    public static String nowUTC() {
        SimpleDateFormat utcFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        utcFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return utcFormat.format(new Date());
    }

    /**
     * Get current datetime in user's timezone
     */
    public static String nowInUserTimezone(Context context) {
        String userTimezone = getUserTimezone(context);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone(userTimezone));
        return format.format(new Date());
    }

    /**
     * Validate timezone ID
     */
    public static boolean isValidTimezone(String timezoneId) {
        try {
            TimeZone tz = TimeZone.getTimeZone(timezoneId);
            // TimeZone.getTimeZone returns GMT if invalid, so check if it's the one we asked for
            return tz.getID().equals(timezoneId);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get debug info for timezone
     */
    public static String getDebugInfo(Context context) {
        String deviceTz = getDeviceTimezoneId();
        String userTz = getUserTimezone(context);
        float deviceOffset = getOffsetHours(deviceTz);
        float userOffset = getOffsetHours(userTz);

        return String.format(Locale.US,
                "🌍 Timezone Debug Info:\n" +
                        "Device TZ: %s (UTC%+.1f)\n" +
                        "User TZ: %s (UTC%+.1f)\n" +
                        "Current UTC: %s\n" +
                        "Current Local: %s",
                deviceTz, deviceOffset,
                userTz, userOffset,
                nowUTC(),
                nowInUserTimezone(context)
        );
    }

    /**
     * Common timezones for picker
     */
    public static String[] getCommonTimezoneIds() {
        return new String[]{
                "UTC",
                "Europe/Zurich",
                "Europe/Paris",
                "Europe/Berlin",
                "Europe/London",
                "Europe/Rome",
                "Europe/Madrid",
                "America/New_York",
                "America/Chicago",
                "America/Los_Angeles",
                "America/Toronto",
                "America/Sao_Paulo",
                "Asia/Dubai",
                "Asia/Tokyo",
                "Asia/Shanghai",
                "Asia/Singapore",
                "Australia/Sydney"
        };
    }

    /**
     * Get display name for timezone
     */
    public static String getTimezoneName(String timezoneId) {
        TimeZone tz = TimeZone.getTimeZone(timezoneId);
        String offset = getOffsetString(timezoneId);
        return timezoneId + " (UTC" + offset + ")";
    }
}
