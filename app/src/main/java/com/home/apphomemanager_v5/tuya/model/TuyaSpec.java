package com.home.apphomemanager_v5.tuya.model;

import java.util.List;

/** Resposta de {@code GET /v1.0/devices/{id}/specifications}. */
public class TuyaSpec {

    /** Pontos de dados que aceitam comando. */
    public List<TuyaFuncao> functions;

    /** Pontos de dados que o dispositivo informa. */
    public List<TuyaFuncao> status;
}
