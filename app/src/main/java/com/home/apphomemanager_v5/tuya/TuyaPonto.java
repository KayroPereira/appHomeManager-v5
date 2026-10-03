package com.home.apphomemanager_v5.tuya;

/** Ponto de dados (DP) de um dispositivo: código, valor atual e a propriedade (JSON) do esquema. */
public class TuyaPonto {

    public final String code;
    public final Object valor;
    /** JSON do esquema, como {"type":"value","min":10,"max":1000,"scale":0,"step":1,"unit":""}. Pode ser nulo. */
    public final String propriedade;

    public TuyaPonto(String code, Object valor, String propriedade) {
        this.code = code;
        this.valor = valor;
        this.propriedade = propriedade;
    }
}
