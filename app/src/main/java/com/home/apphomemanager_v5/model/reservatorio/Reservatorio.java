package com.home.apphomemanager_v5.model.reservatorio;

import lombok.Data;

@Data
public class Reservatorio {

    private Boolean onOff;

    private Boolean autoManual;

    private Long na;

    private Long ni;

    private Long nic;

    private Long ns;

    private Long nsc;

    private Long resetDate;

    private Long status;

    private Long update;

    public int getImageLevel(int quantidadeImagensNiveis){

        long range = (this.ni - this.ns) / quantidadeImagensNiveis;
        long value = ((this.ni - this.na - range) / range);

        if (value < 0)
            return 0;
        else if (value >= quantidadeImagensNiveis)
            return quantidadeImagensNiveis - 1;
        else
            return (int) value;
    }
}