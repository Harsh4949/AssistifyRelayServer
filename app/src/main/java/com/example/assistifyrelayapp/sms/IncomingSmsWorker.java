package com.example.assistifyrelayapp.sms;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.assistifyrelayapp.core.NetClient;
import com.example.assistifyrelayapp.core.jsonclass.ApiResponse;
import com.example.assistifyrelayapp.core.jsonclass.IncomingSmsRequest;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class IncomingSmsWorker extends Worker {
    private static final String TAG = "IncomingSmsWorker";

    public IncomingSmsWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "IncomingSmsWorker started id=" + getId());
        String sessionId = getInputData().getString("sessionId");
        String from = getInputData().getString("from");
        String body = getInputData().getString("body");
        long receivedAtMs = getInputData().getLong("receivedAtMs", System.currentTimeMillis());

        if (sessionId == null || from == null || body == null) {
            Log.e(TAG, "Missing input data, aborting");
            return Result.success(); // nothing to do
        }

        IncomingSmsRequest req = new IncomingSmsRequest();
        req.sessionId = sessionId;
        req.from = from;
        req.body = body;
        req.receivedAt = String.valueOf(receivedAtMs);

        try {
            Call<ApiResponse> call = NetClient.getInstance().getApiService().sendIncomingSms(req);
            Response<ApiResponse> resp = call.execute(); // synchronous on worker thread
            if (resp.isSuccessful()) {
                Log.d(TAG, "Incoming SMS sent to backend for session " + sessionId + " resp=" + resp.code());
                return Result.success();
            } else {
                String err = "";
                try {
                    ResponseBody eb = resp.errorBody();
                    if (eb != null) err = eb.string();
                } catch (Exception ignore) {}
                Log.e(TAG, "Backend returned error: code=" + resp.code() + " body=" + err);
                return Result.retry();
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to send incoming SMS (will retry): " + e.getMessage(), e);
            return Result.retry();
        }
    }
}