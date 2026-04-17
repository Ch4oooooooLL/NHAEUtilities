package com.github.nhaeutilities.modules.patternrouting.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;
import com.github.nhaeutilities.modules.patternrouting.core.PacketRecipeTransferMetadataAccess;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingLog;
import com.github.nhaeutilities.modules.patternrouting.core.RecipeTransferMetadataExtractor;

import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.TemplateRecipeHandler;

@Pseudo
@Mixin(targets = "com.github.vfyjxf.nee.nei.NEEPatternTerminalHandler", remap = false)
public abstract class MixinNEEPatternTerminalHandler {

    @Inject(method = "packRecipe", at = @At("RETURN"), remap = false)
    private void nhaeutilities$attachRecipeTransferMetadata(IRecipeHandler recipe, int recipeIndex, int multiplier,
        CallbackInfoReturnable<Object> cir) {
        Object packet = cir.getReturnValue();
        if (!PatternRoutingRuntime.isEnabled() || packet == null
            || isCraftingRecipe(recipe)
            || !(packet instanceof PacketRecipeTransferMetadataAccess)) {
            return;
        }

        try {
            Recipe.RecipeId recipeId = Recipe.of(recipe, recipeIndex)
                .getRecipeId();
            if (recipeId == null) {
                return;
            }

            String overlayIdentifier = "";
            if (recipe instanceof TemplateRecipeHandler) {
                String current = ((TemplateRecipeHandler) recipe).getOverlayIdentifier();
                overlayIdentifier = current != null ? current : "";
            }

            PacketRecipeTransferMetadataAccess accessor = (PacketRecipeTransferMetadataAccess) packet;
            String recipeIdJson = recipeId.toJsonObject()
                .toString();
            RecipeTransferMetadataExtractor.Metadata metadata = RecipeTransferMetadataExtractor
                .extract(recipe, recipeIndex, recipeIdJson, overlayIdentifier);
            accessor.nhaeutilities$setRecipeId(recipeIdJson);
            accessor.nhaeutilities$setOverlayIdentifier(overlayIdentifier);
            accessor.nhaeutilities$setProgrammingCircuit(metadata.programmingCircuit);
            accessor.nhaeutilities$setNonConsumables(metadata.nonConsumables);
            accessor.nhaeutilities$setRecipeSnapshot(metadata.recipeSnapshot);
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] NEE attach metadata recipeId=%s overlay=%s circuit=%s nc=%s packet=%s",
                accessor.nhaeutilities$getRecipeId(),
                accessor.nhaeutilities$getOverlayIdentifier(),
                accessor.nhaeutilities$getProgrammingCircuit(),
                accessor.nhaeutilities$getNonConsumables(),
                packet.getClass()
                    .getName());
        } catch (Throwable t) {
            PatternRoutingLog.warning("[NHAEUtilities][patternrouting] NEE attach metadata failed: %s", t.toString());
        }
    }

    private static boolean isCraftingRecipe(IRecipeHandler recipe) {
        if (!(recipe instanceof TemplateRecipeHandler)) {
            return false;
        }
        String overlayIdentifier = ((TemplateRecipeHandler) recipe).getOverlayIdentifier();
        return "crafting".equals(overlayIdentifier) || "crafting2x2".equals(overlayIdentifier);
    }
}
