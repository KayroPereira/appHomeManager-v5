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
        if (this.na == null || this.nic == null || this.nsc == null || this.na < 0 || quantidadeImagensNiveis <= 0) {
            return 0;
        }

        double range = (double) (this.nic - this.nsc) / quantidadeImagensNiveis;

        if (range <= 0) {
            return 0;
        }

        long value = (long) Math.ceil((this.nic - this.na - range) / range);

        return (int) Math.max(0, Math.min(value, quantidadeImagensNiveis - 1));
    }

    /** Nível de 0 (vazio) a 1 (cheio) pelas leituras do sensor; -1 enquanto não há leitura válida. */
    public float calculaFracaoNivel(){

        if (this.na == null || this.nic == null || this.nsc == null || this.na < 0) {
            return -1f;
        }

        double faixa = (double) (this.nic - this.nsc);

        if (faixa <= 0) {
            return -1f;
        }

        double fracao = (this.nic - this.na) / faixa;

        return (float) Math.max(0d, Math.min(1d, fracao));
    }

    public int getNivelSuperiorRelativo(){

        if(this.nic == null || this.nsc == null){
            return 0;
        }

        return (int) (this.nic - this.nsc);
    }

    public int getNivelAtualRelativo(){

        if(this.nic == null || this.na == null){
            return 0;
        }

        return (int) (this.nic - this.na);
    }
}