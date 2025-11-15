package com.example.assistifyrelayapp.ui;

import static com.example.assistifyrelayapp.core.NetworkBufferedSender.getBufferedCount;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.assistifyrelayapp.R;
import com.example.assistifyrelayapp.core.NetworkBufferedSender;
import com.example.assistifyrelayapp.core.SendAndReceivePreferences;
import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {

    private TextView displayMsg;
    private Boolean onStopBtnClicked;
    private Button resendBufferBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        FirebaseApp.initializeApp(this);

        setContentView(R.layout.activity_main);

        // Window insets for edge-to-edge UI
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // ------------ UI Views ------------
        displayMsg = findViewById(R.id.displayMsg);
        resendBufferBtn = findViewById(R.id.btn_resend_buffer);

        // ------------ Load Preferences ------------
        onStopBtnClicked = SendAndReceivePreferences.getboolean(
                getApplicationContext(), "onStopBtnClicked", false
        );

        updateServerStatusText();

        // ------------ Auto Resend Buffered Data on App Start ------------
        NetworkBufferedSender.resendBufferedWithCallback(getApplicationContext(), () -> {
            runOnUiThread(() -> {
                int count = getBufferedCount(this);
                if (count == 0) {
                    Toast.makeText(this, "Buffer resend complete.", Toast.LENGTH_SHORT).show();
                }
                resendBufferBtn.setText("Buffer Size : " + count);
            });
        });

        resendBufferBtn.setText("Buffer Size : " + getBufferedCount(this));

        // ------------ Permission for SMS ------------
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.RECEIVE_SMS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS}, 1001);
            }
        }

        // ------------ Manual Resend Button Handler ------------
        resendBufferBtn.setOnClickListener(v -> {
            resendBufferBtn.setEnabled(false);
            Toast.makeText(this, "Trying to resend buffered data...", Toast.LENGTH_SHORT).show();

            NetworkBufferedSender.resendBufferedWithCallback(getApplicationContext(), () -> {
                runOnUiThread(() -> {
                    int count = getBufferedCount(this);
                    resendBufferBtn.setText("Buffer Size : " + count);
                    resendBufferBtn.setEnabled(true);

                    if (count == 0)
                        Toast.makeText(this, "Buffer resend complete.", Toast.LENGTH_SHORT).show();
                });
            });
        });
    }

    // --------------------------- Server Start / Stop ---------------------------

    public void onStartServer(android.view.View view) {
        if (onStopBtnClicked) {
            // Start the server
            onStopBtnClicked = false;
            SendAndReceivePreferences.setboolean(getApplicationContext(), "onStopBtnClicked", false);
            updateServerStatusText();
        } else {
            Toast.makeText(this, "⚙️ Server is already running.", Toast.LENGTH_SHORT).show();
        }
    }

    public void onStopServer(android.view.View view) {
        onStopBtnClicked = true;
        SendAndReceivePreferences.setboolean(getApplicationContext(), "onStopBtnClicked", true);
        updateServerStatusText();
    }

    private void updateServerStatusText() {
        if (onStopBtnClicked) {
            displayMsg.setText("🔴 Server Stopped...");
        } else {
            displayMsg.setText("🟢 Server Started...");
        }
    }

    // ------------------------ Navigation Buttons ------------------------

    public static void onViewDashboard(android.view.View view) {
        Intent intent = new Intent(view.getContext(), DashboardActivity.class);
        view.getContext().startActivity(intent);
    }

    public static void onSetupServer(android.view.View view) {
        Intent intent = new Intent(view.getContext(), SetupActivity.class);
        view.getContext().startActivity(intent);
    }
}
