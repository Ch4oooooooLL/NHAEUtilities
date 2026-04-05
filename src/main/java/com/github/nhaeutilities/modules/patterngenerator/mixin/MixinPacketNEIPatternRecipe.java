package com.github.nhaeutilities.modules.patterngenerator.mixin;

import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.nhaeutilities.modules.patterngenerator.routing.PacketRecipeTransferMetadataAccess;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

@Pseudo
@Mixin(targets = "com.github.vfyjxf.nee.network.packet.PacketNEIPatternRecipe", remap = false)
public abstract class MixinPacketNEIPatternRecipe implements PacketRecipeTransferMetadataAccess {

    @Unique
    private String nhaeutilities$recipeId = "";

    @Unique
    private String nhaeutilities$overlayIdentifier = "";

    @Inject(method = "toBytes", at = @At("TAIL"), remap = false)
    private void nhaeutilities$writeTransferMetadata(ByteBuf buf, CallbackInfo ci) {
        buf.writeBoolean(!nhaeutilities$recipeId.isEmpty());
        if (!nhaeutilities$recipeId.isEmpty()) {
            ByteBufUtils.writeUTF8String(buf, nhaeutilities$recipeId);
        }
        buf.writeBoolean(!nhaeutilities$overlayIdentifier.isEmpty());
        if (!nhaeutilities$overlayIdentifier.isEmpty()) {
            ByteBufUtils.writeUTF8String(buf, nhaeutilities$overlayIdentifier);
        }
    }

    @Inject(method = "fromBytes", at = @At("TAIL"), remap = false)
    private void nhaeutilities$readTransferMetadata(ByteBuf buf, CallbackInfo ci) {
        if (buf.readableBytes() <= 0) {
            return;
        }

        if (buf.readBoolean()) {
            nhaeutilities$recipeId = ByteBufUtils.readUTF8String(buf);
        }
        if (buf.readableBytes() <= 0) {
            return;
        }
        if (buf.readBoolean()) {
            nhaeutilities$overlayIdentifier = ByteBufUtils.readUTF8String(buf);
        }
    }

    @Override
    public String nhaeutilities$getRecipeId() {
        return nhaeutilities$recipeId;
    }

    @Override
    public void nhaeutilities$setRecipeId(String recipeId) {
        nhaeutilities$recipeId = recipeId != null ? recipeId : "";
    }

    @Override
    public String nhaeutilities$getOverlayIdentifier() {
        return nhaeutilities$overlayIdentifier;
    }

    @Override
    public void nhaeutilities$setOverlayIdentifier(String overlayIdentifier) {
        nhaeutilities$overlayIdentifier = overlayIdentifier != null ? overlayIdentifier : "";
    }
}
