package com.github.nhaeutilities.core.module;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public interface ModuleDefinition {

    String id();

    boolean isEnabled();

    void preInit(FMLPreInitializationEvent event);

    void init(FMLInitializationEvent event, Object modInstance);

    void postInit(FMLPostInitializationEvent event);

    void serverStarting(FMLServerStartingEvent event);
}
