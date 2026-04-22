package com.github.nhaeutilities.modules.patternrouting.mixin;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingKeys;
import com.github.nhaeutilities.modules.patternrouting.core.PatternTerminalRoutingSupport;

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
        PatternTerminalRoutingSupport.handleEncode(
            "AE2Ex",
            player,
            ((IContainerCraftingPacket) (Object) this).getNetworkNode(),
            this.patternSlotOUT,
            PatternRoutingKeys.SOURCE_NEI,
            "",
            "",
            new Runnable() {

                @Override
                public void run() {
                    ((ContainerPatternTermEx) (Object) MixinContainerPatternTermEx.this).detectAndSendChanges();
                }
            });
    }
}
