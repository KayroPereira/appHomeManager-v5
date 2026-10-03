package com.home.apphomemanager_v5.tuya;

import java.util.List;

/** Dispositivo já convertido do SDK, sem dependência de Android. */
public class TuyaDispositivo {

    public final String id;
    public final String nome;
    public final boolean online;
    public final List<TuyaPonto> pontos;

    public TuyaDispositivo(String id, String nome, boolean online, List<TuyaPonto> pontos) {
        this.id = id;
        this.nome = nome;
        this.online = online;
        this.pontos = pontos;
    }
}
