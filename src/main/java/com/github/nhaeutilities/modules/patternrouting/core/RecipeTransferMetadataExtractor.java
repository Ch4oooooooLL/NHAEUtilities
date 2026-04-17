package com.github.nhaeutilities.modules.patternrouting.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;

import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.IRecipeHandler;

public final class RecipeTransferMetadataExtractor {

    private static final String EMPTY_JSON_ARRAY = "[]";
    private static final String EMPTY_JSON_OBJECT = "{}";
    private static final String GT_NEI_FIXED_POSITIONED_STACK = "gregtech.nei.GTNEIDefaultHandler$FixedPositionedStack";
    private static final Method GT_IS_ANY_INTEGRATED_CIRCUIT = findGtCircuitMethod();
    private static final Method GT_IS_NOT_CONSUMED = findGtIsNotConsumedMethod();

    private RecipeTransferMetadataExtractor() {}

    public static Metadata extract(IRecipeHandler recipe, int recipeIndex, String recipeId, String overlayIdentifier) {
        List<ItemStack> circuitStacks = new ArrayList<ItemStack>();
        List<ItemStack> nonConsumables = new ArrayList<ItemStack>();
        List<String> inputEntries = new ArrayList<String>();
        List<String> otherEntries = new ArrayList<String>();
        List<String> outputEntries = new ArrayList<String>();

        appendStacks(recipe.getIngredientStacks(recipeIndex), circuitStacks, nonConsumables, inputEntries);
        appendStacks(recipe.getOtherStacks(recipeIndex), circuitStacks, nonConsumables, otherEntries);
        appendSerializedStacks(resolveOutputs(recipe, recipeIndex), outputEntries);

        String programmingCircuit = circuitStacks.isEmpty() ? ""
            : PatternRoutingNbt.itemSignature(circuitStacks.get(0));
        String nonConsumablesJson = toJsonArray(nonConsumables);
        String recipeSnapshot = buildSnapshot(recipeId, overlayIdentifier, inputEntries, otherEntries, outputEntries);
        PatternRoutingLog.info(
            "[NHAEUtilities][patternrouting] extracted recipe metadata recipeId=%s overlay=%s circuit=%s ncCount=%s inputs=%s others=%s outputs=%s snapshotSize=%s",
            recipeId,
            overlayIdentifier,
            programmingCircuit,
            nonConsumables.size(),
            inputEntries.size(),
            otherEntries.size(),
            outputEntries.size(),
            recipeSnapshot.length());

        return new Metadata(programmingCircuit, nonConsumablesJson, recipeSnapshot);
    }

    private static List<PositionedStack> resolveOutputs(IRecipeHandler recipe, int recipeIndex) {
        List<PositionedStack> outputs = new ArrayList<PositionedStack>();
        if (recipe == null) {
            return outputs;
        }
        PositionedStack result = recipe.getResultStack(recipeIndex);
        if (result != null) {
            outputs.add(result);
        }
        return outputs;
    }

    private static void appendStacks(List<PositionedStack> stacks, List<ItemStack> circuitStacks,
        List<ItemStack> nonConsumables, List<String> serializedStacks) {
        if (stacks == null) {
            return;
        }
        for (PositionedStack stack : stacks) {
            ItemStack selected = selectRepresentativeStack(stack);
            if (selected == null) {
                continue;
            }
            boolean nc = isNonConsumable(stack, selected);
            if (isProgrammingCircuit(selected)) {
                circuitStacks.add(selected.copy());
            }
            if (nc) {
                nonConsumables.add(selected.copy());
            }
            serializedStacks.add(serializeStack(selected, nc));
        }
    }

    private static void appendSerializedStacks(List<PositionedStack> stacks, List<String> serializedStacks) {
        if (stacks == null) {
            return;
        }
        for (PositionedStack stack : stacks) {
            ItemStack selected = selectRepresentativeStack(stack);
            if (selected == null) {
                continue;
            }
            serializedStacks.add(serializeStack(selected, false));
        }
    }

