package com.github.nhaeutilities.modules.patternrouting.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import com.github.nhaeutilities.modules.patternrouting.service.FilterRule.RuleType;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

public final class RecipeOutputIndex {

    private RecipeOutputIndex() {}

    public static IndexResult buildIndex(List<ItemStack> targets) {
        return buildIndex(targets, Collections.<FilterRule>emptyList());
    }

    public static IndexResult buildIndex(List<ItemStack> targets, List<FilterRule> rules) {
        Set<String> blacklistedMaps = new HashSet<>();
        Map<String, List<ItemStack>> manualMatches = new HashMap<>();

        for (FilterRule rule : rules) {
            if (rule == null || !rule.isValid()) continue;
            if (rule.type == RuleType.BLACKLIST) {
                blacklistedMaps.add(rule.recipeMapId);
            } else if (rule.type == RuleType.MANUAL_MATCH) {
                if (!manualMatches.containsKey(rule.recipeMapId)) {
                    manualMatches.put(rule.recipeMapId, new ArrayList<ItemStack>());
                }
            }
        }

        Map<String, List<ItemStack>> targetKeys = new HashMap<>();
        for (ItemStack stack : targets) {
            if (stack == null || stack.getItem() == null) continue;
            String key = itemMatchKey(stack);
            if (!targetKeys.containsKey(key)) {
                targetKeys.put(key, new ArrayList<ItemStack>());
            }
            targetKeys.get(key)
                .add(stack);
        }

        Map<String, RecipeInfo> uniqueRecipes = new HashMap<>();
        Map<String, Integer> recipeCounts = new HashMap<>();
        Map<String, String> firstRecipeMap = new HashMap<>();

        for (Map.Entry<String, RecipeMap<?>> entry : RecipeMap.ALL_RECIPE_MAPS.entrySet()) {
            String mapId = entry.getKey();
            RecipeMap<?> recipeMap = entry.getValue();
            if (recipeMap == null) continue;

            boolean manualOnly = manualMatches.containsKey(mapId);

            for (GTRecipe recipe : recipeMap.getAllRecipes()) {
                if (recipe == null || !recipe.mEnabled) continue;
                if (recipe.mOutputs == null) continue;

                for (ItemStack output : recipe.mOutputs) {
                    if (output == null || output.getItem() == null) continue;
                    String key = itemMatchKey(output);
                    if (!targetKeys.containsKey(key)) continue;

                    boolean matchesManual = false;
                    if (!manualMatches.isEmpty()) {
                        for (ItemStack target : targetKeys.get(key)) {
                            if (matchesAnyManualRule(target, rules)) {
                                matchesManual = true;
                                break;
                            }
                        }
                    }

                    if (blacklistedMaps.contains(mapId)) continue;

                    if (!manualMatches.isEmpty() && !matchesManual && !manualOnly) {
                        if (manualMatches.containsKey(mapId)) continue;
                    }
                    if (manualOnly && !matchesManual) continue;

                    int count = recipeCounts.getOrDefault(key, 0) + 1;
                    recipeCounts.put(key, count);

                    if (count == 1) {
                        uniqueRecipes.put(key, new RecipeInfo(recipe, mapId));
                        firstRecipeMap.put(key, mapId);
                    } else if (count == 2) {
                        uniqueRecipes.remove(key);
                    }
                }

                if (targetKeys.size() == recipeCounts.size() && recipeCounts.values()
                    .stream()
                    .allMatch(c -> c >= 2)) {
                    break;
                }
            }
        }

        Map<String, RecipeInfo> forcedUnique = new HashMap<>();
        for (Map.Entry<String, List<ItemStack>> targetEntry : targetKeys.entrySet()) {
            String key = targetEntry.getKey();
            if (uniqueRecipes.containsKey(key)) continue;

            for (ItemStack target : targetEntry.getValue()) {
                FilterRule matchingRule = findMatchingManualRule(target, rules);
                if (matchingRule != null) {
                    RecipeInfo info = findFirstRecipeInMap(matchingRule.recipeMapId, target);
                    if (info != null) {
                        forcedUnique.put(key, info);
                        break;
                    }
                }
            }
        }

        Map<ItemStack, RecipeInfo> result = new HashMap<>();
        for (Map.Entry<String, List<ItemStack>> targetEntry : targetKeys.entrySet()) {
            String key = targetEntry.getKey();
            RecipeInfo info = uniqueRecipes.get(key);
            if (info == null) {
                info = forcedUnique.get(key);
            }
            if (info != null) {
                for (ItemStack target : targetEntry.getValue()) {
                    if (!result.containsKey(target)) {
                        result.put(target, info);
                    }
                }
            }
        }

        return new IndexResult(result, recipeCounts);
    }

