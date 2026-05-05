package com.github.nhaeutilities.core.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.config.Configuration;

public final class ConfigGuiLayout {

    private ConfigGuiLayout() {}

    /**
     * Returns the canonical module category names for the config GUI.
     * Uses a definitive whitelist from {@link CoreConfig#MODULE_CATEGORY_NAMES}
     * rather than scanning the config file, so stale categories from older code
     * versions are never presented as top-level entries.
     */
    public static List<String> getTopLevelModuleCategoryNames(Configuration cfg) {
        return new ArrayList<String>(CoreConfig.MODULE_CATEGORY_NAMES);
    }
}
