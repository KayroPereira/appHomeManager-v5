package com.home.apphomemanager_v5.tuya.model;

import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;

public class TuyaToken {

    public static final Type TIPO = new TypeToken<TuyaResponse<TuyaToken>>() {}.getType();

    @SerializedName("access_token")
    public String accessToken;

    /** Validade em segundos. */
    @SerializedName("expire_time")
    public long expireTime;
}
