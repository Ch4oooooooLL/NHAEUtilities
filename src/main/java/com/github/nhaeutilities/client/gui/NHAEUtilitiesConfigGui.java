package com.github.nhaeutilities.client.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.common.config.Configuration;

import com.github.nhaeutilities.NHAEUtilities;
import com.github.nhaeutilities.core.config.ConfigGuiLayout;
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
     * Builds config elements from module categories only. The shared
     * configuration may contain internal grouping roots, but the GUI should
     * present module entries directly at the top level.
     *
     * <pre>
     *   modules.patternGenerator
     *     basic
     *     conflict
     *     requestProtection
     *     ui
     *     storage
     *     advanced
     *   modules.superWirelessKit
     *     basic
     * </pre>
     */
    private static List<IConfigElement> getConfigElements() {
        Configuration cfg = CoreConfig.getConfiguration();
        List<IConfigElement> list = new ArrayList<IConfigElement>();

        for (String categoryName : ConfigGuiLayout.getTopLevelModuleCategoryNames(cfg)) {
            ConfigCategory category = cfg.getCategory(categoryName);
            list.add(new ConfigElement(category));
        }

        return list;
    }
}
