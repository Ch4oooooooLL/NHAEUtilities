package com.github.nhaeutilities.modules.patternrouting.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.github.nhaeutilities.modules.patternrouting.service.BookmarkBatchEncoder;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketBatchEncodeBookmarks implements IMessage {

    private static final int MAX_ITEMS = 256;

    private List<ItemStack> items;

    public PacketBatchEncodeBookmarks() {
        items = new ArrayList<>();
    }

    public PacketBatchEncodeBookmarks(List<ItemStack> items) {
        this.items = items != null ? new ArrayList<>(items) : new ArrayList<ItemStack>();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        if (count < 0 || count > MAX_ITEMS) {
            count = 0;
        }
        items = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ItemStack stack = ByteBufUtils.readItemStack(buf);
            if (stack != null) {
                items.add(stack);
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        int count = Math.min(items.size(), MAX_ITEMS);
        buf.writeInt(count);
        for (int i = 0; i < count; i++) {
            ByteBufUtils.writeItemStack(buf, items.get(i));
        }
    }

    public static class Handler implements IMessageHandler<PacketBatchEncodeBookmarks, IMessage> {

        @Override
        public IMessage onMessage(PacketBatchEncodeBookmarks message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            BookmarkBatchEncoder.BatchEncodeResult result = BookmarkBatchEncoder.encodeBatch(player, message.items);
            return new PacketBatchEncodeResult(result);
        }
    }
}
