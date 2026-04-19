package com.github.nhaeutilities.modules.patternrouting.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class HatchAssignmentDataTest {

    @Test
    public void toNbtAndFromNbtRoundTripDescriptorAssignmentFields() {
        HatchAssignmentData original = new HatchAssignmentData(
            "gt.recipe.assembler|circuit-a|manual-a",
            "gt.recipe.assembler",
            "circuit-a",
            "manual-a");

        NBTTagCompound tag = original.toNbt();
        HatchAssignmentData restored = HatchAssignmentData.fromNbt(tag);

        assertTrue(restored.isAssigned());
        assertEquals("gt.recipe.assembler|circuit-a|manual-a", restored.assignmentKey);
        assertEquals("gt.recipe.assembler", restored.recipeCategory);
        assertEquals("gt.recipe.assembler", restored.recipeFamily);
        assertEquals("", restored.recipeId);
        assertEquals("circuit-a", restored.circuitKey);
        assertEquals("manual-a", restored.manualItemsKey);
    }

    @Test
    public void fromNbtNormalizesMissingValuesToEmptyStrings() {
        HatchAssignmentData restored = HatchAssignmentData.fromNbt(new NBTTagCompound());

        assertFalse(restored.isAssigned());
        assertEquals("", restored.assignmentKey);
        assertEquals("", restored.recipeCategory);
        assertEquals("", restored.recipeFamily);
        assertEquals("", restored.recipeId);
        assertEquals("", restored.circuitKey);
        assertEquals("", restored.manualItemsKey);
    }

    @Test
    public void sharedItemDescriptorTreatsFirstSharedItemAsCircuitAndRestAsManualItems() {
        ItemStack circuit = new ItemStack(Items.paper, 1, 1);
        ItemStack mold = new ItemStack(Items.paper, 1, 2);

        CraftingInputHatchAccess.SharedItemDescriptor descriptor = new CraftingInputHatchAccess.SharedItemDescriptor(
            circuit,
            new ItemStack[] { mold },
            2);

        assertEquals(PatternRoutingNbt.itemSignature(circuit), PatternRoutingNbt.circuitKey(descriptor.circuit));
        assertEquals(PatternRoutingNbt.itemSignature(mold), PatternRoutingNbt.manualItemsKey(descriptor.manualItems));
    }

    @Test
    public void toDescriptorReturnsRecipeCategoryCircuitAndManualItems() {
        HatchAssignmentData original = new HatchAssignmentData(
            "gt.recipe.assembler|circuit-a|manual-a",
            "gt.recipe.assembler",
            "circuit-a",
            "manual-a");

        RoutingDescriptor descriptor = original.toDescriptor();

        assertEquals("gt.recipe.assembler", descriptor.recipeCategory);
        assertEquals("circuit-a", descriptor.circuitKey);
        assertEquals("manual-a", descriptor.manualItemsKey);
    }
}
