package com.home.apphomemanager_v5.comodo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.home.apphomemanager_v5.comodo.LeituraDispositivo.EstadoAlarme;
import com.home.apphomemanager_v5.tuya.TuyaDispositivo;
import com.home.apphomemanager_v5.tuya.TuyaPonto;
import com.home.apphomemanager_v5.tuya.model.TuyaDevice;
import com.home.apphomemanager_v5.tuya.model.TuyaStatus;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class CortinaChuvaAlarmeTest {

    private static TuyaDevice aparelho(TuyaStatus... status) {

        TuyaDevice dispositivo = new TuyaDevice();
        dispositivo.online = true;
        dispositivo.status = Arrays.asList(status);
        return dispositivo;
    }

    private static TuyaDispositivo dispositivo(String id, String nome, TuyaPonto... pontos) {
        return new TuyaDispositivo(id, nome, true, Arrays.asList(pontos));
    }

    private final List<TuyaDispositivo> conta = Arrays.asList(
            dispositivo("lampada", "Lampada", new TuyaPonto("switch_led", true, null)),
            dispositivo("chuva", "Sensor do telhado", new TuyaPonto("rain_state", "none", null)),
            dispositivo("alarme", "Central", new TuyaPonto("master_mode", "disarmed", null)),
            dispositivo("motor", "Motor da sala", new TuyaPonto("percent_control", 100, null), new TuyaPonto("percent_state", 40, null)));

    @Test
    public void chuvaAceitaBooleanoTextoENumero() {

        assertEquals(Boolean.TRUE, LeituraDispositivo.valorDeChuva(true));
        assertEquals(Boolean.FALSE, LeituraDispositivo.valorDeChuva(0.0));
        assertEquals(Boolean.TRUE, LeituraDispositivo.valorDeChuva("rain"));
        assertEquals(Boolean.FALSE, LeituraDispositivo.valorDeChuva("none"));
        assertNull(LeituraDispositivo.valorDeChuva(null));
    }

    @Test
    public void chovendoUsaOPontoAssociadoOuQualquerCodigoComRain() {

        ItemComodo item = new ItemComodo("Telhado");
        item.associa("chuva", "rain_state", "Sensor");

        assertEquals(Boolean.TRUE, LeituraDispositivo.chovendo(aparelho(new TuyaStatus("rain_state", "rain")), item));
        assertEquals(Boolean.FALSE, LeituraDispositivo.chovendo(aparelho(new TuyaStatus("rain_state", "none")), item));

        ItemComodo semCodigo = new ItemComodo("Outro");
        assertEquals(Boolean.TRUE, LeituraDispositivo.chovendo(aparelho(new TuyaStatus("raining", true)), semCodigo));
        assertNull(LeituraDispositivo.chovendo(aparelho(new TuyaStatus("battery", 80)), semCodigo));
        assertNull(LeituraDispositivo.chovendo(null, semCodigo));
    }

    @Test
    public void estadoDoAlarmeVemDoModoDaCentral() {

        ItemComodo item = new ItemComodo("Central");
        item.associa("alarme", "master_mode", "Central");

        assertEquals(EstadoAlarme.ATIVADO, LeituraDispositivo.estadoAlarme(aparelho(new TuyaStatus("master_mode", "arm")), item));
        assertEquals(EstadoAlarme.DESATIVADO, LeituraDispositivo.estadoAlarme(aparelho(new TuyaStatus("master_mode", "disarmed")), item));
        assertEquals(EstadoAlarme.EM_CASA, LeituraDispositivo.estadoAlarme(aparelho(new TuyaStatus("master_mode", "home")), item));
        assertEquals(EstadoAlarme.DISPARADO, LeituraDispositivo.estadoAlarme(aparelho(new TuyaStatus("master_mode", "sos")), item));
        assertEquals(EstadoAlarme.DESCONHECIDO, LeituraDispositivo.estadoAlarme(aparelho(new TuyaStatus("master_mode", "xyz")), item));
        assertEquals(EstadoAlarme.DESCONHECIDO, LeituraDispositivo.estadoAlarme(null, item));
    }

    @Test
    public void listaDeDispositivosCompativeisPorGrupo() {

        assertEquals(1, TuyaCompativeis.para(GrupoDispositivo.CHUVA, conta).size());
        assertEquals("chuva", TuyaCompativeis.para(GrupoDispositivo.CHUVA, conta).get(0).deviceId);
        assertEquals("rain_state", TuyaCompativeis.para(GrupoDispositivo.CHUVA, conta).get(0).code);

        assertEquals(1, TuyaCompativeis.para(GrupoDispositivo.ALARMES, conta).size());
        assertEquals("alarme", TuyaCompativeis.para(GrupoDispositivo.ALARMES, conta).get(0).deviceId);

        // Motor de cortina: prefere a posição atual à de comando.
        assertEquals(1, TuyaCompativeis.para(GrupoDispositivo.CORTINAS, conta).size());
        assertEquals("motor", TuyaCompativeis.para(GrupoDispositivo.CORTINAS, conta).get(0).deviceId);
        assertEquals("percent_state", TuyaCompativeis.para(GrupoDispositivo.CORTINAS, conta).get(0).code);
    }

    @Test
    public void lampadasNaoRecebemSensoresNemCentrais() {

        List<TuyaCompativeis.Opcao> lampadas = TuyaCompativeis.para(GrupoDispositivo.LAMPADAS, conta);

        assertEquals(1, lampadas.size());
        assertEquals("lampada", lampadas.get(0).deviceId);
    }

    @Test
    public void posicaoDaCortinaUsaOPontoAssociadoEPodeSerInvertida() {

        ItemComodo item = new ItemComodo("Sala");
        item.associa("motor", "percent_state", "Motor");

        TuyaDevice motor = aparelho(new TuyaStatus("percent_control", 100), new TuyaStatus("percent_state", 40));

        assertEquals(40.0, LeituraDispositivo.posicaoCortina(motor, item), 0.0001);
        assertEquals(40, LeituraDispositivo.percentualAberto(40, false));
        assertEquals(60, LeituraDispositivo.percentualAberto(40, true));
        assertEquals(100, LeituraDispositivo.percentualAberto(130, false));
        assertNull(LeituraDispositivo.posicaoCortina(aparelho(new TuyaStatus("switch", true)), item));
        assertNull(LeituraDispositivo.posicaoCortina(null, item));
    }

    @Test
    public void sensorDeAguaComWatersensorStateServeComoSensorDeChuva() {

        List<TuyaDispositivo> conta2 = Arrays.asList(
                dispositivo("agua", "Sensor", new TuyaPonto("watersensor_state", "normal", null), new TuyaPonto("battery_state", "high", null)));

        assertEquals(1, TuyaCompativeis.para(GrupoDispositivo.CHUVA, conta2).size());
        assertEquals("watersensor_state", TuyaCompativeis.para(GrupoDispositivo.CHUVA, conta2).get(0).code);

        ItemComodo item = new ItemComodo("Telhado");
        item.associa("agua", "watersensor_state", "Sensor");

        assertEquals(Boolean.FALSE, LeituraDispositivo.chovendo(aparelho(new TuyaStatus("watersensor_state", "normal"), new TuyaStatus("battery_state", "high")), item));
        assertEquals(Boolean.TRUE, LeituraDispositivo.chovendo(aparelho(new TuyaStatus("watersensor_state", "alarm")), item));
    }

    @Test
    public void masterStateEmAlarmeMarcaACentralComoDisparada() {

        ItemComodo item = new ItemComodo("Central");
        item.associa("alarme", "master_mode", "Central");

        assertEquals(EstadoAlarme.DISPARADO, LeituraDispositivo.estadoAlarme(
                aparelho(new TuyaStatus("master_mode", "arm"), new TuyaStatus("master_state", "alarm")), item));
        assertEquals(EstadoAlarme.ATIVADO, LeituraDispositivo.estadoAlarme(
                aparelho(new TuyaStatus("master_mode", "arm"), new TuyaStatus("master_state", "normal")), item));
    }

    private static TuyaDevice motor(String controle, double percentState, double percentControl) {
        return aparelho(new TuyaStatus("control", controle), new TuyaStatus("work_state", "opening"),
                new TuyaStatus("percent_state", percentState), new TuyaStatus("percent_control", percentControl));
    }

    @Test
    public void percentStateParadoEmZeroFazValerOPercentControl() {

        ItemComodo item = new ItemComodo("Quarto");
        item.associa("motor", "percent_state", "Motor");

        // Valores reais do log: o motor só atualiza percent_control.
        assertEquals(83.0, LeituraDispositivo.posicaoCortina(motor("open", 0.0, 83.0), item), 0.0001);
        assertEquals(40.0, LeituraDispositivo.posicaoCortina(motor("stop", 40.0, 83.0), item), 0.0001);
        assertEquals(0.0, LeituraDispositivo.posicaoCortina(motor("open", 0.0, 0.0), item), 0.0001);
    }

    @Test
    public void motorQueContaZeroComoAbertaMostraAUltimaPosicaoAoParar() {

        ItemComodo item = new ItemComodo("Quarto");
        item.associa("motor", "percent_state", "Motor");
        item.posicaoInvertida = true;

        // Parado com 83% de fechamento: 17% aberta.
        LeituraDispositivo.EstadoCortina parada = LeituraDispositivo.estadoCortina(motor("stop", 0.0, 83.0), item, false);
        assertEquals(LeituraDispositivo.PosicaoCortina.PARCIAL, parada.posicao);
        assertEquals(Integer.valueOf(17), parada.percentual);

        // Chegou no fim de curso nos dois sentidos.
        assertEquals(LeituraDispositivo.PosicaoCortina.FECHADA, LeituraDispositivo.estadoCortina(motor("close", 0.0, 100.0), item, false).posicao);
        assertEquals(LeituraDispositivo.PosicaoCortina.ABERTA, LeituraDispositivo.estadoCortina(motor("open", 0.0, 0.0), item, false).posicao);
    }

    @Test
    public void depoisDeUmComandoMostraAbrindoOuFechandoAteAPosicaoConfirmar() {

        ItemComodo item = new ItemComodo("Quarto");
        item.associa("motor", "percent_state", "Motor");
        item.posicaoInvertida = true;

        // control=open mas a posição ainda é a antiga (83% fechada): em movimento.
        assertEquals(LeituraDispositivo.PosicaoCortina.ABRINDO, LeituraDispositivo.estadoCortina(motor("open", 0.0, 83.0), item, true).posicao);
        assertEquals(LeituraDispositivo.PosicaoCortina.FECHANDO, LeituraDispositivo.estadoCortina(motor("close", 0.0, 0.0), item, true).posicao);

        // Passado o tempo de movimento, vale o comando como estado final.
        assertEquals(LeituraDispositivo.PosicaoCortina.ABERTA, LeituraDispositivo.estadoCortina(motor("open", 0.0, 83.0), item, false).posicao);
        assertEquals(LeituraDispositivo.PosicaoCortina.FECHADA, LeituraDispositivo.estadoCortina(motor("close", 0.0, 0.0), item, false).posicao);
    }

    @Test
    public void semControleNemPosicaoNaoHaEstado() {

        ItemComodo item = new ItemComodo("Quarto");

        assertNull(LeituraDispositivo.estadoCortina(aparelho(new TuyaStatus("battery", 80)), item, false));
        assertNull(LeituraDispositivo.estadoCortina(null, item, false));
    }

    @Test
    public void cenasDoItemSeguemOComando() {

        ItemComodo cortina = new ItemComodo("Sala");
        cortina.defineCena(0, "Subir sala");
        cortina.defineCena(1, "Descer sala");
        cortina.defineCena(2, "Parar sala");

        assertEquals("Subir sala", cortina.cena(0));
        assertEquals("Descer sala", cortina.cena(1));
        assertEquals("Parar sala", cortina.cena(2));
        assertTrue(ItemComodo.preenchida(cortina.cena(2)));
        assertFalse(ItemComodo.preenchida(""));
    }

    @Test
    public void todosOsItensContamComoAssociaveis() {

        Comodo comodo = new Comodo();
        comodo.cortinas.add(new ItemComodo("Cortina"));
        comodo.alarmes.add(new ItemComodo("Alarme"));
        comodo.sensoresChuva.add(new ItemComodo("Chuva"));

        assertEquals(3, comodo.totalItens());
        assertEquals(3, comodo.totalAssociaveis());
        assertEquals(Arrays.asList(GrupoDispositivo.CORTINAS, GrupoDispositivo.CHUVA, GrupoDispositivo.ALARMES), comodo.gruposAtivos());
    }
}