    private static ItemStack selectRepresentativeStack(PositionedStack stack) {
        if (stack == null) {
            return null;
        }
        List<ItemStack> filtered = stack.getFilteredPermutations();
        if (filtered != null && !filtered.isEmpty()) {
            return filtered.get(0)
                .copy();
        }
        if (stack.item != null) {
            return stack.item.copy();
        }
        if (stack.items != null && stack.items.length > 0 && stack.items[0] != null) {
            return stack.items[0].copy();
        }
        return null;
    }

    private static boolean isProgrammingCircuit(ItemStack stack) {
        if (stack == null || GT_IS_ANY_INTEGRATED_CIRCUIT == null) {
            return false;
        }
        try {
            Object result = GT_IS_ANY_INTEGRATED_CIRCUIT.invoke(null, stack);
            return Boolean.TRUE.equals(result);
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isNonConsumable(PositionedStack positionedStack, ItemStack selected) {
        if (positionedStack != null && GT_IS_NOT_CONSUMED != null
            && GT_NEI_FIXED_POSITIONED_STACK.equals(
                positionedStack.getClass()
                    .getName())) {
            try {
                Object result = GT_IS_NOT_CONSUMED.invoke(positionedStack);
                if (Boolean.TRUE.equals(result)) {
                    return true;
                }
            } catch (Exception ignored) {}
        }
        return selected != null && selected.stackSize == 0;
    }

    private static String serializeStack(ItemStack stack, boolean nonConsumable) {
        return "{\"item\":\"" + escapeJson(PatternRoutingNbt.itemSignature(stack))
            + "\",\"count\":"
            + stack.stackSize
            + ",\"nc\":"
            + nonConsumable
            + "}";
    }

    private static String toJsonArray(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) {
            return EMPTY_JSON_ARRAY;
        }
        List<String> serialized = new ArrayList<String>();
        for (ItemStack stack : stacks) {
            if (stack == null) {
                continue;
            }
            serialized.add(serializeStack(stack, true));
        }
        return joinJsonArray(serialized);
    }

    private static String buildSnapshot(String recipeId, String overlayIdentifier, List<String> inputs,
        List<String> others, List<String> outputs) {
        return "{\"recipeId\":\"" + escapeJson(recipeId)
            + "\",\"overlayIdentifier\":\""
            + escapeJson(overlayIdentifier)
            + "\",\"inputs\":"
            + joinJsonArray(inputs)
            + ",\"other\":"
            + joinJsonArray(others)
            + ",\"outputs\":"
            + joinJsonArray(outputs)
            + "}";
    }

    private static String joinJsonArray(List<String> values) {
        if (values == null || values.isEmpty()) {
            return EMPTY_JSON_ARRAY;
        }
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(values.get(i));
        }
        return builder.append(']')
            .toString();
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    private static Method findGtCircuitMethod() {
        try {
            Class<?> gtUtility = Class.forName("gregtech.api.util.GTUtility");
            return gtUtility.getMethod("isAnyIntegratedCircuit", ItemStack.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static Method findGtIsNotConsumedMethod() {
        try {
            Class<?> fixedPositionedStack = Class.forName(GT_NEI_FIXED_POSITIONED_STACK);
            return fixedPositionedStack.getMethod("isNotConsumed");
        } catch (Exception ignored) {
            return null;
        }
    }

    public static final class Metadata {

        public final String programmingCircuit;
        public final String nonConsumables;
        public final String recipeSnapshot;

        private Metadata(String programmingCircuit, String nonConsumables, String recipeSnapshot) {
            this.programmingCircuit = programmingCircuit != null ? programmingCircuit : "";
            this.nonConsumables = nonConsumables != null ? nonConsumables : EMPTY_JSON_ARRAY;
            this.recipeSnapshot = recipeSnapshot != null ? recipeSnapshot : EMPTY_JSON_OBJECT;
        }
    }
}
