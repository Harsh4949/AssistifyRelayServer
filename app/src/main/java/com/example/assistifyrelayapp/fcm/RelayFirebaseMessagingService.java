package com.example.assistifyrelayapp.fcm;

import android.util.Log;

import com.example.assistifyrelayapp.auth.DeviceRegistrationManager;
import com.example.assistifyrelayapp.core.NetClient;
import com.example.assistifyrelayapp.session.SessionController;
import com.example.assistifyrelayapp.sms.SmsSender;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class RelayFirebaseMessagingService extends FirebaseMessagingService {
    private static final String TAG = "RelayFCMService";

    @Override
    public void onNewToken(String token) {
        super.onNewToken(token);
        Log.d(TAG, "New FCM token received: " + token);

        // Re-register device with new token
        DeviceRegistrationManager.registerToken(getApplicationContext(), token);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "FCM message received from: " + remoteMessage.getFrom());

        Map<String, String> data = remoteMessage.getData();
        if (data.isEmpty()) {
            Log.w(TAG, "Empty FCM data payload");
            return;
        }

        String type = data.get("type");
        Log.d(TAG, "Message type: " + type);

        if (type == null) {
            Log.e(TAG, "Message type is null");
            return;
        }

        // Route based on message type
        switch (type) {
            case "start_session":
                handleStartSession(data);
                break;

            case "stop_session":
                handleStopSession(data);
                break;

            case "send_sms":
                handleSendSms(data);
                break;

            case "ping":
                handlePing(data);
                break;

            default:
                Log.w(TAG, "Unknown message type: " + type);
        }
    }

    private void handleStartSession(Map<String, String> data) {
        String sessionId = data.get("sessionId");
        String expiresAtStr = data.get("expiresAt");
        if (sessionId == null || expiresAtStr == null) return;

        try {
            long expiresAt = Instant.parse(expiresAtStr).toEpochMilli();
            SessionController.getInstance(getApplicationContext()).startSession(sessionId, expiresAt);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse session expiresAt", e);
        }
    }

    private void handleStopSession(Map<String, String> data) {
        String sessionId = data.get("sessionId");
        if (sessionId == null) return;

        SessionController.getInstance(getApplicationContext()).stopSession(sessionId);
    }

    private void handleSendSms(Map<String, String> data) {
        String sessionId = data.get("sessionId");
        String to = data.get("to");
        String body = data.get("body");
        String msgId = data.get("msgId");

        if (to == null || body == null || msgId == null) {
            Log.e(TAG, "Missing required fields in send_sms");
            return;
        }

        // Verify session is active
        SessionController controller = SessionController.getInstance(getApplicationContext());
        if (!controller.isSessionActive(sessionId)) {
            Log.w(TAG, "Session not active, ignoring send_sms");
            return;
        }

        Log.d(TAG, "Sending SMS to: " + to + ", msgId: " + msgId + ", sessionId: " + sessionId);
        SmsSender.sendSms(getApplicationContext(), to, body, msgId, sessionId);
    }

    private void handlePing(Map<String, String> data) {
        String pingId = data.get("pingId");
        Log.d(TAG, "Ping received: " + pingId);

        // Send ack back to the backend
        new Thread(() -> {
            try {
                Map<String, Object> req = new HashMap<>();
                req.put("deviceId", DeviceRegistrationManager.getDeviceId(getApplicationContext()));
                req.put("pingId", pingId);
                req.put("status", "ok");
                req.put("message", "Ping response received");

                NetClient.getInstance()
                        .getApiService()
                        .ackDevice(req)
                        .execute();

                Log.d(TAG, "Ping acknowledged to backend: " + pingId);
            } catch (IOException e) {
                Log.e(TAG, "Failed to send ping acknowledgment: " + e.getMessage());
            }
        }).start();
    }
}
