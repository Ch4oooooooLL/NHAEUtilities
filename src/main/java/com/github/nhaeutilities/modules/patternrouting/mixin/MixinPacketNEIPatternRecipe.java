package com.github.nhaeutilities.modules.patternrouting.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.nhaeutilities.modules.patternrouting.core.PacketRecipeTransferMetadataAccess;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingLog;

import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;

@Pseudo
@Mixin(targets = "com.github.vfyjxf.nee.network.packet.PacketNEIPatternRecipe", remap = false)
public abstract class MixinPacketNEIPatternRecipe implements PacketRecipeTransferMetadataAccess {

    @Unique
    private String nhaeutilities$recipeId = "";

    @Unique
    private String nhaeutilities$overlayIdentifier = "";

    @Unique
    private String nhaeutilities$programmingCircuit = "";

    @Unique
    private String nhaeutilities$nonConsumables = "[]";

    @Unique
    private String nhaeutilities$recipeSnapshot = "{}";

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
        buf.writeBoolean(!nhaeutilities$programmingCircuit.isEmpty());
        if (!nhaeutilities$programmingCircuit.isEmpty()) {
            ByteBufUtils.writeUTF8String(buf, nhaeutilities$programmingCircuit);
        }
        buf.writeBoolean(!nhaeutilities$nonConsumables.isEmpty());
        if (!nhaeutilities$nonConsumables.isEmpty()) {
            ByteBufUtils.writeUTF8String(buf, nhaeutilities$nonConsumables);
        }
        buf.writeBoolean(!nhaeutilities$recipeSnapshot.isEmpty());
        if (!nhaeutilities$recipeSnapshot.isEmpty()) {
            ByteBufUtils.writeUTF8String(buf, nhaeutilities$recipeSnapshot);
        }
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting] NEE packet toBytes recipeId=%s overlay=%s circuit=%s nc=%s snapshotSize=%s",
            nhaeutilities$recipeId,
            nhaeutilities$overlayIdentifier,
            nhaeutilities$programmingCircuit,
            nhaeutilities$nonConsumables,
            nhaeutilities$recipeSnapshot.length());
    }

    @Inject(method = "fromBytes", at = @At("TAIL"), remap = false)
    private void nhaeutilities$readTransferMetadata(ByteBuf buf, CallbackInfo ci) {
        if (buf.readableBytes() <= 0) {
            cpw.mods.fml.common.FMLLog
                .info("[NHAEUtilities][patternrouting] NEE packet fromBytes no extra metadata bytes");
            return;
        }

        if (buf.readBoolean()) {
            nhaeutilities$recipeId = ByteBufUtils.readUTF8String(buf);
        }
        if (buf.readableBytes() <= 0) {
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting] NEE packet fromBytes recipeId=%s overlay=%s remaining=0",
                nhaeutilities$recipeId,
                nhaeutilities$overlayIdentifier);
            return;
        }
        if (buf.readBoolean()) {
            nhaeutilities$overlayIdentifier = ByteBufUtils.readUTF8String(buf);
        }
        if (buf.readableBytes() > 0 && buf.readBoolean()) {
            nhaeutilities$programmingCircuit = ByteBufUtils.readUTF8String(buf);
        }
        if (buf.readableBytes() > 0 && buf.readBoolean()) {
            nhaeutilities$nonConsumables = ByteBufUtils.readUTF8String(buf);
        }
        if (buf.readableBytes() > 0 && buf.readBoolean()) {
            nhaeutilities$recipeSnapshot = ByteBufUtils.readUTF8String(buf);
        }
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting] NEE packet fromBytes recipeId=%s overlay=%s circuit=%s nc=%s remaining=%s",
            nhaeutilities$recipeId,
            nhaeutilities$overlayIdentifier,
            nhaeutilities$programmingCircuit,
            nhaeutilities$nonConsumables,
            buf.readableBytes());
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

    @Override
    public String nhaeutilities$getProgrammingCircuit() {
        return nhaeutilities$programmingCircuit;
    }

    @Override
    public void nhaeutilities$setProgrammingCircuit(String programmingCircuit) {
        nhaeutilities$programmingCircuit = programmingCircuit != null ? programmingCircuit : "";
    }

    @Override
    public String nhaeutilities$getNonConsumables() {
        return nhaeutilities$nonConsumables;
    }

    @Override
    public void nhaeutilities$setNonConsumables(String nonConsumables) {
        nhaeutilities$nonConsumables = nonConsumables != null ? nonConsumables : "[]";
    }

    @Override
    public String nhaeutilities$getRecipeSnapshot() {
        return nhaeutilities$recipeSnapshot;
    }

    @Override
    public void nhaeutilities$setRecipeSnapshot(String recipeSnapshot) {
        nhaeutilities$recipeSnapshot = recipeSnapshot != null ? recipeSnapshot : "{}";
    }
}
