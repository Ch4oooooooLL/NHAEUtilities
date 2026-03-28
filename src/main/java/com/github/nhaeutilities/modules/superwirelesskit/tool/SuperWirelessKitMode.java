package com.github.nhaeutilities.modules.superwirelesskit.tool;

public enum SuperWirelessKitMode {

    QUEUE,
    BIND;

    public SuperWirelessKitMode next() {
        return this == QUEUE ? BIND : QUEUE;
    }
}
