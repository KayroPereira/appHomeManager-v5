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
        if (this.na < 0) {
            return 0;
        }

        double range = (this.nic - this.nsc) / quantidadeImagensNiveis;
        long value = (long) Math.ceil((this.nic - this.na - range) / range);

        return (int) Math.max(0, Math.min(value, quantidadeImagensNiveis - 1));
    }
}