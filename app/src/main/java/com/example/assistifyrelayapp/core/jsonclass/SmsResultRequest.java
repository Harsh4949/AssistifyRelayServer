package com.example.assistifyrelayapp.core.jsonclass;

public class SmsResultRequest {
    public String msgId;
    public String status;
    public String providerMessageId;
    public String sentAt;
    public String deliveredAt;
    public String error;
    public String sessionId;

    public SmsResultRequest() {
    }

    public SmsResultRequest(String msgId, String status, String providerMessageId, String sentAt, String deliveredAt, String error) {
        this.msgId = msgId;
        this.status = status;
        this.providerMessageId = providerMessageId;
        this.sentAt = sentAt;
        this.deliveredAt = deliveredAt;
        this.error = error;
    }

    public String getMsgId() {
        return msgId;
    }

    public String getStatus() {
        return status;
    }

    public String getProviderMessageId() {
        return providerMessageId;
    }

    public String getSentAt() {
        return sentAt;
    }

    public String getDeliveredAt() {
        return deliveredAt;
    }

    public String getError() {
        return error;
    }

}
