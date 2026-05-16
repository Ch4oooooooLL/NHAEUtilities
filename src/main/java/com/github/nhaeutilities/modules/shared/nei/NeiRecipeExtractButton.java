package com.github.nhaeutilities.modules.shared.nei;

import java.util.List;
import java.util.Map;

import com.github.nhaeutilities.modules.patternrouting.core.RecipeTransferMetadataExtractor;
import com.github.nhaeutilities.modules.shared.DebugLog;

import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.RecipeHandlerRef;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class NeiRecipeExtractButton extends GuiRecipeButton {

    NeiRecipeExtractButton(RecipeHandlerRef handlerRef, int x, int y) {
        super(handlerRef, x, y, handlerRef.recipeIndex + 64, "+");
        this.enabled = true;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        DebugLog.info("[NHAE] NeiRecipeExtractButton.mouseReleased called");
        NeiRecipeExtractionContext ctx = NeiRecipeExtractionContext.instance();
        if (!ctx.isActive()) {
            DebugLog.info("[NHAE] NeiRecipeExtractButton: context not active, skipping");
            return;
        }

        NeiRecipeExtractionCallback callback = ctx.getCallback();
        if (callback == null) {
            DebugLog.info("[NHAE] NeiRecipeExtractButton: callback is null, skipping");
            return;
        }

        NeiRecipeData data = extractData();
        if (data != null) {
            DebugLog.info(
                "[NHAE] NeiRecipeExtractButton: extracted data, recipeMapId=%s, recipeName=%s",
                data.recipeMapId,
                data.recipeName);
            callback.onRecipeExtracted(data);
        } else {
            DebugLog.info("[NHAE] NeiRecipeExtractButton: extractData returned null");
        }
    }

    private NeiRecipeData extractData() {
        RecipeHandlerRef ref = this.handlerRef;
        if (ref == null || ref.handler == null) {
            return null;
        }

        String recipeMapId = "";
        if (ref.handler instanceof TemplateRecipeHandler) {
            String overlayId = ((TemplateRecipeHandler) ref.handler).getOverlayIdentifier();
            recipeMapId = overlayId != null ? overlayId : "";
        }

        String recipeName = ref.handler.getRecipeName();
        if (recipeName == null) {
            recipeName = "";
        }

        RecipeTransferMetadataExtractor.Metadata snapshot = RecipeTransferMetadataExtractor
            .extract(ref.handler, ref.recipeIndex, recipeMapId);

        return new NeiRecipeData(recipeMapId, recipeName.trim(), ref.handler, ref.recipeIndex, snapshot);
    }

    @Override
    public List<String> handleTooltip(List<String> currenttip) {
        String name = handlerRef.handler.getRecipeName();
        if (name != null && !name.isEmpty()) {
            currenttip.add(name);
        }
        return currenttip;
    }

    @Override
    public Map<String, String> handleHotkeys(int mousex, int mousey, Map<String, String> hotkeys) {
        return hotkeys;
    }

    @Override
    public void lastKeyTyped(char keyChar, int keyID) {}

    @Override
    public void drawItemOverlay() {}
}
