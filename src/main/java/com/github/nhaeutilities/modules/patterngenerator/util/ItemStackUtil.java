package com.github.nhaeutilities.modules.patterngenerator.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

public final class ItemStackUtil {

    private ItemStackUtil() {}

    public static String getSafeDisplayName(ItemStack stack) {
        if (stack == null) {
            return "";
        }

        Item item = stack.getItem();
        if (item == null) {
            return "";
        }

        try {
            String displayName = stack.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                return displayName;
            }
        } catch (RuntimeException ignored) {
        }

        Object registryName = Item.itemRegistry.getNameForObject(item);
        if (registryName != null) {
            return registryName.toString() + ":" + stack.getItemDamage();
        }

        int itemId = Item.getIdFromItem(item);
        return itemId >= 0 ? "[" + itemId + ":" + stack.getItemDamage() + "]" : "";
    }
}
