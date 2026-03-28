package com.github.nhaeutilities.core.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class ModuleRegistry {

    private final List<ModuleDefinition> modules = new ArrayList<ModuleDefinition>();
    private List<ModuleDefinition> bootstrapModules;

    public void register(ModuleDefinition module) {
        if (isFrozen()) {
            throw new IllegalStateException("Cannot register modules after bootstrap has started");
        }
        modules.add(Objects.requireNonNull(module, "module"));
    }

    /**
     * Returns an unmodifiable view of ALL registered modules regardless of enabled state.
     * Useful for config GUI to show every module's settings.
     */
    public List<ModuleDefinition> getAllModules() {
        return Collections.unmodifiableList(modules);
    }

    /**
     * Calls {@link ModuleDefinition#loadConfig(Configuration)} on every registered module
     * (including disabled ones) so that all configuration properties are declared in the
     * shared {@link Configuration} file and visible in the config GUI.
     */
    public void loadAllConfigs(Configuration configuration) {
        Objects.requireNonNull(configuration, "configuration");
        for (ModuleDefinition module : modules) {
            module.loadConfig(configuration);
        }
    }

    public List<ModuleDefinition> getEnabledModules() {
        if (isFrozen()) {
            return bootstrapModules;
        }
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

    private List<ModuleDefinition> freezeBootstrapModules() {
        if (bootstrapModules == null) {
            bootstrapModules = Collections.unmodifiableList(filterEnabledModules());
        }
        return bootstrapModules;
    }

    private boolean isFrozen() {
        return bootstrapModules != null;
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
        for (ModuleDefinition module : freezeBootstrapModules()) {
            phase.dispatch(module, event, modInstance);
        }
    }
}
