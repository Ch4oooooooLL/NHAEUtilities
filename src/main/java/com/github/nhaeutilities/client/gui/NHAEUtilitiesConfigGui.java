package com.github.nhaeutilities.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import com.github.nhaeutilities.NHAEUtilities;
import com.github.nhaeutilities.core.config.CoreConfig;

import cpw.mods.fml.client.config.GuiConfig;
import cpw.mods.fml.client.config.IConfigElement;

public class NHAEUtilitiesConfigGui extends GuiConfig {

    public NHAEUtilitiesConfigGui(GuiScreen parentScreen) {
        super(
            parentScreen,
            getConfigElements(),
            NHAEUtilities.MODID,
            false,
            false,
            GuiConfig.getAbridgedConfigPath(
                CoreConfig.getConfiguration()
                    .getConfigFile()
                    .getAbsolutePath()));
    }

    /**
     * Builds config elements from only the root-level categories (those whose
     * name does not contain a dot). Forge's {@link ConfigElement} automatically
     * renders child categories hierarchically, so listing root categories is
     * sufficient to display the full tree:
     *
     * <pre>
     *   modules
     *     patternGenerator   (enabled toggle)
     *   patternGenerator
     *     conflict
     *     duplicate
     *     ui
     *       patternGen
     *       recipePicker
     *     storage
     *     items
     * </pre>
     */
    private static List<IConfigElement> getConfigElements() {
        Configuration cfg = CoreConfig.getConfiguration();
        List<IConfigElement> list = new ArrayList<IConfigElement>();

        for (String categoryName : cfg.getCategoryNames()) {
            if (categoryName.contains(".")) {
                continue;
            }
            ConfigCategory category = cfg.getCategory(categoryName);
            list.add(new ConfigElement(category));
        }

        return list;
    }
}