    private static boolean matchesAnyManualRule(ItemStack stack, List<FilterRule> rules) {
        for (FilterRule rule : rules) {
            if (rule.type == RuleType.MANUAL_MATCH && matchesItemPattern(stack, rule.itemPattern)) {
                return true;
            }
        }
        return false;
    }

    private static FilterRule findMatchingManualRule(ItemStack stack, List<FilterRule> rules) {
        for (FilterRule rule : rules) {
            if (rule.type == RuleType.MANUAL_MATCH && matchesItemPattern(stack, rule.itemPattern)) {
                return rule;
            }
        }
        return null;
    }

    private static boolean matchesItemPattern(ItemStack stack, String pattern) {
        if (pattern == null || pattern.isEmpty()) return false;
        if (stack == null || stack.getItem() == null) return false;

        Object registryName = Item.itemRegistry.getNameForObject(stack.getItem());
        String name = registryName != null ? registryName.toString() : "";

        if (pattern.startsWith("[") && pattern.endsWith("]")) {
            String exactPattern = pattern.substring(1, pattern.length() - 1);
            return name.equalsIgnoreCase(exactPattern) || name.toLowerCase()
                .contains(exactPattern.toLowerCase());
        }
        if (pattern.startsWith("(") && pattern.endsWith(")")) {
            String orePattern = pattern.substring(1, pattern.length() - 1);
            int[] oreIds = net.minecraftforge.oredict.OreDictionary.getOreIDs(stack);
            for (int id : oreIds) {
                String oreName = net.minecraftforge.oredict.OreDictionary.getOreName(id);
                if (oreName != null && oreName.toLowerCase()
                    .contains(orePattern.toLowerCase())) {
                    return true;
                }
            }
            return false;
        }
        if (pattern.startsWith("{") && pattern.endsWith("}")) {
            String displayPattern = pattern.substring(1, pattern.length() - 1);
            String displayName = stack.getDisplayName();
            return displayName != null && displayName.toLowerCase()
                .contains(displayPattern.toLowerCase());
        }

        return name.equalsIgnoreCase(pattern) || name.toLowerCase()
            .contains(pattern.toLowerCase());
    }

    private static RecipeInfo findFirstRecipeInMap(String mapId, ItemStack target) {
        RecipeMap<?> recipeMap = RecipeMap.ALL_RECIPE_MAPS.get(mapId);
        if (recipeMap == null) return null;

        for (GTRecipe recipe : recipeMap.getAllRecipes()) {
            if (recipe == null || !recipe.mEnabled) continue;
            if (recipe.mOutputs == null) continue;
            for (ItemStack output : recipe.mOutputs) {
                if (output == null || output.getItem() == null) continue;
                if (itemMatchKey(output).equals(itemMatchKey(target))) {
                    return new RecipeInfo(recipe, mapId);
                }
            }
        }
        return null;
    }

    public static String itemMatchKey(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return "NULL";
        Object registryName = Item.itemRegistry.getNameForObject(stack.getItem());
        return (registryName != null ? registryName : Item.getIdFromItem(stack.getItem())) + "@"
            + stack.getItemDamage();
    }

    public static final class RecipeInfo {

        public final GTRecipe recipe;
        public final String recipeMapId;

        RecipeInfo(GTRecipe recipe, String recipeMapId) {
            this.recipe = recipe;
            this.recipeMapId = recipeMapId;
        }
    }

    public static final class IndexResult {

        public final Map<ItemStack, RecipeInfo> uniqueItems;
        public final Map<String, Integer> recipeCounts;

        IndexResult(Map<ItemStack, RecipeInfo> uniqueItems, Map<String, Integer> recipeCounts) {
            this.uniqueItems = Collections.unmodifiableMap(uniqueItems);
            this.recipeCounts = Collections.unmodifiableMap(recipeCounts);
        }

        public boolean hasUniqueFor(ItemStack stack) {
            return uniqueItems.containsKey(stack);
        }

        public RecipeInfo getUniqueRecipe(ItemStack stack) {
            return uniqueItems.get(stack);
        }
    }
}
