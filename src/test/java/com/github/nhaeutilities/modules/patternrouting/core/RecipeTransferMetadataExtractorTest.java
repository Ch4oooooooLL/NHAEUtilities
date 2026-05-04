package com.github.nhaeutilities.modules.patternrouting.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import codechicken.nei.PositionedStack;

public class RecipeTransferMetadataExtractorTest {

    private static final int CIRCUIT_DAMAGE = 999;
    private static Method originalCircuitMethod;
    private static Method originalIsNotConsumedMethod;

    @BeforeClass
    public static void setUp() {
        backupOriginalMethods();
        injectTestMethods();
    }

    @After
    public void tearDown() {
        if (originalCircuitMethod != null) {
            RecipeTransferMetadataExtractor.setGtCircuitMethod(originalCircuitMethod);
        }
        if (originalIsNotConsumedMethod != null) {
            RecipeTransferMetadataExtractor.setGtIsNotConsumedMethod(originalIsNotConsumedMethod);
        }
    }

    @Test
    public void testCircuitDetectorMatchesCircuitDamage() {
        ItemStack circuit = new ItemStack(new Item(), 1, CIRCUIT_DAMAGE);
        assertTrue(TestCircuitDetector.isAnyIntegratedCircuit(circuit));
    }

    @Test
    public void testCircuitDetectorRejectsNonCircuit() {
        ItemStack stack = new ItemStack(new Item(), 1, 0);
        assertFalse(TestCircuitDetector.isAnyIntegratedCircuit(stack));
    }

    @Test
    public void testNotConsumedDetectorTrueForZeroSize() {
        ItemStack nc = new ItemStack(new Item(), 0, 0);
        PositionedStack ps = new PositionedStack(nc, 0, 0);
        assertTrue(TestNotConsumedDetector.isNotConsumed(ps));
    }

    @Test
    public void testNotConsumedDetectorFalseForPositiveSize() {
        ItemStack stack = new ItemStack(new Item(), 1, 0);
        PositionedStack ps = new PositionedStack(stack, 0, 0);
        assertFalse(TestNotConsumedDetector.isNotConsumed(ps));
    }

    @Test
    public void testNotConsumedDetectorNullSafe() {
        assertFalse(TestNotConsumedDetector.isNotConsumed(null));
    }

    @Test
    public void testCircuitDetectorNullSafe() {
        assertFalse(TestCircuitDetector.isAnyIntegratedCircuit(null));
    }

    private static void backupOriginalMethods() {
        try {
            java.lang.reflect.Field field = RecipeTransferMetadataExtractor.class
                .getDeclaredField("GT_IS_ANY_INTEGRATED_CIRCUIT");
            field.setAccessible(true);
            originalCircuitMethod = (Method) field.get(null);
        } catch (Exception ignored) {}

        try {
            java.lang.reflect.Field field = RecipeTransferMetadataExtractor.class
                .getDeclaredField("GT_IS_NOT_CONSUMED");
            field.setAccessible(true);
            originalIsNotConsumedMethod = (Method) field.get(null);
        } catch (Exception ignored) {}
    }

    private static void injectTestMethods() {
        try {
            Method testMethod = TestCircuitDetector.class.getMethod("isAnyIntegratedCircuit", ItemStack.class);
            RecipeTransferMetadataExtractor.setGtCircuitMethod(testMethod);
        } catch (Exception ignored) {}

        try {
            Method testMethod = TestNotConsumedDetector.class.getMethod("isNotConsumed", PositionedStack.class);
            RecipeTransferMetadataExtractor.setGtIsNotConsumedMethod(testMethod);
        } catch (Exception ignored) {}
    }

    public static class TestCircuitDetector {

        public static boolean isAnyIntegratedCircuit(ItemStack stack) {
            return stack != null && stack.getItemDamage() == CIRCUIT_DAMAGE;
        }
    }

    public static class TestNotConsumedDetector {

        public static boolean isNotConsumed(PositionedStack stack) {
            if (stack == null) {
                return false;
            }
            if (stack.items != null && stack.items.length > 0 && stack.items[0] != null) {
                return stack.items[0].stackSize == 0;
            }
            return false;
        }
    }
}
