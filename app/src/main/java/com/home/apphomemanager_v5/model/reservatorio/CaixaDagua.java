package com.home.apphomemanager_v5.model.reservatorio;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CaixaDagua extends Reservatorio{

    private Boolean vlep;

    private Boolean vles;

    public void inicializa(){

        setOnOff(false);
        setAutoManual(true);
        setVlep(false);
        setVles(false);
        setNa(-1L);
        setStatus(0L);
    }
}