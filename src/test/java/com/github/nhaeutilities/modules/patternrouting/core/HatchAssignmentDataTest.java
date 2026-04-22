package com.github.nhaeutilities.modules.patternrouting.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

import gregtech.common.tileentities.machines.IDualInputHatch;

public class HatchAssignmentDataTest {

    @Test
    public void toNbtAndFromNbtRoundTripDescriptorAssignmentFields() {
        HatchAssignmentData original = new HatchAssignmentData(
            "gt.recipe.assembler|circuit-a|manual-a",
            "gt.recipe.assembler",
            "circuit-a",
            "manual-a");

        NBTTagCompound tag = original.toNbt();
        HatchAssignmentData restored = HatchAssignmentData.fromNbt(tag);

        assertTrue(restored.isAssigned());
        assertEquals("gt.recipe.assembler|circuit-a|manual-a", restored.assignmentKey);
        assertEquals("gt.recipe.assembler", restored.recipeCategory);
        assertEquals("gt.recipe.assembler", restored.recipeFamily);
        assertEquals("", restored.recipeId);
        assertEquals("circuit-a", restored.circuitKey);
        assertEquals("manual-a", restored.manualItemsKey);
    }

    @Test
    public void fromNbtNormalizesMissingValuesToEmptyStrings() {
        HatchAssignmentData restored = HatchAssignmentData.fromNbt(new NBTTagCompound());

        assertFalse(restored.isAssigned());
        assertEquals("", restored.assignmentKey);
        assertEquals("", restored.recipeCategory);
        assertEquals("", restored.recipeFamily);
        assertEquals("", restored.recipeId);
        assertEquals("", restored.circuitKey);
        assertEquals("", restored.manualItemsKey);
    }

    @Test
    public void sharedItemDescriptorTreatsFirstSharedItemAsCircuitAndRestAsManualItems() {
        ItemStack circuit = new ItemStack(Items.paper, 1, 1);
        ItemStack mold = new ItemStack(Items.paper, 1, 2);

        CraftingInputHatchAccess.SharedItemDescriptor descriptor = new CraftingInputHatchAccess.SharedItemDescriptor(
            circuit,
            new ItemStack[] { mold },
            2);

        assertEquals(PatternRoutingNbt.itemSignature(circuit), PatternRoutingNbt.circuitKey(descriptor.circuit));
        assertEquals(PatternRoutingNbt.itemSignature(mold), PatternRoutingNbt.manualItemsKey(descriptor.manualItems));
    }

    @Test
    public void toDescriptorReturnsRecipeCategoryCircuitAndManualItems() {
        HatchAssignmentData original = new HatchAssignmentData(
            "gt.recipe.assembler|circuit-a|manual-a",
            "gt.recipe.assembler",
            "circuit-a",
            "manual-a");

        RoutingDescriptor descriptor = original.toDescriptor();

        assertEquals("gt.recipe.assembler", descriptor.recipeCategory);
        assertEquals("circuit-a", descriptor.circuitKey);
        assertEquals("manual-a", descriptor.manualItemsKey);
    }

    @Test
    public void isCraftingInputHatchAcceptsDualInputHatchWithCraftingAccessors() {
        Object hatch = Proxy.newProxyInstance(
            HatchAssignmentDataTest.class.getClassLoader(),
            new Class<?>[] { TestCraftingInputHatch.class },
            new SuperCraftingInputLikeHandler());

        assertTrue(CraftingInputHatchAccess.isCraftingInputHatch(hatch));
    }

    private interface TestCraftingInputHatch extends IDualInputHatch {

        IInventory getPatterns();

        int getCircuitSlot();

        ItemStack[] getSharedItems();
    }

    private static final class SuperCraftingInputLikeHandler implements InvocationHandler {

        private final ItemStack[] sharedItems = new ItemStack[] { new ItemStack(Items.paper, 1, 3),
            new ItemStack(Items.paper, 1, 4) };
        private final IInventory patterns = (IInventory) Proxy.newProxyInstance(
            HatchAssignmentDataTest.class.getClassLoader(),
            new Class<?>[] { IInventory.class },
            new EmptyInventoryHandler());

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();
            if ("getCircuitSlot".equals(methodName)) {
                return 0;
            }
            if ("getSharedItems".equals(methodName)) {
                return sharedItems;
            }
            if ("getPatterns".equals(methodName)) {
                return patterns;
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static final class EmptyInventoryHandler implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("getSizeInventory".equals(method.getName())) {
                return 0;
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0F;
        }
        if (returnType == double.class) {
            return 0D;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }
}
