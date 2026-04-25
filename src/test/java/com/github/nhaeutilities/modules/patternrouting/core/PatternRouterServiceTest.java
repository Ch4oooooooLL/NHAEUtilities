package com.github.nhaeutilities.modules.patternrouting.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;

import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import com.github.nhaeutilities.accessor.patternrouting.HatchAssignmentHolder;

import gregtech.common.tileentities.machines.IDualInputHatch;

public class PatternRouterServiceTest {

    @Test
    public void selectCandidateRejectsDifferentRecipeCategory() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe.canner",
            "",
            "",
            "gt.integrated_circuit@5",
            "minecraft:bucket@0",
            PatternRoutingKeys.SOURCE_NEI,
            false);

        HatchRoutingCandidate selected = PatternRouterService.selectCandidate(
            metadata,
            Arrays.asList(
                HatchRoutingCandidate.empty(
                    new HatchAssignmentData(
                        "a",
                        "gt.recipe.assembler",
                        "gt.integrated_circuit@5",
                        "minecraft:bucket@0"))));

        assertNull(selected);
    }

    @Test
    public void selectCandidateRejectsDifferentCircuitKey() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe.canner",
            "",
            "",
            "gt.integrated_circuit@5",
            "minecraft:bucket@0",
            PatternRoutingKeys.SOURCE_NEI,
            false);

        HatchRoutingCandidate selected = PatternRouterService.selectCandidate(
            metadata,
            Arrays.asList(
                HatchRoutingCandidate.empty(
                    new HatchAssignmentData(
                        "a",
                        "gt.recipe.canner",
                        "gt.integrated_circuit@1",
                        "minecraft:bucket@0"))));

        assertNull(selected);
    }

    @Test
    public void selectCandidateRejectsDifferentManualItemsKey() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe.canner",
            "",
            "",
            "gt.integrated_circuit@5",
            "minecraft:bucket@0|minecraft:cell@0",
            PatternRoutingKeys.SOURCE_NEI,
            false);

        HatchRoutingCandidate selected = PatternRouterService.selectCandidate(
            metadata,
            Arrays.asList(
                HatchRoutingCandidate.empty(
                    new HatchAssignmentData(
                        "a",
                        "gt.recipe.canner",
                        "gt.integrated_circuit@5",
                        "minecraft:bucket@0"))));

        assertNull(selected);
    }

    @Test
    public void matchingHatchesWithExistingPatternsArePreferredOverEmptyHatches() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe.canner",
            "",
            "",
            "gt.integrated_circuit@5",
            "minecraft:bucket@0",
            PatternRoutingKeys.SOURCE_NEI,
            false);

        HatchRoutingCandidate empty = HatchRoutingCandidate
            .empty(new HatchAssignmentData("a", "gt.recipe.canner", "gt.integrated_circuit@5", "minecraft:bucket@0"));
        HatchRoutingCandidate populated = HatchRoutingCandidate.withPatterns(
            new HatchAssignmentData("b", "gt.recipe.canner", "gt.integrated_circuit@5", "minecraft:bucket@0"));

        HatchRoutingCandidate selected = PatternRouterService
            .selectCandidate(metadata, Arrays.asList(empty, populated));

        assertEquals("b", selected.assignment.assignmentKey);
    }

    @Test
    public void fullHatchesAreExcludedBeforeSelection() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe.canner",
            "",
            "",
            "gt.integrated_circuit@5",
            "minecraft:bucket@0",
            PatternRoutingKeys.SOURCE_NEI,
            false);

        HatchRoutingCandidate full = HatchRoutingCandidate
            .full(new HatchAssignmentData("a", "gt.recipe.canner", "gt.integrated_circuit@5", "minecraft:bucket@0"));
        HatchRoutingCandidate empty = HatchRoutingCandidate
            .empty(new HatchAssignmentData("b", "gt.recipe.canner", "gt.integrated_circuit@5", "minecraft:bucket@0"));

        HatchRoutingCandidate selected = PatternRouterService.selectCandidate(metadata, Arrays.asList(full, empty));

        assertEquals("b", selected.assignment.assignmentKey);
    }

    @Test
    public void firstEmptyMatchIsSelectedWhenNoCandidateHasPatterns() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe.canner",
            "",
            "",
            "gt.integrated_circuit@5",
            "minecraft:bucket@0|minecraft:cell@0",
            PatternRoutingKeys.SOURCE_NEI,
            false);

        HatchRoutingCandidate first = HatchRoutingCandidate.empty(
            new HatchAssignmentData(
                "a",
                "gt.recipe.canner",
                "gt.integrated_circuit@5",
                "minecraft:bucket@0|minecraft:cell@0"));
        HatchRoutingCandidate second = HatchRoutingCandidate.empty(
            new HatchAssignmentData(
                "b",
                "gt.recipe.canner",
                "gt.integrated_circuit@5",
                "minecraft:bucket@0|minecraft:cell@0"));

        HatchRoutingCandidate selected = PatternRouterService.selectCandidate(metadata, Arrays.asList(first, second));

        assertEquals("a", selected.assignment.assignmentKey);
    }

    @Test
    public void resolveAssignmentDoesNotFallbackWhenAssignmentKeyIsExplicitButMissing() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe.canner",
            "",
            "missing-key",
            "gt.integrated_circuit@5",
            "minecraft:bucket@0",
            PatternRoutingKeys.SOURCE_NEI,
            false);

        HatchAssignmentData resolved = PatternRouterService.resolveAssignment(
            metadata,
            Arrays.asList(
                new HatchAssignmentData("key-b", "gt.recipe.canner", "gt.integrated_circuit@5", "minecraft:bucket@0")));

        assertNull(resolved);
    }

    @Test
    public void resolveAssignmentReturnsNullWhenMetadataIsNotResolvable() {
        PatternRoutingNbt.RoutingMetadata metadata = PatternRoutingNbt.RoutingMetadata.EMPTY;

        HatchAssignmentData resolved = PatternRouterService.resolveAssignment(
            metadata,
            Arrays.asList(
                new HatchAssignmentData("key-b", "gt.recipe.canner", "gt.integrated_circuit@5", "minecraft:bucket@0")));

        assertNull(resolved);
    }

    @Test
    public void selectBlankFamilyCandidateReturnsFirstMatchingBlankCandidate() {
        HatchRoutingCandidate first = HatchRoutingCandidate
            .empty(new HatchAssignmentData("a", "gt.recipe.canner", "", ""));
        HatchRoutingCandidate second = HatchRoutingCandidate
            .empty(new HatchAssignmentData("b", "gt.recipe.canner", "", ""));

        HatchRoutingCandidate selected = PatternRouterService.selectBlankFamilyCandidate(Arrays.asList(first, second));

        assertEquals("a", selected.assignment.assignmentKey);
    }

    @Test
    public void selectBlankFamilyCandidateReturnsSingleNonFullCandidate() {
        HatchRoutingCandidate full = HatchRoutingCandidate
            .full(new HatchAssignmentData("a", "gt.recipe.canner", "", ""));
        HatchRoutingCandidate blank = HatchRoutingCandidate
            .empty(new HatchAssignmentData("b", "gt.recipe.canner", "", ""));

        HatchRoutingCandidate selected = PatternRouterService.selectBlankFamilyCandidate(Arrays.asList(full, blank));

        assertEquals("b", selected.assignment.assignmentKey);
    }

    @Test
    public void blankFamilyCandidateRequiresSameRecipeCategoryAndBlankSharedConfiguration() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe.canner",
            "",
            "",
            "minecraft:paper@1",
            "minecraft:paper@2",
            PatternRoutingKeys.SOURCE_NEI,
            false,
            "minecraft:paper@1",
            "[{\"item\":\"minecraft:paper@2\",\"count\":0,\"nc\":true}]",
            "{}");
        TestHatchHandler blankHandler = new TestHatchHandler(
            new HatchAssignmentData("blank", "gt.recipe.canner", "", ""));
        TestCraftingInputHatch blankHatch = blankHandler.createProxy();
        HatchRoutingCandidate blankCandidate = new HatchRoutingCandidate(
            blankHatch,
            blankHandler.assignment,
            false,
            false);

        assertTrue(PatternRouterService.isBlankFamilyCandidateForTest(metadata, blankCandidate));

        blankHandler.sharedItems = new ItemStack[] { new ItemStack(Items.paper, 1, 7) };
        assertFalse(PatternRouterService.isBlankFamilyCandidateForTest(metadata, blankCandidate));
    }

    @Test
    public void syncAssignmentUpdatesResolvedWritableHatch() {
        HatchAssignmentData assignment = new HatchAssignmentData(
            "gt.recipe.canner|minecraft:paper@1|minecraft:paper@2",
            "gt.recipe.canner",
            "minecraft:paper@1",
            "minecraft:paper@2");
        TestHatchHandler masterHandler = new TestHatchHandler(HatchAssignmentData.EMPTY);
        TestCraftingInputHatch master = masterHandler.createProxy();
        TestHatchHandler proxyHandler = new TestHatchHandler(HatchAssignmentData.EMPTY);
        proxyHandler.master = master;
        TestCraftingInputHatch proxy = proxyHandler.createProxy();

        PatternRouterService.syncAssignmentForTest(proxy, assignment);

        assertEquals(assignment.assignmentKey, proxyHandler.assignment.assignmentKey);
        assertEquals(assignment.assignmentKey, masterHandler.assignment.assignmentKey);
    }

    @Test
    public void autoConfigurationWritesCircuitThroughSrgInventoryMethods() {
        String circuitKey = PatternRoutingNbt.circuitKey(new ItemStack(Items.paper, 1, 2));
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe.canner",
            "",
            PatternRoutingNbt.buildAssignmentKey("gt.recipe.canner", circuitKey, ""),
            circuitKey,
            "",
            PatternRoutingKeys.SOURCE_NEI,
            true,
            circuitKey,
            "[]",
            "{}");
        TestHatchHandler handler = new TestHatchHandler(HatchAssignmentData.EMPTY);
        TestSrgCraftingInputHatch hatch = handler.createSrgProxy();

        assertTrue(CraftingInputHatchAccess.tryApplyRoutingConfiguration(hatch, metadata, new ItemStack[0]));
        assertEquals(circuitKey, PatternRoutingNbt.circuitKey(handler.slots[0]));
    }

    private interface TestCraftingInputHatch extends IDualInputHatch, HatchAssignmentHolder {

        IInventory getPatterns();

        int getCircuitSlot();

        ItemStack[] getSharedItems();

        ItemStack getStackInSlot(int slot);

        void setInventorySlotContents(int slot, ItemStack stack);

        Object getMaster();
    }

    private interface TestSrgCraftingInputHatch extends IDualInputHatch, HatchAssignmentHolder {

        IInventory getPatterns();

        int getCircuitSlot();

        ItemStack[] getSharedItems();

        ItemStack func_70301_a(int slot);

        void func_70299_a(int slot, ItemStack stack);

        Object getMaster();
    }

    private static final class TestHatchHandler implements InvocationHandler {

        private final TestInventory inventory = new TestInventory(4);
        private HatchAssignmentData assignment;
        private ItemStack[] sharedItems = new ItemStack[0];
        private final ItemStack[] slots = new ItemStack[16];
        private Object master;

        private TestHatchHandler(HatchAssignmentData assignment) {
            this.assignment = assignment;
        }

        private TestCraftingInputHatch createProxy() {
            return (TestCraftingInputHatch) Proxy.newProxyInstance(
                PatternRouterServiceTest.class.getClassLoader(),
                new Class<?>[] { TestCraftingInputHatch.class },
                this);
        }

        private TestSrgCraftingInputHatch createSrgProxy() {
            return (TestSrgCraftingInputHatch) Proxy.newProxyInstance(
                PatternRouterServiceTest.class.getClassLoader(),
                new Class<?>[] { TestSrgCraftingInputHatch.class },
                this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();
            if ("getPatterns".equals(methodName)) {
                return inventory;
            }
            if ("getCircuitSlot".equals(methodName)) {
                return 0;
            }
            if ("getSharedItems".equals(methodName)) {
                return sharedItems;
            }
            if ("getStackInSlot".equals(methodName) || "func_70301_a".equals(methodName)) {
                return slots[((Integer) args[0]).intValue()];
            }
            if ("setInventorySlotContents".equals(methodName) || "func_70299_a".equals(methodName)) {
                int slot = ((Integer) args[0]).intValue();
                slots[slot] = (ItemStack) args[1];
                sharedItems = rebuildSharedItems();
                return null;
            }
            if ("getMaster".equals(methodName)) {
                return master;
            }
            if ("nhaeutilities$getAssignmentData".equals(methodName)) {
                return assignment;
            }
            if ("nhaeutilities$setAssignmentData".equals(methodName)) {
                assignment = (HatchAssignmentData) args[0];
                return null;
            }
            if ("nhaeutilities$clearAssignmentData".equals(methodName)) {
                assignment = HatchAssignmentData.EMPTY;
                return null;
            }
            return defaultValue(method.getReturnType());
        }

        private ItemStack[] rebuildSharedItems() {
            java.util.List<ItemStack> values = new java.util.ArrayList<ItemStack>();
            for (int index = 0; index < 10; index++) {
                if (slots[index] != null) {
                    values.add(slots[index]);
                }
            }
            return values.toArray(new ItemStack[values.size()]);
        }
    }

    private static final class TestInventory implements IInventory {

        private final ItemStack[] slots;

        private TestInventory(int size) {
            this.slots = new ItemStack[size];
        }

        @Override
        public int getSizeInventory() {
            return slots.length;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slots[slot];
        }

        @Override
        public ItemStack decrStackSize(int slot, int amount) {
            return null;
        }

        @Override
        public ItemStack getStackInSlotOnClosing(int slot) {
            return null;
        }

        @Override
        public void setInventorySlotContents(int slot, ItemStack stack) {
            slots[slot] = stack;
        }

        @Override
        public String getInventoryName() {
            return "test";
        }

        @Override
        public boolean hasCustomInventoryName() {
            return false;
        }

        @Override
        public int getInventoryStackLimit() {
            return 64;
        }

        @Override
        public void markDirty() {}

        @Override
        public boolean isUseableByPlayer(net.minecraft.entity.player.EntityPlayer player) {
            return true;
        }

        @Override
        public void openInventory() {}

        @Override
        public void closeInventory() {}

        @Override
        public boolean isItemValidForSlot(int slot, ItemStack stack) {
            return true;
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
