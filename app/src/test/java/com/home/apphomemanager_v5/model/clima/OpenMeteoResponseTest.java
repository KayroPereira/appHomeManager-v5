package com.home.apphomemanager_v5.model.clima;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.google.gson.Gson;

import org.junit.Test;

public class OpenMeteoResponseTest {

    private static final String JSON = "{\"current\":{\"time\":\"2026-09-28T14:15\",\"temperature_2m\":31.9,"
            + "\"relative_humidity_2m\":35,\"weather_code\":1,\"is_day\":0},"
            + "\"daily\":{\"time\":[\"2026-09-28\",\"2026-09-29\",\"2026-09-30\",\"2026-10-01\"],"
            + "\"weather_code\":[3,80,95,95],"
            + "\"temperature_2m_max\":[32.2,32.8,31.3,27.6],"
            + "\"temperature_2m_min\":[17.9,19.4,20.4,16.5]}}";

    @Test
    public void converteRespostaParaModeloDaTela() {

        ForecastActual clima = new Gson().fromJson(JSON, OpenMeteoResponse.class).toForecastActual();

        assertEquals(Long.valueOf(32), clima.getTemp());
        assertEquals(Long.valueOf(35), clima.getHumidity());
        assertEquals("Predominantemente limpo", clima.getDescription());
        assertEquals(Integer.valueOf(1), clima.getWeatherCode());
        assertEquals(Boolean.FALSE, clima.getDia());
        assertEquals(4, clima.getForecasts().size());

        Forecast amanha = clima.getForecasts().get(1);
        assertEquals("Ter", amanha.getWeekday());
        assertEquals(Long.valueOf(33), amanha.getMax());
        assertEquals(Long.valueOf(19), amanha.getMin());
        assertEquals("Pancadas de chuva", amanha.getDescription());
        assertEquals(Integer.valueOf(80), amanha.getWeatherCode());

        assertEquals("Qui", clima.getForecasts().get(3).getWeekday());
    }

    @Test
    public void listasIncompletasNaoQuebram() {

        String json = "{\"daily\":{\"time\":[\"2026-09-28\",\"2026-09-29\"],\"temperature_2m_max\":[30.0]}}";

        ForecastActual clima = new Gson().fromJson(json, OpenMeteoResponse.class).toForecastActual();

        assertNull(clima.getTemp());
        assertEquals(2, clima.getForecasts().size());
        assertNull(clima.getForecasts().get(1).getMax());
    }
}
