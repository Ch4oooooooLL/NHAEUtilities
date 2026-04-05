package com.github.nhaeutilities.modules.superwirelesskit.runtime;

import net.minecraft.world.World;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

public class SuperWirelessLifecycleHandler {

    private static final long DEFERRED_REFRESH_INTERVAL_TICKS = 20L;

    private final SuperWirelessRuntimeManager runtimeManager;

    public SuperWirelessLifecycleHandler(SuperWirelessRuntimeManager runtimeManager) {
        this.runtimeManager = runtimeManager;
    }

    @SubscribeEvent
    public void onChunkLoad(ChunkEvent.Load event) {
        if (event.world == null || event.world.isRemote) {
            return;
        }
        runtimeManager.refreshChunk(event.getChunk());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        World world = event.world;
        if (world == null || world.isRemote) {
            return;
        }
        runtimeManager.onWorldUnload(world);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        World world = event.world;
        if (event.phase != TickEvent.Phase.END || world == null || world.isRemote) {
            return;
        }

        if (world.getTotalWorldTime() % DEFERRED_REFRESH_INTERVAL_TICKS != 0L) {
            return;
        }

        runtimeManager.refreshDeferredBindings(world);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event == null || event.isCanceled() || event.world == null || event.world.isRemote) {
            return;
        }

        runtimeManager.onBlockBroken(event.world, event.x, event.y, event.z);
    }
}
