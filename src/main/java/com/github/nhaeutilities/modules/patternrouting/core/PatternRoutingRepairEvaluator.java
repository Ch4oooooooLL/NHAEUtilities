package com.github.nhaeutilities.modules.patternrouting.core;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.nhaeutilities.accessor.patternrouting.HatchAssignmentHolder;

import gregtech.common.tileentities.machines.IDualInputHatch;

final class PatternRoutingRepairEvaluator {

    private PatternRoutingRepairEvaluator() {}

    static RepairTarget evaluate(Object controller) {
        if (controller == null) {
            return null;
        }
        List<IDualInputHatch> dualInputHatches = resolveDualInputHatches(controller);
        if (dualInputHatches.isEmpty()) {
            return null;
        }

        List<BrokenHatch> brokenHatches = new ArrayList<BrokenHatch>();
        for (IDualInputHatch hatch : dualInputHatches) {
            if (!CraftingInputHatchAccess.isCraftingInputHatch(hatch)) {
                continue;
            }
            if (!CraftingInputHatchAccess.hasBlankSharedConfiguration(hatch)) {
                continue;
            }
            Object writable = CraftingInputHatchAccess.resolveWritableHatch(hatch);
            if (!(writable instanceof HatchAssignmentHolder)) {
                continue;
            }
            HatchAssignmentData expectedAssignment = expectedBlankAssignment(controller);
            HatchAssignmentData actualAssignment = ((HatchAssignmentHolder) writable).nhaeutilities$getAssignmentData();
            if (!matchesExpected(actualAssignment, expectedAssignment)) {
                brokenHatches.add(new BrokenHatch(hatch, expectedAssignment));
            }
        }

        return brokenHatches.isEmpty() ? null : new RepairTarget(controller, brokenHatches);
    }

    static HatchAssignmentData expectedBlankAssignment(Object controller) {
        String recipeCategory = resolveRecipeCategory(controller);
        return new HatchAssignmentData(
            PatternRoutingNbt.buildAssignmentKey(recipeCategory, "", ""),
            recipeCategory,
            "",
            "");
    }

    static boolean matchesExpected(HatchAssignmentData actual, HatchAssignmentData expected) {
        if (expected == null) {
            return actual == null || !actual.isAssigned();
        }
        if (actual == null || !actual.isAssigned()) {
            return false;
        }
        return expected.assignmentKey.equals(actual.assignmentKey)
            && expected.recipeCategory.equals(actual.recipeCategory)
            && expected.circuitKey.equals(actual.circuitKey)
            && expected.manualItemsKey.equals(actual.manualItemsKey);
    }

    @SuppressWarnings("unchecked")
    private static List<IDualInputHatch> resolveDualInputHatches(Object controller) {
        Method getter = findNoArgMethod(controller.getClass(), "getDualInputHatches");
        if (getter == null) {
            return Collections.emptyList();
        }
        try {
            Object value = getter.invoke(controller);
            return value instanceof List<?> ? (List<IDualInputHatch>) value : Collections.<IDualInputHatch>emptyList();
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }

    private static String resolveRecipeCategory(Object controller) {
        Method method = findNoArgMethod(controller.getClass(), "getRecipeMap");
        if (method != null) {
            try {
                Object value = method.invoke(controller);
                if (value instanceof gregtech.api.recipe.RecipeMap<?>) {
                    gregtech.api.recipe.RecipeMap<?> recipeMap = (gregtech.api.recipe.RecipeMap<?>) value;
                    if (recipeMap.getFrontend() != null && recipeMap.getFrontend()
                        .getUIProperties() != null
                        && recipeMap.getFrontend()
                            .getUIProperties().neiTransferRectId != null
                        && !recipeMap.getFrontend()
                            .getUIProperties().neiTransferRectId.isEmpty()) {
                        return recipeMap.getFrontend()
                            .getUIProperties().neiTransferRectId;
                    }
                    if (recipeMap.unlocalizedName != null && !recipeMap.unlocalizedName.isEmpty()) {
                        return recipeMap.unlocalizedName;
                    }
                }
            } catch (Exception ignored) {}
        }
        return controller.getClass()
            .getName();
    }

    private static Method findNoArgMethod(Class<?> type, String methodName) {
        Class<?> current = type;
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    static final class RepairTarget {

        final Object controller;
        final List<BrokenHatch> brokenHatches;

        RepairTarget(Object controller, List<BrokenHatch> brokenHatches) {
            this.controller = controller;
            this.brokenHatches = brokenHatches != null ? brokenHatches : Collections.<BrokenHatch>emptyList();
        }
    }

    static final class BrokenHatch {

        final Object hatch;
        final HatchAssignmentData expectedAssignment;

        BrokenHatch(Object hatch, HatchAssignmentData expectedAssignment) {
            this.hatch = hatch;
            this.expectedAssignment = expectedAssignment;
        }
    }
}
