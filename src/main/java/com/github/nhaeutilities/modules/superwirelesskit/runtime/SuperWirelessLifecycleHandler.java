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
        SuperWirelessDebugLog.log(
            "LIFECYCLE_CHUNK_LOAD",
            "chunk=%d:%d,%d",
            Integer.valueOf(event.world.provider.dimensionId),
            Integer.valueOf(event.getChunk().xPosition),
            Integer.valueOf(event.getChunk().zPosition));
        runtimeManager.refreshChunk(event.getChunk());
    }

    @SubscribeEvent
    public void onWorldUnload(WorldEvent.Unload event) {
        World world = event.world;
        if (world == null || world.isRemote) {
            return;
        }
        SuperWirelessDebugLog.log(
            "LIFECYCLE_WORLD_UNLOAD",
            "world=%d",
            Integer.valueOf(world.provider.dimensionId));
        runtimeManager.onWorldUnload(world);
    }

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        World world = event.world;
        if (event.phase != TickEvent.Phase.END || world == null || world.isRemote) {
            return;
        }

        long totalWorldTime = world.getTotalWorldTime();
        if (totalWorldTime % DEFERRED_REFRESH_INTERVAL_TICKS != 0L) {
            return;
        }

        SuperWirelessDebugLog.log(
            "LIFECYCLE_WORLD_TICK_REFRESH",
            "world=%d time=%d",
            Integer.valueOf(world.provider.dimensionId),
            Long.valueOf(totalWorldTime));
        runtimeManager.refreshDeferredBindings(world);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event == null || event.isCanceled() || event.world == null || event.world.isRemote) {
            return;
        }

        SuperWirelessDebugLog.log(
            "LIFECYCLE_BLOCK_BREAK",
            "block=%d:%d,%d,%d",
            Integer.valueOf(event.world.provider.dimensionId),
            Integer.valueOf(event.x),
            Integer.valueOf(event.y),
            Integer.valueOf(event.z));
        runtimeManager.onBlockBroken(event.world, event.x, event.y, event.z);
    }
}
