package com.github.nhaeutilities.modules.patternrouting.mixin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.nhaeutilities.modules.patterngenerator.PatternRoutingFallbackService;
import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingLog;
import com.github.nhaeutilities.modules.patternrouting.core.PendingRecipeTransferContext;

import appeng.container.implementations.ContainerPatternTermEx;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.IContainerCraftingPacket;

@Mixin(value = ContainerPatternTermEx.class, remap = false)
public abstract class MixinContainerPatternTermEx {

    @Shadow
    @Final
    private SlotRestrictedInput patternSlotOUT;

    @Inject(method = "encode", at = @At("TAIL"), remap = false)
    private void nhaeutilities$handlePatternRoutingAfterEncode(CallbackInfo ci) {
        EntityPlayer player = ((ContainerPatternTermEx) (Object) this).getPlayerInv().player;
        if (!PatternRoutingRuntime.isEnabled() || player == null
            || player.worldObj == null
            || player.worldObj.isRemote) {
            return;
        }

        long now = System.currentTimeMillis();
        PendingRecipeTransferContext.PendingTransfer transfer = PendingRecipeTransferContext
            .peek(player.getUniqueID(), now);
        if (transfer == null) {
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] AE2Ex encode no pending transfer player=%s output=%s",
                player.getCommandSenderName(),
                this.patternSlotOUT.getStack());
            return;
        }

        ItemStack output = this.patternSlotOUT.getStack();
        if (output == null) {
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] AE2Ex encode defers pending player=%s recipeId=%s overlay=%s output=null",
                player.getCommandSenderName(),
                transfer.recipeId,
                transfer.overlayIdentifier);
            return;
        }

        transfer = PendingRecipeTransferContext.consume(player.getUniqueID(), now);
        if (transfer == null) {
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] AE2Ex encode lost pending before consume player=%s output=%s",
                player.getCommandSenderName(),
                output);
            return;
        }
        PatternRoutingLog.info(
            "[NHAEUtilities][patternrouting] AE2Ex encode consume pending player=%s recipeId=%s overlay=%s source=%s circuit=%s nc=%s snapshotSize=%s output=%s",
            player.getCommandSenderName(),
            transfer.recipeId,
            transfer.overlayIdentifier,
            transfer.source,
            transfer.programmingCircuit,
            transfer.nonConsumables,
            transfer.recipeSnapshot.length(),
            output);

        PatternRoutingFallbackService.DeliveryResult result = PatternRoutingFallbackService
            .decorateAndDeliver(player, ((IContainerCraftingPacket) (Object) this).getNetworkNode(), output, transfer);
        if (result != PatternRoutingFallbackService.DeliveryResult.NO_ACTION) {
            this.patternSlotOUT.putStack(null);
            ((ContainerPatternTermEx) (Object) this).detectAndSendChanges();
        }
    }
}
