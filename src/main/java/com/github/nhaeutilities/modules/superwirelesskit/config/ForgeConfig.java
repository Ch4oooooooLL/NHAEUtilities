package com.github.nhaeutilities.modules.superwirelesskit.config;

import java.util.Objects;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import com.github.nhaeutilities.modules.superwirelesskit.runtime.SuperWirelessDebugLog;

import cpw.mods.fml.common.FMLLog;

public final class ForgeConfig {

    private static final String MODULE_CATEGORY = "modules.superWirelessKit";
    private static final String CATEGORY_DEBUG = MODULE_CATEGORY + ".debug";
    private static final String ENABLED_PROPERTY = "enabled";
    private static final String LANG_PREFIX = "nhaeutilities.config.";

    private static volatile boolean debugModeEnabled;

    private ForgeConfig() {}

    public static void load(Configuration cfg) {
        Objects.requireNonNull(cfg, "cfg");
        try {
            Property enabledProperty = cfg.get(
                CATEGORY_DEBUG,
                ENABLED_PROPERTY,
                false,
                "Enable detailed SuperWirelessKit debug logging. Writes logs/superwirelesskit-debug.log and may affect performance.");
            enabledProperty.setLanguageKey(LANG_PREFIX + CATEGORY_DEBUG + "." + ENABLED_PROPERTY);
            debugModeEnabled = enabledProperty.getBoolean(false);
            applyCategoryMetadata(cfg);
            SuperWirelessDebugLog.configure(debugModeEnabled);
        } catch (RuntimeException e) {
            FMLLog.warning("[NHAEUtilities] Failed to load SuperWirelessKit config: %s", e.getMessage());
        }
    }

    private static void applyCategoryMetadata(Configuration cfg) {
        cfg.getCategory(CATEGORY_DEBUG)
            .setLanguageKey(LANG_PREFIX + CATEGORY_DEBUG);
    }

    public static boolean isDebugModeEnabled() {
        return debugModeEnabled;
    }
}
