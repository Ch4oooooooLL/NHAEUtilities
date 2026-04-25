package com.github.nhaeutilities.modules.patternrouting;

import java.util.Objects;

import net.minecraftforge.common.config.Configuration;

import com.github.nhaeutilities.core.config.CoreConfig;
import com.github.nhaeutilities.core.module.ModuleDefinition;
import com.github.nhaeutilities.modules.patternrouting.command.CommandNau;
import com.github.nhaeutilities.modules.patternrouting.config.ForgeConfig;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class PatternRoutingModule implements ModuleDefinition {

    private final CoreConfig coreConfig;

    public PatternRoutingModule(CoreConfig coreConfig) {
        this.coreConfig = Objects.requireNonNull(coreConfig, "coreConfig");
    }

    @Override
    public String id() {
        return "patternRouting";
    }

    @Override
    public boolean isEnabled() {
        return coreConfig.isPatternRoutingEnabled();
    }

    @Override
    public void loadConfig(Configuration configuration) {
        ForgeConfig.load(configuration);
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        PatternRoutingRuntime.setEnabled(coreConfig.isPatternRoutingEnabled());
        PatternRoutingRuntime.setDebugLogEnabled(ForgeConfig.isDebugModeEnabled());
    }

    @Override
    public void init(FMLInitializationEvent event, Object modInstance) {}

    @Override
    public void postInit(FMLPostInitializationEvent event) {}

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandNau());
    }
}
