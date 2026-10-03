package com.home.apphomemanager_v5.model.clima;

import lombok.Data;

@Data
public class Forecast {

    private String date;
    private String weekday;
    private Long max;
    private Long min;
    private String description;
    private String condition;

    /** Código de tempo WMO (Open-Meteo). */
    private Integer weatherCode;
}
