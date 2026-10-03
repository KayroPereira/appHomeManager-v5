package com.home.apphomemanager_v5.tuya;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class TuyaControleTest {

    private static final Locale PT_BR = new Locale("pt", "BR");

    private static final String FAIXA_BRILHO = "{\"type\":\"value\",\"min\":10,\"max\":1000,\"scale\":0,\"step\":1,\"unit\":\"\"}";

    private static TuyaDispositivo dispositivo(String id, String nome, boolean online, TuyaPonto... pontos) {
        return new TuyaDispositivo(id, nome, online, Arrays.asList(pontos));
    }

    private static TuyaPonto ponto(String code, Object valor) {
        return new TuyaPonto(code, valor, null);
    }

    @Test
    public void extraiUmaLinhaPorInterruptor() {

        List<TuyaControle> lista = TuyaControle.extrai(Arrays.asList(
                dispositivo("1", "Lampada", true, ponto("switch_led", true)),
                dispositivo("2", "Tomada dupla", false, ponto("switch_1", false), ponto("switch_2", true)),
                dispositivo("3", "Outro", true, ponto("outro_ponto", 250))));

        // 3 interruptores + o aparelho "Outro", que não tem controle compatível e vira informativo.
        assertEquals(4, lista.size());
        assertEquals(TuyaControle.Tipo.INFORMATIVO, lista.get(3).tipo);

        assertEquals("Lampada", lista.get(0).titulo());
        assertTrue(lista.get(0).ligado);

        assertEquals("Tomada dupla (switch_1)", lista.get(1).titulo());
        assertFalse(lista.get(1).online);
        assertEquals("Tomada dupla (switch_2)", lista.get(2).titulo());
        assertTrue(lista.get(2).ligado);
    }

    @Test
    public void dispositivoSemPontosApareceComoInformativo() {

        List<TuyaControle> lista = TuyaControle.extrai(Collections.singletonList(dispositivo("9", "Vazio", true)));

        assertEquals(1, lista.size());
        assertEquals(TuyaControle.Tipo.INFORMATIVO, lista.get(0).tipo);
        assertEquals("Vazio", lista.get(0).titulo());
    }

    @Test
    public void sliderUsaFaixaDoEsquema() {

        List<TuyaControle> lista = TuyaControle.extrai(Collections.singletonList(
                dispositivo("1", "Lampada", true, ponto("switch_led", true), new TuyaPonto("bright_value_v2", 505, FAIXA_BRILHO))));

        assertEquals(2, lista.size());

        TuyaControle slider = lista.get(1);

        assertEquals(TuyaControle.Tipo.SLIDER, slider.tipo);
        assertEquals("Lampada - Brilho", slider.titulo());
        assertEquals(990, slider.maximoSlider());
        assertEquals(495, slider.progressoSlider());
        assertEquals(505, slider.valorDoProgresso(495));
        assertEquals(50, slider.percentual());
    }

    @Test
    public void sliderSemFaixaNaoViraSlider() {

        List<TuyaControle> lista = TuyaControle.extrai(Collections.singletonList(
                dispositivo("1", "Lampada", true, ponto("bright_value_v2", 505))));

        assertEquals(1, lista.size());
        assertEquals(TuyaControle.Tipo.INFORMATIVO, lista.get(0).tipo);
    }

    @Test
    public void sensorAplicaEscalaEUnidadeDoEsquema() {

        List<TuyaControle> lista = TuyaControle.extrai(Collections.singletonList(
                dispositivo("1", "Sala", true,
                        new TuyaPonto("temp_current", 245, "{\"unit\":\"℃\",\"scale\":1,\"min\":-200,\"max\":1000}"),
                        ponto("humidity_value", 61))));

        assertEquals(2, lista.size());
        assertEquals("Sala - Temperatura", lista.get(0).titulo());
        assertEquals("24,5 ℃", lista.get(0).textoSensor(PT_BR));
        // Sem esquema para a umidade, valem escala e unidade padrão.
        assertEquals("61 %", lista.get(1).textoSensor(PT_BR));
    }

    @Test
    public void pontosNaoMapeadosMostramOsCodigosNoInformativo() {

        List<TuyaControle> lista = TuyaControle.extrai(Collections.singletonList(
                dispositivo("1", "Camera", true, ponto("countdown_1", 0), ponto("doorbell_pic", ""))));

        assertEquals(1, lista.size());
        assertEquals(TuyaControle.Tipo.INFORMATIVO, lista.get(0).tipo);
        assertEquals("countdown_1, doorbell_pic", lista.get(0).rotulo);
    }

    @Test
    public void esquemaInvalidoNaoDerrubaAExtracao() {

        List<TuyaControle> lista = TuyaControle.extrai(Collections.singletonList(
                dispositivo("1", "Sala", true, new TuyaPonto("temp_current", 245, "isso nao e json"))));

        assertEquals(1, lista.size());
        assertEquals("24,5 °C", lista.get(0).textoSensor(PT_BR));
    }

    @Test
    public void interfoneComOsPontosReaisDoLog() {

        List<TuyaControle> lista = TuyaControle.extrai(Collections.singletonList(
                dispositivo("eb90", "Interfone", true,
                        new TuyaPonto("motion_sensitivity", "1", "{\"range\":[\"0\",\"1\",\"2\"]}"),
                        new TuyaPonto("sd_storge", "131066880|1869056|129197824", "{\"maxlen\":255}"),
                        new TuyaPonto("sd_status", 1.0, "{\"min\":1,\"max\":5,\"scale\":0,\"step\":1}"),
                        new TuyaPonto("movement_detect_pic", "$", "{}"),
                        new TuyaPonto("motion_switch", false, "{}"),
                        new TuyaPonto("doorbell_active", "", "{\"maxlen\":255}"),
                        new TuyaPonto("motion_area_switch", true, "{}"),
                        new TuyaPonto("alarm_message", "", "{}"))), PT_BR);

        // Interruptores, depois o seletor de sensibilidade e, por último, os 2 textos do cartão SD.
        assertEquals(5, lista.size());

        assertEquals("Interfone - Detecção de movimento", lista.get(0).titulo());
        assertFalse(lista.get(0).ligado);
        assertEquals("Interfone - Área de detecção", lista.get(1).titulo());
        assertTrue(lista.get(1).ligado);

        TuyaControle sensibilidade = lista.get(2);

        assertEquals(TuyaControle.Tipo.SELETOR, sensibilidade.tipo);
        assertEquals(2, sensibilidade.maximoSlider());
        assertEquals(1, sensibilidade.progressoSlider());
        assertEquals("Média", sensibilidade.textoSlider(1));
        assertEquals("2", sensibilidade.valorParaComando(2));

        assertEquals(TuyaControle.Tipo.TEXTO, lista.get(3).tipo);
        assertEquals("123,2 GB livres de 125 GB", lista.get(3).textoFixo);
        assertEquals(TuyaControle.Tipo.TEXTO, lista.get(4).tipo);
        assertEquals("Normal", lista.get(4).textoFixo);
    }

    @Test
    public void seletorComValorForaDaFaixaNaoAparece() {

        List<TuyaControle> lista = TuyaControle.extrai(Collections.singletonList(
                dispositivo("1", "Camera", true, new TuyaPonto("motion_sensitivity", "9", "{\"range\":[\"0\",\"1\",\"2\"]}"))));

        assertEquals(1, lista.size());
        assertEquals(TuyaControle.Tipo.INFORMATIVO, lista.get(0).tipo);
    }}
