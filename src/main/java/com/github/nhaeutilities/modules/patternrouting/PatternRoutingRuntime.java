package com.github.nhaeutilities.modules.patternrouting;

public final class PatternRoutingRuntime {

    private static volatile boolean enabled = true;
    private static volatile boolean debugLogEnabled = true;

    private PatternRoutingRuntime() {}

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean enabled) {
        PatternRoutingRuntime.enabled = enabled;
    }

    public static boolean isDebugLogEnabled() {
        return debugLogEnabled;
    }

    public static void setDebugLogEnabled(boolean debugLogEnabled) {
        PatternRoutingRuntime.debugLogEnabled = debugLogEnabled;
    }
}
