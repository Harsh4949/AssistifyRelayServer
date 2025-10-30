package com.example.assistifyrelayapp.sms;


import android.content.Context;
import android.util.Log;

import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

public class SmsSender {
    private static final String TAG = "SmsSender";

    public static void sendSms(Context ctx, String phone, String message, String msgId, String sessionId) {
        Data inputData = new Data.Builder()
                .putString("phone", phone)
                .putString("message", message)
                .putString("msgId", msgId)
                .putString("sessionId", sessionId) // new field
                .build();

        OneTimeWorkRequest smsWorkRequest = new OneTimeWorkRequest.Builder(SmsSendWorker.class)
                .setInputData(inputData)
                .build();

        WorkManager.getInstance(ctx).enqueue(smsWorkRequest);
    }
}
