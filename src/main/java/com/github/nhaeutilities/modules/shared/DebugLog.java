package com.github.nhaeutilities.modules.shared;

import cpw.mods.fml.common.FMLLog;

public final class DebugLog {

    private DebugLog() {}

    public static void info(String format, Object... args) {
        try {
            FMLLog.info(format, args);
        } catch (NullPointerException ignored) {}
    }
}
