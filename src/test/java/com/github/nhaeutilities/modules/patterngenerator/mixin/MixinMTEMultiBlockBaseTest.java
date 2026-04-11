package com.github.nhaeutilities.modules.patterngenerator.mixin;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

import org.junit.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.nhaeutilities.accessor.patterngenerator.HatchAssignmentHolder;
import com.github.nhaeutilities.modules.patterngenerator.routing.HatchAssignmentData;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.tileentities.machines.IDualInputHatch;

public class MixinMTEMultiBlockBaseTest {

    @Test
    public void refreshHatchAssignmentsClearsStaleMetadataWhenStructureCheckFails() throws Exception {
        TestMixin mixin = new TestMixin();
        AssignmentTrackingHandler trackingHandler = new AssignmentTrackingHandler();
        mixin.mDualInputHatches = new ArrayList<>();
        mixin.mDualInputHatches.add(trackingHandler.createHatch());
        mixin.mMachine = true;

        invokeRefreshHook(mixin, false);

        assertEquals(1, trackingHandler.clearCount);
        assertEquals(HatchAssignmentData.EMPTY.assignmentKey, trackingHandler.assignmentData.assignmentKey);
    }

    @Test
    public void refreshHatchAssignmentsClearsStaleMetadataWhenControllerIsNotMachineAfterCheck() throws Exception {
        TestMixin mixin = new TestMixin();
        AssignmentTrackingHandler trackingHandler = new AssignmentTrackingHandler();
        mixin.mDualInputHatches = new ArrayList<>();
        mixin.mDualInputHatches.add(trackingHandler.createHatch());
        mixin.mMachine = false;

        invokeRefreshHook(mixin, true);

        assertEquals(1, trackingHandler.clearCount);
        assertEquals(HatchAssignmentData.EMPTY.assignmentKey, trackingHandler.assignmentData.assignmentKey);
    }

    private static void invokeRefreshHook(TestMixin mixin, boolean structureValid) throws Exception {
        Method method = MixinMTEMultiBlockBase.class.getDeclaredMethod(
            "nhaeutilities$refreshHatchAssignments",
            boolean.class,
            IGregTechTileEntity.class,
            CallbackInfoReturnable.class);
        method.setAccessible(true);
        method.invoke(mixin, false, null, new CallbackInfoReturnable<Boolean>("checkStructure", false, structureValid));
    }

    private static final class TestMixin extends MixinMTEMultiBlockBase {
    }

    private static final class AssignmentTrackingHandler implements InvocationHandler {

        private int clearCount;
        private HatchAssignmentData assignmentData = HatchAssignmentData.EMPTY;

        private IDualInputHatch createHatch() {
            return (IDualInputHatch) Proxy.newProxyInstance(
                MixinMTEMultiBlockBaseTest.class.getClassLoader(),
                new Class<?>[] { IDualInputHatch.class, HatchAssignmentHolder.class },
                this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            String methodName = method.getName();
            if ("nhaeutilities$getAssignmentData".equals(methodName)) {
                return assignmentData;
            }
            if ("nhaeutilities$setAssignmentData".equals(methodName)) {
                assignmentData = args[0] instanceof HatchAssignmentData ? (HatchAssignmentData) args[0]
                    : HatchAssignmentData.EMPTY;
                return null;
            }
            if ("nhaeutilities$clearAssignmentData".equals(methodName)) {
                clearCount++;
                assignmentData = HatchAssignmentData.EMPTY;
                return null;
            }
            return defaultValue(method.getReturnType());
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
}
