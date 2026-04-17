package com.github.nhaeutilities.modules.patterngenerator;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.nhaeutilities.modules.patterngenerator.item.PatternIndexLocator;
import com.github.nhaeutilities.modules.patterngenerator.storage.PatternStagingStorage;
import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRouterService;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingDeliveryService;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingLog;
import com.github.nhaeutilities.modules.patternrouting.core.PendingRecipeTransferContext;

import appeng.api.networking.IGridNode;

public final class PatternRoutingFallbackService {

    private PatternRoutingFallbackService() {}

    public static DeliveryResult decorateAndDeliver(EntityPlayer player, IGridNode node, ItemStack pattern,
        PendingRecipeTransferContext.PendingTransfer transfer) {
        if (player == null || pattern == null || transfer == null) {
            return DeliveryResult.NO_ACTION;
        }

        PatternRouterService.RouteResult routeResult = PatternRoutingDeliveryService
            .decorateAndRoute(node, pattern, transfer);
        PatternRoutingLog.info(
            "[NHAEUtilities][patternrouting] fallback route status=%s player=%s recipeId=%s overlay=%s",
            routeResult.status,
            player.getCommandSenderName(),
            transfer.recipeId,
            transfer.overlayIdentifier);
        if (routeResult.status == PatternRouterService.RouteStatus.NO_METADATA) {
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] fallback no action due to missing metadata player=%s",
                player.getCommandSenderName());
            return DeliveryResult.NO_ACTION;
        }
        if (routeResult.isRouted()) {
            send(player, EnumChatFormatting.GREEN, "nhaeutilities.msg.pattern.route_success");
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] fallback delivered via direct route player=%s",
                player.getCommandSenderName());
            return DeliveryResult.ROUTED;
        }

        String warningMessageKey = PatternRoutingDeliveryService.warningMessageKeyFor(routeResult.status);
        if (!warningMessageKey.isEmpty()) {
            send(player, EnumChatFormatting.GOLD, warningMessageKey);
        }

        if (PatternIndexLocator.hasPatternIndex(player)
            && PatternStagingStorage.append(player.getUniqueID(), pattern.copy(), System.currentTimeMillis())) {
            send(player, EnumChatFormatting.YELLOW, "nhaeutilities.msg.pattern.staged_unmatched");
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] fallback staged pattern player=%s",
                player.getCommandSenderName());
            return DeliveryResult.STAGED;
        }

        ItemStack toGive = pattern.copy();
        if (player.inventory.addItemStackToInventory(toGive)) {
            player.inventoryContainer.detectAndSendChanges();
            send(player, EnumChatFormatting.GRAY, "nhaeutilities.msg.pattern.routed_fallback_inventory");
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] fallback moved to inventory player=%s",
                player.getCommandSenderName());
            return DeliveryResult.MOVED_TO_INVENTORY;
        }

        player.entityDropItem(toGive, 0.0F);
        send(player, EnumChatFormatting.RED, "nhaeutilities.msg.pattern.routed_fallback_drop");
        PatternRoutingLog
            .info("[NHAEUtilities][patternrouting] fallback dropped pattern player=%s", player.getCommandSenderName());
        return DeliveryResult.DROPPED;
    }

    private static void send(EntityPlayer player, EnumChatFormatting color, String key, Object... args) {
        player.addChatMessage(new ChatComponentText(color + I18nUtil.tr(key, args)));
    }

    public enum DeliveryResult {
        NO_ACTION,
        ROUTED,
        STAGED,
        MOVED_TO_INVENTORY,
        DROPPED
    }
}
