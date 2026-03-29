package com.github.nhaeutilities.modules.patterngenerator.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;

import com.github.nhaeutilities.modules.patterngenerator.gui.GuiRecipePicker;
import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;

/**
 * Server -> Client: sends a batch of conflict groups.
 */
public class PacketRecipeConflictBatch implements IMessage {

    public int startIndex;
    public int totalConflicts;
    public int maxCandidatesPerGroup;
    public List<String> productNames;
    public List<List<RecipeEntry>> recipeGroups;

    public PacketRecipeConflictBatch() {}

    public PacketRecipeConflictBatch(int startIndex, int totalConflicts, int maxCandidatesPerGroup,
        List<String> productNames, List<List<RecipeEntry>> recipeGroups) {
        this.startIndex = startIndex;
        this.totalConflicts = totalConflicts;
        this.maxCandidatesPerGroup = maxCandidatesPerGroup;
        this.productNames = productNames;
        this.recipeGroups = recipeGroups;
    }

    public static PacketRecipeConflictBatch fromSession(ConflictSession session, int maxGroups) {
        int safeMax = Math.max(1, maxGroups);
        int start = session.getCurrentIndex();
        int end = Math.min(session.getTotalConflicts(), start + safeMax);
        int maxCandidates = 0;

        List<String> names = new ArrayList<String>();
        List<List<RecipeEntry>> groups = new ArrayList<List<RecipeEntry>>();
        for (int i = start; i < end; i++) {
            String key = session.groupKeys.get(i);
            names.add(key != null ? key : "Unknown");
            List<RecipeEntry> recipes = session.conflictGroups.get(key);
            List<RecipeEntry> safeRecipes = recipes != null ? recipes : new ArrayList<RecipeEntry>();
            groups.add(safeRecipes);
            if (safeRecipes.size() > maxCandidates) {
                maxCandidates = safeRecipes.size();
            }
        }

        return new PacketRecipeConflictBatch(
            start + 1,
            session.getTotalConflicts(),
            Math.max(1, maxCandidates),
            names,
            groups);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        startIndex = buf.readInt();
        totalConflicts = buf.readInt();
        maxCandidatesPerGroup = buf.readInt();
        int groupCount = buf.readInt();

        productNames = new ArrayList<String>(groupCount);
        recipeGroups = new ArrayList<List<RecipeEntry>>(groupCount);

        for (int i = 0; i < groupCount; i++) {
            productNames.add(ByteBufUtils.readUTF8String(buf));

            int recipeCount = buf.readInt();
            List<RecipeEntry> recipes = new ArrayList<RecipeEntry>(recipeCount);
            for (int recipeIndex = 0; recipeIndex < recipeCount; recipeIndex++) {
                recipes.add(PacketRecipeConflicts.readRecipe(buf));
            }
            recipeGroups.add(recipes);
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(startIndex);
        buf.writeInt(totalConflicts);
        buf.writeInt(Math.max(1, maxCandidatesPerGroup));

        List<String> safeNames = productNames != null ? productNames : new ArrayList<String>();
        List<List<RecipeEntry>> safeGroups = recipeGroups != null ? recipeGroups : new ArrayList<List<RecipeEntry>>();
        int count = Math.min(safeNames.size(), safeGroups.size());
        buf.writeInt(count);

        for (int i = 0; i < count; i++) {
            ByteBufUtils.writeUTF8String(buf, safeNames.get(i) != null ? safeNames.get(i) : "Unknown");
            List<RecipeEntry> recipes = safeGroups.get(i) != null ? safeGroups.get(i) : new ArrayList<RecipeEntry>();
            buf.writeInt(recipes.size());
            for (RecipeEntry recipe : recipes) {
                PacketRecipeConflicts.writeRecipe(buf, recipe);
            }
        }
    }

    public static class Handler implements IMessageHandler<PacketRecipeConflictBatch, IMessage> {

        @Override
        @SideOnly(Side.CLIENT)
        public IMessage onMessage(PacketRecipeConflictBatch message, MessageContext ctx) {
            Minecraft.getMinecraft()
                .func_152344_a(() -> GuiRecipePicker.openBatch(message));
            return null;
        }
    }
}
