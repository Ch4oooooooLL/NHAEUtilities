package com.github.nhaeutilities.modules.patternrouting.network;

import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;
import com.github.nhaeutilities.modules.patternrouting.core.RecipeMapAnalysisResult;
import com.github.nhaeutilities.modules.patternrouting.core.RecipeMapAnalysisService;
import com.github.nhaeutilities.modules.patternrouting.item.ItemRecipeMapAnalyzer;
import com.github.nhaeutilities.modules.shared.recipecache.SharedRecipeCacheService;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class PacketRequestRecipeMapAnalysis implements IMessage {

    public PacketRequestRecipeMapAnalysis() {}

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<PacketRequestRecipeMapAnalysis, IMessage> {

        private static final String ERROR_NO_RECIPE_MAP = "nhaeutilities.gui.pattern_routing_analysis.error.no_recipe_map";
        private static final String ERROR_CACHE_LOAD_FAILED = "nhaeutilities.gui.pattern_routing_analysis.error.cache_load_failed";

        @Override
        public IMessage onMessage(PacketRequestRecipeMapAnalysis message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            String recipeMapId = readRecipeMapId(player);
            if (recipeMapId.isEmpty()) {
                NetworkHandler.sendTo(new PacketRecipeMapAnalysisResult("", ERROR_NO_RECIPE_MAP), player);
                return null;
            }

            logInfo("[NHAEUtilities][SemanticCache] Analysis request start mapId=%s", recipeMapId);
            try {
                List<RecipeEntry> recipes = SharedRecipeCacheService.loadRecipesEnsuringLatest(recipeMapId);
                RecipeMapAnalysisResult result = RecipeMapAnalysisService.analyzeSemanticEntries(recipes);
                logInfo(
                    "[NHAEUtilities][SemanticCache] Analysis request success mapId=%s recipeCount=%d totalTypeCount=%d incomplete=%s",
                    recipeMapId,
                    recipes.size(),
                    result.totalTypeCount,
                    result.hasIncompleteAnalysis);
                NetworkHandler.sendTo(new PacketRecipeMapAnalysisResult(recipeMapId, result), player);
            } catch (RuntimeException e) {
                String errorKey = resolveErrorKey(e);
                logWarning(
                    "[NHAEUtilities][SemanticCache] Analysis request failed mapId=%s exception=%s message=%s errorKey=%s",
                    recipeMapId,
                    e.getClass()
                        .getSimpleName(),
                    e.getMessage() != null ? e.getMessage() : "",
                    errorKey);
                NetworkHandler.sendTo(new PacketRecipeMapAnalysisResult(recipeMapId, errorKey), player);
            }
            return null;
        }

        private static String readRecipeMapId(EntityPlayerMP player) {
            if (player == null) {
                return "";
            }
            ItemStack heldItem = player.getCurrentEquippedItem();
            return ItemRecipeMapAnalyzer.getStoredRecipeMap(heldItem);
        }

        private static void logInfo(String format, Object... args) {
            try {
                FMLLog.info(format, args);
            } catch (RuntimeException ignored) {}
        }

        private static void logWarning(String format, Object... args) {
            try {
                FMLLog.warning(format, args);
            } catch (RuntimeException ignored) {}
        }

        private static String resolveErrorKey(RuntimeException error) {
            String message = error != null && error.getMessage() != null ? error.getMessage()
                .trim() : "";
            if (message.startsWith("nhaeutilities.")) {
                return message;
            }
            return ERROR_CACHE_LOAD_FAILED;
        }
    }
}
