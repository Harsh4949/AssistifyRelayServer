package com.example.assistifyrelayapp.session;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class SessionController {
    private static final String PREFS_NAME = "assistify.sessions";
    private static final String KEY_SESSIONS = "active_sessions";

    private static SessionController instance;
    private final Context context;

    // Map sessionId -> SessionInfo
    private Map<String, SessionInfo> activeSessions = Collections.synchronizedMap(new HashMap<>());

    private SessionController(Context context) {
        this.context = context.getApplicationContext();
        loadSessions();
    }

    public static SessionController getInstance(Context ctx) {
        if (instance == null) {
            synchronized (SessionController.class) {
                if (instance == null) {
                    instance = new SessionController(ctx);
                }
            }
        }
        return instance;
    }

    // Start a new session or refresh expiresAt
    public synchronized void startSession(String sessionId, long expiresAt) {
        activeSessions.put(sessionId, new SessionInfo(sessionId, expiresAt));
        Log.d("SessionController", "Started session: " + sessionId);
        saveSessions();
    }

    // Stop and remove session
    public synchronized void stopSession(String sessionId) {
        if (activeSessions.containsKey(sessionId)) {
            activeSessions.remove(sessionId);

            Log.d("SessionController", "Stopped session: " + sessionId);
            saveSessions();
        }
    }

    // Check if the specified session is active and not expired
    public synchronized boolean isSessionActive(String sessionId) {
        SessionInfo session = activeSessions.get(sessionId);
        if (session == null) return false;
        boolean active = System.currentTimeMillis() < session.expiresAt;
        if (!active) {
            // Session expired, remove it
            activeSessions.remove(sessionId);
            saveSessions();
        }
        return active;
    }

    // Get all active sessions (sessionId -> SessionInfo)
    public synchronized Map<String, SessionInfo> getActiveSessions() {
        // Remove expired sessions before returning
        activeSessions.entrySet().removeIf(entry -> System.currentTimeMillis() >= entry.getValue().expiresAt);
        saveSessions();
        return activeSessions;
    }

    // Persist sessions map to SharedPreferences as JSON
    private void saveSessions() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        Gson gson = new Gson();
        String json = gson.toJson(activeSessions);

        editor.putString(KEY_SESSIONS, json);
        editor.apply();

        Log.d("SessionController", "Saved sessions: " + activeSessions.size());
    }

    // Load sessions map from SharedPreferences
    private void loadSessions() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String json = prefs.getString(KEY_SESSIONS, null);
        if (json == null) return;

        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, SessionInfo>>() {}.getType();
        Map<String, SessionInfo> savedSessions = gson.fromJson(json, type);

        if (savedSessions != null) {
            activeSessions.putAll(savedSessions);
            Log.d("SessionController", "Loaded sessions: " + activeSessions.size());
        }
    }

    public static class SessionInfo {
        public final String sessionId;
        public final long expiresAt;

        public SessionInfo(String sessionId, long expiresAt) {
            this.sessionId = sessionId;
            this.expiresAt = expiresAt;
        }
    }
}
