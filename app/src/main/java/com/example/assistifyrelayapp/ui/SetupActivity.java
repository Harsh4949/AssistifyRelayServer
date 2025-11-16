package com.example.assistifyrelayapp.ui;

import android.Manifest;
import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.example.assistifyrelayapp.session.SessionController.SessionInfo;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.assistifyrelayapp.R;
import com.example.assistifyrelayapp.auth.DeviceRegistrationManager;
import com.example.assistifyrelayapp.core.Persistence;
import com.example.assistifyrelayapp.core.SendAndReceivePreferences; // import the preferences helper (add this class if missing)
import com.example.assistifyrelayapp.session.SessionController;

import java.util.ArrayList;
import java.util.List;

public class SetupActivity extends AppCompatActivity {
    private static final int PERMISSION_REQ_CODE = 100;
    private static final int REQUEST_CODE_DEVICE_ADMIN = 200;
    private static final int REQUEST_CODE_IGNORE_BATTERY = 300;
    private static final int ADMIN_INTENT = REQUEST_CODE_DEVICE_ADMIN;

    private String[] permissionsRequired = {
            Manifest.permission.SEND_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_PHONE_STATE
    };

    private TextView tvPermissionsStatus;
    private TextView tvDeviceAdminStatus;
    private Button btnRequestPermissions;
    private Button btnRegister;
    private Button btnDeviceAdmin;
    private Button btnIgnoreBattery;
    private SwitchCompat adminPermission; // UI toggle for admin
    private TextView tvDeviceId,tvSessionStatus;
    private ListView lvSessions ;
    private Persistence storage;
    private ComponentName mComponentName;
    private DevicePolicyManager mDevicePolicyManager;
    private ArrayAdapter<String> sessionAdapter;
    private List<String> sessionDisplayList = new ArrayList<>();
    private Handler handler;
    private Runnable updateRunnable;

