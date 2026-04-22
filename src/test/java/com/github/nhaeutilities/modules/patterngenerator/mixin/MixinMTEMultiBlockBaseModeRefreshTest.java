package com.github.nhaeutilities.modules.patternrouting.mixin;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;

import org.junit.Test;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.nhaeutilities.accessor.patternrouting.HatchAssignmentHolder;
import com.github.nhaeutilities.modules.patternrouting.core.HatchAssignmentData;

import gregtech.common.tileentities.machines.IDualInputHatch;

public class MixinMTEMultiBlockBaseModeRefreshTest {

    @Test
    public void modeChangeHookRefreshesAssignmentsWhenMachineIsFormed() throws Exception {
        TestMixin mixin = new TestMixin();
        AssignmentTrackingHandler trackingHandler = new AssignmentTrackingHandler();
        mixin.mDualInputHatches = new ArrayList<IDualInputHatch>();
        mixin.mDualInputHatches.add(trackingHandler.createHatch());
        mixin.mMachine = true;

        invokeModeRefreshHook(mixin, 1);

        assertEquals(1, trackingHandler.setCount);
    }

    private static void invokeModeRefreshHook(TestMixin mixin, int mode) throws Exception {
        Method method = MixinMTEMultiBlockBaseModeRefresh.class
            .getDeclaredMethod("nhaeutilities$refreshAssignmentsAfterModeChange", int.class, CallbackInfo.class);
        method.setAccessible(true);
        method.invoke(mixin, mode, new CallbackInfo("setMachineMode", false));
    }

    private static final class TestMixin extends MixinMTEMultiBlockBaseModeRefresh {
    }

    private static final class AssignmentTrackingHandler implements java.lang.reflect.InvocationHandler {

        private int setCount;
        private HatchAssignmentData assignmentData = HatchAssignmentData.EMPTY;

        private IDualInputHatch createHatch() {
            return (IDualInputHatch) Proxy.newProxyInstance(
                MixinMTEMultiBlockBaseModeRefreshTest.class.getClassLoader(),
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
                setCount++;
                assignmentData = args[0] instanceof HatchAssignmentData ? (HatchAssignmentData) args[0]
                    : HatchAssignmentData.EMPTY;
                return null;
            }
            if ("nhaeutilities$clearAssignmentData".equals(methodName)) {
                assignmentData = HatchAssignmentData.EMPTY;
                return null;
            }
            Class<?> returnType = method.getReturnType();
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
