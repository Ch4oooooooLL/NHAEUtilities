package com.github.nhaeutilities.modules.superwirelesskit.runtime;

import java.util.IdentityHashMap;
import java.util.Map;

import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;

import com.github.nhaeutilities.modules.superwirelesskit.data.SuperWirelessSavedData;
import com.github.nhaeutilities.modules.superwirelesskit.service.BindingRegistry;

public final class BindingDataStore {

    private final Map<World, BindingRegistry> registries = new IdentityHashMap<World, BindingRegistry>();

    public BindingRegistry getRegistry(World world) {
        if (world == null || world.isRemote) {
            throw new IllegalArgumentException("world must be a server world");
        }

        BindingRegistry existing = registries.get(world);
        if (existing != null) {
            return existing;
        }

        BindingRegistry created = new BindingRegistry(loadOrCreate(world));
        registries.put(world, created);
        return created;
    }

    public void clearWorld(World world) {
        registries.remove(world);
    }

    private static SuperWirelessSavedData loadOrCreate(World world) {
        WorldSavedData loaded = world.mapStorage
            .loadData(SuperWirelessSavedData.class, SuperWirelessSavedData.DATA_NAME);
        if (loaded instanceof SuperWirelessSavedData) {
            return (SuperWirelessSavedData) loaded;
        }

        SuperWirelessSavedData created = new SuperWirelessSavedData();
        world.mapStorage.setData(SuperWirelessSavedData.DATA_NAME, created);
        return created;
    }
}
