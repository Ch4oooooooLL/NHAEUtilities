package com.github.nhaeutilities.core.config;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.nhaeutilities.NHAEUtilities;
import com.github.nhaeutilities.core.module.ModuleRegistry;
import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;
import com.github.nhaeutilities.modules.patternrouting.config.ForgeConfig;

import cpw.mods.fml.client.event.ConfigChangedEvent;
import cpw.mods.fml.common.FMLCommonHandler;
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
            String configId = event.configID != null ? event.configID : "";
            boolean isGeneralConfig = configId.isEmpty() || !configId.startsWith("modules.");

            boolean pgBefore = coreConfig.isPatternGeneratorEnabled();
            boolean swkBefore = coreConfig.isSuperWirelessKitEnabled();
            boolean prBefore = coreConfig.isPatternRoutingEnabled();

            coreConfig.reload();
            PatternRoutingRuntime.setEnabled(coreConfig.isPatternRoutingEnabled());
            if (isGeneralConfig || configId.startsWith("modules.patternRouting")) {
                moduleRegistry.loadConfig(CoreConfig.getConfiguration(), "patternRouting");
            }
            if (isGeneralConfig || configId.startsWith("modules.patternGenerator")) {
                moduleRegistry.loadConfig(CoreConfig.getConfiguration(), "patternGenerator");
            }
            if (isGeneralConfig || configId.startsWith("modules.superWirelessKit")) {
                moduleRegistry.loadConfig(CoreConfig.getConfiguration(), "superWirelessKit");
            }
            PatternRoutingRuntime.setDebugLogEnabled(ForgeConfig.isDebugModeEnabled());
            CoreConfig.saveIfChanged();

            boolean moduleChanged = pgBefore != coreConfig.isPatternGeneratorEnabled()
                || swkBefore != coreConfig.isSuperWirelessKitEnabled()
                || prBefore != coreConfig.isPatternRoutingEnabled();

            if (moduleChanged && FMLCommonHandler.instance()
                .getEffectiveSide()
                .isClient()) {
                Minecraft mc = Minecraft.getMinecraft();
                if (mc != null && mc.thePlayer != null) {
                    mc.thePlayer.addChatMessage(
                        new ChatComponentText(
                            EnumChatFormatting.GOLD + "[NHAEUtilities] "
                                + net.minecraft.util.StatCollector
                                    .translateToLocal("nhaeutilities.config.restart_warning")));
                }
            }
        } catch (RuntimeException e) {
            FMLLog.warning("[NHAEUtilities] Failed to reload config from GUI: %s", e.getMessage());
        }
    }
}
