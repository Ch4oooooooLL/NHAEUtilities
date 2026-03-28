package com.github.nhaeutilities.modules.patterngenerator.network;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * Server -> Client: cache build lifecycle progress.
 */
public class PacketCacheProgress implements IMessage {

    public static final String STAGE_STARTED = "started";
    public static final String STAGE_ALREADY_RUNNING = "already_running";
    public static final String STAGE_PROGRESS = "progress";
    public static final String STAGE_ERROR = "error";

    private String stage;
    private String detail;
    private int current;
    private int total;

    public PacketCacheProgress() {}

    public PacketCacheProgress(String stage, String detail, int current, int total) {
        this.stage = stage;
        this.detail = detail;
        this.current = current;
        this.total = total;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        stage = ByteBufUtils.readUTF8String(buf);
        detail = ByteBufUtils.readUTF8String(buf);
        current = buf.readInt();
        total = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, stage != null ? stage : "");
        ByteBufUtils.writeUTF8String(buf, detail != null ? detail : "");
        buf.writeInt(current);
        buf.writeInt(total);
    }

    public static class Handler implements IMessageHandler<PacketCacheProgress, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketCacheProgress message, MessageContext ctx) {
            Minecraft.getMinecraft().func_152344_a(() -> {
                if (Minecraft.getMinecraft().thePlayer == null) {
                    return;
                }

                String text;
                switch (message.stage) {
                    case STAGE_STARTED:
                        text = EnumChatFormatting.GRAY + I18nUtil.tr("nhaeutilities.msg.cache.build_started");
                        break;
                    case STAGE_ALREADY_RUNNING:
                        text = EnumChatFormatting.YELLOW + I18nUtil.tr("nhaeutilities.msg.cache.build_already_running");
                        break;
                    case STAGE_PROGRESS:
                        text = EnumChatFormatting.GRAY
                            + I18nUtil.tr("nhaeutilities.msg.cache.progress", message.detail, message.current, message.total);
                        break;
                    case STAGE_ERROR:
                    default:
                        text = EnumChatFormatting.RED
                            + I18nUtil.tr("nhaeutilities.msg.cache.build_failed", message.detail);
                        break;
                }

                Minecraft.getMinecraft().thePlayer.addChatMessage(new ChatComponentText(text));
            });
            return null;
        }
    }
}
