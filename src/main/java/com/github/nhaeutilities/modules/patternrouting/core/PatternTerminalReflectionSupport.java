package com.github.nhaeutilities.modules.patternrouting.core;

import java.lang.reflect.Field;

import net.minecraft.item.ItemStack;

import appeng.container.slot.SlotRestrictedInput;

public final class PatternTerminalReflectionSupport {

    private PatternTerminalReflectionSupport() {}

    public static String readButtonMessageName(Object message) {
        return readStringField(message, "Name");
    }

    public static SlotRestrictedInput readPatternSlotOut(Object container) {
        Object value = readField(container, "patternSlotOUT");
        if (value instanceof SlotRestrictedInput) {
            return (SlotRestrictedInput) value;
        }
        return null;
    }

    public static ItemStack peekPatternOutput(Object container) {
        SlotRestrictedInput slot = readPatternSlotOut(container);
        return slot != null ? slot.getStack() : null;
    }

    private static String readStringField(Object target, String fieldName) {
        Object value = readField(target, fieldName);
        return value instanceof String ? (String) value : "";
    }

    private static Object readField(Object target, String fieldName) {
        if (target == null || fieldName == null) {
            return null;
        }
        Class<?> current = target.getClass();
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(target);
            } catch (Exception ignored) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
