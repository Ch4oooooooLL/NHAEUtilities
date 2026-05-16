package com.github.nhaeutilities.modules.patterngenerator.network;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;

import com.github.nhaeutilities.modules.patterngenerator.storage.PatternStorage;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketRequestStorageSummary implements IMessage {

    public PacketRequestStorageSummary() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketRequestStorageSummary, IMessage> {

        @Override
        public IMessage onMessage(PacketRequestStorageSummary message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            UUID uuid = player.getUniqueID();
            PatternStorage.StorageSummary summary = PatternStorage.getSummary(uuid);
            return new PacketStorageSummaryResult(summary);
        }
    }
}
