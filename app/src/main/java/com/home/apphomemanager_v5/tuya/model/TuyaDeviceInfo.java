package com.home.apphomemanager_v5.tuya.model;

import com.google.gson.annotations.SerializedName;

/** Parte de {@code GET /v1.0/devices/{id}} que interessa: a casa (family) a que o aparelho pertence. */
public class TuyaDeviceInfo {

    /** ID da casa; é o {@code home_id} das APIs de cenas. */
    @SerializedName("owner_id")
    public String ownerId;
}
