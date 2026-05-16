package com.github.nhaeutilities.modules.patterngenerator.gui;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.integration.nei.NEIDragAndDropHandler;
import com.cleanroommc.modularui.widgets.ButtonWidget;

class FilterDragChoiceButtonWidget extends ButtonWidget<FilterDragChoiceButtonWidget> implements NEIDragAndDropHandler {

    private final FilterTextFieldWidget delegate;

    FilterDragChoiceButtonWidget(FilterTextFieldWidget delegate) {
        this.delegate = delegate;
    }

    @Override
    public @NotNull Result onMousePressed(int mouseButton) {
        if (delegate == null) return Result.IGNORE;
        int direction = mouseButton == 1 ? -1 : 1;
        delegate.cycleCategory(direction);
        return Result.SUCCESS;
    }

    @Override
    public boolean handleDragAndDrop(@NotNull ItemStack draggedStack, int button) {
        return delegate != null && delegate.handleDragAndDrop(draggedStack, button);
    }
}
