package com.home.apphomemanager_v5.tuya.model;

/** Envelope padrão de toda resposta da Tuya Cloud. */
public class TuyaResponse<T> {

    public boolean success;
    public int code;
    public String msg;
    public T result;
}
