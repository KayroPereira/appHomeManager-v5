package com.home.apphomemanager_v5.tuya.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class TuyaDevicesPage {

    public List<TuyaDevice> devices;

    @SerializedName("has_more")
    public boolean hasMore;

    @SerializedName("last_row_key")
    public String lastRowKey;
}
