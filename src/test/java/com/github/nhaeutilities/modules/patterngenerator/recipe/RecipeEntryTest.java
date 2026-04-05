package com.github.nhaeutilities.modules.patterngenerator.recipe;

import static org.junit.Assert.assertEquals;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.junit.Test;

public class RecipeEntryTest {

    @Test
    public void recipeIdDefaultsToEmptyString() {
        RecipeEntry entry = new RecipeEntry(
            "gt",
            "gt.recipe.assembler",
            "Assembler",
            new ItemStack[0],
            new ItemStack[0],
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            100,
            30);

        assertEquals("", entry.recipeId);
    }

    @Test
    public void recipeIdKeepsExplicitValue() {
        RecipeEntry entry = new RecipeEntry(
            "gt",
            "gt.recipe.assembler",
            "Assembler",
            new ItemStack[0],
            new ItemStack[0],
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            100,
            30,
            "recipe-123");

        assertEquals("recipe-123", entry.recipeId);
    }
}
