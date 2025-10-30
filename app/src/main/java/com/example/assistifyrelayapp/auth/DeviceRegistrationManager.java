package com.example.assistifyrelayapp.auth;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.example.assistifyrelayapp.core.NetClient;
import com.example.assistifyrelayapp.core.jsonclass.ApiResponse;
import com.example.assistifyrelayapp.core.jsonclass.DeviceRegisterRequest;
import com.example.assistifyrelayapp.session.HeartbeatScheduler;
import com.google.firebase.messaging.FirebaseMessaging;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeviceRegistrationManager {
    private static final String TAG = "DeviceRegistration";

    /**
     * Register device with backend using FCM token
     */


    public static void registerToken(Context context, String fcmToken) {
        Log.d(TAG, "Registering device with token: " + fcmToken);

        DeviceRegisterRequest request = new DeviceRegisterRequest();
        request.fcmToken = fcmToken;
        request.model = Build.MODEL;
        request.appVersion = "1.0.0";
        request.deviceKeyHash = generateDeviceKeyHash();
        request.capabilities = "{\"smsSend\":true}";

        // Call backend API asynchronously - NOW WORKS CORRECTLY
        NetClient.getInstance().registerDevice(request)
                .enqueue(new Callback<ApiResponse>() {
                    @Override
                    public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            String deviceId = response.body().deviceId;
                            Log.d(TAG, "Device registered successfully. DeviceId: " + deviceId);

                            // Save deviceId locally for future use
                            saveDeviceId(context, deviceId);

                            // Schedule periodic heartbeat
                            HeartbeatScheduler.scheduleHeartbeat(context, deviceId);
                        } else {
                            Log.e(TAG, "Registration failed: " + response.message());
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse> call, Throwable t) {
                        Log.e(TAG, "Registration network error: " + t.getMessage());
                    }
                });
    }

    /**
     * Fetch FCM token and register
     */
    public static void fetchAndRegister(Context context) {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        String token = task.getResult();
                        Log.d(TAG, "FCM Token fetched: " + token);
                        registerToken(context, token);
                    } else {
                        Log.e(TAG, "Failed to fetch FCM token");
                    }
                });
    }

    /**
     * Generate a simple device key hash (replace with secure implementation)
     */
    private static String generateDeviceKeyHash() {
        return "device_key_" + System.currentTimeMillis();
    }

    /**
     * Save device ID to SharedPreferences
     */
    private static void saveDeviceId(Context context, String deviceId) {
        context.getSharedPreferences("assistify_prefs", Context.MODE_PRIVATE)
                .edit()
                .putString("device_id", deviceId)
                .apply();
    }

    /**
     * Get saved device ID
     */
    public static String getDeviceId(Context context) {
        return context.getSharedPreferences("assistify_prefs", Context.MODE_PRIVATE)
                .getString("device_id", null);
    }
}
