package com.github.nhaeutilities.modules.patternrouting.core;

import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.github.nhaeutilities.accessor.patternrouting.HatchAssignmentHolder;

import gregtech.api.interfaces.tileentity.RecipeMapWorkable;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.common.tileentities.machines.IDualInputHatch;

public final class HatchAssignmentService {

    private HatchAssignmentService() {}

    public static void refreshAssignments(Object controller) {
        if (controller == null) {
            PatternRoutingLog
                .debug("[NHAEUtilities][patternrouting][assignment] refresh skipped reason=null-controller");
            return;
        }

        List<IDualInputHatch> dualInputHatches = resolveDualInputHatches(controller);
        String recipeCategory = resolveRecipeCategory(controller);
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][assignment] refresh start controller=%s recipeCategory=%s hatchCount=%s",
            controller.getClass()
                .getName(),
            recipeCategory,
            dualInputHatches.size());
        for (IDualInputHatch dualInputHatch : dualInputHatches) {
            if (!CraftingInputHatchAccess.isCraftingInputHatch(dualInputHatch)) {
                PatternRoutingLog.debug(
                    "[NHAEUtilities][patternrouting][assignment] refresh skip hatch reason=not-crafting-input hatch=%s",
                    dualInputHatch != null ? dualInputHatch.getClass()
                        .getName() : "null");
                continue;
            }

            if (!(dualInputHatch instanceof HatchAssignmentHolder)) {
                PatternRoutingLog.debug(
                    "[NHAEUtilities][patternrouting][assignment] refresh skip hatch reason=missing-holder-interface hatch=%s",
                    dualInputHatch.getClass()
                        .getName());
                continue;
            }

            int circuitSlot = CraftingInputHatchAccess.getCircuitSlot(dualInputHatch);
            CraftingInputHatchAccess.SharedItemDescriptor descriptorItems = CraftingInputHatchAccess
                .getSharedItemDescriptor(dualInputHatch);
            ItemStack circuit = descriptorItems.circuit;
            ItemStack[] manualItems = descriptorItems.manualItems;
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][assignment] hatch introspection hatch=%s circuitSlot=%s sharedCount=%s manualCount=%s hasCircuit=%s",
                dualInputHatch.getClass()
                    .getName(),
                circuitSlot,
                descriptorItems.sharedCount,
                manualItems.length,
                circuit != null);
            String circuitKey = PatternRoutingNbt.circuitKey(circuit);
            String manualItemsKey = PatternRoutingNbt.manualItemsKey(manualItems);
            RoutingDescriptor descriptor = new RoutingDescriptor(recipeCategory, circuitKey, manualItemsKey);
            String assignmentKey = PatternRoutingNbt
                .buildAssignmentKey(descriptor.recipeCategory, descriptor.circuitKey, descriptor.manualItemsKey);

            HatchAssignmentData assignmentData = new HatchAssignmentData(
                assignmentKey,
                descriptor.recipeCategory,
                descriptor.circuitKey,
                descriptor.manualItemsKey);
            ((HatchAssignmentHolder) dualInputHatch).nhaeutilities$setAssignmentData(assignmentData);
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][assignment] write hatch assignment hatch=%s recipeCategory=%s circuit=%s manual=%s assignment=%s",
                dualInputHatch.getClass()
                    .getName(),
                descriptor.recipeCategory,
                descriptor.circuitKey,
                descriptor.manualItemsKey,
                assignmentKey);
        }
    }

    public static void clearAssignments(List<IDualInputHatch> dualInputHatches) {
        if (dualInputHatches == null) {
            return;
        }

        int cleared = 0;
        for (IDualInputHatch dualInputHatch : dualInputHatches) {
            if (dualInputHatch instanceof HatchAssignmentHolder) {
                ((HatchAssignmentHolder) dualInputHatch).nhaeutilities$clearAssignmentData();
                cleared++;
            }
        }
        PatternRoutingLog.debug("[NHAEUtilities][patternrouting][assignment] clear assignments count=%s", cleared);
    }

    private static String resolveRecipeCategory(Object controller) {
        RecipeMap<?> recipeMap = resolveRecipeMap(controller);
        if (recipeMap != null && recipeMap.getFrontend() != null
            && recipeMap.getFrontend()
                .getUIProperties() != null) {
            String transferId = recipeMap.getFrontend()
                .getUIProperties().neiTransferRectId;
            if (transferId != null && !transferId.isEmpty()) {
                PatternRoutingLog.debug(
                    "[NHAEUtilities][patternrouting][assignment] resolve recipeCategory source=neiTransferRectId controller=%s recipeCategory=%s",
                    controller.getClass()
                        .getName(),
                    transferId);
                return transferId;
            }
            if (recipeMap.unlocalizedName != null && !recipeMap.unlocalizedName.isEmpty()) {
                PatternRoutingLog.debug(
                    "[NHAEUtilities][patternrouting][assignment] resolve recipeCategory source=recipeMap.unlocalizedName controller=%s recipeCategory=%s",
                    controller.getClass()
                        .getName(),
                    recipeMap.unlocalizedName);
                return recipeMap.unlocalizedName;
            }
        }
        String fallback = controller.getClass()
            .getName();
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][assignment] resolve recipeCategory source=controller-class controller=%s recipeCategory=%s",
            controller.getClass()
                .getName(),
            fallback);
        return fallback;
    }

    private static RecipeMap<?> resolveRecipeMap(Object controller) {
        if (controller instanceof RecipeMapWorkable) {
            return ((RecipeMapWorkable) controller).getRecipeMap();
        }

        Method method = findNoArgMethod(controller.getClass(), "getRecipeMap");
        if (method == null) {
            return null;
        }

        try {
            Object value = method.invoke(controller);
            return value instanceof RecipeMap<?> ? (RecipeMap<?>) value : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static List<IDualInputHatch> resolveDualInputHatches(Object controller) {
        if (controller instanceof MTEMultiBlockBase) {
            return ((MTEMultiBlockBase) controller).mDualInputHatches != null
                ? ((MTEMultiBlockBase) controller).mDualInputHatches
                : java.util.Collections.<IDualInputHatch>emptyList();
        }

        Class<?> current = controller.getClass();
        while (current != null) {
            try {
                java.lang.reflect.Field field = current.getDeclaredField("mDualInputHatches");
                field.setAccessible(true);
                Object value = field.get(controller);
                return value instanceof List<?> ? (List<IDualInputHatch>) value
                    : java.util.Collections.<IDualInputHatch>emptyList();
            } catch (Exception ignored) {
                current = current.getSuperclass();
            }
        }
        return java.util.Collections.<IDualInputHatch>emptyList();
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
}
