package com.example.assistifyrelayapp.core.jsonclass;

public class ApiResponse {

    public String status;
    public String deviceId;

    public ApiResponse(String status, String deviceId) {
        this.status = status;
        this.deviceId = deviceId;
    }


    // Getters and setters

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }


}