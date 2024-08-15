package com.home.apphomemanager_v5.model.reservatorio;

import lombok.Data;

@Data
public class Cisterna extends Reservatorio{

    private Boolean cx1;

    private Boolean cx2;

    private Boolean cx3;

    private Boolean vlc;

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
        setStatus(0L);
    }
}