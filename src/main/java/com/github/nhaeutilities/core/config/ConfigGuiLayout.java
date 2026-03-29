package com.github.nhaeutilities.core.config;

import java.util.ArrayList;
import java.util.List;

import net.minecraftforge.common.config.Configuration;

public final class ConfigGuiLayout {

    private static final String MODULES_CATEGORY = "modules";

    private ConfigGuiLayout() {}

    public static List<String> getTopLevelModuleCategoryNames(Configuration cfg) {
        List<String> names = new ArrayList<String>();
        if (cfg == null) {
            return names;
        }

        for (String categoryName : cfg.getCategoryNames()) {
            if (!categoryName.startsWith(MODULES_CATEGORY + ".")) {
                continue;
            }

            String relativeName = categoryName.substring(MODULES_CATEGORY.length() + 1);
            if (relativeName.contains(".")) {
                continue;
            }

            names.add(categoryName);
        }

        return names;
    }
}
