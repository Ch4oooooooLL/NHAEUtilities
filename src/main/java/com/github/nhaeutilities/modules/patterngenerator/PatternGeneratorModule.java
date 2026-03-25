package com.github.nhaeutilities.modules.patterngenerator;

import java.util.Objects;

import com.github.nhaeutilities.core.config.CoreConfig;
import com.github.nhaeutilities.core.module.ModuleDefinition;

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
    public void preInit(FMLPreInitializationEvent event) {}

    @Override
    public void init(FMLInitializationEvent event, Object modInstance) {}

    @Override
    public void postInit(FMLPostInitializationEvent event) {}

    @Override
    public void serverStarting(FMLServerStartingEvent event) {}
}
