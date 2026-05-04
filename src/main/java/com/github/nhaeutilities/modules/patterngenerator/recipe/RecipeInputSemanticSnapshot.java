package com.github.nhaeutilities.modules.patterngenerator.recipe;

import net.minecraft.item.ItemStack;

public class RecipeInputSemanticSnapshot {

    public final InputSemanticType type;
    public final int inputIndex;
    public final ItemStack stack;

    public RecipeInputSemanticSnapshot(InputSemanticType type, int inputIndex, ItemStack stack) {
        this.type = type;
        this.inputIndex = inputIndex;
        this.stack = stack;
    }
}
