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

        PendingRecipeTransferContext.PendingTransfer transfer = PendingRecipeTransferContext
            .consume(player.getUniqueID(), System.currentTimeMillis());
        if (transfer == null) {
            return;
        }

        ItemStack output = this.patternSlotOUT.getStack();
        if (output == null) {
            return;
        }

        PatternRoutingFallbackService.DeliveryResult result = PatternRoutingFallbackService
            .decorateAndDeliver(player, ((IContainerCraftingPacket) (Object) this).getNetworkNode(), output, transfer);
        if (result != PatternRoutingFallbackService.DeliveryResult.NO_ACTION) {
            this.patternSlotOUT.putStack(null);
            ((ContainerPatternTermEx) (Object) this).detectAndSendChanges();
        }
    }
}
