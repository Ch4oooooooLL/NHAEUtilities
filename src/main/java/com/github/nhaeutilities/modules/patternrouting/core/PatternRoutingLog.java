package com.github.nhaeutilities.modules.patternrouting.core;

import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;

import cpw.mods.fml.common.FMLLog;

public final class PatternRoutingLog {

    private PatternRoutingLog() {}

    public static void info(String format, Object... args) {
        if (!PatternRoutingRuntime.isDebugLogEnabled()) {
            return;
        }
        try {
            FMLLog.info(format, args);
        } catch (Throwable ignored) {}
    }

    public static void warning(String format, Object... args) {
        if (!PatternRoutingRuntime.isDebugLogEnabled()) {
            return;
        }
        try {
            FMLLog.warning(format, args);
        } catch (Throwable ignored) {}
    }
}
