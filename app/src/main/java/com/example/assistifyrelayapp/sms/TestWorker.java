package com.example.assistifyrelayapp.sms;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class TestWorker extends Worker {
    private static final String TAG = "TestWorker";
    public TestWorker(@NonNull Context context, @NonNull WorkerParameters params) { super(context, params); }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "TestWorker running id=" + getId() + " appInForeground=" + (getApplicationContext()!=null));
        // short-lived test task
        return Result.success();
    }
}