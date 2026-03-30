package com.github.nhaeutilities.modules.patterngenerator.routing;

import java.lang.reflect.Method;
import java.util.List;

import net.minecraft.item.ItemStack;

import com.github.nhaeutilities.accessor.patterngenerator.HatchAssignmentHolder;

import gregtech.api.interfaces.tileentity.RecipeMapWorkable;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.recipe.RecipeMap;
import gregtech.common.tileentities.machines.IDualInputHatch;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

public final class HatchAssignmentService {

    private HatchAssignmentService() {}

    public static void refreshAssignments(MTEMultiBlockBase controller) {
        if (controller == null) {
            return;
        }

        String recipeFamily = resolveRecipeFamily(controller);
        for (IDualInputHatch dualInputHatch : controller.mDualInputHatches) {
            if (!(dualInputHatch instanceof MTEHatchCraftingInputME)) {
                continue;
            }

            MTEHatchCraftingInputME hatch = (MTEHatchCraftingInputME) dualInputHatch;
            if (!(hatch instanceof HatchAssignmentHolder)) {
                continue;
            }

            ItemStack circuit = hatch.getCircuitSlot() >= 0 ? hatch.getStackInSlot(hatch.getCircuitSlot()) : null;
            ItemStack[] sharedItems = hatch.getSharedItems();
            ItemStack[] manualItems = extractManualItems(sharedItems);
            String circuitKey = PatternRoutingNbt.circuitKey(circuit);
            String manualItemsKey = PatternRoutingNbt.manualItemsKey(manualItems);
            String assignmentKey = PatternRoutingNbt.buildAssignmentKey(
                recipeFamily,
                "",
                circuitKey,
                manualItemsKey);

            ((HatchAssignmentHolder) hatch).nhaeutilities$setAssignmentData(
                new HatchAssignmentData(assignmentKey, recipeFamily, "", circuitKey, manualItemsKey));
        }
    }

    public static void clearAssignments(List<IDualInputHatch> dualInputHatches) {
        if (dualInputHatches == null) {
            return;
        }

        for (IDualInputHatch dualInputHatch : dualInputHatches) {
            if (dualInputHatch instanceof HatchAssignmentHolder) {
                ((HatchAssignmentHolder) dualInputHatch).nhaeutilities$clearAssignmentData();
            }
        }
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
        if (recipeMap != null && recipeMap.getFrontend() != null && recipeMap.getFrontend()
            .getUIProperties() != null) {
            String transferId = recipeMap.getFrontend()
                .getUIProperties().neiTransferRectId;
            if (transferId != null && !transferId.isEmpty()) {
                return transferId;
            }
            if (recipeMap.unlocalizedName != null && !recipeMap.unlocalizedName.isEmpty()) {
                return recipeMap.unlocalizedName;
            }
        }
        return controller.getClass()
            .getName();
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
