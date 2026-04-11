package com.github.nhaeutilities.core.config;

import com.github.nhaeutilities.NHAEUtilities;
import com.github.nhaeutilities.core.module.ModuleRegistry;
import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * Listens for {@link ConfigChangedEvent.OnConfigChangedEvent} fired by Forge's
 * built-in config GUI and reloads all configuration values.
 *
 * <p>
 * This allows users to change mod settings from the in-game mod options
 * screen without manually editing the config file.
 */
public class ConfigChangeHandler {

    private final CoreConfig coreConfig;
    private final ModuleRegistry moduleRegistry;

    public ConfigChangeHandler(CoreConfig coreConfig, ModuleRegistry moduleRegistry) {
        this.coreConfig = coreConfig;
        this.moduleRegistry = moduleRegistry;
    }

    @SubscribeEvent
    public void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (!NHAEUtilities.MODID.equals(event.modID)) {
            return;
        }

        try {
            coreConfig.reload();
            PatternRoutingRuntime.setEnabled(coreConfig.isPatternRoutingEnabled());
            moduleRegistry.loadAllConfigs(CoreConfig.getConfiguration());
            CoreConfig.saveIfChanged();
        } catch (RuntimeException e) {
            FMLLog.warning("[NHAEUtilities] Failed to reload config from GUI: %s", e.getMessage());
        }
    }
}
