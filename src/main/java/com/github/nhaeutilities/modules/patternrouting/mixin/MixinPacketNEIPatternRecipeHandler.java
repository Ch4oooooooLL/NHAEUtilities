package com.github.nhaeutilities.modules.patternrouting.mixin;

import net.minecraft.entity.player.EntityPlayerMP;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;
import com.github.nhaeutilities.modules.patternrouting.core.PacketRecipeTransferMetadataAccess;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingKeys;
import com.github.nhaeutilities.modules.patternrouting.core.PendingRecipeTransferContext;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

@Pseudo
@Mixin(targets = "com.github.vfyjxf.nee.network.packet.PacketNEIPatternRecipe$Handler", remap = false)
public abstract class MixinPacketNEIPatternRecipeHandler {

    @Inject(method = "onMessage", at = @At("HEAD"), remap = false)
    private void nhaeutilities$storePendingTransfer(Object message, MessageContext ctx,
        CallbackInfoReturnable<IMessage> cir) {
        if (!PatternRoutingRuntime.isEnabled() || !(message instanceof PacketRecipeTransferMetadataAccess)
            || ctx == null
            || ctx.getServerHandler() == null) {
            return;
        }

        PacketRecipeTransferMetadataAccess accessor = (PacketRecipeTransferMetadataAccess) message;
        if (accessor.nhaeutilities$getRecipeId()
            .isEmpty()) {
            return;
        }

        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        PendingRecipeTransferContext.store(
            player.getUniqueID(),
            accessor.nhaeutilities$getRecipeId(),
            accessor.nhaeutilities$getOverlayIdentifier(),
            PatternRoutingKeys.SOURCE_NEI,
            System.currentTimeMillis());
    }
}
