package com.github.nhaeutilities.modules.patternrouting.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import com.github.nhaeutilities.modules.patternrouting.core.RecipeMapAnalysisResult;
import com.github.nhaeutilities.modules.patternrouting.gui.GuiPatternRoutingAnalysisState;
import com.github.nhaeutilities.modules.patternrouting.gui.PatternRoutingAnalysisClientScreen;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

public class PacketRecipeMapAnalysisResult implements IMessage {

    private static final int MAX_GROUP_COUNT = 256;

    private String recipeMapId;
    private String errorKey;
    private RecipeMapAnalysisResult result;

    public PacketRecipeMapAnalysisResult() {}

    public PacketRecipeMapAnalysisResult(String recipeMapId, RecipeMapAnalysisResult result) {
        this.recipeMapId = normalize(recipeMapId);
        this.errorKey = "";
        this.result = result;
    }

    public PacketRecipeMapAnalysisResult(String recipeMapId, String errorKey) {
        this.recipeMapId = normalize(recipeMapId);
        this.errorKey = normalize(errorKey);
        this.result = null;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        recipeMapId = ByteBufUtils.readUTF8String(buf);
        errorKey = ByteBufUtils.readUTF8String(buf);
        if (buf.readBoolean()) {
            result = readResult(buf);
        } else {
            result = null;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, normalize(recipeMapId));
        ByteBufUtils.writeUTF8String(buf, normalize(errorKey));
        buf.writeBoolean(result != null);
        if (result != null) {
            writeResult(buf, result);
        }
    }

    private static RecipeMapAnalysisResult readResult(ByteBuf buf) {
        int exactTotalTypeCount = buf.readInt();
        boolean hasIncompleteAnalysis = buf.readBoolean();
        int totalGroupCount = buf.readInt();
        if (totalGroupCount < 0 || totalGroupCount > MAX_GROUP_COUNT) {
            throw new IllegalArgumentException("Invalid recipe-map analysis group count: " + totalGroupCount);
        }
        List<RecipeMapAnalysisResult.RecipeTypeGroup> groups = new ArrayList<RecipeMapAnalysisResult.RecipeTypeGroup>(
            totalGroupCount);
        for (int i = 0; i < totalGroupCount; i++) {
            groups.add(
                new RecipeMapAnalysisResult.RecipeTypeGroup(
                    ByteBufUtils.readUTF8String(buf),
                    ByteBufUtils.readUTF8String(buf),
                    ByteBufUtils.readUTF8String(buf),
                    buf.readInt()));
        }
        return new RecipeMapAnalysisResult(groups, exactTotalTypeCount, hasIncompleteAnalysis);
    }

    private static void writeResult(ByteBuf buf, RecipeMapAnalysisResult result) {
        List<RecipeMapAnalysisResult.RecipeTypeGroup> groups = new ArrayList<RecipeMapAnalysisResult.RecipeTypeGroup>();
        groups.addAll(result.repeatedTypes);
        groups.addAll(result.singleOccurrenceTypes);
        int groupCount = Math.min(groups.size(), MAX_GROUP_COUNT);
        buf.writeInt(result.totalTypeCount);
        buf.writeBoolean(result.hasIncompleteAnalysis);
        buf.writeInt(groupCount);
        for (int i = 0; i < groupCount; i++) {
            RecipeMapAnalysisResult.RecipeTypeGroup group = groups.get(i);
            ByteBufUtils.writeUTF8String(buf, group.circuitKey);
            ByteBufUtils.writeUTF8String(buf, group.manualItemsKey);
            ByteBufUtils.writeUTF8String(buf, group.displaySummary);
            buf.writeInt(group.matchCount);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public static class Handler implements IMessageHandler<PacketRecipeMapAnalysisResult, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketRecipeMapAnalysisResult message, MessageContext ctx) {
            Minecraft minecraft = Minecraft.getMinecraft();
            minecraft.func_152344_a(() -> {
                if (!normalize(message.errorKey).isEmpty()) {
                    GuiPatternRoutingAnalysisState.setError(message.recipeMapId, message.errorKey);
                } else {
                    GuiPatternRoutingAnalysisState.setResult(message.recipeMapId, message.result);
                }
                PatternRoutingAnalysisClientScreen.refreshOpenAnalyzerGuiIfNeeded(minecraft, message.recipeMapId);
            });
            return null;
        }
    }
}
