package com.github.nhaeutilities.modules.superwirelesskit;

import java.util.Objects;

import com.github.nhaeutilities.core.config.CoreConfig;
import com.github.nhaeutilities.core.module.ModuleDefinition;
import com.github.nhaeutilities.modules.superwirelesskit.item.ModItems;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.SuperWirelessServices;
import com.github.nhaeutilities.proxy.CommonProxy;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class SuperWirelessKitModule implements ModuleDefinition {

    private final CoreConfig coreConfig;
    private final CommonProxy proxy;

    public SuperWirelessKitModule(CoreConfig coreConfig, CommonProxy proxy) {
        this.coreConfig = Objects.requireNonNull(coreConfig, "coreConfig");
        this.proxy = Objects.requireNonNull(proxy, "proxy");
    }

    @Override
    public String id() {
        return "superWirelessKit";
    }

    @Override
    public boolean isEnabled() {
        return coreConfig.isSuperWirelessKitEnabled();
    }

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        ModItems.init();
        SuperWirelessServices.bootstrap();
    }

    @Override
    public void init(FMLInitializationEvent event, Object modInstance) {
        proxy.registerSuperWirelessKitIntegration();
    }

    @Override
    public void postInit(FMLPostInitializationEvent event) {}

    @Override
    public void serverStarting(FMLServerStartingEvent event) {}
}
