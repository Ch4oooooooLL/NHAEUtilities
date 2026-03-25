package com.github.nhaeutilities.core.module;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class ModuleLifecycleTest {

    @Test
    public void disabledModulesNeverReceiveLifecycleCallbacks() {
        ModuleRegistry registry = new ModuleRegistry();
        List<String> callbacks = new ArrayList<String>();
        RecordingModule enabledModule = new RecordingModule("enabled", true, callbacks);
        RecordingModule disabledModule = new RecordingModule("lateEnabled", false, callbacks);
        Object modInstance = new Object();

        registry.register(enabledModule);
        registry.register(disabledModule);

        registry.preInit(null);
        disabledModule.setEnabled(true);
        registry.init(null, modInstance);
        registry.postInit(null);
        registry.serverStarting(null);

        assertEquals(Arrays.asList(
            "enabled:PRE_INIT",
            "enabled:INIT",
            "enabled:POST_INIT",
            "enabled:SERVER_STARTING"),
            callbacks);
        assertSame(modInstance, enabledModule.lastModInstance);
        assertNull(disabledModule.lastModInstance);
    }

    @Test
    public void enabledModulesReceiveAllLifecycleCallbacksInOrderEvenIfDisabledLater() {
        ModuleRegistry registry = new ModuleRegistry();
        List<String> callbacks = new ArrayList<String>();
        RecordingModule firstModule = new RecordingModule("zeta", true, callbacks);
        RecordingModule secondModule = new RecordingModule("alpha", true, callbacks);
        Object modInstance = new Object();

        registry.register(firstModule);
        registry.register(secondModule);

        registry.preInit(null);
        firstModule.setEnabled(false);
        registry.init(null, modInstance);
        registry.postInit(null);
        registry.serverStarting(null);

        assertEquals(Arrays.asList(
            "zeta:PRE_INIT",
            "alpha:PRE_INIT",
            "zeta:INIT",
            "alpha:INIT",
            "zeta:POST_INIT",
            "alpha:POST_INIT",
            "zeta:SERVER_STARTING",
            "alpha:SERVER_STARTING"),
            callbacks);
        assertSame(modInstance, firstModule.lastModInstance);
        assertSame(modInstance, secondModule.lastModInstance);
    }

    private static final class RecordingModule implements ModuleDefinition {

        private final String id;
        private boolean enabled;
        private final List<String> callbacks;
        private Object lastModInstance;

        private RecordingModule(String id, boolean enabled, List<String> callbacks) {
            this.id = id;
            this.enabled = enabled;
            this.callbacks = callbacks;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public void preInit(FMLPreInitializationEvent event) {
            callbacks.add(id + ":" + ModuleBootstrapPhase.PRE_INIT.name());
        }

        @Override
        public void init(FMLInitializationEvent event, Object modInstance) {
            lastModInstance = modInstance;
            callbacks.add(id + ":" + ModuleBootstrapPhase.INIT.name());
        }

        @Override
        public void postInit(FMLPostInitializationEvent event) {
            callbacks.add(id + ":" + ModuleBootstrapPhase.POST_INIT.name());
        }

        @Override
        public void serverStarting(FMLServerStartingEvent event) {
            callbacks.add(id + ":" + ModuleBootstrapPhase.SERVER_STARTING.name());
        }
    }
}
