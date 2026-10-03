package com.home.apphomemanager_v5.tuya.model;

import java.util.Collections;
import java.util.List;

/** Corpo de {@code POST /v1.0/devices/{id}/commands}. */
public class TuyaCommands {

    public final List<TuyaStatus> commands;

    public TuyaCommands(String code, Object value) {
        this.commands = Collections.singletonList(new TuyaStatus(code, value));
    }
}
