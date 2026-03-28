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
    private static volatile RecipeMapRegistry recipeMapRegistry = new GregTechRecipeMapRegistry();
    private static volatile SmeltingRecipeProvider smeltingRecipeProvider = new FurnaceSmeltingRecipeProvider();

    /**
     * ?????????????GT ????????
     *
     * @return Map: unlocalizedName -> unlocalizedName
     */
    public static Map<String, String> getAvailableRecipeMaps() {
        Map<String, String> result = new LinkedHashMap<>();
        for (Map.Entry<String, RecipeMapView> entry : recipeMapRegistry.getRecipeMaps()
            .entrySet()) {
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
        Map<String, RecipeMapView> recipeMaps = recipeMapRegistry.getRecipeMaps();

        if (recipeMaps.containsKey(normalized)) {
            matches.add(normalized);
        }

        // 2. ????????ID (xxx_1_2_3_4_5)
        RecipeMapView fromLegacy = recipeMapRegistry.getFromLegacyIdentifier(normalized);
        if (fromLegacy != null) {
            matches.add(fromLegacy.getMapId());
        }

        // 3. ?????????????????(map id + NEI transfer id)
        for (Map.Entry<String, RecipeMapView> entry : recipeMaps.entrySet()) {
            String mapId = entry.getKey();
            RecipeMapView map = entry.getValue();

            if (mapId.toLowerCase()
                .contains(lowerKeyword)) {
                matches.add(mapId);
                continue;
            }

            String transferId = map != null ? map.getTransferId() : null;
            if (transferId != null && transferId.toLowerCase()
                .contains(lowerKeyword)) {
                matches.add(mapId);
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

    public static void invalidateCollectionCache() {
        COLLECTED_RECIPE_CACHE.clear();
    }

    static void setRecipeMapRegistry(RecipeMapRegistry registry) {
        recipeMapRegistry = registry != null ? registry : recipeMapRegistry;
        invalidateCollectionCache();
    }

    static void resetRecipeMapRegistry() {
        recipeMapRegistry = new GregTechRecipeMapRegistry();
        invalidateCollectionCache();
    }

    static void setSmeltingRecipeProvider(SmeltingRecipeProvider provider) {
        smeltingRecipeProvider = provider != null ? provider : smeltingRecipeProvider;
        invalidateCollectionCache();
    }

    static void resetSmeltingRecipeProvider() {
        smeltingRecipeProvider = new FurnaceSmeltingRecipeProvider();
        invalidateCollectionCache();
    }

    private static List<RecipeEntry> collectRecipesForMatchedMaps(List<String> matchedMaps) {
        List<RecipeEntry> entries = new ArrayList<>();
        Set<String> processedKeys = new java.util.HashSet<>();
        Map<String, RecipeMapView> recipeMaps = recipeMapRegistry.getRecipeMaps();

        for (String mapId : matchedMaps) {
            RecipeMapView targetMap = recipeMaps.get(mapId);
            if (targetMap == null) continue;

            Collection<RecipeView> recipes = targetMap.getAllRecipes();
            if (recipes != null) {
                for (RecipeView recipe : recipes) {
                    if (recipe == null || !recipe.isEnabled()) continue;

                    // ???????????Key (??????????????????U)
                    String recipeKey = generateRecipeKey(recipe);
                    if (!processedKeys.add(recipeKey)) continue;

                    entries.add(toRecipeEntry(mapId, recipe, recipe.getInputs()));
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
    private static void collectDynamicSmeltingRecipesIfNeeded(String mapId, RecipeMapView targetMap,
        List<RecipeEntry> entries, Set<String> processedKeys) {
        if (targetMap == null || (!"gt.recipe.furnace".equals(mapId) && !"gt.recipe.microwave".equals(mapId))) {
            return;
        }

        Map<ItemStack, ItemStack> smelting = smeltingRecipeProvider.getSmeltingList();
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

            RecipeView dynamic = targetMap.findRecipe(input);
            if (dynamic == null || !dynamic.isEnabled()) {
                continue;
            }
            if ((dynamic.getOutputs() == null || dynamic.getOutputs().length == 0)
                && (dynamic.getFluidOutputs() == null || dynamic.getFluidOutputs().length == 0)) {
                continue;
            }

            String dynamicKey = generateRecipeKey(dynamic) + "|MAP:" + mapId;
            if (!processedKeys.add(dynamicKey)) {
                continue;
            }

            ItemStack[] normalInputs = dynamic.getInputs() != null ? dynamic.getInputs() : new ItemStack[] { input };
            entries.add(toRecipeEntry(mapId, dynamic, normalInputs));
        }

        if ("gt.recipe.microwave".equals(mapId)) {
            ItemStack microwaveBook = new ItemStack(Items.book, 1, 0);
            RecipeView bookRecipe = targetMap.findRecipe(microwaveBook);
            if (bookRecipe != null && bookRecipe.isEnabled()
                && bookRecipe.getOutputs() != null
                && bookRecipe.getOutputs().length > 0) {
                String bookKey = generateRecipeKey(bookRecipe) + "|MAP:" + mapId;
                if (processedKeys.add(bookKey)) {
                    entries.add(toRecipeEntry(mapId, bookRecipe, bookRecipe.getInputs()));
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

    private static String generateRecipeKey(RecipeView recipe) {
        StringBuilder sb = new StringBuilder();
        sb.append(recipe.getDuration())
            .append(":")
            .append(recipe.getEuPerTick())
            .append("|");
        if (recipe.getInputs() != null) {
            for (ItemStack is : recipe.getInputs()) {
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
        if (recipe.getOutputs() != null) {
            for (ItemStack is : recipe.getOutputs()) {
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
        if (recipe.getSpecialItems() instanceof ItemStack) {
            ItemStack is = (ItemStack) recipe.getSpecialItems();
            if (is.getItem() != null) {
                sb.append(net.minecraft.item.Item.itemRegistry.getNameForObject(is.getItem()))
                    .append("@")
                    .append(is.getItemDamage());
            }
        } else if (recipe.getSpecialItems() instanceof ItemStack[]) {
            for (ItemStack is : (ItemStack[]) recipe.getSpecialItems()) {
                if (is != null && is.getItem() != null)
                    sb.append(net.minecraft.item.Item.itemRegistry.getNameForObject(is.getItem()))
                        .append("@")
                        .append(is.getItemDamage())
                        .append(",");
            }
        }
        sb.append("|FI:");
        if (recipe.getFluidInputs() != null) {
            for (net.minecraftforge.fluids.FluidStack fs : recipe.getFluidInputs()) {
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
        if (recipe.getFluidOutputs() != null) {
            for (net.minecraftforge.fluids.FluidStack fs : recipe.getFluidOutputs()) {
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
        for (String mapId : recipeMapRegistry.getRecipeMaps()
            .keySet()) {
            all.addAll(collectRecipes(mapId));
        }
        return all;
    }

    private static RecipeEntry toRecipeEntry(String mapId, RecipeView recipe, ItemStack[] normalInputs) {
        return new RecipeEntry(
            "gt",
            mapId,
            mapId,
            normalInputs != null ? normalInputs : new ItemStack[0],
            recipe.getOutputs(),
            recipe.getFluidInputs(),
            recipe.getFluidOutputs(),
            toSpecialItems(recipe.getSpecialItems()),
            recipe.getDuration(),
            recipe.getEuPerTick());
    }

    private static ItemStack[] toSpecialItems(Object specialItems) {
        if (specialItems instanceof ItemStack[]) {
            return (ItemStack[]) specialItems;
        }
        if (specialItems instanceof ItemStack) {
            return new ItemStack[] { (ItemStack) specialItems };
        }
        return new ItemStack[0];
    }

    interface RecipeMapRegistry {

        Map<String, RecipeMapView> getRecipeMaps();

        RecipeMapView getFromLegacyIdentifier(String identifier);
    }

    interface RecipeMapView {

        String getMapId();

        String getTransferId();

        Collection<RecipeView> getAllRecipes();

        RecipeView findRecipe(ItemStack input);
    }

    interface RecipeView {

        boolean isEnabled();

        ItemStack[] getInputs();

        ItemStack[] getOutputs();

        net.minecraftforge.fluids.FluidStack[] getFluidInputs();

        net.minecraftforge.fluids.FluidStack[] getFluidOutputs();

        Object getSpecialItems();

        int getDuration();

        int getEuPerTick();
    }

    interface SmeltingRecipeProvider {

        Map<ItemStack, ItemStack> getSmeltingList();
    }

    private static final class FurnaceSmeltingRecipeProvider implements SmeltingRecipeProvider {

        @Override
        public Map<ItemStack, ItemStack> getSmeltingList() {
            return FurnaceRecipes.smelting()
                .getSmeltingList();
        }
    }

    private static final class GregTechRecipeMapRegistry implements RecipeMapRegistry {

        @Override
        public Map<String, RecipeMapView> getRecipeMaps() {
            Map<String, RecipeMapView> result = new LinkedHashMap<String, RecipeMapView>();
            for (Map.Entry<String, RecipeMap<?>> entry : RecipeMap.ALL_RECIPE_MAPS.entrySet()) {
                result.put(entry.getKey(), new GregTechRecipeMapView(entry.getKey(), entry.getValue()));
            }
            return result;
        }

        @Override
        public RecipeMapView getFromLegacyIdentifier(String identifier) {
            RecipeMap<?> recipeMap = RecipeMap.getFromOldIdentifier(identifier);
            return recipeMap != null ? new GregTechRecipeMapView(recipeMap.unlocalizedName, recipeMap) : null;
        }
    }

    private static final class GregTechRecipeMapView implements RecipeMapView {

        private final String mapId;
        private final RecipeMap<?> recipeMap;

        private GregTechRecipeMapView(String mapId, RecipeMap<?> recipeMap) {
            this.mapId = mapId;
            this.recipeMap = recipeMap;
        }

        @Override
        public String getMapId() {
            return mapId;
        }

        @Override
        public String getTransferId() {
            if (recipeMap == null || recipeMap.getFrontend() == null
                || recipeMap.getFrontend()
                    .getUIProperties() == null) {
                return null;
            }
            return recipeMap.getFrontend()
                .getUIProperties().neiTransferRectId;
        }

        @Override
        public Collection<RecipeView> getAllRecipes() {
            Collection<GTRecipe> recipes = recipeMap != null ? recipeMap.getAllRecipes() : null;
            if (recipes == null || recipes.isEmpty()) {
                return Collections.emptyList();
            }

            List<RecipeView> wrapped = new ArrayList<RecipeView>(recipes.size());
            for (GTRecipe recipe : recipes) {
                wrapped.add(new GregTechRecipeView(recipe));
            }
            return wrapped;
        }

        @Override
        public RecipeView findRecipe(ItemStack input) {
            if (recipeMap == null) {
                return null;
            }
            GTRecipe recipe = recipeMap.findRecipeQuery()
                .items(input)
                .dontCheckStackSizes(true)
                .find();
            return recipe != null ? new GregTechRecipeView(recipe) : null;
        }
    }

    private static final class GregTechRecipeView implements RecipeView {

        private final GTRecipe recipe;

        private GregTechRecipeView(GTRecipe recipe) {
            this.recipe = recipe;
        }

        @Override
        public boolean isEnabled() {
            return recipe != null && recipe.mEnabled;
        }

        @Override
        public ItemStack[] getInputs() {
            return recipe != null ? recipe.mInputs : null;
        }

        @Override
        public ItemStack[] getOutputs() {
            return recipe != null ? recipe.mOutputs : null;
        }

        @Override
        public net.minecraftforge.fluids.FluidStack[] getFluidInputs() {
            return recipe != null ? recipe.mFluidInputs : null;
        }

        @Override
        public net.minecraftforge.fluids.FluidStack[] getFluidOutputs() {
            return recipe != null ? recipe.mFluidOutputs : null;
        }

        @Override
        public Object getSpecialItems() {
            return recipe != null ? recipe.mSpecialItems : null;
        }

        @Override
        public int getDuration() {
            return recipe != null ? recipe.mDuration : 0;
        }

        @Override
        public int getEuPerTick() {
            return recipe != null ? recipe.mEUt : 0;
        }
    }
}
