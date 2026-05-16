package com.github.nhaeutilities.modules.patterngenerator.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import com.github.nhaeutilities.modules.patterngenerator.storage.PatternStorage;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class PacketStorageDetailResult implements IMessage {

    private static final int MAX_ITEMS = 256;

    private int patternIndex;
    private List<String> inputs;
    private List<String> outputs;

    public PacketStorageDetailResult() {
        patternIndex = -1;
        inputs = new ArrayList<String>();
        outputs = new ArrayList<String>();
    }

    public PacketStorageDetailResult(int patternIndex, PatternStorage.PatternDetail detail) {
        this.patternIndex = patternIndex;
        this.inputs = detail != null ? detail.inputs : new ArrayList<String>();
        this.outputs = detail != null ? detail.outputs : new ArrayList<String>();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        patternIndex = buf.readInt();
        int inCount = Math.min(buf.readInt(), MAX_ITEMS);
        inputs = new ArrayList<String>(inCount);
        for (int i = 0; i < inCount; i++) {
            inputs.add(ByteBufUtils.readUTF8String(buf));
        }
        int outCount = Math.min(buf.readInt(), MAX_ITEMS);
        outputs = new ArrayList<String>(outCount);
        for (int i = 0; i < outCount; i++) {
            outputs.add(ByteBufUtils.readUTF8String(buf));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(patternIndex);
        int inCount = Math.min(inputs != null ? inputs.size() : 0, MAX_ITEMS);
        buf.writeInt(inCount);
        for (int i = 0; i < inCount; i++) {
            ByteBufUtils.writeUTF8String(buf, inputs.get(i));
        }
        int outCount = Math.min(outputs != null ? outputs.size() : 0, MAX_ITEMS);
        buf.writeInt(outCount);
        for (int i = 0; i < outCount; i++) {
            ByteBufUtils.writeUTF8String(buf, outputs.get(i));
        }
    }

    public int getPatternIndex() {
        return patternIndex;
    }

    public List<String> getInputs() {
        return inputs;
    }

    public List<String> getOutputs() {
        return outputs;
    }

    public static class Handler implements IMessageHandler<PacketStorageDetailResult, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketStorageDetailResult message, MessageContext ctx) {
            Minecraft.getMinecraft()
                .func_152344_a(
                    () -> com.github.nhaeutilities.modules.patterngenerator.gui.GuiPatternDetail
                        .openFromNetwork(message.getPatternIndex(), message.getInputs(), message.getOutputs()));
            return null;
        }
    }
}
