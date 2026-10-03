package com.home.apphomemanager_v5.listacompras;

public class Categorias {

    private Categorias() {}

    /** A posição no array é o identificador da categoria no Firebase e o sufixo do drawable {@code ctg}. */
    public static final String[] NOMES = {
            "Mercado", "Lanches", "Bebidas", "Frios", "Limpeza", "Casa",
            "Carnes", "Peixes", "Frutas e Verduras", "Temperos", "Pets", "Outros"
    };

    public static int quantidade() {
        return NOMES.length;
    }

    public static boolean valida(int categoria) {
        return categoria >= 0 && categoria < NOMES.length;
    }

    public static String nome(int categoria) {
        return valida(categoria) ? NOMES[categoria] : "";
    }

    public static String nomeDrawable(int categoria) {
        return "ctg" + categoria;
    }
}
