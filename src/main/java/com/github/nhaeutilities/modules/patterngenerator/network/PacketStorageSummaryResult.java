package com.github.nhaeutilities.modules.patterngenerator.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import com.github.nhaeutilities.modules.patterngenerator.gui.GuiPatternStorage;
import com.github.nhaeutilities.modules.patterngenerator.storage.PatternStorage;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class PacketStorageSummaryResult implements IMessage {

    private static final int MAX_PREVIEWS = 512;

    private int count;
    private String source;
    private long timestamp;
    private List<String> previews;

    public PacketStorageSummaryResult() {
        count = 0;
        source = "";
        timestamp = 0;
        previews = new ArrayList<String>();
    }

    public PacketStorageSummaryResult(PatternStorage.StorageSummary summary) {
        this.count = summary.count;
        this.source = summary.source;
        this.timestamp = summary.timestamp;
        this.previews = summary.previews;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        count = buf.readInt();
        source = ByteBufUtils.readUTF8String(buf);
        timestamp = buf.readLong();
        int previewCount = Math.min(buf.readInt(), MAX_PREVIEWS);
        previews = new ArrayList<String>(previewCount);
        for (int i = 0; i < previewCount; i++) {
            previews.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(count);
        ByteBufUtils.writeUTF8String(buf, source != null ? source : "");
        buf.writeLong(timestamp);
        int previewCount = Math.min(previews != null ? previews.size() : 0, MAX_PREVIEWS);
        buf.writeInt(previewCount);
        for (int i = 0; i < previewCount; i++) {
            ByteBufUtils.writeUTF8String(buf, previews.get(i));
        }
    }

    public PatternStorage.StorageSummary toSummary() {
        return new PatternStorage.StorageSummary(count, source, timestamp, previews);
    }

    public static class Handler implements IMessageHandler<PacketStorageSummaryResult, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketStorageSummaryResult message, MessageContext ctx) {
            Minecraft.getMinecraft()
                .func_152344_a(() -> GuiPatternStorage.rebuildWithSummary(message.toSummary()));
            return null;
        }
    }
}
