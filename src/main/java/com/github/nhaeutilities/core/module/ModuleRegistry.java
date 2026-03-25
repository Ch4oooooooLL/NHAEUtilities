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
    private List<ModuleDefinition> bootstrapModules;

    public void register(ModuleDefinition module) {
        modules.add(Objects.requireNonNull(module, "module"));
    }

    public List<ModuleDefinition> getEnabledModules() {
        return Collections.unmodifiableList(filterEnabledModules());
    }

    private List<ModuleDefinition> filterEnabledModules() {
        List<ModuleDefinition> enabledModules = new ArrayList<ModuleDefinition>();
        for (ModuleDefinition module : modules) {
            if (module.isEnabled()) {
                enabledModules.add(module);
            }
        }
        return enabledModules;
    }

    private List<ModuleDefinition> getBootstrapModules() {
        if (bootstrapModules == null) {
            bootstrapModules = Collections.unmodifiableList(filterEnabledModules());
        }
        return bootstrapModules;
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
        for (ModuleDefinition module : getBootstrapModules()) {
            phase.dispatch(module, event, modInstance);
        }
    }
}
