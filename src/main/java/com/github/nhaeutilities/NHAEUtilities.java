package com.github.nhaeutilities;

import com.github.nhaeutilities.core.config.CoreConfig;
import com.github.nhaeutilities.core.module.ModuleRegistry;
import com.github.nhaeutilities.proxy.CommonProxy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

@Mod(
    modid = NHAEUtilities.MODID,
    name = NHAEUtilities.MODNAME,
    version = Tags.VERSION,
    dependencies = "required-after:gregtech;required-after:appliedenergistics2;required-after:NotEnoughItems")
public class NHAEUtilities {

    public static final String MODID = "nhaeutilities";
    public static final String MODNAME = "NHAEUtilities";

    @Mod.Instance(MODID)
    public static NHAEUtilities instance;

    @SidedProxy(
        clientSide = "com.github.nhaeutilities.proxy.ClientProxy",
        serverSide = "com.github.nhaeutilities.proxy.CommonProxy")
    public static CommonProxy proxy;

    private ModuleRegistry moduleRegistry;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        CoreConfig coreConfig = CoreConfig.load(event.getSuggestedConfigurationFile());
        moduleRegistry = proxy.createModuleRegistry(coreConfig);
        proxy.preInit(event, moduleRegistry);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event, moduleRegistry, this);
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        proxy.postInit(event, moduleRegistry);
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        proxy.serverStarting(event, moduleRegistry);
    }
}
