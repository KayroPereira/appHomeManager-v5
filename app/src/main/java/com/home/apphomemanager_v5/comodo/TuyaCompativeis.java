package com.home.apphomemanager_v5.comodo;

import com.home.apphomemanager_v5.tuya.TuyaControle;
import com.home.apphomemanager_v5.tuya.TuyaDispositivo;
import com.home.apphomemanager_v5.tuya.TuyaPonto;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Dispositivos da Tuya que podem ser associados a cada grupo de um cômodo; os demais nem aparecem na lista. */
public final class TuyaCompativeis {

    /** Uma escolha da lista: o ponto de dados a guardar na associação e as casas decimais das leituras do aparelho. */
    public static final class Opcao {

        public final String deviceId;
        public final String code;
        public final String titulo;
        public final boolean online;
        public final Map<String, Integer> escalas;

        Opcao(TuyaControle controle, String titulo, Map<String, Integer> escalas) {
            this(controle.deviceId, controle.code, titulo, controle.online, escalas);
        }

        Opcao(String deviceId, String code, String titulo, boolean online, Map<String, Integer> escalas) {
            this.deviceId = deviceId;
            this.code = code;
            this.titulo = titulo;
            this.online = online;
            this.escalas = escalas;
        }

        /** Mesma chave de {@link ItemComodo#chave()}. */
        public String chave() {
            return deviceId + "/" + code;
        }
    }

    private static final String[][] GRANDEZAS_ENERGIA = {LeituraDispositivo.POTENCIA, LeituraDispositivo.CORRENTE, LeituraDispositivo.TENSAO};
    private static final String[][] GRANDEZAS_AMBIENTE = {LeituraDispositivo.TEMPERATURA, LeituraDispositivo.UMIDADE};

    private TuyaCompativeis() {}

    public static List<Opcao> para(GrupoDispositivo grupo, List<TuyaDispositivo> dispositivos) {

        if (grupo == GrupoDispositivo.CHUVA || grupo == GrupoDispositivo.ALARMES || grupo == GrupoDispositivo.CORTINAS || grupo == GrupoDispositivo.LUZ) {
            return porPontoDeDados(grupo, dispositivos);
        }

        List<TuyaControle> controles = TuyaControle.extrai(dispositivos);
        Map<String, Map<String, Integer>> medicoes = medicoes(controles);
        List<Opcao> opcoes = new ArrayList<>();
        Set<String> aparelhosVistos = new HashSet<>();

        for (TuyaControle controle : controles) {

            switch (grupo) {
                case LAMPADAS:
                case TOMADAS:
                    if (interruptorComum(controle)) {
                        opcoes.add(new Opcao(controle, controle.titulo(), Collections.emptyMap()));
                    }
                    break;

                case MEDIDORES:
                    // Interruptor de um aparelho que também mede energia.
                    if (interruptorComum(controle) && mede(medicoes.get(controle.deviceId), GRANDEZAS_ENERGIA)) {
                        opcoes.add(new Opcao(controle, controle.titulo(), filtra(medicoes.get(controle.deviceId), GRANDEZAS_ENERGIA)));
                    }
                    break;

                default:
                    // Um termômetro por aparelho, identificado pelo ponto de temperatura.
                    if (controle.tipo == TuyaControle.Tipo.SENSOR && contem(LeituraDispositivo.TEMPERATURA, controle.code) && aparelhosVistos.add(controle.deviceId)) {
                        opcoes.add(new Opcao(controle, controle.nome, filtra(medicoes.get(controle.deviceId), GRANDEZAS_AMBIENTE)));
                    }
                    break;
            }
        }
        return opcoes;
    }

