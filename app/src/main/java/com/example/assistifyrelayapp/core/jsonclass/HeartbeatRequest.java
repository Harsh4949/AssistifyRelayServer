package com.example.assistifyrelayapp.core.jsonclass;

public class HeartbeatRequest {

    public String deviceId;
    public Integer battery;
    public String network;
    public Integer queueDepth;

    public HeartbeatRequest(){}


    public HeartbeatRequest(String deviceId, Integer battery, String network, Integer queueDepth){
        this.deviceId = deviceId;
        this.battery = battery;
        this.network = network;
        this.queueDepth = queueDepth;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public Integer getBattery() {
        return battery;
    }

    public String getNetwork() {
        return network;
    }

    public Integer getQueueDepth() {
        return queueDepth;
    }


}