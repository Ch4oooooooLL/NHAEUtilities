package com.github.nhaeutilities.modules.patternrouting.core;

import net.minecraft.item.ItemStack;

import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;

import appeng.api.networking.IGridNode;

public final class PatternRoutingDeliveryService {

    private PatternRoutingDeliveryService() {}

    public static PatternRouterService.RouteResult decorateAndRoute(IGridNode node, ItemStack pattern,
        PendingRecipeTransferContext.PendingTransfer transfer) {
        if (!PatternRoutingRuntime.isEnabled() || pattern == null || transfer == null) {
            return PatternRouterService.RouteResult.noMetadata();
        }

        PatternRoutingNbt.writeRoutingData(pattern, buildRoutingMetadata(pattern, transfer));
        return PatternRouterService.tryRoute(pattern, node);
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

    public static String warningMessageKeyFor(PatternRouterService.RouteStatus status) {
        return status == PatternRouterService.RouteStatus.TARGET_FULL ? "nhaeutilities.msg.pattern.route_target_full"
            : "";
    }

}
