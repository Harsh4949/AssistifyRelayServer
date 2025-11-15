package com.example.assistifyrelayapp.core;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.Call;

import com.example.assistifyrelayapp.core.jsonclass.*;

import java.io.IOException;

public class NetClient {
    private static final String BASE_URL = "https://assistify-backend-tf6m.onrender.com";
    private static NetClient instance;
    private AssistifyApiService apiService;

    private NetClient() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(AssistifyApiService.class);
    }

    public static NetClient getInstance() {
        if (instance == null) {
            synchronized (NetClient.class) {
                if (instance == null) {
                    instance = new NetClient();
                }
            }
        }
        return instance;
    }

    // Get the raw Retrofit service for async calls
    public AssistifyApiService getApiService() {
        return apiService;
    }

    // Synchronous methods (for background threads like Workers)
    public ApiResponse registerDeviceSync(DeviceRegisterRequest request) throws IOException {
        return apiService.registerDevice(request).execute().body();
    }

    public ApiResponse sendHeartbeatSync(HeartbeatRequest request) throws IOException {
        return apiService.sendHeartbeat(request).execute().body();
    }

    public ApiResponse sendSmsResultSync(SmsResultRequest request) throws IOException {
        return apiService.sendSmsResult(request).execute().body();
    }

    public ApiResponse stopSessionSync(String sessionId) throws IOException {
        return apiService.stopSession(sessionId, new SessionStopRequest()).execute().body();
    }

    // Async methods return Call objects for .enqueue()
    public Call<ApiResponse> registerDevice(DeviceRegisterRequest request) {
        return apiService.registerDevice(request);
    }

    public Call<ApiResponse> sendHeartbeat(HeartbeatRequest request) {
        return apiService.sendHeartbeat(request);
    }

    public Call<ApiResponse> sendSmsResult(SmsResultRequest request) {
        return apiService.sendSmsResult(request);
    }

    public Call<ApiResponse> stopSession(String sessionId) {
        return apiService.stopSession(sessionId, new SessionStopRequest());
    }
}
