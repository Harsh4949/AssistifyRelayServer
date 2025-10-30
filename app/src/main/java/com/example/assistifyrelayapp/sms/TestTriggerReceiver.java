package com.example.assistifyrelayapp.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class TestTriggerReceiver extends BroadcastReceiver {
    private static final String TAG = "TestTriggerReceiver";
    public static final String ACTION = "com.example.assistifyrelayapp.TEST_WORK";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "TestTriggerReceiver.onReceive action=" + intent.getAction());
        OneTimeWorkRequest w = new OneTimeWorkRequest.Builder(TestWorker.class).build();
        WorkManager.getInstance(context).enqueue(w);
        Log.d(TAG, "Enqueued TestWorker, id=" + w.getId());
    }
}