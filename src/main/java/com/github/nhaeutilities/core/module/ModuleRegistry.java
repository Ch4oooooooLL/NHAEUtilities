package com.github.nhaeutilities.core.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class ModuleRegistry {

    private final List<ModuleDefinition> modules = new ArrayList<ModuleDefinition>();

    public void register(ModuleDefinition module) {
        modules.add(Objects.requireNonNull(module, "module"));
    }

    public List<ModuleDefinition> getEnabledModules() {
        List<ModuleDefinition> enabledModules = new ArrayList<ModuleDefinition>();
        for (ModuleDefinition module : modules) {
            if (module.isEnabled()) {
                enabledModules.add(module);
            }
        }
        return Collections.unmodifiableList(enabledModules);
    }

    public void preInit(FMLPreInitializationEvent event) {
        dispatch(ModuleBootstrapPhase.PRE_INIT, event, null);
    }

    public void init(FMLInitializationEvent event, Object modInstance) {
        dispatch(ModuleBootstrapPhase.INIT, event, modInstance);
    }

    public void postInit(FMLPostInitializationEvent event) {
        dispatch(ModuleBootstrapPhase.POST_INIT, event, null);
    }

    public void serverStarting(FMLServerStartingEvent event) {
        dispatch(ModuleBootstrapPhase.SERVER_STARTING, event, null);
    }

    void dispatch(ModuleBootstrapPhase phase, Object event, Object modInstance) {
        Objects.requireNonNull(phase, "phase");
        for (ModuleDefinition module : getEnabledModules()) {
            phase.dispatch(module, event, modInstance);
        }
    }
}
