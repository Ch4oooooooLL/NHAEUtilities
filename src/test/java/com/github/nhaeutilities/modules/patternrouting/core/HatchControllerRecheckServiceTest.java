package com.github.nhaeutilities.modules.patternrouting.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.github.nhaeutilities.accessor.patternrouting.HatchAssignmentHolder;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.tileentities.machines.IDualInputHatch;

public class HatchControllerRecheckServiceTest {

    @Test
    public void recheckUsesControllerCheckStructureWhenAvailableAndVerifiesAssignment() {
        HatchAssignmentData expected = new HatchAssignmentData("cat|circ|manual", "cat", "circ", "manual");
        TestControllerHandler controllerHandler = new TestControllerHandler(expected, true, true);
        TestHatchHandler hatchHandler = new TestHatchHandler();
        Object controller = controllerHandler.createController();
        Object baseTile = controllerHandler.createBaseTile(controller);
        Object hatch = hatchHandler.createHatch(controller, baseTile);
        controllerHandler.addHatch((IDualInputHatch) hatch);

        HatchControllerRecheckService.RecheckResult result = HatchControllerRecheckService
            .recheckAndVerify(hatch, expected);

        assertTrue(result.success);
        assertEquals("checkStructure", result.path);
        assertEquals(1, controllerHandler.checkStructureCalls);
        assertEquals(expected.assignmentKey, hatchHandler.assignment.assignmentKey);
    }

    @Test
    public void recheckFallsBackToAssignmentRefreshWhenCheckStructureIsUnavailable() {
        TestControllerHandler controllerHandler = new TestControllerHandler(HatchAssignmentData.EMPTY, false, false);
        Object controller = controllerHandler.createController();
        HatchAssignmentData expected = new HatchAssignmentData(
            controller.getClass()
                .getName() + "||",
            controller.getClass()
                .getName(),
            "",
            "");
        Object baseTile = controllerHandler.createBaseTile(controller);
        TestHatchHandler hatchHandler = new TestHatchHandler();
        Object hatch = hatchHandler.createHatch(controller, baseTile);
        controllerHandler.addHatch((IDualInputHatch) hatch);

        HatchControllerRecheckService.RecheckResult result = HatchControllerRecheckService
            .recheckAndVerify(hatch, expected);

        assertTrue(result.success);
        assertEquals("refreshAssignments", result.path);
        assertEquals(0, controllerHandler.checkStructureCalls);
        assertEquals(expected.assignmentKey, hatchHandler.assignment.assignmentKey);
    }

    @Test
    public void recheckFailsWhenAssignmentDoesNotMatchAfterSuccessfulCheckStructure() {
        HatchAssignmentData expected = new HatchAssignmentData("cat|circ|manual", "cat", "circ", "manual");
        TestControllerHandler controllerHandler = new TestControllerHandler(HatchAssignmentData.EMPTY, true, true);
        Object controller = controllerHandler.createController();
        Object baseTile = controllerHandler.createBaseTile(controller);
        TestHatchHandler hatchHandler = new TestHatchHandler();
        Object hatch = hatchHandler.createHatch(controller, baseTile);
        controllerHandler.addHatch((IDualInputHatch) hatch);

        HatchControllerRecheckService.RecheckResult result = HatchControllerRecheckService
            .recheckAndVerify(hatch, expected);

        assertFalse(result.success);
        assertEquals("assignment-mismatch", result.failureReason);
    }

    private interface TestHatch extends IDualInputHatch, HatchAssignmentHolder {

        Object getMaster();

        IGregTechTileEntity getBaseMetaTileEntity();
    }

    private static final class TestHatchHandler implements InvocationHandler {

        private HatchAssignmentData assignment = HatchAssignmentData.EMPTY;
        private Object controller;
        private IGregTechTileEntity baseTile;

        private Object createHatch(Object controller, Object baseTile) {
            this.controller = controller;
            this.baseTile = (IGregTechTileEntity) baseTile;
            return Proxy.newProxyInstance(
                HatchControllerRecheckServiceTest.class.getClassLoader(),
                new Class<?>[] { TestHatch.class },
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

    private static final class TestControllerHandler implements InvocationHandler {

        private final HatchAssignmentData assignmentAfterCheck;
        private final boolean checkStructureResult;
        private final boolean checkStructureAvailable;
        private final List<IDualInputHatch> dualInputHatches = new ArrayList<IDualInputHatch>();
        private int checkStructureCalls;

        private TestControllerHandler(HatchAssignmentData assignmentAfterCheck, boolean checkStructureResult,
            boolean checkStructureAvailable) {
            this.assignmentAfterCheck = assignmentAfterCheck;
            this.checkStructureResult = checkStructureResult;
            this.checkStructureAvailable = checkStructureAvailable;
        }

        private Object createController() {
            return Proxy.newProxyInstance(
                HatchControllerRecheckServiceTest.class.getClassLoader(),
                new Class<?>[] {
                    checkStructureAvailable ? TestController.class : TestControllerWithoutCheckStructure.class },
                this);
        }

        private void addHatch(IDualInputHatch hatch) {
            dualInputHatches.add(hatch);
        }

        private Object createBaseTile(Object controller) {
            return Proxy.newProxyInstance(
                HatchControllerRecheckServiceTest.class.getClassLoader(),
                new Class<?>[] { IGregTechTileEntity.class, TestBaseTile.class },
                new TestBaseTileHandler(controller));
        }

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
                if (!checkStructureAvailable) {
                    return defaultValue(method.getReturnType());
                }
                checkStructureCalls++;
                for (IDualInputHatch hatch : dualInputHatches) {
                    if (hatch instanceof HatchAssignmentHolder) {
                        ((HatchAssignmentHolder) hatch).nhaeutilities$setAssignmentData(assignmentAfterCheck);
                    }
                }
                return Boolean.valueOf(checkStructureResult);
            }
            return defaultValue(method.getReturnType());
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
