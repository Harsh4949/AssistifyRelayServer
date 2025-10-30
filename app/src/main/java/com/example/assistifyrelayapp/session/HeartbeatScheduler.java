package com.example.assistifyrelayapp.session;

import android.content.Context;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

public class HeartbeatScheduler {
    private static final String HEARTBEAT_WORK_TAG = "heartbeat_work";

    public static void scheduleHeartbeat(Context context, String deviceId) {
        // Build constraints (requires network connectivity)
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        // Build input data
        Data inputData = new Data.Builder()
                .putString("deviceId", deviceId)
                .build();

        // Create periodic work request (every 15 minutes)
        PeriodicWorkRequest heartbeatRequest =
                new PeriodicWorkRequest.Builder(HeartbeatWorker.class, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .setInputData(inputData)
                        .addTag(HEARTBEAT_WORK_TAG)
                        .build();

        // Enqueue unique work (replace existing if any)
        WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                        HEARTBEAT_WORK_TAG,
                        androidx.work.ExistingPeriodicWorkPolicy.REPLACE,
                        heartbeatRequest
                );
    }

    public static void cancelHeartbeat(Context context) {
        WorkManager.getInstance(context).cancelAllWorkByTag(HEARTBEAT_WORK_TAG);
    }
}
