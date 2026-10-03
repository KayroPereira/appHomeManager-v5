package com.home.apphomemanager_v5.tuya.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/** Cena da casa. Acionamento manual (tap-to-run) não tem condições; automações têm. */
public class TuyaScene {

    @SerializedName("scene_id")
    public String sceneId;

    public String name;

    public boolean enabled;

    public List<Object> conditions;
}
