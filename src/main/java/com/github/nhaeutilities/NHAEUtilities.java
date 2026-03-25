package com.github.nhaeutilities;

import com.github.nhaeutilities.proxy.CommonProxy;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

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

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }
}
