package com.github.nhaeutilities.modules.patterngenerator;

import java.util.Objects;

import com.github.nhaeutilities.core.config.CoreConfig;
import com.github.nhaeutilities.core.module.ModuleDefinition;
import com.github.nhaeutilities.modules.patterngenerator.command.CommandPatternGen;
import com.github.nhaeutilities.modules.patterngenerator.config.ForgeConfig;
import com.github.nhaeutilities.modules.patterngenerator.config.ReplacementConfig;
import com.github.nhaeutilities.modules.patterngenerator.network.NetworkHandler;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class PatternGeneratorModule implements ModuleDefinition {

    private final CoreConfig coreConfig;

    public PatternGeneratorModule(CoreConfig coreConfig) {
        this.coreConfig = Objects.requireNonNull(coreConfig, "coreConfig");
    }

    @Override
    public String id() {
        return "patternGenerator";
    }

    @Override
    public boolean isEnabled() {
        return coreConfig.isPatternGeneratorEnabled();
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        ForgeConfig.load(event.getSuggestedConfigurationFile());
        ReplacementConfig.load();
        NetworkHandler.init();
    }

    @Override
    public void init(FMLInitializationEvent event, Object modInstance) {}

    @Override
    public void postInit(FMLPostInitializationEvent event) {}

    @Override
    public void serverStarting(FMLServerStartingEvent event) {
        event.registerServerCommand(new CommandPatternGen());
    }
}
