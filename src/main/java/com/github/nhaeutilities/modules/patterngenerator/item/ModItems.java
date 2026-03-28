package com.github.nhaeutilities.modules.patterngenerator.item;

import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;

public final class ModItems {

    public static Item itemPatternGenerator;

    private ModItems() {}

    public static void init() {
        if (itemPatternGenerator != null) {
            return;
        }

        itemPatternGenerator = new ItemPatternGenerator();
        GameRegistry.registerItem(itemPatternGenerator, "pattern_generator");
    }
}
