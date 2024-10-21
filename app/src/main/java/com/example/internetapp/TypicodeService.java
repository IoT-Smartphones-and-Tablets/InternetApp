package com.example.internetapp;

import retrofit2.Call;
import retrofit2.http.GET;

public interface TypicodeService {

    @GET("/typicode/demo/profile")
    Call<Profile> getProfile();
}

