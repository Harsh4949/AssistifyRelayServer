package com.example.assistifyrelayapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.assistifyrelayapp.core.NetClient;
import com.example.assistifyrelayapp.core.jsonclass.ApiResponse;
import com.example.assistifyrelayapp.core.jsonclass.IncomingSmsRequest;
import com.example.assistifyrelayapp.sms.IncomingSmsWorker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Response;

public class ForegroundService extends Service {
    public static final String EXTRA_SESSION_ID = "extra_session_id";
    public static final String EXTRA_FROM = "extra_from";
    public static final String EXTRA_BODY = "extra_body";
    public static final String EXTRA_RECEIVED_AT = "extra_received_at";
    private static final String CHANNEL_ID = "relay_sms_channel";
    private static final int NOTIFICATION_ID = 101;
    private static final String TAG = "ForegroundService";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "AssistifyRelay:IncomingSms");
            wakeLock.setReferenceCounted(false);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        final String sessionId = intent.getStringExtra(EXTRA_SESSION_ID);
        final String from = intent.getStringExtra(EXTRA_FROM);
        final String body = intent.getStringExtra(EXTRA_BODY);
        final long receivedAt = intent.getLongExtra(EXTRA_RECEIVED_AT, System.currentTimeMillis());

        if (sessionId == null || from == null || body == null) {
            stopSelf(startId);
            return START_NOT_STICKY;
        }

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Assistify relay active")
                .setContentText("Relaying SMS from " + from)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .build();
        startForeground(NOTIFICATION_ID, notification);

        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(2 * 60 * 1000L); // 2 min max
        }

        executor.execute(() -> {
            try {
                IncomingSmsRequest req = new IncomingSmsRequest();
                req.sessionId = sessionId;
                req.from = from;
                req.body = body;
                req.receivedAt = String.valueOf(receivedAt);

                Call<ApiResponse> call = NetClient.getInstance()
                        .getApiService()
                        .sendIncomingSms(req);
                Response<ApiResponse> resp = call.execute();

                if (resp.isSuccessful()) {
                    Log.d(TAG, "Incoming SMS relayed immediately session=" + sessionId);
                } else {
                    Log.w(TAG, "Immediate relay failed HTTP " + resp.code() + ", scheduling retry");
                    enqueueRetry(sessionId, from, body, receivedAt);
                }
            } catch (Exception e) {
                Log.e(TAG, "Immediate relay failed", e);
                enqueueRetry(sessionId, from, body, receivedAt);
            } finally {
                if (wakeLock != null && wakeLock.isHeld()) {
                    wakeLock.release();
                }
                stopForeground(true);
                stopSelf(startId);
            }
        });

        return START_REDELIVER_INTENT;
    }

    private void enqueueRetry(String sessionId, String from, String body, long receivedAt) {
        Data input = new Data.Builder()
                .putString("sessionId", sessionId)
                .putString("from", from)
                .putString("body", body)
                .putLong("receivedAtMs", receivedAt)
                .build();
        OneTimeWorkRequest request = new OneTimeWorkRequest.Builder(IncomingSmsWorker.class)
                .setInputData(input)
                .build();
        WorkManager.getInstance(this).enqueue(request);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel =
                    new NotificationChannel(CHANNEL_ID,
                            "Assistify Relay",
                            NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Immediate SMS relay channel");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        executor.shutdownNow();
        super.onDestroy();
    }
}