    private ComponentName adminComponent() {
        return new ComponentName(this, com.example.assistifyrelayapp.admin.MyDeviceAdminReceiver.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        storage = Persistence.getInstance(this);
        mComponentName = adminComponent();
        mDevicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        tvPermissionsStatus = findViewById(R.id.tv_permissions_status);
        tvDeviceAdminStatus = findViewById(R.id.tv_device_admin_status);
        btnRequestPermissions = findViewById(R.id.btn_request_permissions);
        btnRegister = findViewById(R.id.btn_register);
        btnDeviceAdmin = findViewById(R.id.btn_request_device_admin);
        btnIgnoreBattery = findViewById(R.id.btn_ignore_battery);
        tvDeviceId = findViewById(R.id.tv_device_id);
        lvSessions = findViewById(R.id.lv_sessions);
        tvSessionStatus = findViewById(R.id.tv_session_status);


        sessionAdapter = new ArrayAdapter<>(
                this,
                R.layout.list_item_session,   // <--- your custom layout
                R.id.listItemText,            // <--- TextView inside custom layout
                sessionDisplayList
        );
        lvSessions.setAdapter(sessionAdapter);

        handler = new Handler(Looper.getMainLooper());
        updateRunnable = this::updateStatus;
        updateStatus();

        btnRequestPermissions.setOnClickListener(v -> requestRuntimePermissions());

        btnRegister.setOnClickListener(v -> {
            if (hasAllPermissions()) {
                DeviceRegistrationManager.fetchAndRegister(SetupActivity.this);
                storage.setRegistered(true);
                Toast.makeText(SetupActivity.this, "Registering device...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(SetupActivity.this, "Please grant all permissions first", Toast.LENGTH_SHORT).show();
            }
            updateStatus();
        });

        btnDeviceAdmin.setOnClickListener(v -> {

            boolean isAdmin = mDevicePolicyManager.isAdminActive(mComponentName);

            if (!isAdmin) {
                // Enable Device Admin
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Assistify needs Device Admin to stay active in background.");
                startActivityForResult(intent, ADMIN_INTENT);

            } else {
                // Disable Device Admin
                mDevicePolicyManager.removeActiveAdmin(mComponentName);
                Toast.makeText(this, "Device Admin Disabled", Toast.LENGTH_SHORT).show();
                updateStatus();
            }
        });



        if (btnIgnoreBattery != null) {
            btnIgnoreBattery.setOnClickListener(v -> requestIgnoreBatteryOptimizations());
        }

        // reflect persisted setup info immediately
        updateStatus();
    }

    private void setupAdminPermissionToggle() {
        adminPermission.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (adminPermission.isChecked()) {
                        SendAndReceivePreferences.setboolean(getApplicationContext(), "adminPermission", true);

                        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName);
                        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Some description abt admin");

                        startActivityForResult(intent, ADMIN_INTENT);
                    } else {
                        SendAndReceivePreferences.setboolean(getApplicationContext(), "adminPermission", false);

                        try {
                            if (mDevicePolicyManager != null) {
                                mDevicePolicyManager.removeActiveAdmin(mComponentName);
                            }
                        } catch (Exception e) {
                            if (e.getMessage() != null && e.getMessage().contains("Admin ComponentInfo")) {
                                Toast.makeText(SetupActivity.this, "YOU DIDN'T GRANT ADMIN PERMISSION BEFORE!!", Toast.LENGTH_LONG).show();
                            }
                        }


                    }
                } catch (Exception e) {
                    if (adminPermission.isChecked() && (mDevicePolicyManager == null || mDevicePolicyManager.getActiveAdmins() == null)) {
                        Toast.makeText(SetupActivity.this, "You have not given admin permission", Toast.LENGTH_LONG).show();
                        return;
                    }
                    Toast.makeText(SetupActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
        handler.postDelayed(updateRunnable, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(updateRunnable);
    }

    private void updateStatus() {
        // === 1️⃣ Permissions & Admin Status ===
        boolean runtimePermissions = hasAllPermissions();
        boolean persistedPermissions = storage.isPermissionsGranted();

        if (runtimePermissions) {
            tvPermissionsStatus.setText("Status: All required permissions granted");
            tvPermissionsStatus.setTextColor(Color.GREEN);
            storage.setPermissionsGranted(true);
        } else if (persistedPermissions) {
            tvPermissionsStatus.setText("Status: Permissions appear granted (persisted) — verify system settings");
        } else {
            tvPermissionsStatus.setText("Status: Permissions missing — tap Request Permissions");
        }

        boolean isAdmin = mDevicePolicyManager != null && mDevicePolicyManager.isAdminActive(mComponentName);
        if (isAdmin) storage.setDeviceAdminEnabled(true);

        boolean persistedAdmin = storage.isDeviceAdminEnabled();
        tvDeviceAdminStatus.setTextColor(persistedAdmin ? Color.GREEN : Color.RED);
        tvDeviceAdminStatus.setText((persistedAdmin ? "Device admin: Enabled "  : "Device admin: Not enabled"));

        boolean registered = storage.isRegistered();
        if (btnRegister != null) {
            if (registered) {
                btnRegister.setText("Device Registered...");
                btnRegister.setTextColor(Color.GREEN);
                btnRegister.setEnabled(false);
            } else {
                btnRegister.setText("Register Device with Backend");
                btnRegister.setEnabled(true);
            }
        }

        // === 2️⃣ Device ID and Session Info ===
        String deviceId = DeviceRegistrationManager.getDeviceId(this);
        tvDeviceId.setText("Device ID: " + (deviceId != null ? deviceId : "Not registered"));
        tvDeviceId.setTextColor(deviceId != null  ? Color.GREEN : Color.RED);

        SessionController controller = SessionController.getInstance(this);
        sessionDisplayList.clear();

        if (controller.getActiveSessions().isEmpty()) {
            tvSessionStatus.setText("Session Status: IDLE");
            sessionDisplayList.add("No active sessions");
        } else {
            tvSessionStatus.setText("Session Status: ACTIVE (" + controller.getActiveSessions().size() + ")");
            for (SessionInfo session : controller.getActiveSessions().values()) {
                String info = "Session ID: " + session.sessionId +
                        "\nExpires at: " + new java.util.Date(session.expiresAt).toString();
                sessionDisplayList.add(info);
            }
        }

        sessionAdapter.notifyDataSetChanged();

        // === 3️⃣ Auto-refresh every 2 seconds ===
        handler.postDelayed(updateRunnable, 2000);
    }


    private void requestRuntimePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsRequired = new String[]{
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        }

        boolean allGranted = true;
        for (String permission : permissionsRequired) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            storage.setPermissionsGranted(true);
            Toast.makeText(this, "All permissions already granted", Toast.LENGTH_SHORT).show();
            updateStatus();
        } else {
            ActivityCompat.requestPermissions(this, permissionsRequired, PERMISSION_REQ_CODE);
        }
    }

    private boolean hasAllPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsRequired = new String[]{
                    Manifest.permission.SEND_SMS,
                    Manifest.permission.RECEIVE_SMS,
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        }

        for (String permission : permissionsRequired) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQ_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            storage.setPermissionsGranted(allGranted);

            if (allGranted) {
                Toast.makeText(this, "All permissions granted! You can now register.",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Some permissions denied. App may not function properly.",
                        Toast.LENGTH_LONG).show();
            }
            updateStatus();
        }
    }

    private void requestDeviceAdmin() {
        Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
        intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, mComponentName);
        intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Enabling Device Admin helps keep the app running in background on some devices.");
        startActivityForResult(intent, REQUEST_CODE_DEVICE_ADMIN);
    }

    private void requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = getPackageName();
            intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + packageName));
            try {
                startActivityForResult(intent, REQUEST_CODE_IGNORE_BATTERY);
            } catch (Exception e) {
                Intent fallback = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                try {
                    startActivity(fallback);
                } catch (Exception ex) {
                    Toast.makeText(this, "Unable to open battery settings", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            Toast.makeText(this, "Battery optimizations not supported on this Android version", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_DEVICE_ADMIN || requestCode == ADMIN_INTENT) {
            boolean isAdmin = mDevicePolicyManager != null && mDevicePolicyManager.isAdminActive(mComponentName);
            storage.setDeviceAdminEnabled(isAdmin);
            SendAndReceivePreferences.setboolean(getApplicationContext(), "adminPermission", isAdmin);

            if (isAdmin) {
                Toast.makeText(this, "Device admin enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Device admin NOT enabled", Toast.LENGTH_LONG).show();
            }
            updateStatus();
        } else if (requestCode == REQUEST_CODE_IGNORE_BATTERY) {
            storage.setBatteryOptimizationsIgnored(true);
            Toast.makeText(this, "Returned from battery settings. Please ensure Assistify is whitelisted.", Toast.LENGTH_LONG).show();
            updateStatus();
        }
    }
}