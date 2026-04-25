package com.github.nhaeutilities.modules.patterngenerator.network;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.item.ItemStack;

import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;

class OutputSlotSelection {

    private static final String KEY_INVALID = "nhaeutilities.msg.generate.output_slots_invalid";
    private static final String KEY_DUPLICATE = "nhaeutilities.msg.generate.output_slots_duplicate";
    private static final String KEY_MISSING = "nhaeutilities.msg.generate.output_slots_missing";

    private final boolean selectAll;
    private final Set<Integer> requestedSlots;

    private OutputSlotSelection(boolean selectAll, Set<Integer> requestedSlots) {
        this.selectAll = selectAll;
        this.requestedSlots = requestedSlots;
    }

    static OutputSlotSelection parse(String rawSelection) {
        if (rawSelection == null || rawSelection.trim().isEmpty()) {
            return new OutputSlotSelection(true, new HashSet<Integer>());
        }

        String[] tokens = rawSelection.split(",", -1);
        Set<Integer> slots = new HashSet<Integer>();
        for (String token : tokens) {
            String trimmed = token.trim();
            if (trimmed.isEmpty()) {
                throw new OutputSlotSelectionException(KEY_INVALID);
            }

            final int slot;
            try {
                slot = Integer.parseInt(trimmed);
            } catch (NumberFormatException e) {
                throw new OutputSlotSelectionException(KEY_INVALID);
            }

            if (slot <= 0) {
                throw new OutputSlotSelectionException(KEY_INVALID);
            }
            if (!slots.add(slot)) {
                throw new OutputSlotSelectionException(KEY_DUPLICATE);
            }
        }

        return new OutputSlotSelection(false, slots);
    }

    RecipeEntry apply(RecipeEntry recipe) {
        if (selectAll) {
            return recipe;
        }

        ItemStack[] outputs = recipe.outputs != null ? recipe.outputs : new ItemStack[0];
        for (Integer requestedSlot : requestedSlots) {
            int index = requestedSlot - 1;
            if (index < 0 || index >= outputs.length || outputs[index] == null) {
                throw new OutputSlotSelectionException(KEY_MISSING);
            }
        }

        List<ItemStack> selectedOutputs = new ArrayList<ItemStack>();
        for (int i = 0; i < outputs.length; i++) {
            int slot = i + 1;
            if (requestedSlots.contains(slot)) {
                selectedOutputs.add(outputs[i]);
            }
        }

        return new RecipeEntry(
            recipe.sourceType,
            recipe.recipeMapId,
            recipe.machineDisplayName,
            recipe.inputs,
            selectedOutputs.toArray(new ItemStack[selectedOutputs.size()]),
            recipe.fluidInputs,
            recipe.fluidOutputs,
            recipe.specialItems,
            recipe.duration,
            recipe.euPerTick,
            recipe.recipeId);
    }

    static class OutputSlotSelectionException extends RuntimeException {

        private final String translationKey;

        OutputSlotSelectionException(String translationKey) {
            super(translationKey);
            this.translationKey = translationKey;
        }

        String getTranslationKey() {
            return translationKey;
        }
    }
}
