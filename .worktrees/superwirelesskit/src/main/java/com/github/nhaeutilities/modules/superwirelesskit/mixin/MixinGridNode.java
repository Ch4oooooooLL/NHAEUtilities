package com.github.nhaeutilities.modules.superwirelesskit.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.nhaeutilities.modules.superwirelesskit.runtime.GridNodeAccess;
import com.github.nhaeutilities.modules.superwirelesskit.runtime.SuperWirelessServices;

import appeng.me.GridNode;

@Mixin(GridNode.class)
public abstract class MixinGridNode implements GridNodeAccess {

    @Accessor(value = "playerID", remap = false)
    @Override
    public abstract int nhaeutilities$getPlayerIdRaw();

    @Accessor(value = "playerID", remap = false)
    @Override
    public abstract void nhaeutilities$setPlayerIdRaw(int playerId);

    @Inject(method = "updateState", at = @At("TAIL"), remap = false)
    private void nhaeutilities$afterUpdateState(CallbackInfo ci) {
        SuperWirelessServices.runtimeManager()
            .refreshNode((GridNode) (Object) this);
    }

    @Inject(method = "destroy", at = @At("HEAD"), remap = false)
    private void nhaeutilities$beforeDestroy(CallbackInfo ci) {
        SuperWirelessServices.runtimeManager()
            .onNodeDestroyed((GridNode) (Object) this);
    }
}
