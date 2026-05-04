package com.github.nhaeutilities.modules.patternrouting.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

public final class RecipeMapAnalysisResult {

    public final int totalTypeCount;
    public final boolean hasIncompleteAnalysis;
    public final List<RecipeTypeGroup> repeatedTypes;
    public final List<RecipeTypeGroup> singleOccurrenceTypes;

    public RecipeMapAnalysisResult(List<RecipeTypeGroup> allTypes) {
        this(allTypes, allTypes != null ? allTypes.size() : 0, false);
    }

    public RecipeMapAnalysisResult(List<RecipeTypeGroup> allTypes, int totalTypeCount, boolean hasIncompleteAnalysis) {
        List<RecipeTypeGroup> safeTypes = allTypes != null ? new ArrayList<RecipeTypeGroup>(allTypes)
            : new ArrayList<RecipeTypeGroup>();
        this.totalTypeCount = Math.max(0, totalTypeCount);
        this.hasIncompleteAnalysis = hasIncompleteAnalysis;

        List<RecipeTypeGroup> repeated = new ArrayList<RecipeTypeGroup>();
        List<RecipeTypeGroup> singleOccurrence = new ArrayList<RecipeTypeGroup>();
        for (RecipeTypeGroup type : safeTypes) {
            if (type.matchCount > 1) {
                repeated.add(type);
            } else {
                singleOccurrence.add(type);
            }
        }

        this.repeatedTypes = Collections.unmodifiableList(repeated);
        this.singleOccurrenceTypes = Collections.unmodifiableList(singleOccurrence);
    }

    public static final class RecipeTypeGroup {

        public final String circuitKey;
        public final String manualItemsKey;
        public final String displaySummary;
        public final int matchCount;
        public final List<ItemStack> ncItemStacks;

        public RecipeTypeGroup(String circuitKey, String manualItemsKey, String displaySummary, int matchCount) {
            this(circuitKey, manualItemsKey, displaySummary, matchCount, Collections.<ItemStack>emptyList());
        }

        public RecipeTypeGroup(String circuitKey, String manualItemsKey, String displaySummary, int matchCount,
            List<ItemStack> ncItemStacks) {
            this.circuitKey = normalize(circuitKey);
            this.manualItemsKey = normalize(manualItemsKey);
            this.displaySummary = normalize(displaySummary);
            this.matchCount = Math.max(0, matchCount);
            this.ncItemStacks = ncItemStacks != null
                ? Collections.unmodifiableList(new ArrayList<ItemStack>(ncItemStacks))
                : Collections.<ItemStack>emptyList();
        }

        private static String normalize(String value) {
            return value != null ? value : "";
        }
    }
}
