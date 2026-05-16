package com.github.nhaeutilities.modules.patterngenerator.network;

import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;

import com.github.nhaeutilities.modules.patterngenerator.storage.PatternStorage;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketRequestStorageDetail implements IMessage {

    private int patternIndex;

    public PacketRequestStorageDetail() {}

    public PacketRequestStorageDetail(int patternIndex) {
        this.patternIndex = patternIndex;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        patternIndex = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(patternIndex);
    }

    public static class Handler implements IMessageHandler<PacketRequestStorageDetail, IMessage> {

        @Override
        public IMessage onMessage(PacketRequestStorageDetail message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            UUID uuid = player.getUniqueID();
            PatternStorage.PatternDetail detail = PatternStorage.getPatternDetail(uuid, message.patternIndex);
            return new PacketStorageDetailResult(message.patternIndex, detail);
        }
    }
}
