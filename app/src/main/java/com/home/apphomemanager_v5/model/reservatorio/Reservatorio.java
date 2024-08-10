package com.home.apphomemanager_v5.model.reservatorio;

import lombok.Data;

@Data
public class Reservatorio {

    private Boolean onOff;
    private Boolean autoManual;
    private Boolean nivelAtual;
    private Boolean nivelInferior;
    private Boolean nivelInferiorCritico;
    private Boolean nivelSuperior;
    private Boolean nivelSuperiorCritico;
    private Long resetDate;
    private Long status;
    private Long update;
}