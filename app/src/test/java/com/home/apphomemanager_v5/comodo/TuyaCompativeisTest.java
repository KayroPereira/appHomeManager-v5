package com.home.apphomemanager_v5.comodo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.home.apphomemanager_v5.tuya.TuyaDispositivo;
import com.home.apphomemanager_v5.tuya.TuyaPonto;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TuyaCompativeisTest {

    private static TuyaDispositivo dispositivo(String id, String nome, TuyaPonto... pontos) {
        return new TuyaDispositivo(id, nome, true, Arrays.asList(pontos));
    }

    private static TuyaPonto ponto(String code, Object valor) {
        return new TuyaPonto(code, valor, null);
    }

    private static TuyaPonto ponto(String code, Object valor, int escala) {
        return new TuyaPonto(code, valor, "{\"type\":\"value\",\"scale\":" + escala + "}");
    }

    private final List<TuyaDispositivo> conta = Arrays.asList(
            dispositivo("lampada", "Lampada da sala", ponto("switch_led", true)),
            dispositivo("tomada", "Tomada simples", ponto("switch_1", false)),
            dispositivo("medidor", "Plug com medidor", ponto("switch_1", true),
                    ponto("cur_power", 125, 1), ponto("cur_voltage", 1271, 1), ponto("cur_current", 98, 0)),
            dispositivo("sensor", "Sensor da varanda", ponto("temp_current", 245, 1), ponto("humidity_value", 58, 0)),
            dispositivo("camera", "Camera", ponto("motion_switch", true)));

    private static List<String> ids(List<TuyaCompativeis.Opcao> opcoes) {

        List<String> ids = new ArrayList<>();

        for (TuyaCompativeis.Opcao opcao : opcoes) {
            ids.add(opcao.deviceId);
        }
        return ids;
    }

    @Test
    public void lampadasETomadasSoRecebemInterruptoresComuns() {

        List<String> esperados = Arrays.asList("lampada", "tomada", "medidor");

        assertEquals(esperados, ids(TuyaCompativeis.para(GrupoDispositivo.LAMPADAS, conta)));
        assertEquals(esperados, ids(TuyaCompativeis.para(GrupoDispositivo.TOMADAS, conta)));
    }

    @Test
    public void medidoresSoRecebemInterruptoresDeAparelhosQueMedemEnergia() {

        List<TuyaCompativeis.Opcao> opcoes = TuyaCompativeis.para(GrupoDispositivo.MEDIDORES, conta);

        assertEquals(Arrays.asList("medidor"), ids(opcoes));
        assertEquals("switch_1", opcoes.get(0).code);
        assertEquals("medidor/switch_1", opcoes.get(0).chave());
    }

    @Test
    public void medidorGuardaAsEscalasDasMedicoes() {

        TuyaCompativeis.Opcao medidor = TuyaCompativeis.para(GrupoDispositivo.MEDIDORES, conta).get(0);

        assertEquals(Integer.valueOf(1), medidor.escalas.get("cur_power"));
        assertEquals(Integer.valueOf(1), medidor.escalas.get("cur_voltage"));
        assertEquals(Integer.valueOf(0), medidor.escalas.get("cur_current"));
    }

    @Test
    public void termometrosSoRecebemAparelhosComTemperatura() {

        List<TuyaCompativeis.Opcao> opcoes = TuyaCompativeis.para(GrupoDispositivo.TERMOMETROS, conta);

        assertEquals(Arrays.asList("sensor"), ids(opcoes));
        assertEquals("temp_current", opcoes.get(0).code);
        assertEquals("Sensor da varanda", opcoes.get(0).titulo);
        assertEquals(Integer.valueOf(1), opcoes.get(0).escalas.get("temp_current"));
        assertEquals(Integer.valueOf(0), opcoes.get(0).escalas.get("humidity_value"));
    }

    @Test
    public void aparelhoComDoisPontosDeTemperaturaApareceUmaVez() {

        List<TuyaDispositivo> dispositivos = Arrays.asList(
                dispositivo("duplo", "Duplo", ponto("temp_current", 200), ponto("va_temperature", 210)));

        assertEquals(1, TuyaCompativeis.para(GrupoDispositivo.TERMOMETROS, dispositivos).size());
    }

    @Test
    public void semDispositivosCompativeisAListaFicaVazia() {

        List<TuyaDispositivo> soLampada = Arrays.asList(dispositivo("l", "L", ponto("switch_led", true)));

        assertTrue(TuyaCompativeis.para(GrupoDispositivo.MEDIDORES, soLampada).isEmpty());
        assertTrue(TuyaCompativeis.para(GrupoDispositivo.TERMOMETROS, soLampada).isEmpty());
    }
}
