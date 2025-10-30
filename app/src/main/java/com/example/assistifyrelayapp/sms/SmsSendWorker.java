package com.example.assistifyrelayapp.sms;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.assistifyrelayapp.core.NetClient;
import com.example.assistifyrelayapp.core.Persistence;

public class SmsSendWorker extends Worker {
    private static final String TAG = "SmsSendWorker";

    public SmsSendWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        String phone = getInputData().getString("phone");
        String message = getInputData().getString("message");
        String msgId = getInputData().getString("msgId");
        String sessionId = getInputData().getString("sessionId"); // New

        Log.d(TAG, "Starting SMS send to " + phone + " msgId: " + msgId + " sessionId: " + sessionId);

        try {
            SmsManager smsManager = SmsManager.getDefault();

            Intent sentIntent = new Intent(getApplicationContext(), SmsSentReceiver.class);
            sentIntent.putExtra("msgId", msgId);
            sentIntent.putExtra("sessionId", sessionId);

            PendingIntent sentPI = PendingIntent.getBroadcast(
                    getApplicationContext(),
                    (int) (System.currentTimeMillis() & 0xfffffff),
                    sentIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Intent deliveredIntent = new Intent(getApplicationContext(), SmsDeliveredReceiver.class);
            deliveredIntent.putExtra("msgId", msgId);
            deliveredIntent.putExtra("sessionId", sessionId);

            PendingIntent deliveredPI = PendingIntent.getBroadcast(
                    getApplicationContext(),
                    (int) ((System.currentTimeMillis() + 1) & 0xfffffff),
                    deliveredIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            smsManager.sendTextMessage(phone, null, message, sentPI, deliveredPI);
            Log.d(TAG, "SMS queued for sending to " + phone);
            return Result.success();

        } catch (Exception e) {
            Log.e(TAG, "SMS send failed: " + e.getMessage());
            return Result.retry();
        }
    }
}
