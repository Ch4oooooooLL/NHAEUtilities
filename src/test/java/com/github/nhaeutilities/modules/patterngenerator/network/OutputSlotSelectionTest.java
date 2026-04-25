package com.github.nhaeutilities.modules.patterngenerator.network;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;

public class OutputSlotSelectionTest {

    @BeforeClass
    public static void initializeMinecraftBootstrap() {
        try {
            Class<?> bootstrap = Class.forName("net.minecraft.init.Bootstrap");
            try {
                bootstrap.getMethod("register")
                    .invoke(null);
                return;
            } catch (NoSuchMethodException ignored) {}

            bootstrap.getMethod("func_151354_b")
                .invoke(null);
        } catch (Exception ignored) {}
    }

    @Test
    public void blankSelectionReturnsOriginalRecipeUnchanged() {
        OutputSlotSelection selection = OutputSlotSelection.parse("   ");
        RecipeEntry recipe = recipeWithOutputs(new ItemStack(Items.iron_ingot), new ItemStack(Items.gold_ingot));

        RecipeEntry filtered = selection.apply(recipe);

        assertSame(recipe, filtered);
    }

    @Test
    public void parsingTrimsWhitespaceAroundTokens() {
        OutputSlotSelection selection = OutputSlotSelection.parse(" 1 , 2 ");
        RecipeEntry recipe = recipeWithOutputs(new ItemStack(Items.iron_ingot), new ItemStack(Items.gold_ingot));

        RecipeEntry filtered = selection.apply(recipe);

        assertEquals(2, filtered.outputs.length);
        assertSame(recipe.outputs[0], filtered.outputs[0]);
        assertSame(recipe.outputs[1], filtered.outputs[1]);
    }

    @Test
    public void applyKeepsRecipeOutputOrderInsteadOfUserEnteredOrder() {
        OutputSlotSelection selection = OutputSlotSelection.parse("2, 1");
        RecipeEntry recipe = recipeWithOutputs(
            new ItemStack(Items.iron_ingot),
            new ItemStack(Items.gold_ingot),
            new ItemStack(Items.diamond));

        RecipeEntry filtered = selection.apply(recipe);

        assertEquals("recipe-42", filtered.recipeId);
        assertEquals(recipe.sourceType, filtered.sourceType);
        assertEquals(recipe.recipeMapId, filtered.recipeMapId);
        assertEquals(recipe.machineDisplayName, filtered.machineDisplayName);
        assertSame(recipe.inputs, filtered.inputs);
        assertSame(recipe.fluidInputs, filtered.fluidInputs);
        assertSame(recipe.fluidOutputs, filtered.fluidOutputs);
        assertSame(recipe.specialItems, filtered.specialItems);
        assertEquals(recipe.duration, filtered.duration);
        assertEquals(recipe.euPerTick, filtered.euPerTick);

        assertArrayEquals(new ItemStack[] { recipe.outputs[0], recipe.outputs[1] }, filtered.outputs);
    }

    @Test
    public void duplicateValuesAreRejected() {
        assertSelectionErrorKey("1, 1", "nhaeutilities.msg.generate.output_slots_duplicate");
    }

    @Test
    public void invalidTokensAreRejected() {
        assertSelectionErrorKey("1,,2", "nhaeutilities.msg.generate.output_slots_invalid");
        assertSelectionErrorKey("a", "nhaeutilities.msg.generate.output_slots_invalid");
        assertSelectionErrorKey("0", "nhaeutilities.msg.generate.output_slots_invalid");
    }

    @Test
    public void missingOrNullRequestedOutputsAreRejected() {
        OutputSlotSelection missingSelection = OutputSlotSelection.parse("2");
        RecipeEntry missingRecipe = recipeWithOutputs(new ItemStack(Items.iron_ingot));
        assertApplyErrorKey(missingSelection, missingRecipe, "nhaeutilities.msg.generate.output_slots_missing");

        OutputSlotSelection nullSelection = OutputSlotSelection.parse("2");
        RecipeEntry nullRecipe = recipeWithOutputs(new ItemStack(Items.iron_ingot), null);
        assertApplyErrorKey(nullSelection, nullRecipe, "nhaeutilities.msg.generate.output_slots_missing");
    }

    private static RecipeEntry recipeWithOutputs(ItemStack... outputs) {
        return new RecipeEntry(
            "gt",
            "gt.recipe.assembler",
            "Assembler",
            new ItemStack[] { new ItemStack(Items.redstone) },
            outputs,
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[] { new ItemStack(Items.paper) },
            120,
            30,
            "recipe-42");
    }

    private static void assertSelectionErrorKey(String input, String expectedKey) {
        try {
            OutputSlotSelection.parse(input);
            fail("Expected OutputSlotSelectionException for input: " + input);
        } catch (OutputSlotSelection.OutputSlotSelectionException e) {
            assertEquals(expectedKey, e.getTranslationKey());
        }
    }

    private static void assertApplyErrorKey(OutputSlotSelection selection, RecipeEntry recipe, String expectedKey) {
        try {
            selection.apply(recipe);
            fail("Expected OutputSlotSelectionException from apply");
        } catch (OutputSlotSelection.OutputSlotSelectionException e) {
            assertEquals(expectedKey, e.getTranslationKey());
        }
    }
}
