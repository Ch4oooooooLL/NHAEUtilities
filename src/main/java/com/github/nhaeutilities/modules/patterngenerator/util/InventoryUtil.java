package com.github.nhaeutilities.modules.patterngenerator.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import cpw.mods.fml.common.registry.GameRegistry;

public class InventoryUtil {

    public static int countItem(EntityPlayer player, ItemStack target) {
        if (player == null || target == null || target.getItem() == null) {
            return 0;
        }

        int count = 0;
        IInventory inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() == target.getItem() && stack.getItemDamage() == target.getItemDamage()) {
                count += stack.stackSize;
            }
        }
        return count;
    }

    public static boolean consumeItem(EntityPlayer player, ItemStack target, int amount) {
        if (countItem(player, target) < amount) {
            return false;
        }

        int remainToConsume = amount;
        IInventory inv = player.inventory;
        for (int i = 0; i < inv.getSizeInventory(); i++) {
            if (remainToConsume <= 0) {
                break;
            }

            ItemStack stack = inv.getStackInSlot(i);
            if (stack != null && stack.getItem() == target.getItem() && stack.getItemDamage() == target.getItemDamage()) {
                if (stack.stackSize <= remainToConsume) {
                    remainToConsume -= stack.stackSize;
                    inv.setInventorySlotContents(i, null);
                } else {
                    stack.stackSize -= remainToConsume;
                    remainToConsume = 0;
                }
            }
        }

        player.inventoryContainer.detectAndSendChanges();
        return true;
    }

    public static ItemStack getBlankPattern() {
        return new ItemStack(GameRegistry.findItem("appliedenergistics2", "item.ItemMultiMaterial"), 1, 52);
    }
}
