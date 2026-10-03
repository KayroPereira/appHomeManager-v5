package com.home.apphomemanager_v5.tuya;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.home.apphomemanager_v5.tuya.model.TuyaDevice;
import com.home.apphomemanager_v5.tuya.model.TuyaFuncao;
import com.home.apphomemanager_v5.tuya.model.TuyaScene;
import com.home.apphomemanager_v5.tuya.model.TuyaSpec;
import com.home.apphomemanager_v5.tuya.model.TuyaStatus;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class TuyaSignerTest {

    private static final String STRING_TO_SIGN = "GET\n" + TuyaSigner.SHA256_VAZIO + "\n\n/v1.0/token?grant_type=1";

    @Test
    public void sha256DeCorpoVazio() {
        assertEquals(TuyaSigner.SHA256_VAZIO, TuyaSigner.sha256Hex(new byte[0]));
    }

    @Test
    public void stringToSignSegueFormatoDaTuya() {
        assertEquals(STRING_TO_SIGN, TuyaSigner.stringToSign("get", TuyaSigner.SHA256_VAZIO, "/v1.0/token?grant_type=1"));
    }

    // Valores esperados calculados por uma implementação independente (.NET HMACSHA256).
    @Test
    public void assinaturaDaRequisicaoDeToken() {
        assertEquals("3474BD2F893A0AA7AAFAD4E3FDCD444DEA02892D483805EB35FF431561B7A926",
                TuyaSigner.assina("abc", "secret", null, "1600000000000", STRING_TO_SIGN));
    }

    @Test
    public void assinaturaDeNegocioIncluiOAccessToken() {
        assertEquals("6FF79F690D870A565E9BAFEF042648E40B6782280AC270F2B4EBD1B3C33E4E33",
                TuyaSigner.assina("abc", "secret", "tok", "1600000000000", STRING_TO_SIGN));
    }

    @Test
    public void converteJuntaEstadoEEsquemaDoPonto() {

        TuyaDevice dispositivo = new TuyaDevice();
        dispositivo.id = "d1";
        dispositivo.name = "Lampada";
        dispositivo.online = true;
        dispositivo.status = Arrays.asList(new TuyaStatus("switch_led", true), new TuyaStatus("bright_value_v2", 505));

        TuyaFuncao brilho = new TuyaFuncao();
        brilho.code = "bright_value_v2";
        brilho.values = "{\"min\":10,\"max\":1000}";

        TuyaSpec spec = new TuyaSpec();
        spec.functions = Collections.singletonList(brilho);

        TuyaDispositivo convertido = TuyaRepository.converte(dispositivo, spec);

        assertEquals("Lampada", convertido.nome);
        assertTrue(convertido.online);
        assertEquals(2, convertido.pontos.size());
        assertNull(convertido.pontos.get(0).propriedade);
        assertEquals("{\"min\":10,\"max\":1000}", convertido.pontos.get(1).propriedade);
    }

    @Test
    public void converteSemEspecificacaoNemStatus() {

        TuyaDevice dispositivo = new TuyaDevice();
        dispositivo.id = "d2";

        assertTrue(TuyaRepository.converte(dispositivo, null).pontos.isEmpty());
    }

    @Test
    public void soCenasDeAcionamentoManualSaoListadas() {

        TuyaScene manual = cena("1", "Abrir porta", null);
        TuyaScene manualVazia = cena("2", "Luz da sala", Collections.emptyList());
        TuyaScene automacao = cena("3", "Ao anoitecer", Collections.singletonList("temp"));
        TuyaScene semNome = cena("4", null, null);

        List<TuyaScene> resultado = TuyaRepository.soAcionamentoManual(Arrays.asList(manual, manualVazia, automacao, semNome));

        assertEquals(2, resultado.size());
        assertEquals("Abrir porta", resultado.get(0).name);
        assertEquals("Luz da sala", resultado.get(1).name);
        assertTrue(TuyaRepository.soAcionamentoManual(null).isEmpty());
    }

    @Test
    public void cenaViraLinhaComOIdDaCena() {

        TuyaControle c = TuyaControle.cena("abc", "Abrir porta");

        assertEquals(TuyaControle.Tipo.CENA, c.tipo);
        assertEquals("Abrir porta", c.titulo());
        assertEquals("abc", c.code);
    }

    @Test
    public void achaACenaPeloNomeIgnorandoCaixaEEspacos() {

        List<TuyaScene> cenas = Arrays.asList(cena("1", "Luz da sala", null), cena("2", "Abrir Entrada", null));

        assertEquals("2", TuyaRepository.achaCena(cenas, "  abrir entrada ").sceneId);
        assertEquals("2", TuyaRepository.achaCena(cenas, TuyaRepository.CENA_PORTA).sceneId);
        assertNull(TuyaRepository.achaCena(cenas, "Abrir Garagem"));
        assertNull(TuyaRepository.achaCena(null, "Abrir Entrada"));
    }
    private static TuyaScene cena(String id, String nome, List<Object> condicoes) {
        TuyaScene c = new TuyaScene();
        c.sceneId = id;
        c.name = nome;
        c.conditions = condicoes;
        return c;
    }}
