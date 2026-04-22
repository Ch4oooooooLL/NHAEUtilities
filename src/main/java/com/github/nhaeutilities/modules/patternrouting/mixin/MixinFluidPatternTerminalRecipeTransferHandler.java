package com.github.nhaeutilities.modules.patternrouting.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;
import com.github.nhaeutilities.modules.patternrouting.core.PacketRecipeTransferMetadataAccess;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingLog;
import com.github.nhaeutilities.modules.patternrouting.core.RecipeTransferMetadataExtractor;

import codechicken.nei.recipe.IRecipeHandler;
import codechicken.nei.recipe.Recipe;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.common.network.simpleimpl.IMessage;

@Pseudo
@Mixin(targets = "com.glodblock.github.nei.FluidPatternTerminalRecipeTransferHandler", remap = false)
public abstract class MixinFluidPatternTerminalRecipeTransferHandler {

    @Redirect(
        method = "overlayRecipe",
        at = @At(
            value = "INVOKE",
            target = "Lcom/glodblock/github/network/wrapper/FCNetworkWrapper;sendToServer(Lcpw/mods/fml/common/network/simpleimpl/IMessage;)V"),
        remap = false)
    private void nhaeutilities$attachRecipeTransferMetadata(
        com.glodblock.github.network.wrapper.FCNetworkWrapper networkWrapper, IMessage message,
        net.minecraft.client.gui.inventory.GuiContainer firstGui, IRecipeHandler recipe, int recipeIndex,
        boolean shift) {
        if (PatternRoutingRuntime.isEnabled() && recipe != null
            && !isCraftingRecipe(recipe)
            && message instanceof PacketRecipeTransferMetadataAccess) {
            try {
                Recipe.RecipeId recipeId = Recipe.of(recipe, recipeIndex)
                    .getRecipeId();
                if (recipeId != null) {
                    String overlayIdentifier = "";
                    if (recipe instanceof TemplateRecipeHandler) {
                        String current = ((TemplateRecipeHandler) recipe).getOverlayIdentifier();
                        overlayIdentifier = current != null ? current : "";
                    }

                    PacketRecipeTransferMetadataAccess access = (PacketRecipeTransferMetadataAccess) message;
                    String recipeIdJson = recipeId.toJsonObject()
                        .toString();
                    RecipeTransferMetadataExtractor.Metadata metadata = RecipeTransferMetadataExtractor
                        .extract(recipe, recipeIndex, recipeIdJson, overlayIdentifier);
                    access.nhaeutilities$setRecipeId(recipeIdJson);
                    access.nhaeutilities$setOverlayIdentifier(overlayIdentifier);
                    access.nhaeutilities$setProgrammingCircuit(metadata.programmingCircuit);
                    access.nhaeutilities$setNonConsumables(metadata.nonConsumables);
                    access.nhaeutilities$setRecipeSnapshot(metadata.recipeSnapshot);
                    PatternRoutingLog.info(
                        "[NHAEUtilities][patternrouting] AE2FC attach metadata recipeId=%s overlay=%s circuit=%s nc=%s packet=%s",
                        access.nhaeutilities$getRecipeId(),
                        access.nhaeutilities$getOverlayIdentifier(),
                        access.nhaeutilities$getProgrammingCircuit(),
                        access.nhaeutilities$getNonConsumables(),
                        message.getClass()
                            .getName());
                }
            } catch (Throwable t) {
                PatternRoutingLog
                    .warning("[NHAEUtilities][patternrouting] AE2FC attach metadata failed: %s", t.toString());
            }
        }

        ((com.glodblock.github.network.wrapper.FCNetworkWrapper) networkWrapper).sendToServer(message);
    }

    private static boolean isCraftingRecipe(IRecipeHandler recipe) {
        if (!(recipe instanceof TemplateRecipeHandler)) {
            return false;
        }
        String overlayIdentifier = ((TemplateRecipeHandler) recipe).getOverlayIdentifier();
        return "crafting".equals(overlayIdentifier) || "crafting2x2".equals(overlayIdentifier);
    }
}
