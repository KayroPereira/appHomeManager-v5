package com.home.apphomemanager_v5.model.reservatorio;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class Reservatorio {

    private Boolean onOff;

    private Boolean autoManual;

//    @JsonProperty("na")
//    private Long nivelAtual;
//
//    @JsonProperty("ni")
//    private Long nivelInferior;
//
//    @JsonProperty("nic")
//    private Long nivelInferiorCritico;
//
//    @JsonProperty("ns")
//    private Long nivelSuperior;
//
//    @JsonProperty("nsc")
//    private Long nivelSuperiorCritico;

    @JsonProperty("na")
    private Long na;

    @JsonProperty("ni")
    private Long ni;

    @JsonProperty("nic")
    private Long nic;

    @JsonProperty("ns")
    private Long ns;

    @JsonProperty("nsc")
    private Long nsc;

    private Long resetDate;

    private Long status;

    private Long update;
}