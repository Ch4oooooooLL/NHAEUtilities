package com.github.nhaeutilities.modules.patternrouting.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class HatchAssignmentDataTest {

    @Test
    public void toNbtAndFromNbtRoundTripAllAssignmentFields() {
        HatchAssignmentData original = new HatchAssignmentData(
            "key-a",
            "gt.recipe.assembler",
            "recipe-a",
            "circuit-a",
            "manual-a");

        NBTTagCompound tag = original.toNbt();
        HatchAssignmentData restored = HatchAssignmentData.fromNbt(tag);

        assertTrue(restored.isAssigned());
        assertEquals("key-a", restored.assignmentKey);
        assertEquals("gt.recipe.assembler", restored.recipeFamily);
        assertEquals("recipe-a", restored.recipeId);
        assertEquals("circuit-a", restored.circuitKey);
        assertEquals("manual-a", restored.manualItemsKey);
    }

    @Test
    public void fromNbtNormalizesMissingValuesToEmptyStrings() {
        HatchAssignmentData restored = HatchAssignmentData.fromNbt(new NBTTagCompound());

        assertFalse(restored.isAssigned());
        assertEquals("", restored.assignmentKey);
        assertEquals("", restored.recipeFamily);
        assertEquals("", restored.recipeId);
        assertEquals("", restored.circuitKey);
        assertEquals("", restored.manualItemsKey);
    }
}
