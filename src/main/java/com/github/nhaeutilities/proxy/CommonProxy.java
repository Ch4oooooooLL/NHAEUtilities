package com.github.nhaeutilities.proxy;

import java.util.Objects;

import com.github.nhaeutilities.core.config.CoreConfig;
import com.github.nhaeutilities.core.module.ModuleRegistry;
import com.github.nhaeutilities.modules.patterngenerator.PatternGeneratorModule;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class CommonProxy {

    public ModuleRegistry createModuleRegistry(CoreConfig coreConfig) {
        ModuleRegistry moduleRegistry = new ModuleRegistry();
        registerBuiltInModules(coreConfig, moduleRegistry);
        return moduleRegistry;
    }

    protected void registerBuiltInModules(CoreConfig coreConfig, ModuleRegistry moduleRegistry) {
        Objects.requireNonNull(coreConfig, "coreConfig");
        Objects.requireNonNull(moduleRegistry, "moduleRegistry");
        moduleRegistry.register(new PatternGeneratorModule(coreConfig));
    }

    public void preInit(FMLPreInitializationEvent event, ModuleRegistry moduleRegistry) {
        moduleRegistry.preInit(event);
    }

    public void init(FMLInitializationEvent event, ModuleRegistry moduleRegistry, Object modInstance) {
        moduleRegistry.init(event, modInstance);
    }

    public void postInit(FMLPostInitializationEvent event, ModuleRegistry moduleRegistry) {
        moduleRegistry.postInit(event);
    }

    public void serverStarting(FMLServerStartingEvent event, ModuleRegistry moduleRegistry) {
        moduleRegistry.serverStarting(event);
    }
}
