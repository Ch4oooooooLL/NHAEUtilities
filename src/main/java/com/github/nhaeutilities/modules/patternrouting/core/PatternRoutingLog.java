package com.github.nhaeutilities.modules.patternrouting.core;

import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;

import cpw.mods.fml.common.FMLLog;

public final class PatternRoutingLog {

    private PatternRoutingLog() {}

    public static boolean isEnabled() {
        return PatternRoutingRuntime.isDebugLogEnabled();
    }

    public static void debug(String format, Object... args) {
        if (!isEnabled()) {
            return;
        }
        try {
            FMLLog.info(format, args);
        } catch (RuntimeException ignored) {}
    }

    public static void info(String format, Object... args) {
        debug(format, args);
    }

    public static void warning(String format, Object... args) {
        if (!isEnabled()) {
            return;
        }
        try {
            FMLLog.warning(format, args);
        } catch (RuntimeException ignored) {}
    }
}
