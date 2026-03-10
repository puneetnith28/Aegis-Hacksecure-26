package com.obsidian.aegis.repository;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

import com.obsidian.aegis.BuildConfig;
import com.obsidian.aegis.models.IndicatorOpacity;
import com.obsidian.aegis.models.IndicatorPosition;
import com.obsidian.aegis.models.IndicatorSize;

public class SharedPrefManager {

    private static final String SP_NAME = BuildConfig.APPLICATION_ID;
    private static final int ACCESS_MODE = Context.MODE_PRIVATE;
    private static final String MIC_ENABLED = "MIC_ENABLED";
    private static final String NOTIFICATION_ENABLED = "NOTIFICATION_ENABLED";
    private static final String LOC_ENABLED = "LOCATION_ENABLED";
    private static final String VIB_ENABLED = "VIBRATION_ENABLED";
    private static final String INDICATOR_COLOR = "INDICATOR_COLOR";
    private static final String INDICATOR_SIZE = "INDICATOR_SIZE";
    private static final String INDICATOR_OPACITY = "INDICATOR_OPACITY";
    private static final String INDICATOR_POSITION = "INDICATOR_POSITION";
    private static final String INDICATOR_BACKGROUND_COLOR = "INDICATOR_BACKGROUND_COLOR";
    private static final String CAMERA_COLOR = "CAMERA_COLOR";
    private static final String MIC_COLOR = "MIC_COLOR";
    private static final String LOCATION_COLOR = "LOCATION_COLOR";
    private static final String SUSPICIOUS_DETECTION_ENABLED = "SUSPICIOUS_DETECTION_ENABLED";
    private static final String SCREEN_OFF_MONITORING_ENABLED = "SCREEN_OFF_MONITORING_ENABLED";
    private static final String SHOW_APP_INFO_ON_INDICATOR = "SHOW_APP_INFO_ON_INDICATOR";
    private static final String WHITELISTED_APPS = "WHITELISTED_APPS";
    private static final String AUTO_REVOKE_ENABLED = "AUTO_REVOKE_ENABLED";

    private static final String DEFAULT_INDICATOR_COLOR = "#3F51B5";
    private static final String DEFAULT_CAMERA_COLOR = "#E91E63"; // Rose
    private static final String DEFAULT_MIC_COLOR = "#3F51B5"; // Deep Indigo
    private static final String DEFAULT_LOCATION_COLOR = "#2196F3"; // Blue
    private static final String DEFAULT_INDICATOR_BACKGROUND_COLOR = "#FFFFFF";
    private static final boolean DEFAULT_CAMERA_ENABLED = true;
    private static final boolean DEFAULT_MIC_ENABLED = true;
    private static final boolean DEFAULT_LOC_ENABLED = false;
    private static final boolean DEFAULT_NOTIFICATION_ENABLED = false;
    private static final boolean DEFAULT_VIB_ENABLED = false;
    private static final boolean DEFAULT_SUSPICIOUS_DETECTION_ENABLED = true;
    private static final boolean DEFAULT_SCREEN_OFF_MONITORING_ENABLED = true;
    private static final boolean DEFAULT_SHOW_APP_INFO_ON_INDICATOR = true;
    private static final boolean DEFAULT_AUTO_REVOKE_ENABLED = false;

    private static final String DEFAULT_INDICATOR_SIZE = IndicatorSize.M.name();
    private static final String DEFAULT_INDICATOR_OPACITY = IndicatorOpacity.EIGHTY.name();
    private static final String DEFAULT_INDICATOR_POSITION = IndicatorPosition.TOP_RIGHT.name();

    private static SharedPrefManager sharedPrefManager;
    private SharedPreferences sharedPreferences;
    private Context context;

    public static SharedPrefManager getInstance(Context context) {
        if (null == sharedPrefManager) {
            sharedPrefManager = new SharedPrefManager(context);
        }
        return sharedPrefManager;
    }

    public SharedPrefManager(Context context) {
        this.context = context;
        this.sharedPreferences = context.getSharedPreferences(SP_NAME, ACCESS_MODE);
    }

