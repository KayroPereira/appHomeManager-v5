package com.home.apphomemanager_v5.inteface;

import com.home.apphomemanager_v5.model.clima.OpenMeteoResponse;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Query;

/** API Open-Meteo: gratuita e sem chave. */
public interface WeatherService {

    String BASE_URL = "https://api.open-meteo.com/";

    /** Hoje + 3 dias de previsão, com datas no fuso da própria coordenada. */
    @GET("v1/forecast?current=temperature_2m,relative_humidity_2m,weather_code,is_day"
            + "&daily=weather_code,temperature_2m_max,temperature_2m_min"
            + "&timezone=auto&forecast_days=4")
    Call<OpenMeteoResponse> getCurrentWeather(
            @Query("latitude") double lat,
            @Query("longitude") double lon
    );
}
