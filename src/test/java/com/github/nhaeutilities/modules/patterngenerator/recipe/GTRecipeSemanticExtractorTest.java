package com.github.nhaeutilities.modules.patterngenerator.recipe;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class GTRecipeSemanticExtractorTest {

    private static final int CIRCUIT_DAMAGE = 999;
    private static final Item TEST_ITEM = new Item();
    private static Method originalCircuitMethod;

    @BeforeClass
    public static void setUp() {
        backupOriginalCircuitMethod();
        injectTestCircuitMethod();
    }

    @After
    public void tearDown() {
        if (originalCircuitMethod != null) {
            GTRecipeSemanticExtractor.setCircuitMethod(originalCircuitMethod);
        }
    }

    private static ItemStack circuit(int amount) {
        return new ItemStack(TEST_ITEM, amount, CIRCUIT_DAMAGE);
    }

    private static ItemStack stack(int amount, int damage) {
        return new ItemStack(TEST_ITEM, amount, damage);
    }

    private static ItemStack nc(int damage) {
        return new ItemStack(TEST_ITEM, 0, damage);
    }

    @Test
    public void assemblerCircuitDetectedAsProgrammingCircuit() {
        RecipeEntry recipe = new RecipeEntry(
            "gt",
            "gt.recipe.assembler",
            "assembler",
            new ItemStack[] { circuit(1), stack(4, 0) },
            new ItemStack[] { stack(1, 0) },
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            200,
            30,
            "recipe1");

        RecipeAnalysisSnapshot snapshot = GTRecipeSemanticExtractor.extract(recipe);

        assertEquals(RecipeAnalysisSnapshot.Status.COMPLETE, snapshot.status);
        assertEquals("gt.recipe.assembler", snapshot.recipeMapId);
        assertFalse(snapshot.programmingCircuit.isEmpty());
        assertEquals(0, snapshot.nonConsumableSignatures.size());
        assertEquals(1, snapshot.consumableCount);

        assertEquals(1, findByType(snapshot.inputSnapshots, InputSemanticType.PROGRAMMING_CIRCUIT).size());
        assertEquals(0, findByType(snapshot.inputSnapshots, InputSemanticType.NON_CONSUMABLE).size());
        assertEquals(1, findByType(snapshot.inputSnapshots, InputSemanticType.CONSUMABLE).size());
    }

    @Test
    public void extruderShapeDetectedAsNonConsumable() {
        RecipeEntry recipe = new RecipeEntry(
            "gt",
            "gt.recipe.extruder",
            "extruder",
            new ItemStack[] { nc(10), stack(1, 0) },
            new ItemStack[] { stack(1, 0) },
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            200,
            30,
            "recipe2");

        RecipeAnalysisSnapshot snapshot = GTRecipeSemanticExtractor.extract(recipe);

        assertEquals(RecipeAnalysisSnapshot.Status.COMPLETE, snapshot.status);
        assertTrue(snapshot.programmingCircuit.isEmpty());
        assertEquals(1, snapshot.nonConsumableSignatures.size());
        assertEquals(1, snapshot.consumableCount);

        List<RecipeInputSemanticSnapshot> ncSnap = findByType(
            snapshot.inputSnapshots,
            InputSemanticType.NON_CONSUMABLE);
        assertEquals(1, ncSnap.size());
        assertEquals(0, ncSnap.get(0).inputIndex);
        assertEquals(0, ncSnap.get(0).stack.stackSize);
    }

    @Test
    public void fluidSolidifierMoldDetectedAsNonConsumable() {
        RecipeEntry recipe = new RecipeEntry(
            "gt",
            "gt.recipe.fluidsolidifier",
            "fluid_solidifier",
            new ItemStack[] { nc(15), stack(1, 0) },
            new ItemStack[] { stack(1, 0) },
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            200,
            30,
            "recipe3");

        RecipeAnalysisSnapshot snapshot = GTRecipeSemanticExtractor.extract(recipe);

        assertEquals(1, snapshot.nonConsumableSignatures.size());
        assertFalse(
            snapshot.nonConsumableSignatures.get(0)
                .isEmpty());
    }

    @Test
    public void metalBenderCircuitDetectedAsProgrammingCircuit() {
        RecipeEntry recipe = new RecipeEntry(
            "gt",
            "gt.recipe.metalbender",
            "metal_bender",
            new ItemStack[] { circuit(1), stack(2, 0) },
            new ItemStack[] { stack(1, 0) },
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            200,
            30,
            "recipe4");

        RecipeAnalysisSnapshot snapshot = GTRecipeSemanticExtractor.extract(recipe);

        assertEquals(RecipeAnalysisSnapshot.Status.COMPLETE, snapshot.status);
        assertFalse(snapshot.programmingCircuit.isEmpty());
        assertEquals(0, snapshot.nonConsumableSignatures.size());
        assertEquals(1, snapshot.consumableCount);
    }

    @Test
    public void genericFutureMapNonCircuitZeroStackSizeDetectedAsNonConsumable() {
        RecipeEntry recipe = new RecipeEntry(
            "gt",
            "gt.recipe.somefuturemap",
            "future_map",
            new ItemStack[] { stack(8, 0), nc(20) },
            new ItemStack[] { stack(1, 0) },
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            200,
            30,
            "recipe5");

        RecipeAnalysisSnapshot snapshot = GTRecipeSemanticExtractor.extract(recipe);

        assertEquals(1, snapshot.nonConsumableSignatures.size());
        assertEquals(1, snapshot.consumableCount);

        List<RecipeInputSemanticSnapshot> ncSnap = findByType(
            snapshot.inputSnapshots,
            InputSemanticType.NON_CONSUMABLE);
        assertEquals(1, ncSnap.size());
        assertEquals(1, ncSnap.get(0).inputIndex);
    }

    @Test
    public void groupingKeyOnlyHasCircuitAndNonConsumables() {
        RecipeEntry recipe = new RecipeEntry(
            "gt",
            "gt.recipe.assembler",
            "assembler",
            new ItemStack[] { circuit(4), nc(12), stack(4, 0), stack(2, 1) },
            new ItemStack[] { stack(1, 0) },
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            200,
            30,
            "recipe6");

        RecipeAnalysisSnapshot snapshot = GTRecipeSemanticExtractor.extract(recipe);

        assertFalse(snapshot.groupingKey.isEmpty());
        String circuitSig = GTRecipeSemanticExtractor.itemSignature(circuit(4));
        String ncSig = GTRecipeSemanticExtractor.itemSignature(nc(12));
        assertTrue(snapshot.groupingKey.contains(circuitSig));
        assertTrue(snapshot.groupingKey.contains(ncSig));
    }

    @Test
    public void nullRecipeReturnsUnresolved() {
        RecipeAnalysisSnapshot snapshot = GTRecipeSemanticExtractor.extract(null);

        assertEquals(RecipeAnalysisSnapshot.Status.UNRESOLVED, snapshot.status);
        assertTrue(snapshot.programmingCircuit.isEmpty());
        assertTrue(snapshot.nonConsumableSignatures.isEmpty());
        assertEquals(0, snapshot.consumableCount);
        assertTrue(snapshot.groupingKey.isEmpty());
    }

    @Test
    public void emptyRecipeReturnsCompleteWithEmptyResults() {
        RecipeEntry recipe = new RecipeEntry(
            "gt",
            "gt.recipe.assembler",
            "assembler",
            new ItemStack[0],
            new ItemStack[0],
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            200,
            30,
            "empty");

        RecipeAnalysisSnapshot snapshot = GTRecipeSemanticExtractor.extract(recipe);

        assertEquals(RecipeAnalysisSnapshot.Status.COMPLETE, snapshot.status);
        assertTrue(snapshot.groupingKey.isEmpty());
    }

    @Test
    public void specialItemsDetectedAsSpecialSlot() {
        RecipeEntry recipe = new RecipeEntry(
            "gt",
            "gt.recipe.assembler",
            "assembler",
            new ItemStack[0],
            new ItemStack[0],
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[] { stack(1, 5) },
            200,
            30,
            "special-recipe");

        RecipeAnalysisSnapshot snapshot = GTRecipeSemanticExtractor.extract(recipe);

        List<RecipeInputSemanticSnapshot> specialSnap = findByType(
            snapshot.inputSnapshots,
            InputSemanticType.SPECIAL_SLOT);
        assertEquals(1, specialSnap.size());
    }

    @Test
    public void buildGroupingKeyEmptyForNoInputs() {
        String key = GTRecipeSemanticExtractor.buildGroupingKey("", null);
        assertEquals("", key);
    }

    @Test
    public void buildGroupingKeyOnlyCircuit() {
        String key = GTRecipeSemanticExtractor.buildGroupingKey("circuit@1", null);
        assertEquals("circuit@1", key);
    }

    @Test
    public void buildGroupingKeyOnlyNonConsumables() {
        List<String> nc = java.util.Arrays.asList("shape@5");
        String key = GTRecipeSemanticExtractor.buildGroupingKey("", nc);
        assertEquals("shape@5", key);
    }

    @Test
    public void buildGroupingKeyCircuitAndNonConsumables() {
        List<String> nc = java.util.Arrays.asList("shape@5", "mold@3");
        String key = GTRecipeSemanticExtractor.buildGroupingKey("circuit@1", nc);
        assertEquals("circuit@1|shape@5|mold@3", key);
    }

    @Test
    public void itemSignatureNullReturnsEmpty() {
        assertEquals("", GTRecipeSemanticExtractor.itemSignature(null));
    }

    @Test
    public void itemSignatureReturnsNonNull() {
        ItemStack s = stack(1, 0);
        String sig = GTRecipeSemanticExtractor.itemSignature(s);
        assertFalse(sig.isEmpty());
    }

    @Test
    public void classifyInputNonConsumableByStackSize() {
        assertEquals(InputSemanticType.NON_CONSUMABLE, GTRecipeSemanticExtractor.classifyInput(nc(0)));
    }

    @Test
    public void classifyInputConsumable() {
        assertEquals(InputSemanticType.CONSUMABLE, GTRecipeSemanticExtractor.classifyInput(stack(1, 0)));
    }

    @Test
    public void classifyInputCircuit() {
        assertEquals(InputSemanticType.PROGRAMMING_CIRCUIT, GTRecipeSemanticExtractor.classifyInput(circuit(1)));
    }

    @Test
    public void classifyInputNullReturnsOther() {
        assertEquals(InputSemanticType.OTHER, GTRecipeSemanticExtractor.classifyInput(null));
    }

    @Test
    public void simpleExtractConsumableAndNonConsumable() {
        RecipeEntry recipe = new RecipeEntry(
            "gt",
            "gt.recipe.test",
            "test",
            new ItemStack[] { nc(5), stack(2, 0) },
            new ItemStack[0],
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            100,
            10,
            "simple");

        RecipeAnalysisSnapshot snapshot = GTRecipeSemanticExtractor.extract(recipe);

        assertEquals(RecipeAnalysisSnapshot.Status.COMPLETE, snapshot.status);
        assertEquals(1, snapshot.nonConsumableSignatures.size());
        assertEquals(1, snapshot.consumableCount);
        assertEquals(2, snapshot.inputSnapshots.size());
        assertEquals(InputSemanticType.NON_CONSUMABLE, snapshot.inputSnapshots.get(0).type);
        assertEquals(InputSemanticType.CONSUMABLE, snapshot.inputSnapshots.get(1).type);
    }

    private static void backupOriginalCircuitMethod() {
        try {
            java.lang.reflect.Field field = GTRecipeSemanticExtractor.class
                .getDeclaredField("GT_IS_ANY_INTEGRATED_CIRCUIT");
            field.setAccessible(true);
            originalCircuitMethod = (Method) field.get(null);
        } catch (Exception ignored) {}
    }

    private static void injectTestCircuitMethod() {
        try {
            Method testMethod = TestCircuitDetector.class.getMethod("isAnyIntegratedCircuit", ItemStack.class);
            GTRecipeSemanticExtractor.setCircuitMethod(testMethod);
        } catch (Exception ignored) {}
    }

    private static List<RecipeInputSemanticSnapshot> findByType(List<RecipeInputSemanticSnapshot> snapshots,
        InputSemanticType type) {
        java.util.List<RecipeInputSemanticSnapshot> result = new java.util.ArrayList<RecipeInputSemanticSnapshot>();
        for (RecipeInputSemanticSnapshot snap : snapshots) {
            if (snap.type == type) {
                result.add(snap);
            }
        }
        return result;
    }

    public static class TestCircuitDetector {

        public static boolean isAnyIntegratedCircuit(ItemStack stack) {
            return stack != null && stack.getItemDamage() == CIRCUIT_DAMAGE;
        }
    }
}
