package com.github.nhaeutilities.core.module;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public interface ModuleDefinition {

    String id();

    boolean isEnabled();

    /**
     * Declare and load this module's configuration properties into the shared
     * {@link Configuration}. Called for ALL registered modules (including disabled ones)
     * so that every module's settings are visible in the config GUI regardless of
     * its enabled state.
     *
     * <p>
     * The default implementation is a no-op for modules that have no extra config.
     */
    default void loadConfig(Configuration configuration) {}

    void preInit(FMLPreInitializationEvent event);

    void init(FMLInitializationEvent event, Object modInstance);

    void postInit(FMLPostInitializationEvent event);

    void serverStarting(FMLServerStartingEvent event);
}
