package com.home.apphomemanager_v5.comodo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.home.apphomemanager_v5.comodo.LeituraDispositivo.EstadoAlarme;
import com.home.apphomemanager_v5.tuya.model.TuyaDevice;
import com.home.apphomemanager_v5.tuya.model.TuyaStatus;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardDadosTest {

    private static TuyaDevice aparelho(boolean online, TuyaStatus... status) {

        TuyaDevice dispositivo = new TuyaDevice();
        dispositivo.online = online;
        dispositivo.status = Arrays.asList(status);
        return dispositivo;
    }

    private static ItemComodo item(String nome, String deviceId, String code) {

        ItemComodo item = new ItemComodo(nome);
        item.associa(deviceId, code, nome);
        return item;
    }

    private static Comodo comodo(String id, String nome) {

        Comodo comodo = new Comodo();
        comodo.id = id;
        comodo.nome = nome;
        return comodo;
    }

    @Test
    public void somaLampadasETomadasLigadasIgnorandoAparelhosForaDoAr() {

        Comodo sala = comodo("sala", "Sala");
        sala.lampadas.add(item("L1", "a", "switch_1"));
        sala.lampadas.add(item("L2", "a", "switch_2"));
        sala.lampadas.add(item("L3", "b", "switch_1"));
        sala.tomadas.add(item("T1", "c", "switch_1"));

        Map<String, TuyaDevice> estados = new HashMap<>();
        estados.put("a", aparelho(true, new TuyaStatus("switch_1", true), new TuyaStatus("switch_2", false)));
        // Fora do ar: o último estado (ligado) não pode contar como acesa.
        estados.put("b", aparelho(false, new TuyaStatus("switch_1", true)));
        estados.put("c", aparelho(true, new TuyaStatus("switch_1", true)));

        DashboardDados dados = DashboardDados.calcula(Arrays.asList(sala), estados::get);

        assertEquals(3, dados.lampadas);
        assertEquals(1, dados.lampadasAcesas);
        assertEquals(1, dados.tomadas);
        assertEquals(1, dados.tomadasLigadas);
        assertEquals(3, dados.dispositivos);
        assertEquals(2, dados.dispositivosOnline);
        assertEquals(Arrays.asList("L3 (Sala)"), dados.foraDoAr);
        assertEquals(1, dados.comodos.get(0).foraDoAr);
    }

    @Test
    public void aparelhoComVariasTomadasContaUmaVez() {

        Comodo cozinha = comodo("cozinha", "Cozinha");
        cozinha.tomadas.add(item("T1", "regua", "switch_1"));
        cozinha.tomadas.add(item("T2", "regua", "switch_2"));

        Map<String, TuyaDevice> estados = new HashMap<>();
        estados.put("regua", aparelho(false));

        DashboardDados dados = DashboardDados.calcula(Arrays.asList(cozinha), estados::get);

        assertEquals(1, dados.dispositivos);
        assertEquals(0, dados.dispositivosOnline);
        assertEquals(1, dados.foraDoAr.size());
    }

    @Test
    public void semEstadoNaoContaComoForaDoAr() {

        Comodo sala = comodo("sala", "Sala");
        sala.lampadas.add(item("L1", "a", "switch_1"));
        sala.lampadas.add(new ItemComodo("Sem associacao"));

        DashboardDados dados = DashboardDados.calcula(Arrays.asList(sala), id -> null);

        assertEquals(2, dados.lampadas);
        assertEquals(0, dados.lampadasAcesas);
        assertEquals(1, dados.dispositivos);
        assertEquals(0, dados.dispositivosOnline);
        assertTrue(dados.foraDoAr.isEmpty());
    }

    @Test
    public void somaConsumoEFazMediaDeTemperaturaEUmidade() {

        Comodo quarto = comodo("quarto", "Quarto");
        quarto.medidores.add(item("M1", "m1", "switch_1"));
        quarto.medidores.add(item("M2", "m2", "switch_1"));
        quarto.termometros.add(item("Term1", "t1", "va_temperature"));
        quarto.termometros.add(item("Term2", "t2", "va_temperature"));

        Comodo sala = comodo("sala", "Sala");
        sala.termometros.add(item("Term3", "t3", "va_temperature"));

        Map<String, TuyaDevice> estados = new HashMap<>();
        // Potência com uma casa decimal por padrão: 1250 = 125,0 W.
        estados.put("m1", aparelho(true, new TuyaStatus("cur_power", 1250)));
        estados.put("m2", aparelho(true, new TuyaStatus("cur_power", 500)));
        estados.put("t1", aparelho(true, new TuyaStatus("va_temperature", 220), new TuyaStatus("va_humidity", 60)));
        estados.put("t2", aparelho(true, new TuyaStatus("va_temperature", 240), new TuyaStatus("va_humidity", 40)));

        DashboardDados dados = DashboardDados.calcula(Arrays.asList(quarto, sala), estados::get);

        assertEquals(175.0, dados.potencia, 0.0001);
        assertEquals(175.0, dados.comodos.get(0).potencia, 0.0001);
        assertEquals(23.0, dados.comodos.get(0).temperatura, 0.0001);
        assertEquals(50.0, dados.comodos.get(0).umidade, 0.0001);
        assertEquals(23.0, dados.temperaturaMedia, 0.0001);
        assertEquals(3, dados.termometros.size());
        // Sem estado do termômetro da sala: o cômodo fica sem média em vez de zero.
        assertNull(dados.comodos.get(1).temperatura);
        assertNull(dados.comodos.get(1).potencia);
    }

    @Test
    public void semMedidoresRespondendoOConsumoFicaNulo() {

        Comodo sala = comodo("sala", "Sala");
        sala.medidores.add(item("M1", "m1", "switch_1"));

        DashboardDados dados = DashboardDados.calcula(Arrays.asList(sala), id -> null);

        assertNull(dados.potencia);
        assertNull(dados.temperaturaMedia);
        assertNull(dados.umidadeMedia);
    }

    @Test
    public void alarmeDisparadoVenceOsDemaisEForaDoArNaoEntra() {

        Comodo hall = comodo("hall", "Hall");
        hall.alarmes.add(item("Central", "c1", "master_mode"));
        hall.alarmes.add(item("Garagem", "c2", "master_mode"));
        hall.alarmes.add(item("Quintal", "c3", "master_mode"));

        Map<String, TuyaDevice> estados = new HashMap<>();
        estados.put("c1", aparelho(true, new TuyaStatus("master_mode", "arm")));
        estados.put("c2", aparelho(true, new TuyaStatus("master_mode", "arm"), new TuyaStatus("master_state", "alarm")));
        estados.put("c3", aparelho(false, new TuyaStatus("master_mode", "sos")));

        DashboardDados dados = DashboardDados.calcula(Arrays.asList(hall), estados::get);

        assertEquals(EstadoAlarme.DISPARADO, dados.alarmeGeral());

        estados.put("c2", aparelho(true, new TuyaStatus("master_mode", "home")));
        dados = DashboardDados.calcula(Arrays.asList(hall), estados::get);

        // c3 está fora do ar (sos não vale); entre ativado e em casa, vale o ativado.
        assertEquals(EstadoAlarme.ATIVADO, dados.alarmeGeral());
        assertTrue(dados.alarmes.get(2).foraDoAr);
    }

    @Test
    public void semAlarmeRespondendoASegurancaGeralEhNula() {

        Comodo hall = comodo("hall", "Hall");
        hall.alarmes.add(item("Central", "c1", "master_mode"));

        assertNull(DashboardDados.calcula(Arrays.asList(hall), id -> null).alarmeGeral());
        assertNull(DashboardDados.calcula(Arrays.asList(comodo("vazio", "Vazio")), id -> null).alarmeGeral());
    }

    @Test
    public void contaCortinasAbertasEChuva() {

        Comodo sala = comodo("sala", "Sala");
        ItemComodo aberta = item("C1", "c1", "percent_control");
        ItemComodo fechada = item("C2", "c2", "percent_control");
        sala.cortinas.add(aberta);
        sala.cortinas.add(fechada);
        sala.sensoresChuva.add(item("Chuva", "r1", "rain_state"));

        Map<String, TuyaDevice> estados = new HashMap<>();
        estados.put("c1", aparelho(true, new TuyaStatus("percent_control", 100)));
        estados.put("c2", aparelho(true, new TuyaStatus("percent_control", 0)));
        estados.put("r1", aparelho(true, new TuyaStatus("rain_state", "rain")));

        DashboardDados dados = DashboardDados.calcula(Arrays.asList(sala), estados::get);

        assertEquals(2, dados.cortinas);
        assertEquals(1, dados.cortinasAbertas);
        assertEquals(Boolean.TRUE, dados.sensoresChuva.get(0).chovendo);
    }

    @Test
    public void resumoDoComodoTrazAlarmeChuvaECortinas() {

        Comodo seguranca = comodo("seguranca", "Segurança");
        seguranca.alarmes.add(item("Central", "c1", "master_mode"));
        seguranca.alarmes.add(item("Garagem", "c2", "master_mode"));
        seguranca.sensoresChuva.add(item("Chuva", "r1", "rain_state"));
        seguranca.cortinas.add(item("Cortina", "k1", "percent_control"));

        Map<String, TuyaDevice> estados = new HashMap<>();
        estados.put("c1", aparelho(true, new TuyaStatus("master_mode", "home")));
        estados.put("c2", aparelho(true, new TuyaStatus("master_mode", "arm")));
        estados.put("r1", aparelho(true, new TuyaStatus("rain_state", "none")));
        estados.put("k1", aparelho(true, new TuyaStatus("percent_control", 100)));

        DashboardDados.ResumoComodo resumo = DashboardDados.calcula(Arrays.asList(seguranca), estados::get).comodos.get(0);

        assertEquals(2, resumo.alarmes);
        assertEquals(EstadoAlarme.ATIVADO, resumo.alarme);
        assertEquals(1, resumo.sensoresChuva);
        assertEquals(Boolean.FALSE, resumo.chovendo);
        assertEquals(1, resumo.cortinas);
        assertEquals(1, resumo.cortinasAbertas);
    }

    @Test
    public void idsTuyaSaoDistintosEIgnoramItensSemAssociacao() {

        Comodo sala = comodo("sala", "Sala");
        sala.lampadas.add(item("L1", "a", "switch_1"));
        sala.tomadas.add(item("T1", "a", "switch_2"));
        sala.tomadas.add(new ItemComodo("Solta"));

        Comodo quarto = comodo("quarto", "Quarto");
        quarto.alarmes.add(item("Central", "b", "master_mode"));

        List<Comodo> comodos = Arrays.asList(sala, quarto);

        assertEquals(2, DashboardDados.idsTuya(comodos).size());
    }
}
