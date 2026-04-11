package com.github.nhaeutilities.modules.patterngenerator.routing;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.nhaeutilities.modules.patterngenerator.item.PatternIndexLocator;
import com.github.nhaeutilities.modules.patterngenerator.storage.PatternStagingStorage;
import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;

import appeng.api.networking.IGridNode;

public final class PatternRoutingDeliveryService {

    private PatternRoutingDeliveryService() {}

    public static DeliveryResult decorateAndDeliver(EntityPlayer player, IGridNode node, ItemStack pattern,
        PendingRecipeTransferContext.PendingTransfer transfer) {
        if (player == null || pattern == null || transfer == null) {
            return DeliveryResult.NO_ACTION;
        }

        PatternRoutingNbt.writeRoutingData(pattern, buildRoutingMetadata(pattern, transfer));

        PatternRouterService.RouteResult routeResult = PatternRouterService.tryRoute(pattern, node);
        if (routeResult.isRouted()) {
            send(player, EnumChatFormatting.GREEN, "nhaeutilities.msg.pattern.route_success");
            return DeliveryResult.ROUTED;
        }

        String warningMessageKey = warningMessageKeyFor(routeResult.status);
        if (!warningMessageKey.isEmpty()) {
            send(player, EnumChatFormatting.GOLD, warningMessageKey);
        }

        if (PatternIndexLocator.hasPatternIndex(player)
            && PatternStagingStorage.append(player.getUniqueID(), pattern.copy(), System.currentTimeMillis())) {
            send(player, EnumChatFormatting.YELLOW, "nhaeutilities.msg.pattern.staged_unmatched");
            return DeliveryResult.STAGED;
        }

        ItemStack toGive = pattern.copy();
        if (player.inventory.addItemStackToInventory(toGive)) {
            player.inventoryContainer.detectAndSendChanges();
            send(player, EnumChatFormatting.GRAY, "nhaeutilities.msg.pattern.routed_fallback_inventory");
            return DeliveryResult.MOVED_TO_INVENTORY;
        }

        player.entityDropItem(toGive, 0.0F);
        send(player, EnumChatFormatting.RED, "nhaeutilities.msg.pattern.routed_fallback_drop");
        return DeliveryResult.DROPPED;
    }

    private static void send(EntityPlayer player, EnumChatFormatting color, String key, Object... args) {
        player.addChatMessage(new ChatComponentText(color + I18nUtil.tr(key, args)));
    }

    static PatternRoutingNbt.RoutingMetadata buildRoutingMetadata(ItemStack pattern,
        PendingRecipeTransferContext.PendingTransfer transfer) {
        String circuitKey = PatternRoutingNbt.inferCircuitKeyFromEncodedPattern(pattern);
        String manualItemsKey = "";
        return new PatternRoutingNbt.RoutingMetadata(
            PatternRoutingKeys.CURRENT_VERSION,
            transfer.recipeId,
            "",
            circuitKey,
            manualItemsKey,
            transfer.source,
            false,
            transfer.overlayIdentifier);
    }

    static String warningMessageKeyFor(PatternRouterService.RouteStatus status) {
        return status == PatternRouterService.RouteStatus.TARGET_FULL ? "nhaeutilities.msg.pattern.route_target_full"
            : "";
    }

    public enum DeliveryResult {
        NO_ACTION,
        ROUTED,
        STAGED,
        MOVED_TO_INVENTORY,
        DROPPED
    }
}
