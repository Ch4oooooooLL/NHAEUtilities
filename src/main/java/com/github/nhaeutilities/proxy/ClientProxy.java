package com.github.nhaeutilities.proxy;

import com.github.nhaeutilities.core.config.CoreConfig;
import com.github.nhaeutilities.core.module.ModuleRegistry;

public class ClientProxy extends CommonProxy {

    @Override
    protected void registerBuiltInModules(CoreConfig coreConfig, ModuleRegistry moduleRegistry) {
        super.registerBuiltInModules(coreConfig, moduleRegistry);
    }
}
