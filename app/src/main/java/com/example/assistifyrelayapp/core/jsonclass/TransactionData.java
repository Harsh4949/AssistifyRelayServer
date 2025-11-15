package com.example.assistifyrelayapp.core.jsonclass;

public class TransactionData {

    public String sessionId;
    public String from;
    public String body;
    public String receivedAt;
    public String status ;

    public TransactionData() {

    }

    public TransactionData(String sessionId, String from, String body, String receivedAt, String status) {
        this.sessionId = sessionId;
        this.from = from;
        this.body = body;
        this.receivedAt = receivedAt;
        this.status = status;
    }

    public String getSessionId() {
        return this.sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getFrom() {
        return this.from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getBody() {
        return this.body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getReceivedAt() {
        return this.receivedAt;
    }

    public void setReceivedAt(String receivedAt) {
        this.receivedAt = receivedAt;
    }


    public String getStatus() {
        return this.status;
    }

    public void setStatus(String success) {

        this.status=success;
    }
}
