package com.home.apphomemanager_v5.comodo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class ComodoTest {

    @Test
    public void comodoNovoComecaSemNenhumGrupo() {

        Comodo comodo = new Comodo();

        assertTrue(comodo.gruposAtivos().isEmpty());
        assertEquals(0, comodo.totalItens());
    }

    @Test
    public void grupoEscolhidoContaMesmoSemItens() {

        Comodo comodo = new Comodo();
        comodo.grupos.add(GrupoDispositivo.TERMOMETROS.chave);

        assertTrue(comodo.temGrupo(GrupoDispositivo.TERMOMETROS));
        assertFalse(comodo.temGrupo(GrupoDispositivo.LAMPADAS));
    }

    @Test
    public void comodoAntigoSemListaDeGruposUsaOsGruposComItens() {

        Comodo comodo = new Comodo();
        comodo.lampadas.add(new ItemComodo("Teto"));
        comodo.tomadas.add(new ItemComodo("Parede"));

        assertEquals(Arrays.asList(GrupoDispositivo.LAMPADAS, GrupoDispositivo.TOMADAS), comodo.gruposAtivos());
    }

    @Test
    public void gruposAtivosSeguemAOrdemDoEnumIndependenteDaEscolha() {

        Comodo comodo = new Comodo();
        comodo.grupos.addAll(Arrays.asList(GrupoDispositivo.TERMOMETROS.chave, GrupoDispositivo.MEDIDORES.chave));

        assertEquals(Arrays.asList(GrupoDispositivo.MEDIDORES, GrupoDispositivo.TERMOMETROS), comodo.gruposAtivos());
    }

    @Test
    public void somaItensEAssociadosDeTodosOsGrupos() {

        Comodo comodo = new Comodo();

        ItemComodo associado = new ItemComodo("Medidor");
        associado.associa("dev", "switch_1", "Plug");

        comodo.medidores.add(associado);
        comodo.termometros.add(new ItemComodo("Sensor"));
        comodo.lampadas.addAll(Collections.singletonList(new ItemComodo("Luz")));

        assertEquals(3, comodo.totalItens());
        assertEquals(1, comodo.totalAssociados());
    }

    @Test
    public void desassociarLimpaAsEscalas() {

        ItemComodo item = new ItemComodo("Medidor");
        item.associa("dev", "switch_1", "Plug");
        item.escalas.put("cur_power", 1);

        item.desassocia();

        assertTrue(item.escalas.isEmpty());
        assertFalse(item.associado());
    }

    @Test
    public void chavesDosGruposSaoUnicasEResolvemDeVolta() {

        for (GrupoDispositivo grupo : GrupoDispositivo.values()) {
            assertEquals(grupo, GrupoDispositivo.daChave(grupo.chave));
        }
        assertEquals(null, GrupoDispositivo.daChave("inexistente"));
    }
}
