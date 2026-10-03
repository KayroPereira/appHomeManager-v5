package com.home.apphomemanager_v5.comodo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.home.apphomemanager_v5.tuya.model.TuyaDevice;
import com.home.apphomemanager_v5.tuya.model.TuyaStatus;

import org.junit.Test;

import java.util.Arrays;
import java.util.Locale;

public class LeituraDispositivoTest {

    private static final Locale PT_BR = new Locale("pt", "BR");

    private static TuyaDevice dispositivo(TuyaStatus... status) {

        TuyaDevice dispositivo = new TuyaDevice();
        dispositivo.online = true;
        dispositivo.status = Arrays.asList(status);
        return dispositivo;
    }

    @Test
    public void aplicaAEscalaGuardadaNaAssociacao() {

        ItemComodo item = new ItemComodo("Medidor");
        item.escalas.put("cur_power", 2);

        TuyaDevice aparelho = dispositivo(new TuyaStatus("cur_power", 1250.0));

        assertEquals(12.5, LeituraDispositivo.valor(aparelho, item, LeituraDispositivo.POTENCIA), 0.0001);
    }

    @Test
    public void semEscalaGuardadaUsaOPadraoDoCodigo() {

        ItemComodo item = new ItemComodo("Medidor");

        // Potência e tensão têm uma casa decimal por padrão; corrente vem direto em mA.
        TuyaDevice aparelho = dispositivo(new TuyaStatus("cur_power", 125), new TuyaStatus("cur_current", 98));

        assertEquals(12.5, LeituraDispositivo.valor(aparelho, item, LeituraDispositivo.POTENCIA), 0.0001);
        assertEquals(98.0, LeituraDispositivo.valor(aparelho, item, LeituraDispositivo.CORRENTE), 0.0001);
    }

    @Test
    public void usaOPrimeiroCodigoPresenteDaGrandeza() {

        ItemComodo item = new ItemComodo("Termometro");

        TuyaDevice aparelho = dispositivo(new TuyaStatus("va_temperature", 231));

        assertEquals(23.1, LeituraDispositivo.valor(aparelho, item, LeituraDispositivo.TEMPERATURA), 0.0001);
    }

    @Test
    public void grandezaAusenteOuAparelhoDesconhecidoDevolveNulo() {

        ItemComodo item = new ItemComodo("Termometro");

        assertNull(LeituraDispositivo.valor(dispositivo(new TuyaStatus("switch_1", true)), item, LeituraDispositivo.UMIDADE));
        assertNull(LeituraDispositivo.valor(null, item, LeituraDispositivo.TEMPERATURA));
    }

    @Test
    public void formataCadaGrandezaComSuaUnidade() {

        assertEquals("12,5 W", LeituraDispositivo.potencia(12.5, PT_BR));
        assertEquals("127,1 V", LeituraDispositivo.tensao(127.1, PT_BR));
        assertEquals("24,5 °C", LeituraDispositivo.temperatura(24.5, PT_BR));
        assertEquals("58 %", LeituraDispositivo.umidade(58.0, PT_BR));
    }

    @Test
    public void correnteMudaDeMiliampereParaAmpere() {

        assertEquals("98 mA", LeituraDispositivo.corrente(98.0, PT_BR));
        assertEquals("1,25 A", LeituraDispositivo.corrente(1250.0, PT_BR));
    }

    @Test
    public void semLeituraMostraTraco() {

        assertEquals("--", LeituraDispositivo.potencia(null, PT_BR));
        assertEquals("--", LeituraDispositivo.corrente(null, PT_BR));
        assertEquals("--", LeituraDispositivo.tensao(null, PT_BR));
        assertEquals("--", LeituraDispositivo.temperatura(null, PT_BR));
        assertEquals("--", LeituraDispositivo.umidade(null, PT_BR));
    }
}
