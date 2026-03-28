package com.github.nhaeutilities.modules.patterngenerator.network;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;

import com.github.nhaeutilities.modules.patterngenerator.config.ForgeConfig;
import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;

/**
 * Coordinates batched conflict resolution.
 */
public final class ConflictResolutionService {

    private ConflictResolutionService() {}

    public static int currentServerStartIndex(ConflictSession session) {
        return session.getCurrentIndex() + 1;
    }

    public static void sendCurrentBatch(EntityPlayerMP player, ConflictSession session) {
        PacketRecipeConflictBatch batchPacket = PacketRecipeConflictBatch
            .fromSession(session, ForgeConfig.getConflictBatchSize());
        NetworkHandler.INSTANCE.sendTo(batchPacket, player);
    }

    public static List<RecipeEntry> collectFinalRecipes(ConflictSession session) {
        List<RecipeEntry> finalRecipes = new ArrayList<RecipeEntry>(session.nonConflictingRecipes);
        for (String key : session.groupKeys) {
            Integer index = session.selections.get(key);
            if (index != null) {
                finalRecipes.add(session.conflictGroups.get(key).get(index));
            }
        }
        return finalRecipes;
    }

    public static void finalizeSession(EntityPlayerMP player, ConflictSession session) {
        List<RecipeEntry> finalRecipes = collectFinalRecipes(session);
        PatternGenerationService.generateAndStore(player, session.recipeMapId, finalRecipes);
    }
}
