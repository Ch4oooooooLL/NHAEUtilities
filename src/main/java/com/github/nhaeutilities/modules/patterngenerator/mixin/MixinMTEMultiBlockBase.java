package com.github.nhaeutilities.modules.patterngenerator.mixin;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.nhaeutilities.modules.patterngenerator.routing.HatchAssignmentService;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.common.tileentities.machines.IDualInputHatch;

@Mixin(value = MTEMultiBlockBase.class, remap = false)
public abstract class MixinMTEMultiBlockBase {

    @Shadow
    public ArrayList<IDualInputHatch> mDualInputHatches;

    @Shadow
    protected boolean mMachine;

    @Inject(method = "clearHatches", at = @At("HEAD"), remap = false)
    private void nhaeutilities$clearHatchAssignments(CallbackInfo ci) {
        HatchAssignmentService.clearAssignments(this.mDualInputHatches);
    }

    @Inject(method = "checkStructure(ZLgregtech/api/interfaces/tileentity/IGregTechTileEntity;)Z", at = @At("RETURN"), remap = false)
    private void nhaeutilities$refreshHatchAssignments(boolean aForceReset, IGregTechTileEntity aBaseMetaTileEntity,
        CallbackInfoReturnable<Boolean> cir) {
        if (Boolean.TRUE.equals(cir.getReturnValue()) && this.mMachine) {
            HatchAssignmentService.refreshAssignments((MTEMultiBlockBase) (Object) this);
        } else {
            HatchAssignmentService.clearAssignments(this.mDualInputHatches);
        }
    }
}
