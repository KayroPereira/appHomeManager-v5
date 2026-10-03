package com.home.apphomemanager_v5.comodo;

import com.home.apphomemanager_v5.R;

import java.util.ArrayList;
import java.util.List;

public class Comodo {

    /** Nomes dos ícones disponíveis, na mesma ordem do array {@code iconesComodo} de strings. */
    public static final String[] ICONES = {"sala", "cozinha", "quarto", "banheiro", "hall", "escritorio", "manutencao", "climatizacao", "piscina", "churrasqueira", "quintal", "seguranca"};

    public String id;
    public String nome = "";
    public String icone = ICONES[0];
    public long ordem;

    /** Desabilitado, o cômodo continua visível mas nenhuma lâmpada ou tomada pode ser acionada. */
    public boolean habilitado = true;

    /** Chaves dos grupos escolhidos para o cômodo (um cômodo novo começa sem nenhum). */
    public List<String> grupos = new ArrayList<>();

    public List<ItemComodo> lampadas = new ArrayList<>();
    public List<ItemComodo> tomadas = new ArrayList<>();
    public List<ItemComodo> medidores = new ArrayList<>();
    public List<ItemComodo> termometros = new ArrayList<>();
    public List<ItemComodo> cortinas = new ArrayList<>();
    public List<ItemComodo> sensoresChuva = new ArrayList<>();
    public List<ItemComodo> alarmes = new ArrayList<>();
    public List<ItemComodo> luzes = new ArrayList<>();

    public List<ItemComodo> itens(GrupoDispositivo grupo) {

        switch (grupo) {
            case LAMPADAS:
                return lampadas;
            case TOMADAS:
                return tomadas;
            case MEDIDORES:
                return medidores;
            case TERMOMETROS:
                return termometros;
            case CORTINAS:
                return cortinas;
            case CHUVA:
                return sensoresChuva;
            case ALARMES:
                return alarmes;
            default:
                return luzes;
        }
    }

    /**
     * Cômodos cadastrados antes da escolha de grupos (ou cópias locais antigas) não têm a lista:
     * nesse caso um grupo com itens conta como escolhido.
     */
    public boolean temGrupo(GrupoDispositivo grupo) {
        return grupos.contains(grupo.chave) || !itens(grupo).isEmpty();
    }

    /** Grupos do cômodo, sempre na ordem de {@link GrupoDispositivo}. */
    public List<GrupoDispositivo> gruposAtivos() {

        List<GrupoDispositivo> ativos = new ArrayList<>();

        for (GrupoDispositivo grupo : GrupoDispositivo.values()) {
            if (temGrupo(grupo)) {
                ativos.add(grupo);
            }
        }
        return ativos;
    }

    public int totalItens() {

        int total = 0;

        for (GrupoDispositivo grupo : GrupoDispositivo.values()) {
            total += itens(grupo).size();
        }
        return total;
    }

    /** Itens que se associam a um dispositivo da Tuya (a cortina só usa cenas). */
    public int totalAssociaveis() {

        int total = 0;

        for (GrupoDispositivo grupo : GrupoDispositivo.values()) {
            total += grupo.dispositivo ? itens(grupo).size() : 0;
        }
        return total;
    }

    public int totalAssociados() {

        int total = 0;

        for (GrupoDispositivo grupo : GrupoDispositivo.values()) {
            for (ItemComodo item : itens(grupo)) {
                total += item.associado() ? 1 : 0;
            }
        }
        return total;
    }

    public int drawableIcone() {

        switch (icone) {
            case "cozinha":
                return R.drawable.cozinha;
            case "quarto":
                return R.drawable.quarto;
            case "banheiro":
                return R.drawable.banheiro;
            case "hall":
                return R.drawable.hall;
            case "escritorio":
                return R.drawable.escritorio;
            case "manutencao":
                return R.drawable.manutencao;
            case "climatizacao":
                return R.drawable.clima_d;
            case "piscina":
                return R.drawable.piscina_a;
            case "churrasqueira":
                return R.drawable.churrasqueira_b;
            case "quintal":
                return R.drawable.quintal_d;
            case "seguranca":
                return R.drawable.seguranca_b;
            default:
                return R.drawable.sofa;
        }
    }

    public int posicaoIcone() {

        for (int i = 0; i < ICONES.length; i++) {
            if (ICONES[i].equals(icone)) {
                return i;
            }
        }
        return 0;
    }
}
