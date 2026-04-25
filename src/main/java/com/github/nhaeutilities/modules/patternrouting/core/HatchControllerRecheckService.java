package com.github.nhaeutilities.modules.patternrouting.core;

import java.lang.reflect.Method;

import com.github.nhaeutilities.accessor.patternrouting.HatchAssignmentHolder;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

final class HatchControllerRecheckService {

    private HatchControllerRecheckService() {}

    static RecheckResult recheckAndVerify(Object hatch, HatchAssignmentData expectedAssignment) {
        Object controller = resolveController(hatch);
        if (controller == null) {
            return RecheckResult.failure("controller-unresolved");
        }

        IGregTechTileEntity baseTile = resolveBaseTile(hatch, controller);
        RecheckResult invoked = invokeCheckStructure(controller, baseTile);
        if (!invoked.success && "checkStructure-unavailable".equals(invoked.failureReason)) {
            HatchAssignmentService.refreshAssignments(controller);
            invoked = RecheckResult.success("refreshAssignments", controller);
        }
        if (!invoked.success) {
            return invoked;
        }

        HatchAssignmentData actual = currentAssignment(hatch);
        if (!matchesAssignment(actual, expectedAssignment)) {
            return RecheckResult.failureWithPath("assignment-mismatch", invoked.path, controller);
        }
        return RecheckResult.success(invoked.path, controller);
    }

    private static boolean matchesAssignment(HatchAssignmentData actual, HatchAssignmentData expected) {
        if (expected == null) {
            return actual == null || !actual.isAssigned();
        }
        if (actual == null) {
            return false;
        }
        return expected.assignmentKey.equals(actual.assignmentKey);
    }

    private static HatchAssignmentData currentAssignment(Object hatch) {
        Object writable = CraftingInputHatchAccess.resolveWritableHatch(hatch);
        if (writable instanceof HatchAssignmentHolder) {
            return ((HatchAssignmentHolder) writable).nhaeutilities$getAssignmentData();
        }
        if (hatch instanceof HatchAssignmentHolder) {
            return ((HatchAssignmentHolder) hatch).nhaeutilities$getAssignmentData();
        }
        return null;
    }

    private static Object resolveController(Object hatch) {
        Object writable = CraftingInputHatchAccess.resolveWritableHatch(hatch);
        Object directMaster = invokeNoArg(writable, "getMaster");
        if (directMaster != null && directMaster != writable) {
            return directMaster;
        }
        Object superMaster = invokeNoArg(writable, "getMasterSuper");
        if (superMaster != null && superMaster != writable) {
            return superMaster;
        }
        IGregTechTileEntity baseTile = resolveBaseTile(hatch, writable);
        if (baseTile != null) {
            Object metaTile = invokeNoArg(baseTile, "getMetaTileEntity");
            if (metaTile != null) {
                return metaTile;
            }
        }
        return writable != hatch ? writable : null;
    }

    private static IGregTechTileEntity resolveBaseTile(Object hatch, Object controller) {
        Object writable = CraftingInputHatchAccess.resolveWritableHatch(hatch);
        Object baseTile = invokeNoArg(writable, "getBaseMetaTileEntity");
        if (baseTile instanceof IGregTechTileEntity) {
            return (IGregTechTileEntity) baseTile;
        }
        baseTile = invokeNoArg(hatch, "getBaseMetaTileEntity");
        if (baseTile instanceof IGregTechTileEntity) {
            return (IGregTechTileEntity) baseTile;
        }
        baseTile = invokeNoArg(controller, "getBaseMetaTileEntity");
        return baseTile instanceof IGregTechTileEntity ? (IGregTechTileEntity) baseTile : null;
    }

    private static RecheckResult invokeCheckStructure(Object controller, IGregTechTileEntity baseTile) {
        Method method = findMethod(controller.getClass(), "checkStructure", new Class<?>[] { boolean.class });
        if (method == null) {
            method = findMethod(
                controller.getClass(),
                "checkStructure",
                new Class<?>[] { boolean.class, IGregTechTileEntity.class });
        }
        if (method == null) {
            method = findMethod(
                controller.getClass(),
                "checkMachine",
                new Class<?>[] { IGregTechTileEntity.class, net.minecraft.item.ItemStack.class });
        }
        if (method == null) {
            return RecheckResult.failure("checkStructure-unavailable");
        }

        try {
            Object result;
            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length == 1) {
                result = method.invoke(controller, false);
            } else if (parameterTypes.length == 2 && parameterTypes[0] == boolean.class) {
                result = method.invoke(controller, false, baseTile);
            } else {
                result = method.invoke(controller, baseTile, null);
            }
            if (result instanceof Boolean && !((Boolean) result).booleanValue()) {
                return RecheckResult.failureWithPath("checkStructure-failed", "checkStructure", controller);
            }
            return RecheckResult.success("checkStructure", controller);
        } catch (Exception ignored) {
            return RecheckResult.failureWithPath("checkStructure-failed", "checkStructure", controller);
        }
    }

    private static Method findMethod(Class<?> type, String methodName, Class<?>[] parameterTypes) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        try {
            Method method = type.getMethod(methodName, parameterTypes);
            method.setAccessible(true);
            return method;
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private static Object invokeNoArg(Object target, String methodName) {
        if (target == null) {
            return null;
        }
        Method method = findMethod(target.getClass(), methodName, new Class<?>[0]);
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(target);
        } catch (Exception ignored) {
            return null;
        }
    }

    static final class RecheckResult {

        final boolean success;
        final String path;
        final String failureReason;
        final Object controller;

        private RecheckResult(boolean success, String path, String failureReason, Object controller) {
            this.success = success;
            this.path = path;
            this.failureReason = failureReason;
            this.controller = controller;
        }

        static RecheckResult success(String path, Object controller) {
            return new RecheckResult(true, path, "", controller);
        }

        static RecheckResult failure(String reason) {
            return new RecheckResult(false, "", reason, null);
        }

        static RecheckResult failureWithPath(String reason, String path, Object controller) {
            return new RecheckResult(false, path, reason, controller);
        }
    }
}
