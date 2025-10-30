package com.example.assistifyrelayapp.sms;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.example.assistifyrelayapp.core.NetClient;
import com.example.assistifyrelayapp.core.jsonclass.SmsResultRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SmsDeliveredReceiver extends BroadcastReceiver {
    private static final String TAG = "SmsDeliveredReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String msgId = intent.getStringExtra("msgId");
        String sessionId = intent.getStringExtra("sessionId");

        String status = "delivered";
        int resultCode = getResultCode();
        if (resultCode != Activity.RESULT_OK) {
            status = "failed";
        }

        Log.d(TAG, "onReceive delivered status for msgId=" + msgId + " sessionId=" + sessionId + " status=" + status);

        SmsResultRequest req = new SmsResultRequest();
        req.msgId = msgId;
        req.status = status;
        req.deliveredAt = String.valueOf(System.currentTimeMillis());
        req.sessionId = sessionId;

        NetClient.getInstance().getApiService()
                .sendSmsResult(req)
                .enqueue(new Callback<com.example.assistifyrelayapp.core.jsonclass.ApiResponse>() {
                    @Override
                    public void onResponse(Call<com.example.assistifyrelayapp.core.jsonclass.ApiResponse> call, Response<com.example.assistifyrelayapp.core.jsonclass.ApiResponse> response) {
                        Log.d(TAG, "Reported SMS delivered result for " + msgId + " success=" + response.isSuccessful());
                    }

                    @Override
                    public void onFailure(Call<com.example.assistifyrelayapp.core.jsonclass.ApiResponse> call, Throwable t) {
                        Log.e(TAG, "Failed to report delivered result: " + t.getMessage());
                    }
                });
    }
}

