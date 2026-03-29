package com.github.nhaeutilities.modules.superwirelesskit.item;

import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;

public final class ModItems {

    public static Item itemSuperWirelessKit;

    private ModItems() {}

    public static void init() {
        if (itemSuperWirelessKit != null) {
            return;
        }

        itemSuperWirelessKit = new ItemSuperWirelessKit();
        GameRegistry.registerItem(itemSuperWirelessKit, "super_wireless_kit");
    }
}
