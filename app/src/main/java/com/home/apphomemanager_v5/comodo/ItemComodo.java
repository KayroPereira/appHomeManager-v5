package com.home.apphomemanager_v5.comodo;

import java.util.HashMap;
import java.util.Map;

/** Lâmpada ou tomada de um cômodo, opcionalmente associada a um interruptor de um dispositivo da Tuya. */
public class ItemComodo {

    public String nome;
    public String tuyaDeviceId;
    public String tuyaCode;

    /** Nome do interruptor na Tuya, guardado para a edição mostrá-lo sem consultar a nuvem. */
    public String tuyaNome;

    /**
     * Cena de acionamento manual do Smart Life usada para comandar o item no lugar do comando direto
     * ao aparelho (que consome a cota de aparelhos controláveis). O interruptor associado continua
     * servindo só para ler o estado.
     */
    public String tuyaCena;

    /** Cena para parar a cortina no meio do curso (no alarme: remover alarmes). */
    public String tuyaCenaParar;

    /** Quarta e quinta cenas, só do alarme: proteção parcial e SOS. */
    public String tuyaCenaExtra1;
    public String tuyaCenaExtra2;

    /** Sexta e sétima cenas, só da luz ajustável (brilho 75% e 100%). */
    public String tuyaCenaExtra3;
    public String tuyaCenaExtra4;

    /** Cena para desligar; sem ela {@link #tuyaCena} é usada nos dois sentidos (cena que alterna). */
    public String tuyaCenaDesliga;

    /**
     * Casas decimais de cada ponto de medição do aparelho (código -> escala), guardadas na associação
     * porque o estado consultado na tela do cômodo não traz o esquema do ponto de dados.
     */
    public Map<String, Integer> escalas = new HashMap<>();

    /** Cortina: o motor conta 100% como fechada (o padrão é 100% = aberta). */
    public boolean posicaoInvertida;

    public ItemComodo(String nome) {
        this.nome = nome;
    }

    public boolean temCena() {
        return tuyaCena != null && !tuyaCena.isEmpty();
    }

    /** Nome da cena a executar para levar o item ao estado pedido. */
    public String cenaPara(boolean ligar) {

        if (!ligar && tuyaCenaDesliga != null && !tuyaCenaDesliga.isEmpty()) {
            return tuyaCenaDesliga;
        }
        return tuyaCena;
    }

    /**
     * Cena de um dos comandos do item: 0 = principal (liga, sobe ou proteção total), 1 = secundária
     * (desliga, desce ou desativar), 2 = parar (ou remover alarmes), 3 = proteção parcial e 4 = SOS;
     * na luz ajustável 0 a 2 são as temperaturas e 3 a 6 os brilhos.
     */
    public String cena(int comando) {
        switch (comando) {
            case 0:
                return tuyaCena;
            case 1:
                return tuyaCenaDesliga;
            case 2:
                return tuyaCenaParar;
            case 3:
                return tuyaCenaExtra1;
            case 4:
                return tuyaCenaExtra2;
            case 5:
                return tuyaCenaExtra3;
            default:
                return tuyaCenaExtra4;
        }
    }

    public void defineCena(int comando, String nome) {

        switch (comando) {
            case 0:
                tuyaCena = nome;
                break;
            case 1:
                tuyaCenaDesliga = nome;
                break;
            case 2:
                tuyaCenaParar = nome;
                break;
            case 3:
                tuyaCenaExtra1 = nome;
                break;
            case 4:
                tuyaCenaExtra2 = nome;
                break;
            case 5:
                tuyaCenaExtra3 = nome;
                break;
            default:
                tuyaCenaExtra4 = nome;
                break;
        }
    }

    public static boolean preenchida(String cena) {
        return cena != null && !cena.isEmpty();
    }

    public boolean associado() {
        return tuyaDeviceId != null && !tuyaDeviceId.isEmpty() && tuyaCode != null && !tuyaCode.isEmpty();
    }

    /** Mesma chave de {@code TuyaControle.chave()}. */
    public String chave() {
        return tuyaDeviceId + "/" + tuyaCode;
    }

    public void associa(String deviceId, String code, String titulo) {
        tuyaDeviceId = deviceId;
        tuyaCode = code;
        tuyaNome = titulo;
    }

    public int escala(String code, int padrao) {

        Integer escala = escalas != null ? escalas.get(code) : null;
        return escala != null ? escala : padrao;
    }

    public void desassocia() {
        tuyaDeviceId = null;
        tuyaCode = null;
        tuyaNome = null;
        escalas = new HashMap<>();
    }
}
