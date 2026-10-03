package com.home.apphomemanager_v5.listacompras;

import java.util.Locale;

/**
 * Produto da despensa. Quando está na lista de compras carrega também quantidade, unidade e status.
 * <p>
 * Formato no Firebase:
 * <ul>
 *     <li>{@code listaCompras/despensa/{categoria}/{nome} = unidade}</li>
 *     <li>{@code listaCompras/minhaLst/{categoria}/{nome} = "quantidade#unidade#status"}</li>
 * </ul>
 */
public class Produto {

    /** Na despensa e fora da lista de compras. Nunca é gravado em minhaLst. */
    public static final int STATUS_DISPONIVEL = 1;
    /** Na lista de compras, ainda por comprar. */
    public static final int STATUS_PENDENTE = 2;
    /** Na lista de compras e já colocado na cesta. */
    public static final int STATUS_COMPRADO = 0;

    public static final int UNIDADE_UN = 0;
    public static final int UNIDADE_ML = 1;
    public static final int UNIDADE_KG = 2;

    public static final String[] NOMES_UNIDADE = {"un", "ml", "Kg"};

    private static final String SEPARADOR = "#";

    /** Caracteres que o Firebase não aceita em chaves. */
    private static final String CARACTERES_INVALIDOS = ".$#[]/";

    private final int categoria;
    private final String nome;
    private final float quantidade;
    private final int unidade;
    private final int status;

    public Produto(int categoria, String nome, float quantidade, int unidade, int status) {
        this.categoria = categoria;
        this.nome = nome;
        this.quantidade = quantidade;
        this.unidade = unidade;
        this.status = status;
    }

    public int getCategoria() {
        return categoria;
    }

    public String getNome() {
        return nome;
    }

    public float getQuantidade() {
        return quantidade;
    }

    public int getUnidade() {
        return unidade;
    }

    public int getStatus() {
        return status;
    }

    public boolean estaNaLista() {
        return status == STATUS_PENDENTE || status == STATUS_COMPRADO;
    }

    public String getNomeUnidade() {
        return NOMES_UNIDADE[unidade];
    }

    /** Identifica o produto entre a despensa e a lista de compras. */
    public String chave() {
        return categoria + "/" + nome;
    }

    public Produto comStatus(int novoStatus) {
        return new Produto(categoria, nome, quantidade, unidade, novoStatus);
    }

    public Produto comQuantidadeEUnidade(float novaQuantidade, int novaUnidade) {
        return new Produto(categoria, nome, novaQuantidade, novaUnidade, status);
    }

    /** Valor gravado em {@code minhaLst}. Usa ponto decimal independente do idioma do aparelho. */
    public String valorLista() {
        return String.format(Locale.US, "%s%s%d%s%d", formatoGravacao(quantidade), SEPARADOR, unidade, SEPARADOR, status);
    }

    /** Produto da despensa, com quantidade 1 e disponível. Retorna null se o valor for inválido. */
    public static Produto daDespensa(int categoria, String nome, Object valor) {

        Integer unidade = paraInteiro(valor);

        if (unidade == null || !unidadeValida(unidade)) {
            return null;
        }

        return new Produto(categoria, nome, 1f, unidade, STATUS_DISPONIVEL);
    }

    /** Produto da lista de compras. Retorna null se o valor estiver malformado. */
    public static Produto daLista(int categoria, String nome, Object valor) {

        if (valor == null) {
            return null;
        }

        String[] partes = valor.toString().split(SEPARADOR);

        if (partes.length != 3) {
            return null;
        }

        try {
            float quantidade = Float.parseFloat(partes[0]);
            int unidade = Integer.parseInt(partes[1]);
            int status = Integer.parseInt(partes[2]);

            boolean statusValido = status == STATUS_PENDENTE || status == STATUS_COMPRADO;

            if (!unidadeValida(unidade) || !statusValido || !(quantidade > 0)) {
                return null;
            }

            return new Produto(categoria, nome, quantidade, unidade, status);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Deixa o nome com a primeira letra maiúscula e o restante minúsculo. Retorna vazio se não houver texto. */
    public static String normalizaNome(String nome) {

        if (nome == null) {
            return "";
        }

        String limpo = nome.trim().replaceAll("\\s+", " ");

        if (limpo.isEmpty()) {
            return "";
        }

        return limpo.substring(0, 1).toUpperCase(Locale.getDefault()) + limpo.substring(1).toLowerCase(Locale.getDefault());
    }

    /** O nome vira chave no Firebase, que rejeita ., $, #, [, ] e /. */
    public static boolean nomeValido(String nome) {

        if (nome == null || nome.trim().isEmpty()) {
            return false;
        }

        for (char c : CARACTERES_INVALIDOS.toCharArray()) {
            if (nome.indexOf(c) >= 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean unidadeValida(int unidade) {
        return unidade >= UNIDADE_UN && unidade <= UNIDADE_KG;
    }

    private static Integer paraInteiro(Object valor) {

        if (valor == null) {
            return null;
        }

        try {
            return Integer.parseInt(valor.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String formatoGravacao(float quantidade) {
        return quantidade == (long) quantidade ? String.valueOf((long) quantidade) : String.valueOf(quantidade);
    }
}
