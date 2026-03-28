package com.github.nhaeutilities.modules.patterngenerator.recipe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;

import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;

/**
 * GT ????????????RecipeMap ?????????????? RecipeEntry
 * <p>
 * ?????????: ??? "blender" ?????"gt.recipe.metablender"
 */
public class GTRecipeSource {

    private static final RecipeCollectionCache<String, List<RecipeEntry>> COLLECTED_RECIPE_CACHE = new RecipeCollectionCache<String, List<RecipeEntry>>();

    /**
     * ?????????????GT ????????
     *
     * @return Map: unlocalizedName -> unlocalizedName
     */
    public static Map<String, String> getAvailableRecipeMaps() {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, RecipeMap<?>> entry : RecipeMap.ALL_RECIPE_MAPS.entrySet()) {
            result.put(entry.getKey(), entry.getKey());
        }
        return result;
    }

    /**
     * ???????????ID
     * <p>
     * ?????? (??????):
     * 1. ??????
     * 2. ?????????????????(??"blender" -> "gt.recipe.metablender")
     * 3. ??????????????????????????
     *
     * @param keyword ????????????
     * @return ???????????ID ???
     */
    public static List<String> findMatchingRecipeMaps(String keyword) {
        if (keyword == null) {
            return new ArrayList<>();
        }

        String normalized = keyword.trim();
        if (normalized.isEmpty()) {
            return new ArrayList<>();
        }

        Set<String> matches = new LinkedHashSet<>();
        String lowerKeyword = normalized.toLowerCase();

        // 1. ??????
        if (RecipeMap.ALL_RECIPE_MAPS.containsKey(normalized)) {
            matches.add(normalized);
        }

        // 2. ????????ID (xxx_1_2_3_4_5)
        RecipeMap<?> fromLegacy = RecipeMap.getFromOldIdentifier(normalized);
        if (fromLegacy != null) {
            matches.add(fromLegacy.unlocalizedName);
        }

        // 3. ?????????????????(map id + NEI transfer id)
        for (Map.Entry<String, RecipeMap<?>> entry : RecipeMap.ALL_RECIPE_MAPS.entrySet()) {
            String mapId = entry.getKey();
            RecipeMap<?> map = entry.getValue();

            if (mapId.toLowerCase()
                .contains(lowerKeyword)) {
                matches.add(mapId);
                continue;
            }

            if (map != null && map.getFrontend() != null
                && map.getFrontend()
                    .getUIProperties() != null) {
                String transferId = map.getFrontend()
                    .getUIProperties().neiTransferRectId;
                if (transferId != null && transferId.toLowerCase()
                    .contains(lowerKeyword)) {
                    matches.add(mapId);
                }
            }
        }

        return new ArrayList<>(matches);
    }

    /**
     * ??????????????????????(?????????)
     *
     * @param keyword ????????? (?????????)
     * @return ?????????????RecipeEntry ???
     */
    public static List<RecipeEntry> collectRecipes(String keyword) {
        List<String> matchedMaps = findMatchingRecipeMaps(keyword);
        if (matchedMaps.isEmpty()) {
            return new ArrayList<>();
        }

        String cacheKey = buildCollectionCacheKey(matchedMaps);
        return COLLECTED_RECIPE_CACHE
            .getOrCompute(cacheKey, () -> Collections.unmodifiableList(collectRecipesForMatchedMaps(matchedMaps)));
    }

    private static List<RecipeEntry> collectRecipesForMatchedMaps(List<String> matchedMaps) {
        List<RecipeEntry> entries = new ArrayList<>();
        Set<String> processedKeys = new java.util.HashSet<>();

        for (String mapId : matchedMaps) {
            RecipeMap<?> targetMap = RecipeMap.ALL_RECIPE_MAPS.get(mapId);
            if (targetMap == null) continue;

            Collection<GTRecipe> recipes = targetMap.getAllRecipes();
            if (recipes != null) {
                for (GTRecipe recipe : recipes) {
                    if (recipe == null || !recipe.mEnabled) continue;

                    // ???????????Key (??????????????????U)
                    String recipeKey = generateRecipeKey(recipe);
                    if (!processedKeys.add(recipeKey)) continue;

                    ItemStack[] normalInputs = recipe.mInputs;
                    ItemStack[] specialItems = new ItemStack[0];
                    if (recipe.mSpecialItems instanceof ItemStack[]) {
                        specialItems = (ItemStack[]) recipe.mSpecialItems;
                    } else if (recipe.mSpecialItems instanceof ItemStack) {
                        specialItems = new ItemStack[] { (ItemStack) recipe.mSpecialItems };
                    }

                    RecipeEntry entry = new RecipeEntry(
                        "gt",
                        mapId,
                        mapId,
                        normalInputs,
                        recipe.mOutputs,
                        recipe.mFluidInputs,
                        recipe.mFluidOutputs,
                        specialItems,
                        recipe.mDuration,
                        recipe.mEUt);

                    entries.add(entry);
                }
            }

            collectDynamicSmeltingRecipesIfNeeded(mapId, targetMap, entries, processedKeys);
        }

        return entries;
    }

    private static String buildCollectionCacheKey(List<String> matchedMaps) {
        if (matchedMaps == null || matchedMaps.isEmpty()) {
            return "";
        }
        return String.join("\u001F", matchedMaps);
    }

    /**
     * GT ??furnace / microwave ??? NonGTBackend ???????????????????? getAllRecipes()??
     * ???????????????FurnaceRecipes ????????RecipeEntry??
     */
    private static void collectDynamicSmeltingRecipesIfNeeded(String mapId, RecipeMap<?> targetMap,
        List<RecipeEntry> entries, Set<String> processedKeys) {
        if (targetMap == null || (!"gt.recipe.furnace".equals(mapId) && !"gt.recipe.microwave".equals(mapId))) {
            return;
        }

        Map<ItemStack, ItemStack> smelting = FurnaceRecipes.smelting()
            .getSmeltingList();
        if (smelting == null || smelting.isEmpty()) {
            return;
        }

        // NonGTBackend ??????????????????????????????????findRecipe??
        // ???????????????????????????????????????????
        Set<String> inputDedup = new LinkedHashSet<>();
        for (Map.Entry<ItemStack, ItemStack> e : smelting.entrySet()) {
            ItemStack rawInput = e.getKey();
            if (rawInput == null || rawInput.getItem() == null) {
                continue;
            }
            ItemStack input = rawInput.copy();
            if (input.stackSize <= 0) {
                input.stackSize = 1;
            }
            String inputKey = stackKey(input);
            if (!inputDedup.add(inputKey)) {
                continue;
            }

            GTRecipe dynamic = targetMap.findRecipeQuery()
                .items(input)
                .dontCheckStackSizes(true)
                .find();
            if (dynamic == null || !dynamic.mEnabled) {
                continue;
            }
            if ((dynamic.mOutputs == null || dynamic.mOutputs.length == 0)
                && (dynamic.mFluidOutputs == null || dynamic.mFluidOutputs.length == 0)) {
                continue;
            }

            ItemStack[] normalInputs = dynamic.mInputs != null ? dynamic.mInputs : new ItemStack[] { input };
            ItemStack[] specialItems = new ItemStack[0];
            if (dynamic.mSpecialItems instanceof ItemStack[]) {
                specialItems = (ItemStack[]) dynamic.mSpecialItems;
            } else if (dynamic.mSpecialItems instanceof ItemStack) {
                specialItems = new ItemStack[] { (ItemStack) dynamic.mSpecialItems };
            }

            String dynamicKey = generateRecipeKey(dynamic) + "|MAP:" + mapId;
            if (!processedKeys.add(dynamicKey)) {
                continue;
            }

            entries.add(
                new RecipeEntry(
                    "gt",
                    mapId,
                    mapId,
                    normalInputs,
                    dynamic.mOutputs,
                    dynamic.mFluidInputs,
                    dynamic.mFluidOutputs,
                    specialItems,
                    dynamic.mDuration,
                    dynamic.mEUt));
        }

        if ("gt.recipe.microwave".equals(mapId)) {
            ItemStack microwaveBook = new ItemStack(Items.book, 1, 0);
            GTRecipe bookRecipe = targetMap.findRecipeQuery()
                .items(microwaveBook)
                .dontCheckStackSizes(true)
                .find();
            if (bookRecipe != null && bookRecipe.mEnabled
                && bookRecipe.mOutputs != null
                && bookRecipe.mOutputs.length > 0) {
                String bookKey = generateRecipeKey(bookRecipe) + "|MAP:" + mapId;
                if (processedKeys.add(bookKey)) {
                    entries.add(
                        new RecipeEntry(
                            "gt",
                            mapId,
                            mapId,
                            bookRecipe.mInputs,
                            bookRecipe.mOutputs,
                            bookRecipe.mFluidInputs,
                            bookRecipe.mFluidOutputs,
                            new ItemStack[0],
                            bookRecipe.mDuration,
                            bookRecipe.mEUt));
                }
            }
        }
    }

    private static String stackKey(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return "NULL";
        }
        Item item = stack.getItem();
        Object name = Item.itemRegistry.getNameForObject(item);
        return String.valueOf(name) + "@" + stack.getItemDamage() + "@" + stack.stackSize;
    }

    private static String generateRecipeKey(GTRecipe recipe) {
        StringBuilder sb = new StringBuilder();
        sb.append(recipe.mDuration)
            .append(":")
            .append(recipe.mEUt)
            .append("|");
        if (recipe.mInputs != null) {
            for (ItemStack is : recipe.mInputs) {
                if (is != null && is.getItem() != null)
                    sb.append(net.minecraft.item.Item.itemRegistry.getNameForObject(is.getItem()))
                        .append("@")
                        .append(is.getItemDamage())
                        .append("@")
                        .append(is.stackSize)
                        .append(",");
                else sb.append("NULL,");
            }
        }
        sb.append("|");
        if (recipe.mOutputs != null) {
            for (ItemStack is : recipe.mOutputs) {
                if (is != null && is.getItem() != null)
                    sb.append(net.minecraft.item.Item.itemRegistry.getNameForObject(is.getItem()))
                        .append("@")
                        .append(is.getItemDamage())
                        .append("@")
                        .append(is.stackSize)
                        .append(",");
                else sb.append("NULL,");
            }
        }
        sb.append("|SP:");
        if (recipe.mSpecialItems instanceof ItemStack) {
            ItemStack is = (ItemStack) recipe.mSpecialItems;
            if (is.getItem() != null) {
                sb.append(net.minecraft.item.Item.itemRegistry.getNameForObject(is.getItem()))
                    .append("@")
                    .append(is.getItemDamage());
            }
        } else if (recipe.mSpecialItems instanceof ItemStack[]) {
            for (ItemStack is : (ItemStack[]) recipe.mSpecialItems) {
                if (is != null && is.getItem() != null)
                    sb.append(net.minecraft.item.Item.itemRegistry.getNameForObject(is.getItem()))
                        .append("@")
                        .append(is.getItemDamage())
                        .append(",");
            }
        }
        sb.append("|FI:");
        if (recipe.mFluidInputs != null) {
            for (net.minecraftforge.fluids.FluidStack fs : recipe.mFluidInputs) {
                if (fs != null && fs.getFluid() != null) {
                    sb.append(
                        fs.getFluid()
                            .getName())
                        .append("@")
                        .append(fs.amount)
                        .append(",");
                }
            }
        }
        sb.append("|FO:");
        if (recipe.mFluidOutputs != null) {
            for (net.minecraftforge.fluids.FluidStack fs : recipe.mFluidOutputs) {
                if (fs != null && fs.getFluid() != null) {
                    sb.append(
                        fs.getFluid()
                            .getName())
                        .append("@")
                        .append(fs.amount)
                        .append(",");
                }
            }
        }
        return sb.toString();
    }

    /**
     * ???????????????????
     */
    public static List<RecipeEntry> collectAllRecipes() {
        List<RecipeEntry> all = new ArrayList<>();
        for (String mapId : RecipeMap.ALL_RECIPE_MAPS.keySet()) {
            all.addAll(collectRecipes(mapId));
        }
        return all;
    }
}