    /** Sensor de chuva e central de alarme se reconhecem por um ponto de dados específico, sem interruptor. */
    private static List<Opcao> porPontoDeDados(GrupoDispositivo grupo, List<TuyaDispositivo> dispositivos) {

        List<Opcao> opcoes = new ArrayList<>();

        for (TuyaDispositivo dispositivo : dispositivos) {

            if (grupo == GrupoDispositivo.LUZ) {

                TuyaPonto brilho = pontoDe(dispositivo, LeituraDispositivo.BRILHO_LUZ);
                TuyaPonto temperatura = pontoDe(dispositivo, LeituraDispositivo.TEMPERATURA_COR_LUZ);

                if (brilho != null || temperatura != null) {

                    // As faixas ficam na associação: o estado consultado na tela do cômodo não traz o esquema.
                    Map<String, Integer> faixas = new HashMap<>();

                    if (brilho != null) {
                        faixas.put("bright_min", faixa(brilho.propriedade, "min", 10));
                        faixas.put("bright_max", faixa(brilho.propriedade, "max", 1000));
                    }
                    if (temperatura != null) {
                        faixas.put("temp_min", faixa(temperatura.propriedade, "min", 0));
                        faixas.put("temp_max", faixa(temperatura.propriedade, "max", 1000));
                    }

                    opcoes.add(new Opcao(dispositivo.id, brilho != null ? brilho.code : temperatura.code,
                            dispositivo.nome != null ? dispositivo.nome : dispositivo.id, dispositivo.online, faixas));
                }
                continue;
            }

            if (grupo == GrupoDispositivo.CORTINAS) {
                String codigo = codigoDePosicao(dispositivo);

                if (codigo != null) {
                    opcoes.add(new Opcao(dispositivo.id, codigo, dispositivo.nome != null ? dispositivo.nome : dispositivo.id,
                            dispositivo.online, Collections.emptyMap()));
                }
                continue;
            }

            for (TuyaPonto ponto : dispositivo.pontos) {

                boolean serve = grupo == GrupoDispositivo.CHUVA
                        ? LeituraDispositivo.codigoDeChuva(ponto.code) && LeituraDispositivo.valorDeChuva(ponto.valor) != null
                        : contem(LeituraDispositivo.ALARME, ponto.code) && ponto.valor instanceof String;

                if (serve) {
                    opcoes.add(new Opcao(dispositivo.id, ponto.code, dispositivo.nome != null ? dispositivo.nome : dispositivo.id,
                            dispositivo.online, Collections.emptyMap()));
                    // Um por aparelho.
                    break;
                }
            }
        }
        return opcoes;
    }

    private static TuyaPonto pontoDe(TuyaDispositivo dispositivo, String[] codigos) {

        for (String codigo : codigos) {
            for (TuyaPonto ponto : dispositivo.pontos) {
                if (codigo.equals(ponto.code) && ponto.valor instanceof Number) {
                    return ponto;
                }
            }
        }
        return null;
    }

    /** Lê min ou max do JSON do esquema do ponto (ex.: {"min":10,"max":1000}); o padrão vale se faltar. */
    static int faixa(String propriedade, String campo, int padrao) {

        if (propriedade == null || propriedade.isEmpty()) {
            return padrao;
        }

        try {
            com.google.gson.JsonObject json = com.google.gson.JsonParser.parseString(propriedade).getAsJsonObject();
            return json.has(campo) && json.get(campo).isJsonPrimitive() ? json.get(campo).getAsInt() : padrao;
        } catch (RuntimeException e) {
            return padrao;
        }
    }

    /** Motor de cortina: o ponto de posição que o aparelho tiver, preferindo a posição atual à de comando. */
    private static String codigoDePosicao(TuyaDispositivo dispositivo) {

        for (String codigo : LeituraDispositivo.POSICAO_CORTINA) {
            for (TuyaPonto ponto : dispositivo.pontos) {
                if (codigo.equals(ponto.code) && ponto.valor instanceof Number) {
                    return codigo;
                }
            }
        }
        return null;
    }

    /** Interruptores on/off comuns (switch, switch_1...); os extras (movimento, gravação...) vêm com rótulo. */
    private static boolean interruptorComum(TuyaControle controle) {
        return controle.tipo == TuyaControle.Tipo.INTERRUPTOR && controle.rotulo.isEmpty();
    }

    /** Aparelho -> código -> escala de cada sensor numérico. */
    private static Map<String, Map<String, Integer>> medicoes(List<TuyaControle> controles) {

        Map<String, Map<String, Integer>> medicoes = new HashMap<>();

        for (TuyaControle controle : controles) {
            if (controle.tipo == TuyaControle.Tipo.SENSOR) {
                medicoes.computeIfAbsent(controle.deviceId, id -> new HashMap<>()).put(controle.code, controle.escala);
            }
        }
        return medicoes;
    }

    private static boolean mede(Map<String, Integer> medicoes, String[][] grandezas) {
        return !filtra(medicoes, grandezas).isEmpty();
    }

    /** Só as medições das grandezas pedidas. */
    private static Map<String, Integer> filtra(Map<String, Integer> medicoes, String[][] grandezas) {

        Map<String, Integer> escalas = new HashMap<>();

        if (medicoes == null) {
            return escalas;
        }

        for (String[] codigos : grandezas) {
            for (String codigo : codigos) {
                if (medicoes.containsKey(codigo)) {
                    escalas.put(codigo, medicoes.get(codigo));
                }
            }
        }
        return escalas;
    }

    private static boolean contem(String[] codigos, String codigo) {

        for (String candidato : codigos) {
            if (candidato.equals(codigo)) {
                return true;
            }
        }
        return false;
    }
}
