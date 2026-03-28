package com.github.nhaeutilities.modules.superwirelesskit.runtime;

import net.minecraftforge.common.MinecraftForge;

public final class SuperWirelessServices {

    private static final BindingDataStore DATA_STORE = new BindingDataStore();
    private static final BindingNodeResolver NODE_RESOLVER = new BindingNodeResolver();
    private static final SuperWirelessRuntimeManager RUNTIME_MANAGER = new SuperWirelessRuntimeManager(
        DATA_STORE,
        NODE_RESOLVER);

    private static boolean bootstrapComplete;

    private SuperWirelessServices() {}

    public static void bootstrap() {
        if (bootstrapComplete) {
            return;
        }

        MinecraftForge.EVENT_BUS.register(new SuperWirelessLifecycleHandler(RUNTIME_MANAGER));
        bootstrapComplete = true;
    }

    public static SuperWirelessRuntimeManager runtimeManager() {
        return RUNTIME_MANAGER;
    }

    public static BindingNodeResolver resolver() {
        return NODE_RESOLVER;
    }
}
