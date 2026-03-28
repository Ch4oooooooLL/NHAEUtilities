package com.github.nhaeutilities.modules.patterngenerator.network;

import com.github.nhaeutilities.NHAEUtilities;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/**
 * Pattern generator packet registry.
 */
public final class NetworkHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(NHAEUtilities.MODID);

    private static int packetId = 0;
    private static boolean initialized = false;

    private NetworkHandler() {}

    public static synchronized void init() {
        if (initialized) {
            return;
        }

        INSTANCE.registerMessage(PacketCreateCache.Handler.class, PacketCreateCache.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(
            PacketPreviewRecipeCount.Handler.class,
            PacketPreviewRecipeCount.class,
            packetId++,
            Side.SERVER);
        INSTANCE.registerMessage(
            PacketGeneratePatterns.Handler.class,
            PacketGeneratePatterns.class,
            packetId++,
            Side.SERVER);
        INSTANCE.registerMessage(PacketSaveFields.Handler.class, PacketSaveFields.class, packetId++, Side.SERVER);
        INSTANCE.registerMessage(PacketStorageAction.Handler.class, PacketStorageAction.class, packetId++, Side.SERVER);
        INSTANCE
            .registerMessage(PacketRecipeConflicts.Handler.class, PacketRecipeConflicts.class, packetId++, Side.CLIENT);
        INSTANCE.registerMessage(
            PacketPreviewRecipeCountResult.Handler.class,
            PacketPreviewRecipeCountResult.class,
            packetId++,
            Side.CLIENT);
        INSTANCE.registerMessage(PacketCacheProgress.Handler.class, PacketCacheProgress.class, packetId++, Side.CLIENT);
        INSTANCE
            .registerMessage(PacketCacheStatistics.Handler.class, PacketCacheStatistics.class, packetId++, Side.CLIENT);
        INSTANCE.registerMessage(
            PacketResolveConflicts.Handler.class,
            PacketResolveConflicts.class,
            packetId++,
            Side.SERVER);
        INSTANCE.registerMessage(
            PacketRecipeConflictBatch.Handler.class,
            PacketRecipeConflictBatch.class,
            packetId++,
            Side.CLIENT);
        INSTANCE.registerMessage(
            PacketResolveConflictsBatch.Handler.class,
            PacketResolveConflictsBatch.class,
            packetId++,
            Side.SERVER);

        initialized = true;
    }

    public static void sendToServer(IMessage packet) {
        INSTANCE.sendToServer(packet);
    }
}