    public void setString(Context context, String key, String value) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public String getString(Context context, String key, String def_value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, ACCESS_MODE);
        return sharedPreferences.getString(key, def_value);
    }

    public void setInteger(Context context, String key, int value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, ACCESS_MODE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    public int getInteger(Context context, String key, int def_value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, ACCESS_MODE);
        return sharedPreferences.getInt(key, def_value);
    }

    public void setBoolean(Context context, String key, boolean value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, ACCESS_MODE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    public boolean getBoolean(Context context, String key, boolean def_value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(SP_NAME, ACCESS_MODE);
        return sharedPreferences.getBoolean(key, def_value);
    }

    public void setCameraIndicatorEnabled(boolean value) {
        setBoolean(context, "CAMERA_ENABLED", value);
    }

    public boolean isCameraIndicatorEnabled() {
        return getBoolean(context, "CAMERA_ENABLED", DEFAULT_CAMERA_ENABLED);
    }

    public void setMicIndicatorEnabled(boolean value) {
        setBoolean(context, MIC_ENABLED, value);
    }

    public boolean isMicIndicatorEnabled() {
        return getBoolean(context, MIC_ENABLED, DEFAULT_MIC_ENABLED);
    }

    public void setNotificationEnabled(boolean value) {
        setBoolean(context, NOTIFICATION_ENABLED, value);
    }

    public boolean isNotificationEnabled() {
        return getBoolean(context, NOTIFICATION_ENABLED, DEFAULT_NOTIFICATION_ENABLED);
    }

    public boolean isLocationEnabled() {
        return getBoolean(context, LOC_ENABLED, DEFAULT_LOC_ENABLED);
    }

    public void setLocationEnabled(boolean value) {
        setBoolean(context, LOC_ENABLED, value);
    }

    public void setVibrationEnabled(boolean value) {
        setBoolean(context, VIB_ENABLED, value);
    }

    public boolean isVibrationEnabled() {
        return getBoolean(context, VIB_ENABLED, DEFAULT_VIB_ENABLED);
    }

    public boolean isSuspiciousDetectionEnabled() {
        return getBoolean(context, SUSPICIOUS_DETECTION_ENABLED, DEFAULT_SUSPICIOUS_DETECTION_ENABLED);
    }

    public void setSuspiciousDetectionEnabled(boolean value) {
        setBoolean(context, SUSPICIOUS_DETECTION_ENABLED, value);
    }

    public boolean isScreenOffMonitoringEnabled() {
        return getBoolean(context, SCREEN_OFF_MONITORING_ENABLED, DEFAULT_SCREEN_OFF_MONITORING_ENABLED);
    }

    public void setScreenOffMonitoringEnabled(boolean value) {
        setBoolean(context, SCREEN_OFF_MONITORING_ENABLED, value);
    }

    public boolean isShowAppInfoOnIndicator() {
        return getBoolean(context, SHOW_APP_INFO_ON_INDICATOR, DEFAULT_SHOW_APP_INFO_ON_INDICATOR);
    }

    public void setShowAppInfoOnIndicator(boolean value) {
        setBoolean(context, SHOW_APP_INFO_ON_INDICATOR, value);
    }

    public boolean isAutoRevokeEnabled() {
        return getBoolean(context, AUTO_REVOKE_ENABLED, DEFAULT_AUTO_REVOKE_ENABLED);
    }

    public void setAutoRevokeEnabled(boolean value) {
        setBoolean(context, AUTO_REVOKE_ENABLED, value);
    }

    public void setIndicatorColor(String value) {
        setString(context, INDICATOR_COLOR, value);
    }

    public String getIndicatorColor() {
        return getString(context, INDICATOR_COLOR, DEFAULT_INDICATOR_COLOR);
    }

    public void setIndicatorBackgroundColor(String value) {
        setString(context, INDICATOR_BACKGROUND_COLOR, value);
    }

    public String getIndicatorBackgroundColor() {
        return getString(context, INDICATOR_BACKGROUND_COLOR, DEFAULT_INDICATOR_BACKGROUND_COLOR);
    }

    public void setCameraColor(String value) {
        setString(context, CAMERA_COLOR, value);
    }

    public String getCameraColor() {
        return getString(context, CAMERA_COLOR, DEFAULT_CAMERA_COLOR);
    }

    public void setMicColor(String value) {
        setString(context, MIC_COLOR, value);
    }

    public String getMicColor() {
        return getString(context, MIC_COLOR, DEFAULT_MIC_COLOR);
    }

    public void setLocationColor(String value) {
        setString(context, LOCATION_COLOR, value);
    }

    public String getLocationColor() {
        return getString(context, LOCATION_COLOR, DEFAULT_LOCATION_COLOR);
    }

    public void setIndicatorSize(IndicatorSize value) {
        setString(context, INDICATOR_SIZE, value.name());
    }

    public IndicatorSize getIndicatorSize() {
        return IndicatorSize.valueOf(getString(context, INDICATOR_SIZE, DEFAULT_INDICATOR_SIZE));
    }

    public void setIndicatorOpacity(IndicatorOpacity value) {
        setString(context, INDICATOR_OPACITY, value.name());
    }

    public IndicatorOpacity getIndicatorOpacity() {
        return IndicatorOpacity.valueOf(getString(context, INDICATOR_OPACITY, DEFAULT_INDICATOR_OPACITY));
    }

    public void setIndicatorPosition(IndicatorPosition value) {
        setString(context, INDICATOR_POSITION, value.name());
    }

    public IndicatorPosition getIndicatorPosition() {
        return IndicatorPosition.valueOf(getString(context, INDICATOR_POSITION, DEFAULT_INDICATOR_POSITION));
    }

    public Set<String> getWhitelistedApps() {
        return sharedPreferences.getStringSet(WHITELISTED_APPS, new HashSet<>());
    }

    public void addAppToWhitelist(String packageName) {
        Set<String> whitelist = new HashSet<>(getWhitelistedApps());
        whitelist.add(packageName);
        sharedPreferences.edit().putStringSet(WHITELISTED_APPS, whitelist).apply();
    }

    public void removeAppFromWhitelist(String packageName) {
        Set<String> whitelist = new HashSet<>(getWhitelistedApps());
        whitelist.remove(packageName);
        sharedPreferences.edit().putStringSet(WHITELISTED_APPS, whitelist).apply();
    }

    public boolean isAppWhitelisted(String packageName) {
        return getWhitelistedApps().contains(packageName);
    }

}
