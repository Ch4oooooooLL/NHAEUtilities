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
            return;
        }

        if (!(controller instanceof MTEMultiBlockBase)) {
            return;
        }

        MTEMultiBlockBase multiBlock = (MTEMultiBlockBase) controller;

        String recipeFamily = resolveRecipeFamily(multiBlock);
        PatternRoutingLog.info(
            "[NHAEUtilities][patternrouting] refresh assignments controller=%s recipeFamily=%s hatchCount=%s",
            multiBlock.getClass()
                .getName(),
            recipeFamily,
            multiBlock.mDualInputHatches != null ? multiBlock.mDualInputHatches.size() : 0);
        for (IDualInputHatch dualInputHatch : multiBlock.mDualInputHatches) {
            if (!CraftingInputHatchAccess.isCraftingInputHatch(dualInputHatch)) {
                continue;
            }

            if (!(dualInputHatch instanceof HatchAssignmentHolder)) {
                continue;
            }

            int circuitSlot = CraftingInputHatchAccess.getCircuitSlot(dualInputHatch);
            ItemStack circuit = circuitSlot >= 0 ? CraftingInputHatchAccess.getStackInSlot(dualInputHatch, circuitSlot)
                : null;
            ItemStack[] sharedItems = CraftingInputHatchAccess.getSharedItems(dualInputHatch);
            ItemStack[] manualItems = extractManualItems(sharedItems);
            String circuitKey = PatternRoutingNbt.circuitKey(circuit);
            String manualItemsKey = PatternRoutingNbt.manualItemsKey(manualItems);
            String assignmentKey = PatternRoutingNbt.buildAssignmentKey(recipeFamily, "", circuitKey, manualItemsKey);

            HatchAssignmentData assignmentData = new HatchAssignmentData(
                assignmentKey,
                recipeFamily,
                "",
                circuitKey,
                manualItemsKey);
            ((HatchAssignmentHolder) dualInputHatch).nhaeutilities$setAssignmentData(assignmentData);
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] assign hatch hatch=%s recipeFamily=%s recipeId=%s circuit=%s manual=%s assignment=%s",
                dualInputHatch.getClass()
                    .getName(),
                recipeFamily,
                assignmentData.recipeId,
                circuitKey,
                manualItemsKey,
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
        PatternRoutingLog.info("[NHAEUtilities][patternrouting] cleared hatch assignments count=%s", cleared);
    }

    private static ItemStack[] extractManualItems(ItemStack[] sharedItems) {
        if (sharedItems == null || sharedItems.length <= 1) {
            return new ItemStack[0];
        }

        ItemStack[] manualItems = new ItemStack[sharedItems.length - 1];
        System.arraycopy(sharedItems, 1, manualItems, 0, manualItems.length);
        return manualItems;
    }

    private static String resolveRecipeFamily(MTEMultiBlockBase controller) {
        RecipeMap<?> recipeMap = resolveRecipeMap(controller);
        if (recipeMap != null && recipeMap.getFrontend() != null
            && recipeMap.getFrontend()
                .getUIProperties() != null) {
            String transferId = recipeMap.getFrontend()
                .getUIProperties().neiTransferRectId;
            if (transferId != null && !transferId.isEmpty()) {
                PatternRoutingLog.info(
                    "[NHAEUtilities][patternrouting] resolve recipeFamily via neiTransferRectId controller=%s recipeFamily=%s",
                    controller.getClass()
                        .getName(),
                    transferId);
                return transferId;
            }
            if (recipeMap.unlocalizedName != null && !recipeMap.unlocalizedName.isEmpty()) {
                PatternRoutingLog.info(
                    "[NHAEUtilities][patternrouting] resolve recipeFamily via recipeMap.unlocalizedName controller=%s recipeFamily=%s",
                    controller.getClass()
                        .getName(),
                    recipeMap.unlocalizedName);
                return recipeMap.unlocalizedName;
            }
        }
        String fallback = controller.getClass()
            .getName();
        PatternRoutingLog.info(
            "[NHAEUtilities][patternrouting] resolve recipeFamily via controller class controller=%s recipeFamily=%s",
            controller.getClass()
                .getName(),
            fallback);
        return fallback;
    }

    private static RecipeMap<?> resolveRecipeMap(MTEMultiBlockBase controller) {
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
