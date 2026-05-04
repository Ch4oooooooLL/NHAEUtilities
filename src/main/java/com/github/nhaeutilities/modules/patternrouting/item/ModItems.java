package com.github.nhaeutilities.modules.patternrouting.item;

import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameRegistry;

public final class ModItems {

    public static Item itemRecipeMapAnalyzer;

    private ModItems() {}

    public static void init() {
        if (itemRecipeMapAnalyzer != null) {
            return;
        }

        itemRecipeMapAnalyzer = new ItemRecipeMapAnalyzer();
        GameRegistry.registerItem(itemRecipeMapAnalyzer, "pattern_routing_recipe_map_analyzer");
    }
}
