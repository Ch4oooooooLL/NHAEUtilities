package com.github.nhaeutilities.modules.patternrouting.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.item.ItemStack;

import com.github.nhaeutilities.modules.patterngenerator.recipe.GTRecipeSemanticExtractor;
import com.github.nhaeutilities.modules.patterngenerator.recipe.InputSemanticType;
import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeAnalysisSnapshot;
import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;
import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeInputSemanticSnapshot;

public final class RecipeMapAnalysisService {

    private RecipeMapAnalysisService() {}

    public static RecipeMapAnalysisResult analyzeSemanticEntries(List<RecipeEntry> recipes) {
        Map<String, TypeAccumulator> grouped = new LinkedHashMap<String, TypeAccumulator>();
        boolean hasIncompleteAnalysis = false;
        int circuitCount = 0;
        int ncCount = 0;
        int noCircuitNcCount = 0;
        int otherCount = 0;
        int inputSkipCount = 0;
        if (recipes != null) {
            for (RecipeEntry recipe : recipes) {
                RecipeAnalysisSnapshot snapshot = GTRecipeSemanticExtractor.extract(recipe);
                boolean hasCircuit = !snapshot.programmingCircuit.isEmpty();
                boolean hasNc = !snapshot.nonConsumableSignatures.isEmpty();
                if (hasCircuit) circuitCount++;
                if (hasNc) ncCount++;
                if (!hasCircuit && !hasNc) noCircuitNcCount++;

                int recipeInputs = recipe.inputs.length;
                int recipeClassified = 0;
                for (RecipeInputSemanticSnapshot s : snapshot.inputSnapshots) {
                    if (s.type != com.github.nhaeutilities.modules.patterngenerator.recipe.InputSemanticType.FLUID
                        && s.type
                            != com.github.nhaeutilities.modules.patterngenerator.recipe.InputSemanticType.SPECIAL_SLOT) {
                        recipeClassified++;
                    }
                }
                if (recipeClassified < recipeInputs) inputSkipCount++;

                TypeDescriptor descriptor = describe(snapshot);
                if (descriptor.incomplete) {
                    hasIncompleteAnalysis = true;
                }
                TypeAccumulator existing = grouped.get(descriptor.groupingKey);
                if (existing == null) {
                    grouped.put(descriptor.groupingKey, new TypeAccumulator(descriptor));
                } else {
                    existing.matchCount++;
                }
            }

            if (recipes.size() > 0) {
                try {
                    cpw.mods.fml.common.FMLLog.info(
                        "[NHAEUtilities][SemanticCache] Analysis stats totalRecipes=%d circuitDetected=%d ncDetected=%d noCircuitNc=%d inputsSkipped=%d",
                        recipes.size(),
                        circuitCount,
                        ncCount,
                        noCircuitNcCount,
                        inputSkipCount);
                } catch (Throwable ignored) {}
            }
        }

        return buildResult(grouped, grouped.size(), hasIncompleteAnalysis);
    }

    private static TypeDescriptor describe(RecipeAnalysisSnapshot snapshot) {
        String circuitKey = normalize(snapshot != null ? snapshot.programmingCircuit : "");
        String manualItemsKey = joinSignatures(
            snapshot != null ? snapshot.nonConsumableSignatures : Collections.<String>emptyList());
        String groupingKey = normalize(snapshot != null ? snapshot.groupingKey : "");
        boolean incomplete = snapshot == null || snapshot.status != RecipeAnalysisSnapshot.Status.COMPLETE;

        String circuitName = "";
        List<String> ncNames = new ArrayList<String>();
        List<ItemStack> ncItemStacks = new ArrayList<ItemStack>();
        if (snapshot != null) {
            for (RecipeInputSemanticSnapshot input : snapshot.inputSnapshots) {
                if (input.type == InputSemanticType.PROGRAMMING_CIRCUIT && input.stack != null) {
                    int num = snapshot.circuitNumber;
                    circuitName = num >= 0 ? "Programmed Circuit #" + num : displayName(input.stack);
                } else if (input.type == InputSemanticType.NON_CONSUMABLE && input.stack != null) {
                    ncNames.add(displayName(input.stack));
                    ncItemStacks.add(input.stack.copy());
                }
            }
        }

        String displaySummary = buildDisplaySummary(circuitName, ncNames);
        return new TypeDescriptor(groupingKey, circuitKey, manualItemsKey, displaySummary, incomplete, ncItemStacks);
    }

