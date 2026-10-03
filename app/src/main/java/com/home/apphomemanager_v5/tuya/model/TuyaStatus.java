package com.home.apphomemanager_v5.tuya.model;

/** Um ponto de dados do dispositivo, por exemplo {@code switch_1 = true}. */
public class TuyaStatus {

    public String code;
    public Object value;

    public TuyaStatus() {}

    public TuyaStatus(String code, Object value) {
        this.code = code;
        this.value = value;
    }
}
