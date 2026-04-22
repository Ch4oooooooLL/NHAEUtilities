package com.github.nhaeutilities.modules.patternrouting.core;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.github.nhaeutilities.accessor.patternrouting.HatchAssignmentHolder;

import gregtech.common.tileentities.machines.IDualInputHatch;

final class CraftingInputHatchAccess {

    private static final String HATCH_CLASS_NAME = "gregtech.common.tileentities.machines.MTEHatchCraftingInputME";
    private static final int GT_MANUAL_SLOT_COUNT = 9;

    private CraftingInputHatchAccess() {}

    static boolean isCraftingInputHatch(Object machine) {
        if (machine == null) {
            return false;
        }
        if (machine instanceof IDualInputHatch && machine instanceof HatchAssignmentHolder) {
            return true;
        }
        Class<?> current = machine.getClass();
        while (current != null) {
            if (HATCH_CLASS_NAME.equals(current.getName())) {
                return true;
            }
            current = current.getSuperclass();
        }
        if (!(machine instanceof IDualInputHatch)) {
            return false;
        }
        return hasNoArgMethod(machine, "getPatterns") && hasNoArgMethod(machine, "getCircuitSlot")
            && hasNoArgMethod(machine, "getSharedItems");
    }

    static Object resolveWritableHatch(Object hatch) {
        if (hatch == null) {
            return null;
        }
        Object master = invokeNoArg(hatch, "getMasterSuper");
        if (master == null) {
            master = invokeNoArg(hatch, "getCraftingMaster");
        }
        if (master == null) {
            master = invokeNoArg(hatch, "getMaster");
        }
        if (master == null || master == hatch) {
            return hatch;
        }
        return resolveWritableHatch(master);
    }

    static int getCircuitSlot(Object hatch) {
        Object writable = resolveWritableHatch(hatch);
        Object value = invokeNoArg(writable, "getCircuitSlot");
        return value instanceof Integer ? ((Integer) value).intValue() : -1;
    }

    static ItemStack getStackInSlot(Object hatch, int slot) {
        Object writable = resolveWritableHatch(hatch);
        Object value = invoke(writable, "getStackInSlot", new Class<?>[] { Integer.TYPE }, Integer.valueOf(slot));
        return value instanceof ItemStack ? (ItemStack) value : null;
    }

    static ItemStack[] getSharedItems(Object hatch) {
        Object writable = resolveWritableHatch(hatch);
        Object value = invokeNoArg(writable, "getSharedItems");
        return value instanceof ItemStack[] ? (ItemStack[]) value : new ItemStack[0];
    }

    static SharedItemDescriptor getSharedItemDescriptor(Object hatch) {
        ItemStack[] sharedItems = getSharedItems(hatch);
        ItemStack circuit = sharedItems.length > 0 ? sharedItems[0] : null;
        ItemStack[] manualItems = new ItemStack[Math.max(0, sharedItems.length - 1)];
        if (manualItems.length > 0) {
            System.arraycopy(sharedItems, 1, manualItems, 0, manualItems.length);
        }
        return new SharedItemDescriptor(circuit, manualItems, sharedItems.length);
    }

    static boolean hasBlankSharedConfiguration(Object hatch) {
        SharedItemDescriptor descriptor = getSharedItemDescriptor(hatch);
        if (descriptor.circuit != null) {
            return false;
        }
        for (ItemStack manualItem : descriptor.manualItems) {
            if (manualItem != null) {
                return false;
            }
        }
        return true;
    }

