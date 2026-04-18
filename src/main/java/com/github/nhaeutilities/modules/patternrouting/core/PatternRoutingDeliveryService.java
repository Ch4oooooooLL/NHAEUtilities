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

        PatternRoutingNbt.RoutingMetadata metadata = buildRoutingMetadata(pattern, transfer);
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][nbt] decorate start item=%s node=%s recipeId=%s overlay=%s circuit=%s manual=%s source=%s nc=%s",
            PatternRoutingNbt.itemSignature(pattern),
            node != null,
            metadata.recipeId,
            metadata.overlayIdentifier,
            metadata.circuitKey,
            metadata.manualItemsKey,
            metadata.source,
            metadata.nonConsumables);
        PatternRoutingNbt.writeRoutingData(pattern, metadata);
        PatternRouterService.RouteResult result = PatternRouterService.tryRoute(pattern, node);
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][nbt] decorate route status=%s target=%s item=%s",
            result.status,
            result.target != null ? result.target.getClass()
                .getName() : "null",
            PatternRoutingNbt.itemSignature(pattern));
        return result;
    }

    static PatternRoutingNbt.RoutingMetadata buildRoutingMetadata(ItemStack pattern,
        PendingRecipeTransferContext.PendingTransfer transfer) {
        boolean usedTransferCircuit = !transfer.programmingCircuit.isEmpty();
        String circuitKey = usedTransferCircuit ? transfer.programmingCircuit
            : PatternRoutingNbt.inferCircuitKeyFromEncodedPattern(pattern);
        String manualItemsKey = PatternRoutingNbt.manualItemsKeyFromJson(transfer.nonConsumables);
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][nbt] build metadata recipeCategory=%s recipeId=%s circuitSource=%s manualSource=%s circuit=%s manual=%s snapshotSize=%s",
            transfer.recipeCategory,
            transfer.recipeId,
            usedTransferCircuit ? "transfer" : "encodedPatternFallback",
            transfer.nonConsumables != null && !transfer.nonConsumables.isEmpty()
                && !"[]".equals(transfer.nonConsumables) ? "transferNc" : "empty",
            circuitKey,
            manualItemsKey,
            transfer.recipeSnapshot.length());
        return new PatternRoutingNbt.RoutingMetadata(
            PatternRoutingKeys.CURRENT_VERSION,
            transfer.recipeCategory,
            transfer.recipeId,
            "",
            circuitKey,
            manualItemsKey,
            transfer.source,
            false,
            transfer.programmingCircuit,
            transfer.nonConsumables,
            transfer.recipeSnapshot);
    }

    public static String warningMessageKeyFor(PatternRouterService.RouteStatus status) {
        return status == PatternRouterService.RouteStatus.TARGET_FULL ? "nhaeutilities.msg.pattern.route_target_full"
            : "";
    }

}
