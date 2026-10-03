package com.home.apphomemanager_v5.util;

import static org.junit.Assert.assertEquals;

import com.home.apphomemanager_v5.R;

import org.junit.Test;

public class IconeClimaTest {

    @Test
    public void condicaoRespeitaDiaENoite() {
        assertEquals(R.drawable.clear_day, IconeClima.condicao(0, true));
        assertEquals(R.drawable.clear_night, IconeClima.condicao(0, false));
        assertEquals(R.drawable.cloudly_day, IconeClima.condicao(2, true));
        assertEquals(R.drawable.cloudly_night, IconeClima.condicao(1, false));
    }

    @Test
    public void condicoesSemVarianteDiaNoite() {
        assertEquals(R.drawable.cloud, IconeClima.condicao(3, true));
        assertEquals(R.drawable.cloud, IconeClima.condicao(45, false));
        assertEquals(R.drawable.rain, IconeClima.condicao(61, true));
        assertEquals(R.drawable.rain, IconeClima.condicao(80, true));
        assertEquals(R.drawable.snow, IconeClima.condicao(73, true));
        assertEquals(R.drawable.storm, IconeClima.condicao(95, false));
    }

    @Test
    public void codigoDesconhecidoOuNulo() {
        assertEquals(R.drawable.none_day, IconeClima.condicao(null, true));
        assertEquals(R.drawable.none_night, IconeClima.condicao(42, false));
    }

    @Test
    public void termometroPorFaixa() {
        assertEquals(R.drawable.tlow, IconeClima.termometro(14L));
        assertEquals(R.drawable.taverage, IconeClima.termometro(15L));
        assertEquals(R.drawable.taverage, IconeClima.termometro(25L));
        assertEquals(R.drawable.thigh, IconeClima.termometro(26L));
        assertEquals(R.drawable.taverage, IconeClima.termometro(null));
    }
}
