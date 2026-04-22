package com.github.nhaeutilities.modules.patternrouting.core;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.github.nhaeutilities.modules.patterngenerator.PatternRoutingFallbackService;
import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;

import appeng.api.networking.IGridNode;
import appeng.container.slot.SlotRestrictedInput;

public final class PatternTerminalRoutingSupport {

    private PatternTerminalRoutingSupport() {}

    public static PatternRoutingFallbackService.DeliveryResult handleEncode(String terminalKind, EntityPlayer player,
        IGridNode node, SlotRestrictedInput patternSlotOUT, String source, String recipeId, String overlayIdentifier,
        Runnable syncAction) {
        if (!PatternRoutingRuntime.isEnabled() || player == null
            || player.worldObj == null
            || player.worldObj.isRemote
            || patternSlotOUT == null) {
            return PatternRoutingFallbackService.DeliveryResult.NO_ACTION;
        }

        long now = System.currentTimeMillis();
        PendingRecipeTransferContext.PendingTransfer transfer = PendingRecipeTransferContext
            .peek(player.getUniqueID(), now);
        if (transfer == null) {
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] %s encode no pending transfer player=%s output=%s",
                terminalKind,
                player.getCommandSenderName(),
                patternSlotOUT.getStack());
            return PatternRoutingFallbackService.DeliveryResult.NO_ACTION;
        }

        ItemStack output = patternSlotOUT.getStack();
        if (output == null) {
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] %s encode defers pending player=%s recipeId=%s overlay=%s output=null",
                terminalKind,
                player.getCommandSenderName(),
                transfer.recipeId,
                transfer.overlayIdentifier);
            return PatternRoutingFallbackService.DeliveryResult.NO_ACTION;
        }

        transfer = PendingRecipeTransferContext.consume(player.getUniqueID(), source, recipeId, overlayIdentifier, now);
        if (transfer == null) {
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] %s encode lost pending before consume player=%s output=%s",
                terminalKind,
                player.getCommandSenderName(),
                output);
            return PatternRoutingFallbackService.DeliveryResult.NO_ACTION;
        }

        PatternRoutingLog.info(
            "[NHAEUtilities][patternrouting] %s encode consume pending player=%s recipeId=%s overlay=%s source=%s circuit=%s nc=%s snapshotSize=%s output=%s",
            terminalKind,
            player.getCommandSenderName(),
            transfer.recipeId,
            transfer.overlayIdentifier,
            transfer.source,
            transfer.programmingCircuit,
            transfer.nonConsumables,
            transfer.recipeSnapshot.length(),
            output);

        PatternRoutingFallbackService.DeliveryResult result = PatternRoutingFallbackService
            .decorateAndDeliver(player, node, output, transfer);
        if (result != PatternRoutingFallbackService.DeliveryResult.NO_ACTION) {
            patternSlotOUT.putStack(null);
            if (syncAction != null) {
                syncAction.run();
            }
        }
        return result;
    }
}
