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
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingLog;
import com.github.nhaeutilities.modules.patternrouting.core.PendingRecipeTransferContext;

import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;

@Pseudo
@Mixin(targets = "com.github.vfyjxf.nee.network.packet.PacketNEIPatternRecipe$Handler", remap = false)
public abstract class MixinPacketNEIPatternRecipeHandler {

    @Inject(method = "onMessage", at = @At("HEAD"), remap = false)
    private void nhaeutilities$storePendingTransfer(com.github.vfyjxf.nee.network.packet.PacketNEIPatternRecipe message,
        MessageContext ctx, CallbackInfoReturnable<IMessage> cir) {
        if (!PatternRoutingRuntime.isEnabled() || !(message instanceof PacketRecipeTransferMetadataAccess)
            || ctx == null
            || ctx.getServerHandler() == null) {
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] NEE handler skip enabled=%s messageAccess=%s ctx=%s serverHandler=%s messageClass=%s",
                PatternRoutingRuntime.isEnabled(),
                message instanceof PacketRecipeTransferMetadataAccess,
                ctx != null,
                ctx != null && ctx.getServerHandler() != null,
                message != null ? message.getClass()
                    .getName() : "null");
            return;
        }

        PacketRecipeTransferMetadataAccess accessor = (PacketRecipeTransferMetadataAccess) message;
        if (accessor.nhaeutilities$getOverlayIdentifier()
            .isEmpty()) {
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] NEE handler saw empty recipeCategory recipeId=%s messageClass=%s",
                accessor.nhaeutilities$getRecipeId(),
                message.getClass()
                    .getName());
            return;
        }

        EntityPlayerMP player = ctx.getServerHandler().playerEntity;
        PendingRecipeTransferContext.store(
            player.getUniqueID(),
            accessor.nhaeutilities$getRecipeId(),
            accessor.nhaeutilities$getOverlayIdentifier(),
            accessor.nhaeutilities$getProgrammingCircuit(),
            accessor.nhaeutilities$getNonConsumables(),
            accessor.nhaeutilities$getRecipeSnapshot(),
            PatternRoutingKeys.SOURCE_NEI,
            System.currentTimeMillis());
        PatternRoutingLog.info(
            "[NHAEUtilities][patternrouting] store pending transfer source=%s player=%s recipeId=%s recipeCategory=%s circuit=%s nc=%s",
            PatternRoutingKeys.SOURCE_NEI,
            player.getCommandSenderName(),
            accessor.nhaeutilities$getRecipeId(),
            accessor.nhaeutilities$getOverlayIdentifier(),
            accessor.nhaeutilities$getProgrammingCircuit(),
            accessor.nhaeutilities$getNonConsumables());
    }
}
