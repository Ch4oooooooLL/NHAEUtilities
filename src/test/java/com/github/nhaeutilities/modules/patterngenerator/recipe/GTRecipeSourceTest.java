package com.github.nhaeutilities.modules.patterngenerator.recipe;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class GTRecipeSourceTest {

    @BeforeClass
    public static void initializeMinecraftBootstrap() {
        try {
            Class<?> bootstrap = Class.forName("net.minecraft.init.Bootstrap");
            try {
                bootstrap.getMethod("register")
                    .invoke(null);
                return;
            } catch (NoSuchMethodException ignored) {}

            bootstrap.getMethod("func_151354_b")
                .invoke(null);
        } catch (Exception ignored) {}
    }

    @After
    public void tearDown() {
        GTRecipeSource.invalidateCollectionCache();
        GTRecipeSource.resetRecipeMapRegistry();
        GTRecipeSource.resetSmeltingRecipeProvider();
    }

    @Test
    public void invalidateCollectionCacheDropsPreviouslyCollectedResults() {
        MutableRecipeMap furnaceMap = new MutableRecipeMap("gt.recipe.furnace", "furnace");
        furnaceMap.staticRecipes = Collections
            .singletonList(recipe(20, new ItemStack[] { new ItemStack(new TestItem(), 1, 0) }));
        GTRecipeSource.setRecipeMapRegistry(new FakeRegistry(furnaceMap));

        assertEquals(
            20,
            GTRecipeSource.collectRecipes("furnace")
                .get(0).duration);

        furnaceMap.staticRecipes = Collections
            .singletonList(recipe(40, new ItemStack[] { new ItemStack(new TestItem(), 1, 0) }));
        assertEquals(
            "cached value should still be returned before invalidation",
            20,
            GTRecipeSource.collectRecipes("furnace")
                .get(0).duration);

        GTRecipeSource.invalidateCollectionCache();

        assertEquals(
            40,
            GTRecipeSource.collectRecipes("furnace")
                .get(0).duration);
    }

    @Test
    public void findMatchingRecipeMapsReturnsExactMapIdMatch() {
        GTRecipeSource.setRecipeMapRegistry(
            new FakeRegistry(
                new MutableRecipeMap("gt.recipe.assembler", "assembler"),
                new MutableRecipeMap("gt.recipe.microwave", "microwave")));

        assertEquals(
            Collections.singletonList("gt.recipe.assembler"),
            GTRecipeSource.findMatchingRecipeMaps("gt.recipe.assembler"));
    }

    @Test
    public void findMatchingRecipeMapsMatchesSubstringAgainstMapIdAndTransferId() {
        GTRecipeSource.setRecipeMapRegistry(
            new FakeRegistry(
                new MutableRecipeMap("gt.recipe.furnace", "smelting"),
                new MutableRecipeMap("gt.recipe.microwave", "microwave")));

        assertEquals(Collections.singletonList("gt.recipe.microwave"), GTRecipeSource.findMatchingRecipeMaps("micro"));
        assertEquals(Collections.singletonList("gt.recipe.furnace"), GTRecipeSource.findMatchingRecipeMaps("smelt"));
    }

    @Test
    public void findMatchingRecipeMapsResolvesLegacyIdentifiers() {
        MutableRecipeMap blastMap = new MutableRecipeMap("gt.recipe.blastfurnace", "blast");
        Map<String, GTRecipeSource.RecipeMapView> legacyMappings = new LinkedHashMap<String, GTRecipeSource.RecipeMapView>();
        legacyMappings.put("legacy_blast_identifier", blastMap);
        GTRecipeSource.setRecipeMapRegistry(new FakeRegistry(legacyMappings, blastMap));

        assertEquals(
            Collections.singletonList("gt.recipe.blastfurnace"),
            GTRecipeSource.findMatchingRecipeMaps("legacy_blast_identifier"));
    }

    @Test
    public void collectRecipesIncludesDynamicFurnaceRecipesFromSmeltingList() {
        MutableRecipeMap furnaceMap = new MutableRecipeMap("gt.recipe.furnace", "furnace");
        Item inputItem = new TestItem();
        Item outputItem = new TestItem();
        ItemStack input = new ItemStack(inputItem, 1, 0);
        ItemStack output = new ItemStack(outputItem, 1, 0);
        furnaceMap.dynamicRecipes
            .put(stackKey(input), recipe(120, new ItemStack[] { input.copy() }, new ItemStack[] { output.copy() }));
        GTRecipeSource.setRecipeMapRegistry(new FakeRegistry(furnaceMap));
        GTRecipeSource.setSmeltingRecipeProvider(() -> {
            Map<ItemStack, ItemStack> smelting = new LinkedHashMap<ItemStack, ItemStack>();
            smelting.put(input.copy(), output.copy());
            return smelting;
        });

        java.util.List<RecipeEntry> collected = GTRecipeSource.collectRecipes("furnace");

        assertEquals(1, collected.size());
        assertEquals(120, collected.get(0).duration);
        assertEquals("gt.recipe.furnace", collected.get(0).recipeMapId);
        assertEquals(outputItem, collected.get(0).outputs[0].getItem());
    }

    @Test
    public void collectRecipesIncludesMicrowaveBookFallbackRecipe() {
        MutableRecipeMap microwaveMap = new MutableRecipeMap("gt.recipe.microwave", "microwave");
        ItemStack bookInput = new ItemStack(net.minecraft.init.Items.book, 1, 0);
        ItemStack bookOutput = new ItemStack(new TestItem(), 1, 0);
        ItemStack unrelatedInput = new ItemStack(new TestItem(), 1, 0);
        ItemStack unrelatedOutput = new ItemStack(new TestItem(), 1, 0);
        microwaveMap.dynamicRecipes.put(
            stackKey(bookInput),
            recipe(60, new ItemStack[] { bookInput.copy() }, new ItemStack[] { bookOutput.copy() }));
        GTRecipeSource.setRecipeMapRegistry(new FakeRegistry(microwaveMap));
        GTRecipeSource.setSmeltingRecipeProvider(() -> {
            Map<ItemStack, ItemStack> smelting = new LinkedHashMap<ItemStack, ItemStack>();
            smelting.put(unrelatedInput.copy(), unrelatedOutput.copy());
            return smelting;
        });

        java.util.List<RecipeEntry> collected = GTRecipeSource.collectRecipes("microwave");

        assertEquals(1, collected.size());
        assertEquals("gt.recipe.microwave", collected.get(0).recipeMapId);
        assertEquals(2, microwaveMap.findRecipeInputs.size());
        assertEquals("NULL", microwaveMap.findRecipeInputs.get(1));
        assertEquals(bookOutput.getItem(), collected.get(0).outputs[0].getItem());
    }

    private static FakeRecipe recipe(int duration, ItemStack[] inputs) {
        return recipe(duration, inputs, new ItemStack[] { new ItemStack(new TestItem(), 1, 0) });
    }

    private static FakeRecipe recipe(int duration, ItemStack[] inputs, ItemStack[] outputs) {
        return new FakeRecipe(
            true,
            inputs,
            outputs,
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            duration,
            8);
    }

    private static String stackKey(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return "NULL";
        }
        return stack.getItem() + "@" + stack.getItemDamage() + "@" + stack.stackSize;
    }

    private static final class TestItem extends Item {
    }

    private static final class FakeRegistry implements GTRecipeSource.RecipeMapRegistry {

        private final Map<String, GTRecipeSource.RecipeMapView> maps = new LinkedHashMap<String, GTRecipeSource.RecipeMapView>();
        private final Map<String, GTRecipeSource.RecipeMapView> legacyMappings = new LinkedHashMap<String, GTRecipeSource.RecipeMapView>();

        private FakeRegistry(GTRecipeSource.RecipeMapView... maps) {
            this(Collections.<String, GTRecipeSource.RecipeMapView>emptyMap(), maps);
        }

        private FakeRegistry(Map<String, GTRecipeSource.RecipeMapView> legacyMappings,
            GTRecipeSource.RecipeMapView... maps) {
            for (GTRecipeSource.RecipeMapView map : maps) {
                this.maps.put(map.getMapId(), map);
            }
            if (legacyMappings != null) {
                this.legacyMappings.putAll(legacyMappings);
            }
        }

        @Override
        public Map<String, GTRecipeSource.RecipeMapView> getRecipeMaps() {
            return maps;
        }

        @Override
        public GTRecipeSource.RecipeMapView getFromLegacyIdentifier(String identifier) {
            return legacyMappings.get(identifier);
        }
    }

    private static final class MutableRecipeMap implements GTRecipeSource.RecipeMapView {

        private final String mapId;
        private final String transferId;
        private Collection<GTRecipeSource.RecipeView> staticRecipes = Collections.emptyList();
        private final Map<String, GTRecipeSource.RecipeView> dynamicRecipes = new LinkedHashMap<String, GTRecipeSource.RecipeView>();
        private final java.util.List<String> findRecipeInputs = new java.util.ArrayList<String>();

        private MutableRecipeMap(String mapId, String transferId) {
            this.mapId = mapId;
            this.transferId = transferId;
        }

        @Override
        public String getMapId() {
            return mapId;
        }

        @Override
        public String getTransferId() {
            return transferId;
        }

        @Override
        public Collection<GTRecipeSource.RecipeView> getAllRecipes() {
            return staticRecipes;
        }

        @Override
        public GTRecipeSource.RecipeView findRecipe(ItemStack input) {
            String key = stackKey(input);
            findRecipeInputs.add(key);
            return dynamicRecipes.get(key);
        }
    }

    private static final class FakeRecipe implements GTRecipeSource.RecipeView {

        private final boolean enabled;
        private final ItemStack[] inputs;
        private final ItemStack[] outputs;
        private final FluidStack[] fluidInputs;
        private final FluidStack[] fluidOutputs;
        private final Object specialItems;
        private final int duration;
        private final int euPerTick;

        private FakeRecipe(boolean enabled, ItemStack[] inputs, ItemStack[] outputs, FluidStack[] fluidInputs,
            FluidStack[] fluidOutputs, Object specialItems, int duration, int euPerTick) {
            this.enabled = enabled;
            this.inputs = inputs;
            this.outputs = outputs;
            this.fluidInputs = fluidInputs;
            this.fluidOutputs = fluidOutputs;
            this.specialItems = specialItems;
            this.duration = duration;
            this.euPerTick = euPerTick;
        }

        @Override
        public boolean isEnabled() {
            return enabled;
        }

        @Override
        public ItemStack[] getInputs() {
            return inputs;
        }

        @Override
        public ItemStack[] getOutputs() {
            return outputs;
        }

        @Override
        public FluidStack[] getFluidInputs() {
            return fluidInputs;
        }

        @Override
        public FluidStack[] getFluidOutputs() {
            return fluidOutputs;
        }

        @Override
        public Object getSpecialItems() {
            return specialItems;
        }

        @Override
        public int getDuration() {
            return duration;
        }

        @Override
        public int getEuPerTick() {
            return euPerTick;
        }
    }
}
