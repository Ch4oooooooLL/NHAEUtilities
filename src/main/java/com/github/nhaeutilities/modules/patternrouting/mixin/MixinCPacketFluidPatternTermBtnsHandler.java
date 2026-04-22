package com.github.nhaeutilities.modules.patternrouting.mixin;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingKeys;
import com.github.nhaeutilities.modules.patternrouting.core.PatternTerminalReflectionSupport;
import com.github.nhaeutilities.modules.patternrouting.core.PatternTerminalRoutingSupport;

import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.IContainerCraftingPacket;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

@Pseudo
@Mixin(targets = "com.glodblock.github.network.CPacketFluidPatternTermBtns$Handler", remap = false)
public abstract class MixinCPacketFluidPatternTermBtnsHandler {

    @Inject(method = "onMessage", at = @At("TAIL"), remap = false)
    private void nhaeutilities$handlePatternRoutingAfterEncode(
        com.glodblock.github.network.CPacketFluidPatternTermBtns message, MessageContext ctx,
        CallbackInfoReturnable<IMessage> cir) {
        if (!PatternRoutingRuntime.isEnabled() || ctx == null || ctx.getServerHandler() == null) {
            return;
        }

        if (!"PatternTerminal.Encode".equals(PatternTerminalReflectionSupport.readButtonMessageName(message))) {
            return;
        }

        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        Container openContainer = player.openContainer;
        if (!(openContainer instanceof IContainerCraftingPacket)) {
            return;
        }

        SlotRestrictedInput patternSlotOUT = PatternTerminalReflectionSupport.readPatternSlotOut(openContainer);
        if (patternSlotOUT == null) {
            return;
        }

        PatternTerminalRoutingSupport.handleEncode(
            "AE2FC button",
            player,
            ((IContainerCraftingPacket) openContainer).getNetworkNode(),
            patternSlotOUT,
            PatternRoutingKeys.SOURCE_AE2FC,
            "",
            "",
            new Runnable() {

                @Override
                public void run() {
                    openContainer.detectAndSendChanges();
                }
            });
    }
}
