package com.example.assistifyrelayapp.core.jsonclass;

public class DeviceRegisterRequest {
    public String fcmToken;
    public String model;
    public String appVersion;
    public String deviceKeyHash;
    public String capabilities;

    public DeviceRegisterRequest() {
    }


    public DeviceRegisterRequest(String fcmToken, String model, String appVersion, String deviceKeyHash, String capabilities) {
        this.fcmToken = fcmToken;
        this.model = model;
        this.appVersion = appVersion;
        this.deviceKeyHash = deviceKeyHash;
        this.capabilities = capabilities;
    }

    public String getFcmToken() {
        return fcmToken;
    }

    public String getModel() {
        return model;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public String getDeviceKeyHash() {
        return deviceKeyHash;
    }

    public String getCapabilities() {
        return capabilities;
    }
}

