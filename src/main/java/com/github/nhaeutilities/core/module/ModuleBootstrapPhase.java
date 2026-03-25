package com.github.nhaeutilities.core.module;

import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;

public enum ModuleBootstrapPhase {
    PRE_INIT {
        @Override
        void dispatch(ModuleDefinition module, Object event, Object modInstance) {
            module.preInit((FMLPreInitializationEvent) event);
        }
    },
    INIT {
        @Override
        void dispatch(ModuleDefinition module, Object event, Object modInstance) {
            module.init((FMLInitializationEvent) event, modInstance);
        }
    },
    POST_INIT {
        @Override
        void dispatch(ModuleDefinition module, Object event, Object modInstance) {
            module.postInit((FMLPostInitializationEvent) event);
        }
    },
    SERVER_STARTING {
        @Override
        void dispatch(ModuleDefinition module, Object event, Object modInstance) {
            module.serverStarting((FMLServerStartingEvent) event);
        }
    };

    abstract void dispatch(ModuleDefinition module, Object event, Object modInstance);
}
