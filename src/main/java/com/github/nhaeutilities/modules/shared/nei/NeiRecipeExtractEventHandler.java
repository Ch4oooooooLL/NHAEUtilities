package com.github.nhaeutilities.modules.shared.nei;

import java.util.List;

import com.github.nhaeutilities.modules.shared.DebugLog;

import codechicken.nei.recipe.GuiRecipeButton;
import codechicken.nei.recipe.GuiRecipeButton.UpdateRecipeButtonsEvent;
import codechicken.nei.recipe.NEIRecipeWidget;
import codechicken.nei.recipe.RecipeHandlerRef;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class NeiRecipeExtractEventHandler {

    private static final int BUTTON_GAP = 1;

    @SubscribeEvent
    public void onRecipeButtonsUpdate(UpdateRecipeButtonsEvent.Post event) {
        boolean active = NeiRecipeExtractionContext.instance()
            .isActive();
        DebugLog.info("[NHAE] NeiRecipeExtractEventHandler.onRecipeButtonsUpdate fired: active=%s", active);
        if (!active) {
            return;
        }

        NEIRecipeWidget widget = event.recipeWidget;
        if (widget == null) {
            DebugLog.info("[NHAE] NeiRecipeExtractEventHandler: widget is null, skipping");
            return;
        }

        RecipeHandlerRef handlerRef = widget.getRecipeHandlerRef();
        if (handlerRef == null || handlerRef.handler == null) {
            DebugLog.info("[NHAE] NeiRecipeExtractEventHandler: handlerRef is null, skipping");
            return;
        }

        List<GuiRecipeButton> buttons = event.buttonList;
        int x = computeButtonX(widget, buttons);
        int y = computeButtonY(widget, buttons);

        DebugLog.info(
            "[NHAE] NeiRecipeExtractEventHandler: adding '+' button at (%d, %d), handler=%s, recipeIndex=%d",
            x,
            y,
            handlerRef.handler.getRecipeName(),
            handlerRef.recipeIndex);
        buttons.add(new NeiRecipeExtractButton(handlerRef, x, y));
    }

    private static int computeButtonX(NEIRecipeWidget widget, List<GuiRecipeButton> buttons) {
        int x = Math.min(168, widget.w) - GuiRecipeButton.BUTTON_WIDTH;
        if (!buttons.isEmpty()) {
            GuiRecipeButton last = buttons.get(buttons.size() - 1);
            x = last.xPosition;
        }
        return x;
    }

    private static int computeButtonY(NEIRecipeWidget widget, List<GuiRecipeButton> buttons) {
        int y = widget.h - GuiRecipeButton.BUTTON_HEIGHT - 6;
        if (!buttons.isEmpty()) {
            GuiRecipeButton last = buttons.get(buttons.size() - 1);
            y = last.yPosition - GuiRecipeButton.BUTTON_HEIGHT - BUTTON_GAP;
        }
        return y;
    }
}
