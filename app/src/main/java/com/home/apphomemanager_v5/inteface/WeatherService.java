package com.home.apphomemanager_v5.inteface;

import com.home.apphomemanager_v5.model.WeatherResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

public interface WeatherService {

    @GET("weather")
    Call<WeatherResponse> getCurrentWeather(
            @Query("key") String apiKey,
            @Query("lat") double lat,
            @Query("lon") double lon,
            @Query("user_ip") boolean useUserIp
    );
}