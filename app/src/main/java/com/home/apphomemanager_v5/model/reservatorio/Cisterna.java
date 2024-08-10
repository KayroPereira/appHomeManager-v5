package com.home.apphomemanager_v5.model.reservatorio;

import lombok.Data;

@Data
public class Cisterna extends Reservatorio{

    private Boolean caixa1;
    private Boolean caixa2;
    private Boolean caixa3;
    private Boolean valvulaControle;
    private Boolean valvulaEntrada;
    private Boolean pump;
}