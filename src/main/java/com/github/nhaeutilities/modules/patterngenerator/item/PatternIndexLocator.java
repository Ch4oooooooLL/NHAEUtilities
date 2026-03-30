package com.github.nhaeutilities.modules.patterngenerator.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

public final class PatternIndexLocator {

    private PatternIndexLocator() {}

    public static boolean hasPatternIndex(EntityPlayer player) {
        if (player == null || ModItems.itemPatternIndex == null) {
            return false;
        }

        for (int slot = 0; slot < player.inventory.getSizeInventory(); slot++) {
            ItemStack stack = player.inventory.getStackInSlot(slot);
            if (stack != null && stack.getItem() == ModItems.itemPatternIndex) {
                return true;
            }
        }
        return false;
    }
}
