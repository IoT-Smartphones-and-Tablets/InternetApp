package com.example.internetapp;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface SaveDatabaseService {

    @FormUrlEncoded
    @POST("/save")
    Call<String> saveData(@Field("lat") float lat,
                          @Field("lon") float lon);
}
