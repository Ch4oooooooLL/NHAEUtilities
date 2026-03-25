package com.github.nhaeutilities.core.module;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public class ModuleRegistryTest {

    @Test
    public void onlyEnabledModulesAreReturned() {
        ModuleRegistry registry = new ModuleRegistry();

        registry.register(new FakeModule("alpha", true));
        registry.register(new FakeModule("beta", false));
        registry.register(new FakeModule("gamma", true));

        assertEquals(Arrays.asList("alpha", "gamma"), moduleIds(registry.getEnabledModules()));
    }

    @Test
    public void registryPreservesStableRegistrationOrder() {
        ModuleRegistry registry = new ModuleRegistry();

        registry.register(new FakeModule("zeta", true));
        registry.register(new FakeModule("alpha", true));
        registry.register(new FakeModule("mu", true));

        assertEquals(Arrays.asList("zeta", "alpha", "mu"), moduleIds(registry.getEnabledModules()));
    }

    private static List<String> moduleIds(List<ModuleDefinition> modules) {
        return modules.stream().map(ModuleDefinition::id).collect(Collectors.toList());
    }

    private static final class FakeModule implements ModuleDefinition {

        private final String id;
        private final boolean enabled;

        private FakeModule(String id, boolean enabled) {
            this.id = id;
            this.enabled = enabled;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public void preInit(FMLPreInitializationEvent event) {}

        @Override
        public void init(FMLInitializationEvent event, Object modInstance) {}

        @Override
        public void postInit(FMLPostInitializationEvent event) {}

        @Override
        public void serverStarting(FMLServerStartingEvent event) {}
    }
}
