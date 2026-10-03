package com.home.apphomemanager_v5.listacompras;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.home.apphomemanager_v5.listacompras.ListaComprasUtils.ItemMinhaLista;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class ListaComprasUtilsTest {

    private static final Locale PT_BR = new Locale("pt", "BR");

    private static Produto disponivel(int categoria, String nome) {
        return new Produto(categoria, nome, 1f, Produto.UNIDADE_UN, Produto.STATUS_DISPONIVEL);
    }

    private static Produto naLista(int categoria, String nome, float quantidade, int unidade, int status) {
        return new Produto(categoria, nome, quantidade, unidade, status);
    }

    @Test
    public void daListaAceitaFormatoAntigoENovo() {

        Produto antigo = Produto.daLista(2, "Cerveja", "1.0#0#2");
        Produto novo = Produto.daLista(2, "Cerveja", "1#0#2");

        assertNotNull(antigo);
        assertEquals(1f, antigo.getQuantidade(), 0f);
        assertEquals(antigo.valorLista(), novo.valorLista());
    }

    @Test
    public void daListaIgnoraValorMalformado() {

        assertNull(Produto.daLista(0, "A", null));
        assertNull(Produto.daLista(0, "A", ""));
        assertNull(Produto.daLista(0, "A", "abc#0#2"));
        assertNull(Produto.daLista(0, "A", "1#0"));
        assertNull(Produto.daLista(0, "A", "1#9#2"));
        assertNull(Produto.daLista(0, "A", "1#0#1"));
        assertNull(Produto.daLista(0, "A", "0#0#2"));
    }

    @Test
    public void valorListaUsaPontoDecimalIndependenteDoIdioma() {

        Locale anterior = Locale.getDefault();
        Locale.setDefault(PT_BR);

        try {
            assertEquals("0.5#2#2", naLista(0, "Queijo", 0.5f, Produto.UNIDADE_KG, Produto.STATUS_PENDENTE).valorLista());
        } finally {
            Locale.setDefault(anterior);
        }
    }

    @Test
    public void daDespensaValidaUnidade() {

        assertNotNull(Produto.daDespensa(1, "Bolacha", 0L));
        assertNotNull(Produto.daDespensa(1, "Bolacha", "2"));
        assertNull(Produto.daDespensa(1, "Bolacha", 7L));
        assertNull(Produto.daDespensa(1, "Bolacha", "x"));
        assertNull(Produto.daDespensa(1, "Bolacha", null));
    }

    @Test
    public void nomeInvalidoParaChaveDoFirebase() {

        assertTrue(Produto.nomeValido("Arroz integral"));
        assertFalse(Produto.nomeValido(""));
        assertFalse(Produto.nomeValido("   "));
        assertFalse(Produto.nomeValido("Leite 1.5"));
        assertFalse(Produto.nomeValido("Pao/queijo"));
        assertFalse(Produto.nomeValido("#promo"));
        assertFalse(Produto.nomeValido("a[1]"));
        assertFalse(Produto.nomeValido("R$"));
    }

    @Test
    public void normalizaNome() {

        assertEquals("Arroz integral", Produto.normalizaNome("  aRROZ   INTEGRAL "));
        assertEquals("", Produto.normalizaNome("   "));
        assertEquals("", Produto.normalizaNome(null));
    }

    @Test
    public void mesclaPrevalecemDadosDaListaEOrdenaPorCategoriaENome() {

        List<Produto> despensa = Arrays.asList(disponivel(2, "Cerveja"), disponivel(0, "Sal"), disponivel(0, "Arroz"));
        List<Produto> lista = Collections.singletonList(naLista(0, "Sal", 3f, Produto.UNIDADE_KG, Produto.STATUS_PENDENTE));

        List<Produto> resultado = ListaComprasUtils.mescla(despensa, lista);

        assertEquals(3, resultado.size());
        assertEquals("Arroz", resultado.get(0).getNome());
        assertEquals("Sal", resultado.get(1).getNome());
        assertEquals(Produto.STATUS_PENDENTE, resultado.get(1).getStatus());
        assertEquals(3f, resultado.get(1).getQuantidade(), 0f);
        assertEquals("Cerveja", resultado.get(2).getNome());
    }

    @Test
    public void mesclaMantemProdutoSoDaLista() {

        List<Produto> resultado = ListaComprasUtils.mescla(
                Collections.emptyList(),
                Collections.singletonList(naLista(5, "Vela", 1f, Produto.UNIDADE_UN, Produto.STATUS_PENDENTE)));

        assertEquals(1, resultado.size());
    }

    @Test
    public void temPendenteIgnoraComprados() {

        assertFalse(ListaComprasUtils.temPendente(Collections.emptyList()));
        assertFalse(ListaComprasUtils.temPendente(Collections.singletonList(naLista(0, "A", 1f, 0, Produto.STATUS_COMPRADO))));
        assertTrue(ListaComprasUtils.temPendente(Collections.singletonList(naLista(0, "A", 1f, 0, Produto.STATUS_PENDENTE))));
    }

    @Test
    public void parseQuantidadeAceitaVirgulaEPonto() {

        assertEquals(1.5f, ListaComprasUtils.parseQuantidade("1,5"), 0f);
        assertEquals(2f, ListaComprasUtils.parseQuantidade(" 2 "), 0f);
        assertNull(ListaComprasUtils.parseQuantidade(""));
        assertNull(ListaComprasUtils.parseQuantidade("abc"));
        assertNull(ListaComprasUtils.parseQuantidade("0"));
        assertNull(ListaComprasUtils.parseQuantidade("-3"));
        assertNull(ListaComprasUtils.parseQuantidade(null));
    }

    @Test
    public void formataQuantidadeSemZerosSobrando() {

        assertEquals("1", ListaComprasUtils.formataQuantidade(1f, PT_BR));
        assertEquals("0,5", ListaComprasUtils.formataQuantidade(0.5f, PT_BR));
        assertEquals("2,25", ListaComprasUtils.formataQuantidade(2.25f, PT_BR));
    }

    @Test
    public void itensDaMinhaListaAgrupamPorCategoriaECestaPorUltimo() {

        List<Produto> lista = Arrays.asList(
                naLista(2, "Cerveja", 6f, Produto.UNIDADE_UN, Produto.STATUS_PENDENTE),
                naLista(0, "Sal", 1f, Produto.UNIDADE_KG, Produto.STATUS_COMPRADO),
                naLista(0, "Arroz", 2f, Produto.UNIDADE_KG, Produto.STATUS_PENDENTE));

        List<ItemMinhaLista> itens = ItemMinhaLista.monta(lista);

        int[] esperado = {
                ItemMinhaLista.TIPO_CATEGORIA, ItemMinhaLista.TIPO_PRODUTO,
                ItemMinhaLista.TIPO_CATEGORIA, ItemMinhaLista.TIPO_PRODUTO,
                ItemMinhaLista.TIPO_CESTA, ItemMinhaLista.TIPO_PRODUTO_COMPRADO
        };

        assertEquals(esperado.length, itens.size());

        for (int i = 0; i < esperado.length; i++) {
            assertEquals("posicao " + i, esperado[i], itens.get(i).tipo);
        }

        assertEquals("Arroz", itens.get(1).produto.getNome());
        assertEquals("Sal", itens.get(5).produto.getNome());
    }

    @Test
    public void listaVaziaGeraUmItemDeAviso() {

        List<ItemMinhaLista> itens = ItemMinhaLista.monta(Collections.emptyList());

        assertEquals(1, itens.size());
        assertEquals(ItemMinhaLista.TIPO_VAZIO, itens.get(0).tipo);
    }

    @Test
    public void textoCompartilhamento() {

        List<Produto> lista = Arrays.asList(
                naLista(0, "Arroz", 2f, Produto.UNIDADE_KG, Produto.STATUS_PENDENTE),
                naLista(0, "Sal", 0.5f, Produto.UNIDADE_KG, Produto.STATUS_COMPRADO));

        String texto = ListaComprasUtils.textoCompartilhamento(lista, "Lista de Compras", "Ja na cesta", "(Lista vazia)", PT_BR);

        assertEquals("*Lista de Compras*\n\n*-Mercado*\nArroz - 2 Kg\n\n*Ja na cesta*\n\n~Sal - 0,5 Kg~\n", texto);
    }

    @Test
    public void textoCompartilhamentoDeListaVazia() {

        String texto = ListaComprasUtils.textoCompartilhamento(Collections.emptyList(), "Lista de Compras", "Ja na cesta", "(Lista vazia)", PT_BR);

        assertEquals("*Lista de Compras*\n\n(Lista vazia)", texto);
    }
}
