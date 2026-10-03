package com.home.apphomemanager_v5.model.clima;

import com.google.gson.annotations.SerializedName;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import lombok.Data;

/** Resposta de /v1/forecast da Open-Meteo, convertida para {@link ForecastActual} para a tela. */
@Data
public class OpenMeteoResponse {

    private static final Locale PT_BR = new Locale("pt", "BR");

    private Current current;

    private Daily daily;

    @Data
    public static class Current {

        private String time;

        @SerializedName("temperature_2m")
        private Double temperature;

        @SerializedName("relative_humidity_2m")
        private Double humidity;

        @SerializedName("weather_code")
        private Integer weatherCode;

        /** 1 = dia, 0 = noite. */
        @SerializedName("is_day")
        private Integer isDay;
    }

    /** Listas paralelas: o índice i de cada uma corresponde ao dia time[i]. */
    @Data
    public static class Daily {

        private List<String> time;

        @SerializedName("weather_code")
        private List<Integer> weatherCode;

        @SerializedName("temperature_2m_max")
        private List<Double> temperatureMax;

        @SerializedName("temperature_2m_min")
        private List<Double> temperatureMin;
    }

    public ForecastActual toForecastActual() {

        ForecastActual clima = new ForecastActual();
        List<Forecast> previsoes = new ArrayList<>();

        if (current != null) {
            clima.setTemp(arredonda(current.getTemperature()));
            clima.setHumidity(arredonda(current.getHumidity()));
            clima.setDate(current.getTime());
            clima.setDescription(CondicaoClima.descricao(current.getWeatherCode()));
            clima.setWeatherCode(current.getWeatherCode());
            clima.setDia(current.getIsDay() == null || current.getIsDay() == 1);
        }

        if (daily != null && daily.getTime() != null) {
            for (int i = 0; i < daily.getTime().size(); i++) {

                String data = daily.getTime().get(i);

                Forecast previsao = new Forecast();
                previsao.setDate(data);
                previsao.setWeekday(diaDaSemana(data));
                previsao.setMax(arredonda(item(daily.getTemperatureMax(), i)));
                previsao.setMin(arredonda(item(daily.getTemperatureMin(), i)));
                previsao.setDescription(CondicaoClima.descricao(item(daily.getWeatherCode(), i)));
                previsao.setWeatherCode(item(daily.getWeatherCode(), i));

                previsoes.add(previsao);
            }
        }

        clima.setForecasts(previsoes);

        return clima;
    }

    /** "2026-09-29" -> "Ter". */
    static String diaDaSemana(String dataIso) {

        try {
            String dia = LocalDate.parse(dataIso).getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "");
            return dia.isEmpty() ? dia : dia.substring(0, 1).toUpperCase(PT_BR) + dia.substring(1);
        } catch (Exception e) {
            return dataIso;
        }
    }

    private static Long arredonda(Double valor) {
        return valor != null ? Math.round(valor) : null;
    }

    private static <T> T item(List<T> lista, int indice) {
        return lista != null && indice < lista.size() ? lista.get(indice) : null;
    }
}
