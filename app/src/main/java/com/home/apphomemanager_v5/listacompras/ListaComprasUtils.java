package com.home.apphomemanager_v5.listacompras;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Regras da lista de compras que não dependem de Android nem do Firebase. */
public class ListaComprasUtils {

    private ListaComprasUtils() {}

    private static final Comparator<Produto> POR_CATEGORIA_E_NOME = (a, b) -> {
        int porCategoria = Integer.compare(a.getCategoria(), b.getCategoria());
        return porCategoria != 0 ? porCategoria : a.getNome().compareToIgnoreCase(b.getNome());
    };

    /**
     * Junta a despensa com a lista de compras: o produto que está na lista prevalece
     * (quantidade, unidade e status). Produto só da lista continua aparecendo.
     */
    public static List<Produto> mescla(List<Produto> despensa, List<Produto> minhaLista) {

        Map<String, Produto> porChave = new LinkedHashMap<>();

        for (Produto produto : despensa) {
            porChave.put(produto.chave(), produto);
        }

        for (Produto produto : minhaLista) {
            porChave.put(produto.chave(), produto);
        }

        List<Produto> resultado = new ArrayList<>(porChave.values());
        resultado.sort(POR_CATEGORIA_E_NOME);

        return resultado;
    }

    public static List<Produto> daLista(List<Produto> produtos) {

        List<Produto> naLista = new ArrayList<>();

        for (Produto produto : produtos) {
            if (produto.estaNaLista()) {
                naLista.add(produto);
            }
        }
        return naLista;
    }

    public static List<Produto> daCategoria(List<Produto> produtos, int categoria) {

        List<Produto> daCategoria = new ArrayList<>();

        for (Produto produto : produtos) {
            if (produto.getCategoria() == categoria) {
                daCategoria.add(produto);
            }
        }
        return daCategoria;
    }

    public static boolean temPendente(List<Produto> produtos) {
        return contaPorStatus(produtos, Produto.STATUS_PENDENTE) > 0;
    }

    public static int contaPorStatus(List<Produto> produtos, int status) {

        int total = 0;

        for (Produto produto : produtos) {
            if (produto.getStatus() == status) {
                total++;
            }
        }
        return total;
    }

    /** Aceita vírgula ou ponto. Retorna null se não for um número maior que zero. */
    public static Float parseQuantidade(String texto) {

        if (texto == null) {
            return null;
        }

        try {
            float quantidade = Float.parseFloat(texto.trim().replace(',', '.'));
            return quantidade > 0 && !Float.isInfinite(quantidade) ? quantidade : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** 1.0 vira "1" e 0.5 vira "0,5" (conforme o idioma). */
    public static String formataQuantidade(float quantidade, Locale locale) {

        DecimalFormat formato = new DecimalFormat("0.###", DecimalFormatSymbols.getInstance(locale));

        return formato.format(quantidade);
    }

    public static String formataProduto(Produto produto, Locale locale) {

        return produto.getNome() + " - " + formataQuantidade(produto.getQuantidade(), locale) + " " + produto.getNomeUnidade();
    }

    /** Texto da lista para enviar por WhatsApp (*negrito* e ~riscado~). */
    public static String textoCompartilhamento(List<Produto> minhaLista, String titulo, String tituloCesta, String listaVazia, Locale locale) {

        StringBuilder texto = new StringBuilder("*").append(titulo).append("*\n\n");

        if (minhaLista.isEmpty()) {
            return texto.append(listaVazia).toString();
        }

        List<Produto> ordenada = new ArrayList<>(minhaLista);
        ordenada.sort(POR_CATEGORIA_E_NOME);

        int categoriaAtual = -1;
        StringBuilder comprados = new StringBuilder();

        for (Produto produto : ordenada) {

            if (produto.getStatus() == Produto.STATUS_COMPRADO) {
                comprados.append("~").append(formataProduto(produto, locale)).append("~\n");
                continue;
            }

            if (produto.getCategoria() != categoriaAtual) {
                categoriaAtual = produto.getCategoria();
                texto.append("*-").append(Categorias.nome(categoriaAtual)).append("*\n");
            }

            texto.append(formataProduto(produto, locale)).append("\n");
        }

        if (comprados.length() > 0) {
            texto.append("\n*").append(tituloCesta).append("*\n\n").append(comprados);
        }

        return texto.toString();
    }

    /** Linha da tela "Minha Lista": cabeçalho de categoria, produto ou aviso. */
    public static class ItemMinhaLista {

        public static final int TIPO_CATEGORIA = 0;
        public static final int TIPO_PRODUTO = 1;
        public static final int TIPO_CESTA = 2;
        public static final int TIPO_PRODUTO_COMPRADO = 3;
        public static final int TIPO_VAZIO = 4;

        public final int tipo;
        public final int categoria;
        public final Produto produto;

        private ItemMinhaLista(int tipo, int categoria, Produto produto) {
            this.tipo = tipo;
            this.categoria = categoria;
            this.produto = produto;
        }

        /** Pendentes agrupados por categoria e, por último, os que já estão na cesta. */
        public static List<ItemMinhaLista> monta(List<Produto> minhaLista) {

            List<ItemMinhaLista> itens = new ArrayList<>();

            if (minhaLista.isEmpty()) {
                itens.add(new ItemMinhaLista(TIPO_VAZIO, -1, null));
                return itens;
            }

            List<Produto> ordenada = new ArrayList<>(minhaLista);
            ordenada.sort(POR_CATEGORIA_E_NOME);

            int categoriaAtual = -1;
            List<Produto> comprados = new ArrayList<>();

            for (Produto produto : ordenada) {

                if (produto.getStatus() == Produto.STATUS_COMPRADO) {
                    comprados.add(produto);
                    continue;
                }

                if (produto.getCategoria() != categoriaAtual) {
                    categoriaAtual = produto.getCategoria();
                    itens.add(new ItemMinhaLista(TIPO_CATEGORIA, categoriaAtual, null));
                }

                itens.add(new ItemMinhaLista(TIPO_PRODUTO, produto.getCategoria(), produto));
            }

            if (!comprados.isEmpty()) {
                itens.add(new ItemMinhaLista(TIPO_CESTA, -1, null));

                for (Produto produto : comprados) {
                    itens.add(new ItemMinhaLista(TIPO_PRODUTO_COMPRADO, produto.getCategoria(), produto));
                }
            }

            return itens;
        }
    }
}
