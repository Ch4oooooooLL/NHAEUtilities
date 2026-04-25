package com.github.nhaeutilities.modules.patternrouting.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import org.junit.After;
import org.junit.Test;

import com.github.nhaeutilities.accessor.patternrouting.HatchAssignmentHolder;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IGridStorage;
import appeng.api.networking.security.BaseActionSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.IReadOnlyCollection;
import cpw.mods.fml.common.registry.GameData;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.tileentities.machines.IDualInputHatch;

public class PatternRouterServiceTest {

    private static final int TEST_CIRCUIT_ITEM_ID = 5100;
    private static final int TEST_MANUAL_ITEM_ID = 5101;
    private static final String TEST_CIRCUIT_ITEM_NAME = "nhaeutilities:test_blank_family_circuit";
    private static final String TEST_MANUAL_ITEM_NAME = "nhaeutilities:test_blank_family_manual";

    private static PatternRoutingNbt.RoutingMetadata blankFamilyMetadata() {
        return new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe",
            "recipe-id",
            "",
            "",
            "",
            "source",
            false,
            blankFamilyCircuitKey(),
            "[{\"item\":\"" + PatternRoutingNbt.itemSignature(blankFamilyManualStack())
                + "\",\"count\":0,\"nc\":true}]",
            "snapshot");
    }

    private static String blankFamilyCircuitKey() {
        return PatternRoutingNbt.circuitKey(blankFamilyCircuitStack());
    }

    private static ItemStack blankFamilyCircuitStack() {
        return new ItemStack(getOrCreateTestItem(TEST_CIRCUIT_ITEM_ID, TEST_CIRCUIT_ITEM_NAME), 1, 0);
    }

    private static ItemStack blankFamilyManualStack() {
        return new ItemStack(getOrCreateTestItem(TEST_MANUAL_ITEM_ID, TEST_MANUAL_ITEM_NAME), 1, 2);
    }

    private static String blankFamilyManualItemsKey() {
        return PatternRoutingNbt.manualItemsKey(new ItemStack[] { blankFamilyManualStack() });
    }

    private static PatternRoutingNbt.RoutingMetadata blankFamilyManualOnlyMetadata() {
        return new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe",
            "recipe-id",
            "",
            "",
            blankFamilyManualItemsKey(),
            "source",
            false,
            "",
            "[{\"item\":\"" + PatternRoutingNbt.itemSignature(blankFamilyManualStack())
                + "\",\"count\":0,\"nc\":true}]",
            "snapshot");
    }

    private static String blankFamilyManualItemId() {
        Object name = net.minecraft.item.Item.itemRegistry.getNameForObject(blankFamilyManualStack().getItem());
        return String.valueOf(name);
    }

    private static net.minecraft.item.Item getOrCreateTestItem(int id, String name) {
        net.minecraft.item.Item existing = (net.minecraft.item.Item) GameData.getItemRegistry()
            .getObject(name);
        if (existing != null) {
            return existing;
        }
        net.minecraft.item.Item created = new net.minecraft.item.Item();
        try {
            Method addObjectRaw = GameData.getItemRegistry()
                .getClass()
                .getDeclaredMethod("addObjectRaw", int.class, String.class, Object.class);
            addObjectRaw.setAccessible(true);
            addObjectRaw.invoke(GameData.getItemRegistry(), id, name, created);
            return created;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to register test item " + name, e);
        }
    }

    @After
    public void resetAeItemStackFactory() {
        PatternRouterService.resetAeItemStackFactoryForTest();
    }

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

        blankHandler.slots[0] = new ItemStack(Items.paper, 1, 7);
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

    @Test
    public void autoConfigurationKeepsManualOnlySharedItemOutOfCircuitDescriptor() {
        PatternRoutingNbt.RoutingMetadata metadata = blankFamilyManualOnlyMetadata();
        PatternRoutingNbt.RoutingMetadata configured = PatternRoutingNbt.withConfiguredAssignment(metadata);
        TestHatchHandler handler = new TestHatchHandler(HatchAssignmentData.EMPTY);
        TestCraftingInputHatch hatch = handler.createProxy();

        assertTrue(
            CraftingInputHatchAccess.tryApplyRoutingConfiguration(
                hatch,
                configured,
                PatternRoutingNbt.manualItemStacks(configured)));
        CraftingInputHatchAccess.SharedItemDescriptor descriptor = CraftingInputHatchAccess.getSharedItemDescriptor(hatch);

        assertNull(descriptor.circuit);
        assertEquals(blankFamilyManualItemsKey(), PatternRoutingNbt.manualItemsKey(descriptor.manualItems));
        assertEquals(
            PatternRoutingNbt.itemSignature(blankFamilyManualStack()),
            PatternRoutingNbt.itemSignature(handler.slots[1]));
    }

    @Test
    public void refreshAssignmentsTreatsManualOnlySharedItemAsManual() {
        PatternRoutingNbt.RoutingMetadata metadata = blankFamilyManualOnlyMetadata();
        PatternRoutingNbt.RoutingMetadata configured = PatternRoutingNbt.withConfiguredAssignment(metadata);
        TestControllerHandler controllerHandler = new TestControllerHandler(HatchAssignmentData.EMPTY, true, true);
        Object controller = controllerHandler.createController();
        IGregTechTileEntity baseTile = (IGregTechTileEntity) controllerHandler.createBaseTile(controller);
        TestHatchHandler hatchHandler = new TestHatchHandler(HatchAssignmentData.EMPTY);
        hatchHandler.baseTile = baseTile;
        TestCraftingInputHatch hatch = hatchHandler.createProxy();
        controllerHandler.register(hatch);

        assertTrue(
            CraftingInputHatchAccess.tryApplyRoutingConfiguration(
                hatch,
                configured,
                PatternRoutingNbt.manualItemStacks(configured)));

        HatchAssignmentService.refreshAssignments(controller);

        assertEquals("", hatchHandler.assignment.circuitKey);
        assertEquals(blankFamilyManualItemsKey(), hatchHandler.assignment.manualItemsKey);
    }

    @Test
    public void blankFamilyFixtureUsesCheckStructureForDirectRecheck() {
        PatternRoutingNbt.RoutingMetadata metadata = blankFamilyMetadata();
        TestRouteFixture fixture = TestRouteFixture.blankFamily(metadata);
        PatternRoutingNbt.RoutingMetadata configured = PatternRoutingNbt.withConfiguredAssignment(metadata);

        assertTrue(
            CraftingInputHatchAccess.tryApplyRoutingConfiguration(
                fixture.hatch,
                configured,
                PatternRoutingNbt.manualItemStacks(configured)));
        PatternRouterService.syncAssignmentForTest(fixture.hatch, fixture.expectedAssignment);

        HatchControllerRecheckService.RecheckResult result = HatchControllerRecheckService
            .recheckAndVerify(fixture.hatch, fixture.expectedAssignment);

        assertTrue(result.success);
        assertEquals("checkStructure", result.path);
        assertEquals(1, fixture.controllerHandler.checkStructureCalls);
    }

    @Test
    public void blankFamilyRouteStagesSharedConfigRechecksControllerSyncsAssignmentAndInsertsConfiguredPattern() {
        PatternRoutingNbt.RoutingMetadata metadata = blankFamilyMetadata();
        TestRouteFixture fixture = TestRouteFixture.blankFamily(metadata);
        PatternRoutingNbt.RoutingMetadata raw = PatternRoutingNbt.readRoutingData(fixture.pattern);
        PatternRoutingNbt.RoutingMetadata derived = PatternRoutingNbt.withDerivedDescriptor(raw);

        assertEquals(blankFamilyCircuitKey(), derived.circuitKey);
        assertEquals(blankFamilyManualItemsKey(), derived.manualItemsKey);

        PatternRouterService.RouteResult result = PatternRouterService
            .tryRoute(fixture.pattern, fixture.node, fixture.actionSource);

        assertTrue(result.isRouted());
        assertEquals(1, fixture.controllerHandler.checkStructureCalls);
        assertEquals(1, fixture.controllerHandler.assignmentSeenDuringCheck.size());
        assertEquals(
            PatternRoutingNbt.withConfiguredAssignment(metadata).assignmentKey,
            fixture.controllerHandler.assignmentSeenDuringCheck.get(0));
        assertEquals(blankFamilyCircuitKey(), PatternRoutingNbt.circuitKey(fixture.hatchHandler.slots[0]));
        assertEquals(
            PatternRoutingNbt.itemSignature(blankFamilyManualStack()),
            PatternRoutingNbt.itemSignature(fixture.hatchHandler.slots[1]));
        assertEquals(
            PatternRoutingNbt.withConfiguredAssignment(metadata).assignmentKey,
            fixture.hatchHandler.assignment.assignmentKey);
        assertEquals(
            PatternRoutingNbt.withConfiguredAssignment(metadata).assignmentKey,
            fixture.insertedPatternAssignmentKey());
    }

    @Test
    public void blankFamilyRouteRollsBackSharedWritesAeExtractionAssignmentSyncAndPatternWhenControllerRecheckFails() {
        TestRouteFixture fixture = TestRouteFixture.blankFamilyWithFailedRecheck();
        PatternRoutingNbt.RoutingMetadata raw = PatternRoutingNbt.readRoutingData(fixture.pattern);
        PatternRoutingNbt.RoutingMetadata derived = PatternRoutingNbt.withDerivedDescriptor(raw);

        assertEquals(blankFamilyCircuitKey(), derived.circuitKey);
        assertEquals(blankFamilyManualItemsKey(), derived.manualItemsKey);

        PatternRouterService.RouteResult result = PatternRouterService
            .tryRoute(fixture.pattern, fixture.node, fixture.actionSource);

        assertEquals(PatternRouterService.RouteStatus.NO_MATCHING_HATCH, result.status);
        assertEquals(1, fixture.controllerHandler.checkStructureCalls);
        assertEquals(1, fixture.controllerHandler.assignmentSeenDuringCheck.size());
        assertEquals(
            fixture.expectedAssignment.assignmentKey,
            fixture.controllerHandler.assignmentSeenDuringCheck.get(0));
        assertEquals(0, fixture.patternCountInTarget());
        assertNull(fixture.hatchHandler.slots[0]);
        assertNull(fixture.hatchHandler.slots[1]);
        assertTrue(CraftingInputHatchAccess.hasBlankSharedConfiguration(fixture.hatch));
        assertEquals(HatchAssignmentData.EMPTY.assignmentKey, fixture.hatchHandler.assignment.assignmentKey);
        assertEquals(1L, fixture.aeInventory.extractedCount(blankFamilyManualItemId(), 2));
        assertEquals(1L, fixture.aeInventory.restoredCount(blankFamilyManualItemId(), 2));
        assertNull(fixture.hatchHandler.inventory.getStackInSlot(0));
    }

    private static final class TestRouteFixture {

        private final ItemStack pattern;
        private final IGridNode node;
        private final BaseActionSource actionSource;
        private final TestCraftingInputHatch hatch;
        private final TestHatchHandler hatchHandler;
        private final TestControllerHandler controllerHandler;
        private final TestAeInventory aeInventory;
        private final HatchAssignmentData expectedAssignment;

        private TestRouteFixture(ItemStack pattern, IGridNode node, BaseActionSource actionSource,
            TestCraftingInputHatch hatch, TestHatchHandler hatchHandler, TestControllerHandler controllerHandler,
            TestAeInventory aeInventory, HatchAssignmentData expectedAssignment) {
            this.pattern = pattern;
            this.node = node;
            this.actionSource = actionSource;
            this.hatch = hatch;
            this.hatchHandler = hatchHandler;
            this.controllerHandler = controllerHandler;
            this.aeInventory = aeInventory;
            this.expectedAssignment = expectedAssignment;
        }

        private static TestRouteFixture blankFamily(PatternRoutingNbt.RoutingMetadata metadata) {
            return blankFamily(metadata, true, true);
        }

        private static TestRouteFixture blankFamilyWithFailedRecheck() {
            return blankFamily(blankFamilyMetadata(), false, true);
        }

        private static TestRouteFixture blankFamily(PatternRoutingNbt.RoutingMetadata metadata,
            boolean checkStructureResult, boolean checkStructureAvailable) {
            HatchAssignmentData expectedAssignment = PatternRoutingNbt
                .assignmentDataFor(PatternRoutingNbt.withConfiguredAssignment(metadata));
            TestControllerHandler controllerHandler = new TestControllerHandler(
                expectedAssignment,
                checkStructureResult,
                checkStructureAvailable);
            Object controller = controllerHandler.createController();
            IGregTechTileEntity baseTile = (IGregTechTileEntity) controllerHandler.createBaseTile(controller);
            TestHatchHandler hatchHandler = new TestHatchHandler(
                new HatchAssignmentData(
                    PatternRoutingNbt.buildAssignmentKey(metadata.recipeCategory, "", ""),
                    metadata.recipeCategory,
                    "",
                    ""));
            hatchHandler.master = null;
            hatchHandler.baseTile = baseTile;
            TestCraftingInputHatch hatch = hatchHandler.createProxy();
            controllerHandler.register(hatch);

            TestAeInventory aeInventory = new TestAeInventory();
            aeInventory.seed(blankFamilyManualStack());
            PatternRouterService.setAeItemStackFactoryForTest(new PatternRouterService.AeItemStackFactory() {

                @Override
                public IAEItemStack create(ItemStack stack) {
                    return createAeItemStack(stack.copy(), stack.stackSize);
                }
            });
            TestGrid grid = new TestGrid(hatch, aeInventory);
            IGridNode node = grid.createNode(hatch);
            BaseActionSource actionSource = new TestActionSource();
            ItemStack pattern = new ItemStack(Items.paper);
            PatternRoutingNbt.writeRoutingData(pattern, metadata);
            return new TestRouteFixture(
                pattern,
                node,
                actionSource,
                hatch,
                hatchHandler,
                controllerHandler,
                aeInventory,
                expectedAssignment);
        }

        private String insertedPatternAssignmentKey() {
            ItemStack inserted = hatchHandler.inventory.getStackInSlot(0);
            PatternRoutingNbt.RoutingMetadata metadata = PatternRoutingNbt.readRoutingData(inserted);
            return metadata.assignmentKey;
        }

        private int patternCountInTarget() {
            int count = 0;
            for (int slot = 0; slot < hatchHandler.inventory.getSizeInventory(); slot++) {
                if (hatchHandler.inventory.getStackInSlot(slot) != null) {
                    count++;
                }
            }
            return count;
        }
    }

    private static final class TestGrid {

        private final List<IGridNode> nodes = new ArrayList<IGridNode>();
        private final IStorageGrid storageGrid;

        private TestGrid(Object machine, TestAeInventory aeInventory) {
            this.storageGrid = new TestStorageGrid(aeInventory);
            this.nodes.add(createNode(machine));
        }

        private IGridNode createNode(final Object machine) {
            final IGrid grid = (IGrid) Proxy.newProxyInstance(
                PatternRouterServiceTest.class.getClassLoader(),
                new Class<?>[] { IGrid.class },
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("getNodes".equals(name)) {
                            return createReadOnlyCollection(nodes);
                        }
                        if ("getCache".equals(name) && args != null
                            && args.length == 1
                            && args[0] == IStorageGrid.class) {
                            return storageGrid;
                        }
                        return defaultValue(method.getReturnType());
                    }
                });
            return (IGridNode) Proxy.newProxyInstance(
                PatternRouterServiceTest.class.getClassLoader(),
                new Class<?>[] { IGridNode.class },
                new InvocationHandler() {

                    private final IGridHost gridHost = machine instanceof IGridHost ? (IGridHost) machine : null;

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("getGrid".equals(name)) {
                            return grid;
                        }
                        if ("getMachine".equals(name)) {
                            return gridHost;
                        }
                        return defaultValue(method.getReturnType());
                    }
                });
        }
    }

    private static final class TestStorageGrid implements IStorageGrid {

        private final TestAeInventory inventory;

        private TestStorageGrid(TestAeInventory inventory) {
            this.inventory = inventory;
        }

        @Override
        public IMEMonitor<IAEItemStack> getItemInventory() {
            return inventory.proxy();
        }

        @Override
        public IMEMonitor<appeng.api.storage.data.IAEFluidStack> getFluidInventory() {
            return null;
        }

        @Override
        public void registerCellProvider(appeng.api.storage.ICellProvider provider) {}

        @Override
        public void unregisterCellProvider(appeng.api.storage.ICellProvider provider) {}

        @Override
        public void postAlterationOfStoredItems(appeng.api.storage.StorageChannel channel,
            Iterable<? extends appeng.api.storage.data.IAEStack> changes, BaseActionSource src) {}

        @Override
        public void onUpdateTick() {}

        @Override
        public void removeNode(IGridNode gridNode, IGridHost machine) {}

        @Override
        public void addNode(IGridNode gridNode, IGridHost machine) {}

        @Override
        public void onSplit(IGridStorage destinationStorage) {}

        @Override
        public void onJoin(IGridStorage sourceStorage) {}

        @Override
        public void populateGridStorage(IGridStorage destinationStorage) {}
    }

    private static final class TestAeInventory {

        private final Map<String, Long> available = new HashMap<String, Long>();
        private final Map<String, Long> restored = new HashMap<String, Long>();
        private final Map<String, Long> extracted = new HashMap<String, Long>();
        private IMEMonitor<IAEItemStack> proxy;

        private IMEMonitor<IAEItemStack> proxy() {
            if (proxy == null) {
                proxy = (IMEMonitor<IAEItemStack>) Proxy.newProxyInstance(
                    PatternRouterServiceTest.class.getClassLoader(),
                    new Class<?>[] { IMEMonitor.class },
                    new InvocationHandler() {

                        @Override
                        public Object invoke(Object p, Method method, Object[] args) {
                            String name = method.getName();
                            if ("extractItems".equals(name)) {
                                return extractItems(
                                    (IAEItemStack) args[0],
                                    (Actionable) args[1],
                                    (BaseActionSource) args[2]);
                            }
                            if ("injectItems".equals(name)) {
                                return injectItems(
                                    (IAEItemStack) args[0],
                                    (Actionable) args[1],
                                    (BaseActionSource) args[2]);
                            }
                            if ("getStorageList".equals(name) || "getPartitionList".equals(name)) {
                                return null;
                            }
                            if ("validForPass".equals(name) || "canAccept".equals(name) || "isValid".equals(name)) {
                                return true;
                            }
                            if ("isPrioritized".equals(name) || "partitionsChanged".equals(name)) {
                                return false;
                            }
                            if ("getSlot".equals(name) || "getPriority".equals(name) || "getSortValue".equals(name)) {
                                return 0;
                            }
                            if ("getAccess".equals(name)) {
                                return AccessRestriction.READ_WRITE;
                            }
                            if ("removeListener".equals(name) || "addListener".equals(name)
                                || "setPartitionList".equals(name)
                                || "setChannel".equals(name)) {
                                return null;
                            }
                            return defaultValue(method.getReturnType());
                        }
                    });
            }
            return proxy;
        }

        private void seed(ItemStack stack) {
            if (stack == null || stack.getItem() == null) {
                return;
            }
            available.put(key(stack), Long.valueOf(Math.max(1, stack.stackSize)));
        }

        private long restoredCount(String itemId, int meta) {
            Long count = restored.get(itemId + "@" + meta);
            return count != null ? count.longValue() : 0L;
        }

        private long extractedCount(String itemId, int meta) {
            Long count = extracted.get(itemId + "@" + meta);
            return count != null ? count.longValue() : 0L;
        }

        private IAEItemStack extractItems(IAEItemStack request, Actionable actionable, BaseActionSource src) {
            if (request == null || request.getItemStack() == null) {
                return null;
            }
            String key = key(request.getItemStack());
            long needed = request.getStackSize();
            long present = count(available, key);
            if (present < needed) {
                return null;
            }
            if (actionable == Actionable.MODULATE) {
                available.put(key, Long.valueOf(present - needed));
                extracted.put(key, Long.valueOf(count(extracted, key) + needed));
            }
            return createAeItemStack(copyWithSize(request.getItemStack(), needed), needed);
        }

        private IAEItemStack injectItems(IAEItemStack input, Actionable actionable, BaseActionSource src) {
            if (input == null || input.getItemStack() == null) {
                return null;
            }
            String key = key(input.getItemStack());
            long amount = input.getStackSize();
            if (actionable == Actionable.MODULATE) {
                available.put(key, Long.valueOf(count(available, key) + amount));
                restored.put(key, Long.valueOf(count(restored, key) + amount));
            }
            return null;
        }

        private long count(Map<String, Long> source, String key) {
            Long value = source.get(key);
            return value != null ? value.longValue() : 0L;
        }

        private String key(ItemStack stack) {
            Object name = net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem());
            return String.valueOf(name) + "@" + stack.getItemDamage();
        }
    }

    private static IAEItemStack createAeItemStack(final ItemStack stack, long stackSize) {
        final long[] amount = new long[] { stackSize };
        return (IAEItemStack) Proxy.newProxyInstance(
            PatternRouterServiceTest.class.getClassLoader(),
            new Class<?>[] { IAEItemStack.class },
            new InvocationHandler() {

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String name = method.getName();
                    if ("getItemStack".equals(name)) {
                        return stack;
                    }
                    if ("getStackSize".equals(name)) {
                        return amount[0];
                    }
                    if ("setStackSize".equals(name)) {
                        amount[0] = ((Long) args[0]).longValue();
                        if (stack != null) {
                            stack.stackSize = (int) amount[0];
                        }
                        return proxy;
                    }
                    if ("copy".equals(name) || "copyRequest".equals(name)) {
                        return createAeItemStack(copyWithSize(stack, amount[0]), amount[0]);
                    }
                    if ("isSameType".equals(name)) {
                        Object other = args != null && args.length > 0 ? args[0] : null;
                        ItemStack otherStack = other instanceof IAEItemStack ? ((IAEItemStack) other).getItemStack()
                            : other instanceof ItemStack ? (ItemStack) other : null;
                        return stack != null && otherStack != null
                            && PatternRoutingNbt.itemSignature(stack)
                                .equals(PatternRoutingNbt.itemSignature(otherStack));
                    }
                    if ("sameOre".equals(name)) {
                        Object other = args != null && args.length > 0 ? args[0] : null;
                        ItemStack otherStack = other instanceof IAEItemStack ? ((IAEItemStack) other).getItemStack()
                            : other instanceof ItemStack ? (ItemStack) other : null;
                        return stack != null && otherStack != null
                            && PatternRoutingNbt.itemSignature(stack)
                                .equals(PatternRoutingNbt.itemSignature(otherStack));
                    }
                    if ("getItemDamage".equals(name)) {
                        return stack != null ? stack.getItemDamage() : 0;
                    }
                    if ("getItem".equals(name)) {
                        return stack != null ? stack.getItem() : null;
                    }
                    if ("add".equals(name)) {
                        if (args[0] instanceof IAEItemStack) {
                            amount[0] += ((IAEItemStack) args[0]).getStackSize();
                            if (stack != null) {
                                stack.stackSize = (int) amount[0];
                            }
                        }
                        return null;
                    }
                    if ("reset".equals(name)) {
                        amount[0] = 0L;
                        if (stack != null) {
                            stack.stackSize = 0;
                        }
                        return proxy;
                    }
                    if ("isMeaningful".equals(name)) {
                        return amount[0] > 0;
                    }
                    if ("hasTagCompound".equals(name)) {
                        return stack != null && stack.hasTagCompound();
                    }
                    if ("getLocalizedName".equals(name)) {
                        return stack != null ? stack.getDisplayName() : "";
                    }
                    return defaultValue(method.getReturnType());
                }
            });
    }

    private static final class TestActionSource extends BaseActionSource {
    }

    @SuppressWarnings("unchecked")
    private static <T> IReadOnlyCollection<T> createReadOnlyCollection(final List<T> values) {
        return (IReadOnlyCollection<T>) Proxy.newProxyInstance(
            PatternRouterServiceTest.class.getClassLoader(),
            new Class<?>[] { IReadOnlyCollection.class },
            new InvocationHandler() {

                @Override
                public Object invoke(Object proxy, Method method, Object[] args) {
                    String name = method.getName();
                    if ("iterator".equals(name)) {
                        return values.iterator();
                    }
                    if ("size".equals(name)) {
                        return values.size();
                    }
                    if ("isEmpty".equals(name)) {
                        return values.isEmpty();
                    }
                    if ("contains".equals(name)) {
                        return values.contains(args[0]);
                    }
                    if ("toArray".equals(name)) {
                        return args == null || args.length == 0 ? values.toArray() : values.toArray((Object[]) args[0]);
                    }
                    if ("hashCode".equals(name)) {
                        return values.hashCode();
                    }
                    if ("equals".equals(name)) {
                        return proxy == args[0];
                    }
                    if ("toString".equals(name)) {
                        return values.toString();
                    }
                    return defaultValue(method.getReturnType());
                }
            });
    }

    private static ItemStack copyWithSize(ItemStack stack, long stackSize) {
        ItemStack copy = stack.copy();
        copy.stackSize = (int) stackSize;
        return copy;
    }

    private interface TestCraftingInputHatch extends IDualInputHatch, HatchAssignmentHolder, IGridHost {

        IInventory getPatterns();

        int getCircuitSlot();

        ItemStack[] getSharedItems();

        ItemStack getStackInSlot(int slot);

        void setInventorySlotContents(int slot, ItemStack stack);

        Object getMaster();

        IGregTechTileEntity getBaseMetaTileEntity();
    }

    private interface TestSrgCraftingInputHatch extends IDualInputHatch, HatchAssignmentHolder, IGridHost {

        IInventory getPatterns();

        int getCircuitSlot();

        ItemStack[] getSharedItems();

        ItemStack func_70301_a(int slot);

        void func_70299_a(int slot, ItemStack stack);

        Object getMaster();
    }

    static final class TestControllerState {

        Object controller;
        IGregTechTileEntity baseTile;
        boolean checkStructureResult;
        HatchAssignmentData assignmentAfterCheck;
    }

    private static final class TestControllerHandler {

        private final HatchAssignmentData assignmentAfterCheck;
        private final boolean checkStructureResult;
        private final boolean checkStructureAvailable;
        private final List<IDualInputHatch> dualInputHatches = new ArrayList<IDualInputHatch>();
        private final List<String> assignmentSeenDuringCheck = new ArrayList<String>();
        private int checkStructureCalls;

        private TestControllerHandler(HatchAssignmentData assignmentAfterCheck, boolean checkStructureResult,
            boolean checkStructureAvailable) {
            this.assignmentAfterCheck = assignmentAfterCheck;
            this.checkStructureResult = checkStructureResult;
            this.checkStructureAvailable = checkStructureAvailable;
        }

        private Object createController() {
            return Proxy.newProxyInstance(
                PatternRouterServiceTest.class.getClassLoader(),
                new Class<?>[] {
                    checkStructureAvailable ? TestController.class : TestControllerWithoutCheckStructure.class,
                    gregtech.api.interfaces.metatileentity.IMetaTileEntity.class },
                new InvocationHandler() {

                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) {
                        String name = method.getName();
                        if ("registerHatch".equals(name)) {
                            dualInputHatches.add((IDualInputHatch) args[0]);
                            return null;
                        }
                        if ("getDualInputHatches".equals(name)) {
                            return dualInputHatches;
                        }
                        if ("checkStructure".equals(name)) {
                            checkStructureCalls++;
                            for (IDualInputHatch hatch : dualInputHatches) {
                                if (hatch instanceof HatchAssignmentHolder) {
                                    HatchAssignmentData assignment = ((HatchAssignmentHolder) hatch)
                                        .nhaeutilities$getAssignmentData();
                                    assignmentSeenDuringCheck.add(assignment != null ? assignment.assignmentKey : "");
                                    ((HatchAssignmentHolder) hatch)
                                        .nhaeutilities$setAssignmentData(assignmentAfterCheck);
                                }
                            }
                            return Boolean.valueOf(checkStructureResult);
                        }
                        return defaultValue(method.getReturnType());
                    }
                });
        }

        private Object createBaseTile(Object controller) {
            return Proxy.newProxyInstance(
                PatternRouterServiceTest.class.getClassLoader(),
                new Class<?>[] { IGregTechTileEntity.class, TestBaseTile.class },
                new TestBaseTileHandler(controller));
        }

        private void register(IDualInputHatch hatch) {
            dualInputHatches.add(hatch);
        }

    }

    private interface TestControllerWithoutCheckStructure {

        void registerHatch(IDualInputHatch hatch);

        List<IDualInputHatch> getDualInputHatches();
    }

    private interface TestController extends TestControllerWithoutCheckStructure {

        boolean checkStructure(boolean aStack);
    }

    private interface TestBaseTile {

        Object getMetaTileEntity();
    }

    private static final class TestBaseTileHandler implements InvocationHandler {

        private final Object controller;

        private TestBaseTileHandler(Object controller) {
            this.controller = controller;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("getMetaTileEntity".equals(method.getName())) {
                return controller;
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static final class TestHatchHandler implements InvocationHandler {

        private final TestInventory inventory = new TestInventory(4);
        private HatchAssignmentData assignment;
        private ItemStack[] sharedItems = new ItemStack[0];
        private final ItemStack[] slots = new ItemStack[16];
        private Object master;
        private IGregTechTileEntity baseTile;
        private final TestControllerState controllerState = new TestControllerState();

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
            if ("getBaseMetaTileEntity".equals(methodName)) {
                return baseTile;
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

    static Object defaultValue(Class<?> returnType) {
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
