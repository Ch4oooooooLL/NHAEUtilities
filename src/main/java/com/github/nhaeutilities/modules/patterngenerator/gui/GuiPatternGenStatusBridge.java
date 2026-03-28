package com.github.nhaeutilities.modules.patterngenerator.gui;

import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;

/**
 * Client-side status bridge for the open Pattern Generator window.
 */
public final class GuiPatternGenStatusBridge {

    private static volatile String statusText = "";

    private GuiPatternGenStatusBridge() {}

    public static void setStatus(String status) {
        statusText = normalize(status);
    }

    public static void clearStatus() {
        statusText = "";
    }

    public static String getStatus() {
        return normalize(statusText);
    }

    private static String normalize(String status) {
        if (status == null || status.trim().isEmpty()) {
            return I18nUtil.tr("nhaeutilities.gui.pattern_gen.status.ready");
        }
        return status.trim();
    }
}