    private static String displayName(ItemStack stack) {
        if (stack == null) {
            return "";
        }
        try {
            String name = stack.getDisplayName();
            if (name != null && !name.trim()
                .isEmpty()) {
                return name.trim();
            }
            return GTRecipeSemanticExtractor.itemSignature(stack);
        } catch (Exception ignored) {
            return GTRecipeSemanticExtractor.itemSignature(stack);
        }
    }

    private static String buildDisplaySummary(String circuitName, List<String> ncNames) {
        StringBuilder summary = new StringBuilder();
        if (!circuitName.isEmpty()) {
            summary.append(circuitName);
        }
        for (String ncName : ncNames) {
            if (ncName.isEmpty()) {
                continue;
            }
            if (summary.length() > 0) {
                summary.append(" + ");
            }
            summary.append(ncName);
        }
        if (summary.length() == 0) {
            summary.append("No circuit or non-consumables");
        }
        return summary.toString();
    }

    private static RecipeMapAnalysisResult buildResult(Map<String, TypeAccumulator> grouped, int totalTypeCount,
        boolean hasIncompleteAnalysis) {
        List<RecipeMapAnalysisResult.RecipeTypeGroup> groups = new ArrayList<RecipeMapAnalysisResult.RecipeTypeGroup>();
        for (TypeAccumulator accumulator : grouped.values()) {
            groups.add(
                new RecipeMapAnalysisResult.RecipeTypeGroup(
                    accumulator.circuitKey,
                    accumulator.manualItemsKey,
                    accumulator.displaySummary,
                    accumulator.matchCount,
                    accumulator.ncItemStacks));
        }
        return new RecipeMapAnalysisResult(groups, totalTypeCount, hasIncompleteAnalysis);
    }

    private static String joinSignatures(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            String normalizedValue = normalize(value);
            if (normalizedValue.isEmpty()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append("|");
            }
            builder.append(normalizedValue);
        }
        return builder.toString();
    }

    private static String normalize(String value) {
        return value != null ? value : "";
    }

    private static final class TypeDescriptor {

        private final String groupingKey;
        private final String circuitKey;
        private final String manualItemsKey;
        private final String displaySummary;
        private final boolean incomplete;
        private final List<ItemStack> ncItemStacks;

        private TypeDescriptor(String groupingKey, String circuitKey, String manualItemsKey, String displaySummary,
            boolean incomplete, List<ItemStack> ncItemStacks) {
            this.groupingKey = normalize(groupingKey);
            this.circuitKey = normalize(circuitKey);
            this.manualItemsKey = normalize(manualItemsKey);
            this.displaySummary = normalize(displaySummary);
            this.incomplete = incomplete;
            this.ncItemStacks = ncItemStacks != null ? new ArrayList<ItemStack>(ncItemStacks)
                : new ArrayList<ItemStack>();
        }
    }

    private static final class TypeAccumulator {

        private final String circuitKey;
        private final String manualItemsKey;
        private final String displaySummary;
        private final List<ItemStack> ncItemStacks;
        private int matchCount;

        private TypeAccumulator(TypeDescriptor descriptor) {
            this.circuitKey = descriptor.circuitKey;
            this.manualItemsKey = descriptor.manualItemsKey;
            this.displaySummary = descriptor.displaySummary;
            this.ncItemStacks = descriptor.ncItemStacks;
            this.matchCount = 1;
        }
    }
}
