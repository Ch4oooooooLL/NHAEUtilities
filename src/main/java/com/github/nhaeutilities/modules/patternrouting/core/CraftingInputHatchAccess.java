package com.github.nhaeutilities.modules.patternrouting.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.github.nhaeutilities.accessor.patternrouting.HatchAssignmentHolder;

import gregtech.common.tileentities.machines.IDualInputHatch;

final class CraftingInputHatchAccess {

    private static final String HATCH_CLASS_NAME = "gregtech.common.tileentities.machines.MTEHatchCraftingInputME";

    private CraftingInputHatchAccess() {}

    static boolean isCraftingInputHatch(Object machine) {
        if (machine instanceof IDualInputHatch && machine instanceof HatchAssignmentHolder) {
            return true;
        }
        Class<?> current = machine != null ? machine.getClass() : null;
        while (current != null) {
            if (HATCH_CLASS_NAME.equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        return false;
    }

    static int getCircuitSlot(Object hatch) {
        Object value = invokeNoArg(hatch, "getCircuitSlot");
        return value instanceof Integer ? ((Integer) value).intValue() : -1;
    }

    static ItemStack getStackInSlot(Object hatch, int slot) {
        Object value = invoke(hatch, "getStackInSlot", new Class<?>[] { Integer.TYPE }, Integer.valueOf(slot));
        return value instanceof ItemStack ? (ItemStack) value : null;
    }

    static ItemStack[] getSharedItems(Object hatch) {
        Object value = invokeNoArg(hatch, "getSharedItems");
        return value instanceof ItemStack[] ? (ItemStack[]) value : new ItemStack[0];
    }

    static IInventory getPatterns(Object hatch) {
        Object value = invokeNoArg(hatch, "getPatterns");
        return value instanceof IInventory ? (IInventory) value : null;
    }

    static int getPatternSlotLimit(Object hatch, IInventory inventory, int fallback) {
        int reflected = readStaticIntField(hatch != null ? hatch.getClass() : null, "MAX_PATTERN_COUNT");
        int inventorySize = inventory != null ? inventory.getSizeInventory() : 0;
        int limit = reflected > 0 ? Math.min(reflected, inventorySize) : Math.min(fallback, inventorySize);
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][assignment] hatch introspection pattern slot limit hatch=%s reflected=%s fallback=%s inventorySize=%s limit=%s",
            hatch != null ? hatch.getClass()
                .getName() : "null",
            reflected,
            fallback,
            inventorySize,
            limit);
        return limit;
    }

    private static Object invokeNoArg(Object target, String methodName) {
        return invoke(target, methodName, new Class<?>[0]);
    }

    private static Object invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (target == null) {
            return null;
        }

        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Method method = current.getDeclaredMethod(methodName, parameterTypes);
                method.setAccessible(true);
                return method.invoke(target, args);
            } catch (Exception ignored) {
                current = current.getSuperclass();
            }
        }
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][assignment] hatch introspection method-miss target=%s method=%s",
            target.getClass()
                .getName(),
            methodName);
        return null;
    }

    private static int readStaticIntField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.getInt(null);
            } catch (Exception ignored) {
                current = current.getSuperclass();
            }
        }
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][assignment] hatch introspection static-field-miss type=%s field=%s",
            type != null ? type.getName() : "null",
            fieldName);
        return -1;
    }
}
