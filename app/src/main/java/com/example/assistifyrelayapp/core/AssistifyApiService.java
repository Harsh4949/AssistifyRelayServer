package com.example.assistifyrelayapp.core;

import com.example.assistifyrelayapp.core.jsonclass.ApiResponse;
import com.example.assistifyrelayapp.core.jsonclass.DeviceRegisterRequest;
import com.example.assistifyrelayapp.core.jsonclass.HeartbeatRequest;
import com.example.assistifyrelayapp.core.jsonclass.IncomingSmsRequest;
import com.example.assistifyrelayapp.core.jsonclass.SessionStopRequest;
import com.example.assistifyrelayapp.core.jsonclass.SmsResultRequest;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface AssistifyApiService {

    @POST("api/v1/devices/register")
    Call<ApiResponse> registerDevice(@Body DeviceRegisterRequest request);

    @POST("api/v1/devices/heartbeat")
    Call<ApiResponse> sendHeartbeat(@Body HeartbeatRequest request);

    @POST("api/v1/devices/sms-result")
    Call<ApiResponse> sendSmsResult(@Body SmsResultRequest request);

    @POST("api/v1/sessions/{id}/stop")
    Call<ApiResponse> stopSession(@Path("id") String sessionId, @Body SessionStopRequest req);

    @POST("api/v1/incoming-sms")
    Call<ApiResponse> sendIncomingSms(@Body IncomingSmsRequest request);

    @POST("api/v1devices/ack")
    Call<ApiResponse> ackDevice(@Body Map<String, Object> request);
}