package com.github.nhaeutilities.modules.patterngenerator.gui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.github.nhaeutilities.modules.patterngenerator.network.PacketRecipeConflictBatch;
import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;

public class GuiRecipePickerStateTest {

    @Test
    public void clientBatchStateInitializesFromPacketData() {
        PacketRecipeConflictBatch packet = new PacketRecipeConflictBatch(
            3,
            5,
            2,
            Arrays.asList("Assembler", "Circuit Assembler"),
            Arrays.asList(createRecipes(1), createRecipes(2)));

        GuiRecipePicker.ClientBatchState state = GuiRecipePicker.ClientBatchState.from(packet);

        assertNotNull(state);
        assertEquals(3, state.startIndex);
        assertEquals(5, state.totalConflicts);
        assertEquals(2, state.rowCapacity);
        assertEquals(0, state.localIndex);
        assertEquals(0, state.selectedRecipeIndex);
        assertEquals(
            1,
            state.getCurrentRecipes()
                .size());
    }

    @Test
    public void clientBatchStateRejectsEmptyPackets() {
        PacketRecipeConflictBatch packet = new PacketRecipeConflictBatch(
            1,
            1,
            1,
            new ArrayList<String>(),
            new ArrayList<List<RecipeEntry>>());

        assertTrue(GuiRecipePicker.ClientBatchState.from(packet) == null);
    }

    private static List<RecipeEntry> createRecipes(int count) {
        List<RecipeEntry> recipes = new ArrayList<RecipeEntry>();
        for (int i = 0; i < count; i++) {
            recipes.add(new RecipeEntry("gt", "map", "machine", null, null, null, null, null, 20, 30));
        }
        return recipes;
    }
}
