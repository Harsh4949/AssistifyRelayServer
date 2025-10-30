package com.example.assistifyrelayapp.session;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.assistifyrelayapp.core.NetClient;
import com.example.assistifyrelayapp.core.Persistence;
import com.example.assistifyrelayapp.core.jsonclass.HeartbeatRequest;

import java.io.IOException;

public class HeartbeatWorker extends Worker {
    private static final String TAG = "HeartbeatWorker";

    public HeartbeatWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Heartbeat worker started");

        try {
            // Get device ID from input data
            String deviceId = getInputData().getString("deviceId");
            if (deviceId == null || deviceId.isEmpty()) {
                Log.e(TAG, "Device ID not found, skipping heartbeat");
                return Result.failure();
            }

            // Get battery level
            int batteryLevel = getBatteryLevel();

            // Get network type (simplified)
            String networkType = "wifi"; // TODO: Get actual network type

            // Get queue depth (number of pending SMS)
            int queueDepth = Persistence.getInstance(getApplicationContext())
                    .getPendingSmsIds().size();

            // Build heartbeat request
            HeartbeatRequest request = new HeartbeatRequest();
            request.deviceId = deviceId;
            request.battery = batteryLevel;
            request.network = networkType;
            request.queueDepth = queueDepth;

            // Send to backend using SYNC method (works in background thread)
            NetClient.getInstance().sendHeartbeatSync(request);

            Log.d(TAG, "Heartbeat sent successfully: battery=" + batteryLevel +
                    ", queue=" + queueDepth);
            return Result.success();

        } catch (IOException e) {
            Log.e(TAG, "Heartbeat failed: " + e.getMessage());
            return Result.retry();
        }
    }

    /**
     * Get current battery level percentage
     */
    private int getBatteryLevel() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = getApplicationContext().registerReceiver(null, ifilter);

        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            return (int) ((level / (float) scale) * 100);
        }
        return -1;
    }
}
