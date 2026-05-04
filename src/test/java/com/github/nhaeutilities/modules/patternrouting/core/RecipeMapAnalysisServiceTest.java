package com.github.nhaeutilities.modules.patternrouting.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.nhaeutilities.modules.patterngenerator.recipe.GTRecipeSemanticExtractor;
import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;

public class RecipeMapAnalysisServiceTest {

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

    private static ItemStack circuit(int damage) {
        return new ItemStack(TEST_ITEM, 1, CIRCUIT_DAMAGE + damage);
    }

    private static ItemStack stack(int amount, int damage) {
        return new ItemStack(TEST_ITEM, amount, damage);
    }

    private static ItemStack nc(int damage) {
        return new ItemStack(TEST_ITEM, 0, damage);
    }

    @Test
    public void analyzeSemanticEntriesGroupsByCircuit() {
        RecipeEntry r1 = new RecipeEntry(
            "gt",
            "gt.recipe.assembler",
            "assembler",
            new ItemStack[] { circuit(0), stack(1, 0) },
            new ItemStack[] { stack(1, 0) },
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            200,
            30,
            "r1");
        RecipeEntry r2 = new RecipeEntry(
            "gt",
            "gt.recipe.assembler",
            "assembler",
            new ItemStack[] { circuit(0), stack(2, 0) },
            new ItemStack[] { stack(1, 0) },
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            200,
            30,
            "r2");
        RecipeEntry r3 = new RecipeEntry(
            "gt",
            "gt.recipe.assembler",
            "assembler",
            new ItemStack[] { circuit(1), stack(1, 0) },
            new ItemStack[] { stack(2, 0) },
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            200,
            30,
            "r3");

        RecipeMapAnalysisResult result = RecipeMapAnalysisService.analyzeSemanticEntries(Arrays.asList(r1, r2, r3));

        assertEquals(2, result.totalTypeCount);
        assertEquals(1, result.repeatedTypes.size());
        assertEquals(2, result.repeatedTypes.get(0).matchCount);
        assertEquals(1, result.singleOccurrenceTypes.size());
        assertEquals(1, result.singleOccurrenceTypes.get(0).matchCount);
        assertTrue(!result.hasIncompleteAnalysis);
    }

    @Test
    public void analyzeSemanticEntriesGroupsByNonConsumable() {
        RecipeEntry r1 = new RecipeEntry(
            "gt",
            "gt.recipe.fluidsolidifier",
            "fluidsolidifier",
            new ItemStack[] { nc(10), stack(1, 0) },
            new ItemStack[] { stack(1, 0) },
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            200,
            30,
            "f1");
        RecipeEntry r2 = new RecipeEntry(
            "gt",
            "gt.recipe.fluidsolidifier",
            "fluidsolidifier",
            new ItemStack[] { nc(10), stack(4, 0) },
            new ItemStack[] { stack(1, 0) },
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            200,
            30,
            "f2");
        RecipeEntry r3 = new RecipeEntry(
            "gt",
            "gt.recipe.fluidsolidifier",
            "fluidsolidifier",
            new ItemStack[] { nc(20), stack(4, 0) },
            new ItemStack[] { stack(1, 0) },
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            200,
            30,
            "f3");

        RecipeMapAnalysisResult result = RecipeMapAnalysisService.analyzeSemanticEntries(Arrays.asList(r1, r2, r3));

        assertEquals(2, result.totalTypeCount);
        assertEquals(1, result.repeatedTypes.size());
        assertEquals(2, result.repeatedTypes.get(0).matchCount);
        assertEquals(1, result.singleOccurrenceTypes.size());
    }

    @Test
    public void emptyInputReturnsEmptyResult() {
        RecipeMapAnalysisResult result = RecipeMapAnalysisService
            .analyzeSemanticEntries(Collections.<RecipeEntry>emptyList());
        assertNotNull(result);
        assertEquals(0, result.totalTypeCount);
        assertTrue(result.repeatedTypes.isEmpty());
        assertTrue(result.singleOccurrenceTypes.isEmpty());
    }

    @Test
    public void nullInputReturnsEmptyResult() {
        RecipeMapAnalysisResult result = RecipeMapAnalysisService.analyzeSemanticEntries(null);
        assertNotNull(result);
        assertEquals(0, result.totalTypeCount);
    }

    @Test
    public void displaySummaryShowsCircuitAndNc() {
        RecipeEntry recipe = new RecipeEntry(
            "gt",
            "gt.recipe.assembler",
            "assembler",
            new ItemStack[] { circuit(0), nc(10) },
            new ItemStack[] { stack(1, 0) },
            new FluidStack[0],
            new FluidStack[0],
            new ItemStack[0],
            200,
            30,
            "c1");

        RecipeMapAnalysisResult result = RecipeMapAnalysisService.analyzeSemanticEntries(Arrays.asList(recipe));

        assertEquals(1, result.totalTypeCount);
        String summary = result.singleOccurrenceTypes.get(0).displaySummary;
        assertFalse(summary.isEmpty());
        assertFalse(summary.equals("No circuit or non-consumables"));
        assertTrue(summary.contains("+"));
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

    public static class TestCircuitDetector {

        public static boolean isAnyIntegratedCircuit(ItemStack stack) {
            if (stack == null) {
                return false;
            }
            return stack.getItemDamage() >= CIRCUIT_DAMAGE;
        }
    }
}
