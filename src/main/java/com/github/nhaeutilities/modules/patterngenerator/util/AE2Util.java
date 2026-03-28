package com.github.nhaeutilities.modules.patterngenerator.util;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;

public class AE2Util {

    public static boolean tryWirelessConsume(EntityPlayerMP player, int requiredCount, ItemStack blankPattern) {
        ItemStack heldItem = player.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof IWirelessTermHandler)) {
            return false;
        }

        try {
            return internalWirelessConsume(player, requiredCount, blankPattern, heldItem);
        } catch (Throwable e) {
            return false;
        }
    }

    private static boolean internalWirelessConsume(EntityPlayerMP player, int requiredCount, ItemStack blankPattern,
        ItemStack heldItem) {
        IWirelessTermHandler handler = (IWirelessTermHandler) heldItem.getItem();
        String key = handler.getEncryptionKey(heldItem);
        if (key == null || key.isEmpty()) {
            return false;
        }

        long serial;
        try {
            serial = Long.parseLong(key);
        } catch (NumberFormatException e) {
            return false;
        }

        Object obj = AEApi.instance().registries().locatable().getLocatableBy(serial);
        if (obj instanceof IActionHost) {
            IGrid grid = ((IActionHost) obj).getActionableNode().getGrid();
            if (grid != null) {
                IStorageGrid storage = grid.getCache(IStorageGrid.class);
                IMEMonitor<IAEItemStack> inventory = storage.getItemInventory();
                IAEItemStack required = AEApi.instance().storage().createItemStack(blankPattern).setStackSize(requiredCount);

                IAEItemStack available =
                    inventory.extractItems(required, Actionable.SIMULATE, new PlayerSource(player, null));
                if (available != null && available.getStackSize() >= requiredCount) {
                    inventory.extractItems(required, Actionable.MODULATE, new PlayerSource(player, null));
                    player.addChatMessage(new ChatComponentText(
                        EnumChatFormatting.AQUA
                            + "[NHAEUtilities] \u5df2\u4ece\u7ed1\u5b9a\u7684 ME \u7f51\u7edc\u4e2d\u65e0\u7ebf\u63d0\u53d6\u7a7a\u767d\u6837\u677f\u3002"));
                    return true;
                }
            }
        }

        return false;
    }
}
