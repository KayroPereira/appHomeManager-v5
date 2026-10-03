package com.home.apphomemanager_v5.comodo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.home.apphomemanager_v5.tuya.TuyaDispositivo;
import com.home.apphomemanager_v5.tuya.TuyaPonto;
import com.home.apphomemanager_v5.tuya.model.TuyaDevice;
import com.home.apphomemanager_v5.tuya.model.TuyaStatus;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class LuzAjustavelTest {

    private static final String FAIXA_BRILHO = "{\"type\":\"value\",\"min\":10,\"max\":1000,\"scale\":0,\"step\":1}";
    private static final String FAIXA_TEMP = "{\"type\":\"value\",\"min\":0,\"max\":1000,\"scale\":0,\"step\":1}";

    private static TuyaDevice aparelho(TuyaStatus... status) {

        TuyaDevice dispositivo = new TuyaDevice();
        dispositivo.online = true;
        dispositivo.status = Arrays.asList(status);
        return dispositivo;
    }

    @Test
    public void listaSoLampadasComBrilhoOuTemperatura() {

        List<TuyaDispositivo> conta = Arrays.asList(
                new TuyaDispositivo("lampada", "Sancra", true, Arrays.asList(
                        new TuyaPonto("switch_led", true, null),
                        new TuyaPonto("bright_value_v2", 500, FAIXA_BRILHO),
                        new TuyaPonto("temp_value_v2", 300, FAIXA_TEMP))),
                new TuyaDispositivo("tomada", "Tomada", true, Arrays.asList(new TuyaPonto("switch_1", true, null))));

        List<TuyaCompativeis.Opcao> opcoes = TuyaCompativeis.para(GrupoDispositivo.LUZ, conta);

        assertEquals(1, opcoes.size());
        assertEquals("lampada", opcoes.get(0).deviceId);
        assertEquals("bright_value_v2", opcoes.get(0).code);
        assertEquals(Integer.valueOf(10), opcoes.get(0).escalas.get("bright_min"));
        assertEquals(Integer.valueOf(1000), opcoes.get(0).escalas.get("bright_max"));
        assertEquals(Integer.valueOf(0), opcoes.get(0).escalas.get("temp_min"));
    }

    @Test
    public void leEstadoUsandoAFaixaGuardadaNaAssociacao() {

        ItemComodo item = new ItemComodo("Sancra");
        item.associa("lampada", "bright_value_v2", "Sancra");
        item.escalas.put("bright_min", 10);
        item.escalas.put("bright_max", 1000);
        item.escalas.put("temp_min", 0);
        item.escalas.put("temp_max", 1000);

        // 505 de 10..1000 = 50%; temperatura 900 de 0..1000 = 90% (fria).
        LeituraDispositivo.EstadoLuz estado = LeituraDispositivo.estadoLuz(
                aparelho(new TuyaStatus("switch_led", true), new TuyaStatus("bright_value_v2", 505), new TuyaStatus("temp_value_v2", 900)), item);

        assertEquals(Boolean.TRUE, estado.ligada);
        assertEquals(Integer.valueOf(50), estado.brilho);
        assertEquals(Integer.valueOf(90), estado.temperatura);

        assertNull(LeituraDispositivo.estadoLuz(null, item));
    }

    @Test
    public void porcentagemViraODegrauMaisProximo() {

        assertEquals(0, LeituraDispositivo.degrauDeTemperatura(0));
        assertEquals(1, LeituraDispositivo.degrauDeTemperatura(50));
        assertEquals(2, LeituraDispositivo.degrauDeTemperatura(100));

        assertEquals(0, LeituraDispositivo.degrauDeBrilho(20));
        assertEquals(1, LeituraDispositivo.degrauDeBrilho(55));
        assertEquals(2, LeituraDispositivo.degrauDeBrilho(80));
        assertEquals(3, LeituraDispositivo.degrauDeBrilho(100));
    }

    @Test
    public void lampadaDesligadaInformaQueEstaDesligada() {

        ItemComodo item = new ItemComodo("Sancra");
        item.associa("lampada", "bright_value", "Sancra");

        LeituraDispositivo.EstadoLuz estado = LeituraDispositivo.estadoLuz(
                aparelho(new TuyaStatus("switch_led", false), new TuyaStatus("bright_value", 10)), item);

        assertEquals(Boolean.FALSE, estado.ligada);
        assertEquals(Integer.valueOf(0), estado.brilho);
        assertTrue(estado.temperatura == null);
    }
}
