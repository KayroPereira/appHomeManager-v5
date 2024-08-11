package com.home.apphomemanager_v5.model.reservatorio;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Cisterna extends Reservatorio{

//    @JsonProperty("cx1")
//    private Boolean caixa1;
//
//    @JsonProperty("cx2")
//    private Boolean caixa2;
//
//    @JsonProperty("cx3")
//    private Boolean caixa3;
//
//    @JsonProperty("vlc")
//    private Boolean valvulaControle;
//
//    @JsonProperty("vle")
//    private Boolean valvulaEntrada;

    @JsonProperty("cx1")
    private Boolean cx1;

    @JsonProperty("cx2")
    private Boolean cx2;

    @JsonProperty("cx3")
    private Boolean cx3;

    @JsonProperty("vlc")
    private Boolean vlc;

    @JsonProperty("vle")
    private Boolean vle;

    private Boolean pump;

    public void inicializa(){

        setOnOff(false);
        setAutoManual(true);
        setCx1(false);
        setCx2(false);
        setCx3(false);
        setPump(false);
        setVlc(false);
        setVle(false);
        setNa(-1L);
    }
}