package com.github.nhaeutilities.modules.patternrouting.mixin;

import java.util.ArrayList;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;
import com.github.nhaeutilities.modules.patternrouting.core.HatchAssignmentService;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingLog;

import gregtech.common.tileentities.machines.IDualInputHatch;

@Pseudo
@Mixin(targets = "gregtech.api.metatileentity.implementations.MTEMultiBlockBase", remap = false)
public abstract class MixinMTEMultiBlockBaseModeRefresh {

    @Shadow
    protected boolean mMachine;

    @Shadow
    public ArrayList<IDualInputHatch> mDualInputHatches;

    @Inject(method = "setMachineMode(I)V", at = @At("TAIL"), remap = false)
    private void nhaeutilities$refreshAssignmentsAfterModeChange(int index, CallbackInfo ci) {
        if (!PatternRoutingRuntime.isEnabled()) {
            return;
        }
        if (!this.mMachine) {
            return;
        }
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][assignment] controller refresh start source=gt-setMachineMode controller=%s mode=%s dualInputCount=%s",
            this.getClass()
                .getName(),
            index,
            this.mDualInputHatches != null ? this.mDualInputHatches.size() : 0);
        HatchAssignmentService.refreshAssignments(this);
    }
}
