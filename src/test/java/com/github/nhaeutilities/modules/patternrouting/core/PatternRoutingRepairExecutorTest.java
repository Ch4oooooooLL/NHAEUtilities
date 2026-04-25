package com.github.nhaeutilities.modules.patternrouting.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.ItemStack;

import org.junit.Test;

import com.github.nhaeutilities.accessor.patternrouting.HatchAssignmentHolder;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.tileentities.machines.IDualInputHatch;

public class PatternRoutingRepairExecutorTest {

    @Test
    public void repairRefreshesMatchingControllerAssignmentsBeforeEscalating() {
        RefreshingController controller = new RefreshingController();
        TestRefreshableHatchHandler hatchHandler = new TestRefreshableHatchHandler(HatchAssignmentData.EMPTY);
        Object hatch = hatchHandler.createHatch();
        controller.addHatch((IDualInputHatch) hatch);
        PatternRoutingRepairEvaluator.RepairTarget target = PatternRoutingRepairEvaluator.evaluate(controller);

        PatternRoutingRepairExecutor.RepairResult result = PatternRoutingRepairExecutor.repair(target);

        assertEquals(1, result.repairedHatchCount);
        assertEquals(0, result.failedHatchCount);
        assertEquals(0, result.recheckedHatchCount);
        assertTrue(result.controllerRepaired);
        assertEquals(expectedBlankAssignment(controller).assignmentKey, hatchHandler.assignment.assignmentKey);
    }

    @Test
    public void repairEscalatesToControllerRecheckWhenRefreshDoesNotRepairTargetedHatch() {
        EscalatingController controller = new EscalatingController();
        Object baseTile = controller.createBaseTile();
        TestRecheckableHatchHandler hatchHandler = new TestRecheckableHatchHandler(
            HatchAssignmentData.EMPTY,
            controller,
            baseTile);
        Object hatch = hatchHandler.createHatch();
        controller.addHatch((IDualInputHatch) hatch);
        HatchAssignmentData expected = expectedBlankAssignment(controller);
        PatternRoutingRepairEvaluator.BrokenHatch brokenHatch = new PatternRoutingRepairEvaluator.BrokenHatch(
            hatch,
            expected);
        PatternRoutingRepairEvaluator.RepairTarget target = new PatternRoutingRepairEvaluator.RepairTarget(
            controller,
            Collections.singletonList(brokenHatch));
        controller.assignmentAfterCheck = expected;

        PatternRoutingRepairExecutor.RepairResult result = PatternRoutingRepairExecutor.repair(target);

        assertEquals(1, result.repairedHatchCount);
        assertEquals(0, result.failedHatchCount);
        assertEquals(1, result.recheckedHatchCount);
        assertEquals(1, controller.checkStructureCalls);
        assertEquals(expected.assignmentKey, hatchHandler.assignment.assignmentKey);
    }

    private static HatchAssignmentData expectedBlankAssignment(Object controller) {
        String recipeCategory = controller.getClass()
            .getName();
        return new HatchAssignmentData(
            PatternRoutingNbt.buildAssignmentKey(recipeCategory, "", ""),
            recipeCategory,
            "",
            "");
    }

    private static final class RefreshingController {

        private final List<IDualInputHatch> dualInputHatches = new ArrayList<IDualInputHatch>();

        public List<IDualInputHatch> getDualInputHatches() {
            return dualInputHatches;
        }

        private void addHatch(IDualInputHatch hatch) {
            dualInputHatches.add(hatch);
        }
    }

    private static final class EscalatingController {

        private final List<IDualInputHatch> recheckHatches = new ArrayList<IDualInputHatch>();
        private HatchAssignmentData assignmentAfterCheck = HatchAssignmentData.EMPTY;
        private int checkStructureCalls;

        public List<IDualInputHatch> getDualInputHatches() {
            return Collections.emptyList();
        }

        public boolean checkStructure(boolean force) {
            checkStructureCalls++;
            for (IDualInputHatch hatch : recheckHatches) {
                if (hatch instanceof HatchAssignmentHolder) {
                    ((HatchAssignmentHolder) hatch).nhaeutilities$setAssignmentData(assignmentAfterCheck);
                }
            }
            return true;
        }

        private void addHatch(IDualInputHatch hatch) {
            recheckHatches.add(hatch);
        }

        private Object createBaseTile() {
            return Proxy.newProxyInstance(
                PatternRoutingRepairExecutorTest.class.getClassLoader(),
                new Class<?>[] { IGregTechTileEntity.class, TestBaseTile.class },
                new TestBaseTileHandler(this));
        }
    }

    private interface TestBaseTile {

        Object getMetaTileEntity();
    }

    private interface TestRefreshableHatch extends IDualInputHatch, HatchAssignmentHolder {

        ItemStack[] getSharedItems();
    }

    private interface TestRecheckableHatch extends IDualInputHatch, HatchAssignmentHolder {

        Object getMaster();

        IGregTechTileEntity getBaseMetaTileEntity();
    }

    private static final class TestRefreshableHatchHandler implements InvocationHandler {

        private HatchAssignmentData assignment;

        private TestRefreshableHatchHandler(HatchAssignmentData assignment) {
            this.assignment = assignment;
        }

        private Object createHatch() {
            return Proxy.newProxyInstance(
                PatternRoutingRepairExecutorTest.class.getClassLoader(),
                new Class<?>[] { TestRefreshableHatch.class },
                this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("getSharedItems".equals(name)) {
                return new ItemStack[0];
            }
            if ("nhaeutilities$getAssignmentData".equals(name)) {
                return assignment;
            }
            if ("nhaeutilities$setAssignmentData".equals(name)) {
                assignment = (HatchAssignmentData) args[0];
                return null;
            }
            if ("nhaeutilities$clearAssignmentData".equals(name)) {
                assignment = HatchAssignmentData.EMPTY;
                return null;
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static final class TestRecheckableHatchHandler implements InvocationHandler {

        private HatchAssignmentData assignment;
        private final Object controller;
        private final IGregTechTileEntity baseTile;

        private TestRecheckableHatchHandler(HatchAssignmentData assignment, Object controller, Object baseTile) {
            this.assignment = assignment;
            this.controller = controller;
            this.baseTile = (IGregTechTileEntity) baseTile;
        }

        private Object createHatch() {
            return Proxy.newProxyInstance(
                PatternRoutingRepairExecutorTest.class.getClassLoader(),
                new Class<?>[] { TestRecheckableHatch.class },
                this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("getMaster".equals(name)) {
                return controller;
            }
            if ("getBaseMetaTileEntity".equals(name)) {
                return baseTile;
            }
            if ("nhaeutilities$getAssignmentData".equals(name)) {
                return assignment;
            }
            if ("nhaeutilities$setAssignmentData".equals(name)) {
                assignment = (HatchAssignmentData) args[0];
                return null;
            }
            if ("nhaeutilities$clearAssignmentData".equals(name)) {
                assignment = HatchAssignmentData.EMPTY;
                return null;
            }
            return defaultValue(method.getReturnType());
        }
    }

    private static final class TestBaseTileHandler implements InvocationHandler {

        private final EscalatingController controller;

        private TestBaseTileHandler(EscalatingController controller) {
            this.controller = controller;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("getMetaTileEntity".equals(method.getName())) {
                return controller;
            }
            if ("checkStructure".equals(method.getName())) {
                controller.checkStructureCalls++;
                return true;
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
