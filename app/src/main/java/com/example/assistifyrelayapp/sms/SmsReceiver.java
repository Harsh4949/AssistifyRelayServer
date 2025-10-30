package com.example.assistifyrelayapp.sms;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import com.example.assistifyrelayapp.core.NetClient;
import com.example.assistifyrelayapp.core.jsonclass.IncomingSmsRequest;
import com.example.assistifyrelayapp.core.jsonclass.SmsResultRequest;
import com.example.assistifyrelayapp.session.SessionController;

import java.io.IOException;
import java.util.Map;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    String format = bundle.getString("format"); // Needed on Android 6+

                    if (pdus != null) {
                        for (Object pdu : pdus) {
                            SmsMessage sms;

                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                sms = SmsMessage.createFromPdu((byte[]) pdu, format);
                            } else {
                                sms = SmsMessage.createFromPdu((byte[]) pdu);
                            }

                            String sender = sms.getDisplayOriginatingAddress();
                            String messageBody = sms.getMessageBody();
                            Log.d(TAG, "SMS from " + sender + ": " + messageBody);

                            // Debug: Show all active sessions
                            SessionController controller = SessionController.getInstance(context);
                            Map<String, SessionController.SessionInfo> activeSessions = controller.getActiveSessions();

                            Log.d(TAG, "Active sessions count: " + activeSessions.size());
                            for (String sessionId : activeSessions.keySet()) {
                                Log.d(TAG, "Active session ID: " + sessionId);
                            }

                            if (activeSessions.isEmpty()) {
                                Log.d(TAG, "Incoming SMS ignored (no active relay sessions)");
                            } else {
                                for (String sessionId : activeSessions.keySet()) {
                                    new Thread(() -> {
                                        try {
                                            SmsResultRequest req = new SmsResultRequest();
                                            req.msgId = null; // Or assign a local ID if needed
                                            req.status = "received";
                                            req.sessionId = sessionId;
                                            req.sentAt = null; // Not relevant for received
                                            req.deliveredAt = null; // Not relevant for incoming
                                            req.error = null;
                                            req.providerMessageId = null;

                                            // Overloading error for actual message body and providerMessageId for sender
                                            req.error = messageBody;
                                            req.providerMessageId = sender;

                                            NetClient.getInstance().getApiService().sendSmsResult(req).execute();

                                            Log.d(TAG, "Forwarded incoming SMS to backend for session " + sessionId + ": " + sender + ": " + messageBody);

                                        } catch (IOException e) {
                                            Log.e(TAG, "Failed to forward SMS to backend for session " + sessionId + ": " + e.getMessage());
                                        }
                                    }).start();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    Toast.makeText(context, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Error processing received SMS", e);
                }
            }
        }
    }
}
