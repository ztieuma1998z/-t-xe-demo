package com.nghiatv.uberdemo.remote;

import com.nghiatv.uberdemo.model.DataMessage;
import com.nghiatv.uberdemo.model.FCMResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface IFCMService {
    @Headers({
            "Content-Type:application/json",
            "Authorization:key=AAAAR1ODy9k:APA91bETLeCo1zJfw1zPgxZnzmZjc-nUkT9LuasDYPMN5eIaaS_OnbeQopjBNASIpwZ4i3OTFS4eBpgEipnXyGkT0HZz7xCksD-8CUOOAjKLcNlbTVKvlZPHZTKdunkpbcOysVrWoIZ3XHnldtPrf40VaAktKT36og"
    })

    @POST("fcm/send")
    Call<FCMResponse> sendMessage(@Body DataMessage body);
}
