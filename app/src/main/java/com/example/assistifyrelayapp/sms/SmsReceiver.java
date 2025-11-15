package com.example.assistifyrelayapp.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.example.assistifyrelayapp.LocalStorage.LocalTransactionStorage;
import com.example.assistifyrelayapp.core.NetClient;
import com.example.assistifyrelayapp.core.NetworkBufferedSender;
import com.example.assistifyrelayapp.core.SendAndReceivePreferences;
import com.example.assistifyrelayapp.core.jsonclass.IncomingSmsRequest;
import com.example.assistifyrelayapp.core.jsonclass.SmsResultRequest;
import com.example.assistifyrelayapp.core.jsonclass.TransactionData;
import com.example.assistifyrelayapp.session.SessionController;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.IOException;
import java.util.Map;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";

    private static final ExecutorService executor = Executors.newFixedThreadPool(6);
    private static final long DEBOUNCE_INTERVAL_MS = 2000; // 2 seconds to prevent double-processing
    private static final Set<String> recentRefs = new HashSet<>();
    private static long lastProcessedTime = 0;


    @Override
    public void onReceive(Context context, Intent intent) {

        // 👇 Hold the broadcast to do async work safely
        final PendingResult pendingResult = goAsync();

        executor.execute(() -> {
            try {
                handleIncomingSMS(context, intent);
            } catch (Exception e) {
                Log.e("Receive_SMS", "Executor Error: " + e.getMessage());
            } finally {
                pendingResult.finish();  // ✅ Must call finish to release the broadcast
            }
        });


//        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
//            Bundle bundle = intent.getExtras();
//            if (bundle != null) {
//                try {
//                    Object[] pdus = (Object[]) bundle.get("pdus");
//                    String format = bundle.getString("format"); // Needed on Android 6+
//
//                    if (pdus != null) {
//                        for (Object pdu : pdus) {
//                            SmsMessage sms;
//
//                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
//                                sms = SmsMessage.createFromPdu((byte[]) pdu, format);
//                            } else {
//                                sms = SmsMessage.createFromPdu((byte[]) pdu);
//                            }
//
//                            String sender = sms.getDisplayOriginatingAddress();
//                            String messageBody = sms.getMessageBody();
//                            Log.d(TAG, "SMS from " + sender + ": " + messageBody);
//
//                            // Debug: Show all active sessions
//                            SessionController controller = SessionController.getInstance(context);
//                            Map<String, SessionController.SessionInfo> activeSessions = controller.getActiveSessions();
//
//                            Log.d(TAG, "Active sessions count: " + activeSessions.size());
//                            for (String sessionId : activeSessions.keySet()) {
//                                Log.d(TAG, "Active session ID: " + sessionId);
//                            }
//
//                            if (activeSessions.isEmpty()) {
//                                Log.d(TAG, "Incoming SMS ignored (no active relay sessions)");
//                            } else {
//                                for (String sessionId : activeSessions.keySet()) {
//                                    new Thread(() -> {
//                                        try {
//                                            SmsResultRequest req = new SmsResultRequest();
//                                            req.msgId = null; // Or assign a local ID if needed
//                                            req.status = "received";
//                                            req.sessionId = sessionId;
//                                            req.sentAt = null; // Not relevant for received
//                                            req.deliveredAt = null; // Not relevant for incoming
//                                            req.error = null;
//                                            req.providerMessageId = null;
//
//                                            // Overloading error for actual message body and providerMessageId for sender
//                                            req.error = messageBody;
//                                            req.providerMessageId = sender;
//
//                                            NetClient.getInstance().getApiService().sendSmsResult(req).execute();
//
//                                            Log.d(TAG, "Forwarded incoming SMS to backend for session " + sessionId + ": " + sender + ": " + messageBody);
//
//                                        } catch (IOException e) {
//                                            Log.e(TAG, "Failed to forward SMS to backend for session " + sessionId + ": " + e.getMessage());
//                                        }
//                                    }).start();
//                                }
//                            }
//                        }
//                    }
//                } catch (Exception e) {
//                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
//                    Log.e(TAG, "Error processing received SMS", e);
//                }
//            }
//        }
    }


    private void handleIncomingSMS(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        String format = bundle.getString("format");
        Object[] smsObj = (Object[]) bundle.get("pdus");
        if (smsObj == null) return;

        for (Object obj : smsObj) {
            SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) obj, format);
            String senderPhNo = smsMessage.getDisplayOriginatingAddress();
            String msgBody = smsMessage.getDisplayMessageBody();
            String receivedTime = getCurrentDateTime();

            // Optional: If Assistify has multiple relay sessions
            SessionController controller = SessionController.getInstance(context);
            Map<String, SessionController.SessionInfo> activeSessions = controller.getActiveSessions();

            if (activeSessions.isEmpty()) {
                Log.d(TAG, "No active sessions — storing message locally only.");
            }

            // For every active session, create & send TransactionData
            for (String sessionId : activeSessions.keySet()) {

                TransactionData transaction = new TransactionData();
                transaction.setSessionId(sessionId);
                transaction.setFrom(senderPhNo);
                transaction.setBody(msgBody);
                transaction.setReceivedAt(receivedTime);
                transaction.setStatus("received");

                // --- Local backup ---
                LocalTransactionStorage.saveTransaction(context, transaction);

                // --- Buffered network send ---
                NetworkBufferedSender.trySend(context, transaction);

                Log.d(TAG, "📨 New TransactionData created and queued: " +
                        "Session=" + sessionId +
                        ", From=" + senderPhNo +
                        ", Body=" + msgBody +
                        ", Time=" + receivedTime);
            }
        }
    }

    private String getCurrentDateTime() {
        java.text.SimpleDateFormat sdf =
                new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date());
    }


}
