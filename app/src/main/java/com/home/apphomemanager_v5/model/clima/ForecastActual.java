package com.home.apphomemanager_v5.model.clima;

import java.util.List;

import lombok.Data;

/** Clima atual + previsão diária (posição 0 = hoje), no formato exibido pelo dashboard. */
@Data
public class ForecastActual {

    private Long temp;

    private String date;

    private Long humidity;

    private String description;

    /** Código de tempo WMO (Open-Meteo). */
    private Integer weatherCode;

    private Boolean dia;

    private String cityName;

    private List<Forecast> forecasts;
}
