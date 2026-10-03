package com.home.apphomemanager_v5.model.reservatorio;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReservatorioTest {

    private static Cisterna cisterna(Long na, Long nic, Long nsc) {
        Cisterna c = new Cisterna();
        c.setNa(na);
        c.setNic(nic);
        c.setNsc(nsc);
        return c;
    }

    @Test
    public void imagemZeroQuandoCamposNulos() {
        assertEquals(0, cisterna(null, 100L, 20L).getImageLevel(20));
        assertEquals(0, cisterna(50L, null, 20L).getImageLevel(20));
        assertEquals(0, cisterna(50L, 100L, null).getImageLevel(20));
    }

    @Test
    public void imagemZeroQuandoLeituraInvalida() {
        assertEquals(0, cisterna(-1L, 100L, 20L).getImageLevel(20));
    }

    @Test
    public void faixaMenorQueQuantidadeDeImagensNaoDividePorZero() {
        // nic - nsc = 10 < 20 imagens: antes a divisão inteira zerava a faixa.
        assertEquals(19, cisterna(20L, 30L, 20L).getImageLevel(20));
        assertEquals(0, cisterna(30L, 30L, 20L).getImageLevel(20));
    }

    @Test
    public void imagemProporcionalAoNivel() {
        // Sensor mede a distância até a água: na == nic é vazio, na == nsc é cheio.
        assertEquals(0, cisterna(100L, 100L, 0L).getImageLevel(20));
        assertEquals(19, cisterna(0L, 100L, 0L).getImageLevel(20));
        assertEquals(9, cisterna(50L, 100L, 0L).getImageLevel(20));
    }

    @Test
    public void niveisRelativos() {
        Cisterna c = cisterna(40L, 100L, 20L);
        assertEquals(80, c.getNivelSuperiorRelativo());
        assertEquals(60, c.getNivelAtualRelativo());
    }
}
