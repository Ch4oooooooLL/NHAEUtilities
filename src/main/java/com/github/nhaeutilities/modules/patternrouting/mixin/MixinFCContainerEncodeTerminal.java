package com.github.nhaeutilities.modules.patternrouting.mixin;

import net.minecraft.entity.player.EntityPlayer;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingKeys;
import com.github.nhaeutilities.modules.patternrouting.core.PatternTerminalRoutingSupport;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.IContainerCraftingPacket;

@Pseudo
@Mixin(targets = "com.glodblock.github.client.gui.container.base.FCContainerEncodeTerminal", remap = false)
public abstract class MixinFCContainerEncodeTerminal {

    @Shadow
    @Final
    protected SlotRestrictedInput patternSlotOUT;

    @Inject(method = "encode", at = @At("TAIL"), remap = false)
    private void nhaeutilities$handlePatternRoutingAfterEncode(CallbackInfo ci) {
        EntityPlayer player = ((AEBaseContainer) (Object) this).getInventoryPlayer().player;
        PatternTerminalRoutingSupport.handleEncode(
            "AE2FC",
            player,
            ((IContainerCraftingPacket) (Object) this).getNetworkNode(),
            this.patternSlotOUT,
            PatternRoutingKeys.SOURCE_AE2FC,
            "",
            "",
            new Runnable() {

                @Override
                public void run() {
                    ((AEBaseContainer) (Object) MixinFCContainerEncodeTerminal.this).detectAndSendChanges();
                }
            });
    }
}
