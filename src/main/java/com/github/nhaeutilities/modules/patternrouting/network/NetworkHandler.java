package com.github.nhaeutilities.modules.patternrouting.network;

import net.minecraft.entity.player.EntityPlayerMP;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class NetworkHandler {

    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel("nhae_pr");

    private static int packetId = 0;
    private static boolean initialized = false;

    private NetworkHandler() {}

    public static synchronized void init() {
        if (initialized) {
            return;
        }
        INSTANCE.registerMessage(
            PacketRequestRecipeMapAnalysis.Handler.class,
            PacketRequestRecipeMapAnalysis.class,
            packetId++,
            Side.SERVER);
        INSTANCE.registerMessage(
            PacketRecipeMapAnalysisResult.Handler.class,
            PacketRecipeMapAnalysisResult.class,
            packetId++,
            Side.CLIENT);
        initialized = true;
    }

    public static void sendToServer(IMessage packet) {
        INSTANCE.sendToServer(packet);
    }

    public static void sendTo(IMessage packet, EntityPlayerMP player) {
        INSTANCE.sendTo(packet, player);
    }
}
