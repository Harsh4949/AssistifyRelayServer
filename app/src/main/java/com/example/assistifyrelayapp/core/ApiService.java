package com.example.assistifyrelayapp.core;

import com.example.assistifyrelayapp.core.jsonclass.TransactionData;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;


public interface ApiService {

    //    @POST("queue-received-payments")
    //https://assistify-backend-tf6m.onrender.com/api/v1

    @POST
    Call<Void> sendTransaction(@retrofit2.http.Url String url, @Body TransactionData data);
}
