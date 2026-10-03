package com.home.apphomemanager_v5.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.home.apphomemanager_v5.model.reservatorio.CaixaDagua;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AtributoUtilsTest {

    @Test
    public void detectaApenasCamposAlterados() {
        CaixaDagua local = new CaixaDagua();
        local.inicializa();

        CaixaDagua firebase = new CaixaDagua();
        firebase.inicializa();
        firebase.setVlep(true);
        firebase.setNa(55L);

        List<String> alterados = new ArrayList<>();
        AtributoUtils.atributosAlterados(firebase, local, alterados);

        assertTrue(alterados.contains("vlep"));
        assertTrue(alterados.contains("na"));
        assertTrue(!alterados.contains("vles"));
        assertTrue(!alterados.contains("onOff"));
    }

    @Test
    public void transfereValoresNaoNulos() {
        CaixaDagua local = new CaixaDagua();
        local.inicializa();

        CaixaDagua firebase = new CaixaDagua();
        firebase.setNa(70L);

        AtributoUtils.transferirValoresEntreObjetos(firebase, local, Arrays.asList("na", "onOff"));

        assertEquals(Long.valueOf(70L), local.getNa());
        // onOff nulo no Firebase não apaga o valor local.
        assertEquals(Boolean.FALSE, local.getOnOff());
    }

    @Test
    public void camposAusentesSaoOsNulosNaOrigem() {
        CaixaDagua firebase = new CaixaDagua();
        firebase.setOnOff(true);
        firebase.setNa(10L);

        List<String> ausentes = AtributoUtils.camposAusentes(firebase, Arrays.asList("onOff", "na", "vlep", "status"));

        assertEquals(Arrays.asList("vlep", "status"), ausentes);
    }
}
