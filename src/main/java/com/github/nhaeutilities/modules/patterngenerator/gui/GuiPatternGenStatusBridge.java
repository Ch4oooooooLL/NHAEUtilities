package com.github.nhaeutilities.modules.patterngenerator.gui;

import java.util.function.Consumer;

import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;

/**
 * Client-side status bridge for the open Pattern Generator window.
 * Uses a callback pattern so multiple sources can push status updates
 * without sharing a mutable global variable.
 */
public final class GuiPatternGenStatusBridge {

    private static Consumer<String> listener;
    private static String lastStatus = "";

    private GuiPatternGenStatusBridge() {}

    public static void register(Consumer<String> listener) {
        GuiPatternGenStatusBridge.listener = listener;
        if (listener != null && !lastStatus.isEmpty()) {
            listener.accept(lastStatus);
        }
    }

    public static void unregister() {
        listener = null;
        lastStatus = "";
    }

    public static void setStatus(String status) {
        if (listener == null) {
            return;
        }
        lastStatus = status != null ? status.trim() : "";
        listener.accept(lastStatus);
    }

    public static String getStatus() {
        return lastStatus.isEmpty() ? I18nUtil.tr("nhaeutilities.gui.pattern_gen.status.ready") : lastStatus;
    }

    public static void clearStatus() {
        lastStatus = "";
        if (listener != null) {
            listener.accept("");
        }
    }
}
