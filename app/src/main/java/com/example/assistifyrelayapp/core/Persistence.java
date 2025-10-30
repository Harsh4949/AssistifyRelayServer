package com.example.assistifyrelayapp.core;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public class Persistence {
    private static final String PREFS_NAME = "assistify_prefs";
    private static final String KEY_PENDING_SMS = "pending_sms_ids";
    private static final String KEY_SESSION_ID = "session_id";
    private static final String KEY_SESSION_STATE = "session_state";

    // new keys for setup info
    private static final String KEY_PERMISSIONS_GRANTED = "permissions_granted";
    private static final String KEY_DEVICE_ADMIN_ENABLED = "device_admin_enabled";
    private static final String KEY_BATTERY_IGNORED = "battery_ignored";
    private static final String KEY_REGISTERED = "device_registered";

    private SharedPreferences prefs;
    private static Persistence instance;

    private Persistence(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static Persistence getInstance(Context ctx) {
        if (instance == null) {
            synchronized (Persistence.class) {
                if (instance == null) {
                    instance = new Persistence(ctx.getApplicationContext());
                }
            }
        }
        return instance;
    }

    // Store pending SMS IDs (you may store JSON if complex)
    public void addPendingSmsId(String msgId) {
        Set<String> ids = prefs.getStringSet(KEY_PENDING_SMS, new HashSet<>());
        Set<String> newSet = new HashSet<>(ids);
        newSet.add(msgId);
        prefs.edit().putStringSet(KEY_PENDING_SMS, newSet).apply();
    }

    public void removePendingSmsId(String msgId) {
        Set<String> ids = prefs.getStringSet(KEY_PENDING_SMS, new HashSet<>());
        if (ids.contains(msgId)) {
            Set<String> newSet = new HashSet<>(ids);
            newSet.remove(msgId);
            prefs.edit().putStringSet(KEY_PENDING_SMS, newSet).apply();
        }
    }

    public Set<String> getPendingSmsIds() {
        return prefs.getStringSet(KEY_PENDING_SMS, new HashSet<>());
    }

    // Store session state using SharedPreferences key-value as needed
    public void saveSessionState(String sessionId, String sessionState) {
        prefs.edit()
                .putString(KEY_SESSION_ID, sessionId)
                .putString(KEY_SESSION_STATE, sessionState)
                .apply();
    }

    public String getSessionId() {
        return prefs.getString(KEY_SESSION_ID, null);
    }

    public String getSessionState() {
        return prefs.getString(KEY_SESSION_STATE, "idle");
    }

    public void clearSession() {
        prefs.edit()
                .remove(KEY_SESSION_ID)
                .remove(KEY_SESSION_STATE)
                .apply();
    }

    // --- new helper methods to persist setup info ---
    public void setPermissionsGranted(boolean granted) {
        prefs.edit().putBoolean(KEY_PERMISSIONS_GRANTED, granted).apply();
    }

    public boolean isPermissionsGranted() {
        return prefs.getBoolean(KEY_PERMISSIONS_GRANTED, false);
    }

    public void setDeviceAdminEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DEVICE_ADMIN_ENABLED, enabled).apply();
    }

    public boolean isDeviceAdminEnabled() {
        return prefs.getBoolean(KEY_DEVICE_ADMIN_ENABLED, false);
    }

    public void setBatteryOptimizationsIgnored(boolean ignored) {
        prefs.edit().putBoolean(KEY_BATTERY_IGNORED, ignored).apply();
    }

    public boolean isBatteryOptimizationsIgnored() {
        return prefs.getBoolean(KEY_BATTERY_IGNORED, false);
    }

    public void setRegistered(boolean registered) {
        prefs.edit().putBoolean(KEY_REGISTERED, registered).apply();
    }

    public boolean isRegistered() {
        return prefs.getBoolean(KEY_REGISTERED, false);
    }
}