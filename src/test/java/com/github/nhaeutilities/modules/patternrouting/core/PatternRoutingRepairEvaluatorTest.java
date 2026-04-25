package com.github.nhaeutilities.modules.patternrouting.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.Test;

import com.github.nhaeutilities.accessor.patternrouting.HatchAssignmentHolder;

import gregtech.common.tileentities.machines.IDualInputHatch;

public class PatternRoutingRepairEvaluatorTest {

    @Test
    public void evaluateReturnsBrokenBlankCraftingHatchWhenAssignmentMissing() {
        TestController controller = new TestController();
        Object hatch = new TestHatchHandler(HatchAssignmentData.EMPTY, new ItemStack[0]).createHatch();
        controller.addHatch((IDualInputHatch) hatch);

        PatternRoutingRepairEvaluator.RepairTarget target = PatternRoutingRepairEvaluator.evaluate(controller);

        assertNotNull(target);
        assertEquals(controller, target.controller);
        assertEquals(1, target.brokenHatches.size());
        assertEquals(
            expectedBlankAssignment(controller).assignmentKey,
            target.brokenHatches.get(0).expectedAssignment.assignmentKey);
    }

    @Test
    public void evaluateSkipsBlankCraftingHatchWhenAssignmentMatchesExpectedBlankDescriptor() {
        TestController controller = new TestController();
        HatchAssignmentData expected = expectedBlankAssignment(controller);
        Object hatch = new TestHatchHandler(expected, new ItemStack[0]).createHatch();
        controller.addHatch((IDualInputHatch) hatch);

        PatternRoutingRepairEvaluator.RepairTarget target = PatternRoutingRepairEvaluator.evaluate(controller);

        assertNull(target);
    }

    @Test
    public void evaluateSkipsHatchWhenSharedConfigurationIsNotBlank() {
        TestController controller = new TestController();
        Object hatch = new TestHatchHandler(
            HatchAssignmentData.EMPTY,
            new ItemStack[] { new ItemStack(Items.paper, 1, 0) }).createHatch();
        controller.addHatch((IDualInputHatch) hatch);

        PatternRoutingRepairEvaluator.RepairTarget target = PatternRoutingRepairEvaluator.evaluate(controller);

        assertNull(target);
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

    private static final class TestController {

        private final List<IDualInputHatch> dualInputHatches = new ArrayList<IDualInputHatch>();

        public List<IDualInputHatch> getDualInputHatches() {
            return dualInputHatches;
        }

        private void addHatch(IDualInputHatch hatch) {
            dualInputHatches.add(hatch);
        }
    }

    private interface TestCraftingHatch extends IDualInputHatch, HatchAssignmentHolder {

        ItemStack[] getSharedItems();
    }

    private static final class TestHatchHandler implements InvocationHandler {

        private HatchAssignmentData assignment;
        private final ItemStack[] sharedItems;

        private TestHatchHandler(HatchAssignmentData assignment, ItemStack[] sharedItems) {
            this.assignment = assignment;
            this.sharedItems = sharedItems != null ? sharedItems : new ItemStack[0];
        }

        private Object createHatch() {
            return Proxy.newProxyInstance(
                PatternRoutingRepairEvaluatorTest.class.getClassLoader(),
                new Class<?>[] { TestCraftingHatch.class },
                this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String name = method.getName();
            if ("getSharedItems".equals(name)) {
                return sharedItems;
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
