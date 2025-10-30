package com.example.assistifyrelayapp.ui;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.assistifyrelayapp.R;
import com.example.assistifyrelayapp.auth.DeviceRegistrationManager;
import com.example.assistifyrelayapp.session.SessionController;
import com.example.assistifyrelayapp.session.SessionController.SessionInfo;

import java.util.ArrayList;
import java.util.List;

public class DashboardActivity extends AppCompatActivity {
    private TextView tvDeviceId;
    private ListView lvSessions;
    private Handler handler;
    private Runnable updateRunnable;
    private ArrayAdapter<String> sessionAdapter;
    private List<String> sessionDisplayList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        tvDeviceId = findViewById(R.id.tv_device_id);
        lvSessions = findViewById(R.id.lv_sessions);

        sessionAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1,
                sessionDisplayList);
        lvSessions.setAdapter(sessionAdapter);

        handler = new Handler(Looper.getMainLooper());
        updateRunnable = this::updateStatus;

        updateStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.postDelayed(updateRunnable, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }

    private void updateStatus() {
        String deviceId = DeviceRegistrationManager.getDeviceId(this);
        tvDeviceId.setText("Device ID: " + (deviceId != null ? deviceId : "Not registered"));

        SessionController controller = SessionController.getInstance(this);

        // Clear old data
        sessionDisplayList.clear();

        if (controller.getActiveSessions().isEmpty()) {
            sessionDisplayList.add("No active sessions");
        } else {
            for (SessionInfo session : controller.getActiveSessions().values()) {
                String info = "Session ID: " + session.sessionId + "\nExpires at: " +
                        new java.util.Date(session.expiresAt).toString();
                sessionDisplayList.add(info);
            }
        }

        sessionAdapter.notifyDataSetChanged();

        handler.postDelayed(updateRunnable, 2000); // Update every 2 seconds
    }
}
