package com.github.nhaeutilities.modules.patternrouting.mixin;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;
import com.github.nhaeutilities.modules.patternrouting.core.HatchAssignmentService;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.tileentities.machines.IDualInputHatch;

@Pseudo
@Mixin(targets = "gregtech.api.metatileentity.implementations.MTEMultiBlockBase", remap = false)
public abstract class MixinMTEMultiBlockBase {

    @Shadow
    public ArrayList<IDualInputHatch> mDualInputHatches;

    @Shadow
    protected boolean mMachine;

    @Inject(method = "clearHatches", at = @At("HEAD"), remap = false)
    private void nhaeutilities$clearHatchAssignments(CallbackInfo ci) {
        if (!PatternRoutingRuntime.isEnabled()) {
            return;
        }
        HatchAssignmentService.clearAssignments(this.mDualInputHatches);
    }

    @Inject(
        method = "checkStructure(ZLgregtech/api/interfaces/tileentity/IGregTechTileEntity;)Z",
        at = @At("RETURN"),
        remap = false)
    private void nhaeutilities$refreshHatchAssignments(boolean aForceReset, IGregTechTileEntity aBaseMetaTileEntity,
        CallbackInfoReturnable<Boolean> cir) {
        if (!PatternRoutingRuntime.isEnabled()) {
            return;
        }
        if (Boolean.TRUE.equals(cir.getReturnValue()) && this.mMachine) {
            HatchAssignmentService.refreshAssignments(this);
        } else {
            HatchAssignmentService.clearAssignments(this.mDualInputHatches);
        }
    }
}