    static boolean tryApplyRoutingConfiguration(Object hatch, PatternRoutingNbt.RoutingMetadata metadata,
        ItemStack[] manualItems) {
        Object writable = resolveWritableHatch(hatch);
        if (writable == null || metadata == null) {
            return false;
        }
        if (!hasBlankSharedConfiguration(writable)) {
            return false;
        }

        ItemStack circuit = PatternRoutingNbt.programmingCircuitStack(metadata);
        ItemStack[] requestedManualItems = manualItems != null ? manualItems : new ItemStack[0];
        int circuitSlot = getCircuitSlot(writable);
        SlotRange manualSlots = resolveManualSlots(writable, circuitSlot);
        if ((circuit != null && circuitSlot < 0) || requestedManualItems.length > manualSlots.size) {
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][assignment] auto-config skipped hatch=%s reason=slot-unavailable circuitSlot=%s manualSize=%s needed=%s",
                writable.getClass()
                    .getName(),
                circuitSlot,
                manualSlots.size,
                requestedManualItems.length);
            return false;
        }

        java.util.List<Integer> writtenSlots = new java.util.ArrayList<Integer>();
        if (circuit != null) {
            if (!writeSlot(writable, circuitSlot, circuit)) {
                return false;
            }
            writtenSlots.add(Integer.valueOf(circuitSlot));
        }
        for (int index = 0; index < requestedManualItems.length; index++) {
            int slot = manualSlots.start + index;
            if (!writeSlot(writable, slot, requestedManualItems[index])) {
                rollbackWrittenSlots(writable, writtenSlots);
                return false;
            }
            writtenSlots.add(Integer.valueOf(slot));
        }

        SharedItemDescriptor descriptor = getSharedItemDescriptor(writable);
        boolean circuitMatches = metadata.circuitKey.isEmpty() ? descriptor.circuit == null
            : metadata.circuitKey.equals(PatternRoutingNbt.circuitKey(descriptor.circuit));
        boolean manualMatches = metadata.manualItemsKey
            .equals(PatternRoutingNbt.manualItemsKey(descriptor.manualItems));
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][assignment] auto-config hatch=%s circuitMatches=%s manualMatches=%s circuit=%s manual=%s",
            writable.getClass()
                .getName(),
            circuitMatches,
            manualMatches,
            descriptor.circuit != null ? PatternRoutingNbt.circuitKey(descriptor.circuit) : "",
            PatternRoutingNbt.manualItemsKey(descriptor.manualItems));
        return circuitMatches && manualMatches;
    }

    static final class SharedItemDescriptor {

        final ItemStack circuit;
        final ItemStack[] manualItems;
        final int sharedCount;

        SharedItemDescriptor(ItemStack circuit, ItemStack[] manualItems, int sharedCount) {
            this.circuit = circuit;
            this.manualItems = manualItems != null ? manualItems : new ItemStack[0];
            this.sharedCount = sharedCount;
        }
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

    private static boolean writeSlot(Object hatch, int slot, ItemStack stack) {
        if (hatch == null || slot < 0) {
            return false;
        }
        ItemStack copy = stack != null ? stack.copy() : null;
        Object result = invoke(
            hatch,
            "setInventorySlotContents",
            new Class<?>[] { Integer.TYPE, ItemStack.class },
            Integer.valueOf(slot),
            copy);
        ItemStack written = getStackInSlot(hatch, slot);
        boolean success = stack == null ? written == null
            : PatternRoutingNbt.itemSignature(stack)
                .equals(PatternRoutingNbt.itemSignature(written));
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][assignment] hatch write slot hatch=%s slot=%s success=%s expected=%s actual=%s result=%s",
            hatch.getClass()
                .getName(),
            slot,
            success,
            stack != null ? PatternRoutingNbt.itemSignature(stack) : "",
            written != null ? PatternRoutingNbt.itemSignature(written) : "",
            result != null);
        return success;
    }

    private static SlotRange resolveManualSlots(Object hatch, int circuitSlot) {
        int manualStart = readStaticIntField(hatch != null ? hatch.getClass() : null, "SLOT_MANUAL_START");
        int manualSize = readStaticIntField(hatch != null ? hatch.getClass() : null, "SLOT_MANUAL_SIZE");
        if (manualStart >= 0 && manualSize > 0) {
            return new SlotRange(manualStart, manualSize);
        }
        return new SlotRange(circuitSlot >= 0 ? circuitSlot + 1 : -1, GT_MANUAL_SLOT_COUNT);
    }

    private static void rollbackWrittenSlots(Object hatch, java.util.List<Integer> writtenSlots) {
        if (hatch == null || writtenSlots == null) {
            return;
        }
        for (Integer slot : writtenSlots) {
            if (slot != null) {
                writeSlot(hatch, slot.intValue(), null);
            }
        }
    }

    private static Object invokeNoArg(Object target, String methodName) {
        return invoke(target, methodName, new Class<?>[0]);
    }

    private static boolean hasNoArgMethod(Object target, String methodName) {
        if (target == null) {
            return false;
        }
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                current.getDeclaredMethod(methodName, new Class<?>[0]);
                return true;
            } catch (Exception ignored) {
                current = current.getSuperclass();
            }
        }
        return false;
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

    private static final class SlotRange {

        final int start;
        final int size;

        SlotRange(int start, int size) {
            this.start = start;
            this.size = Math.max(0, size);
        }
    }
}
