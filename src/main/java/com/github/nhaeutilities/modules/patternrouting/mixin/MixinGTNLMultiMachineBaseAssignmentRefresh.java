package com.github.nhaeutilities.modules.patternrouting.mixin;

import java.util.ArrayList;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;
import com.github.nhaeutilities.modules.patternrouting.core.HatchAssignmentService;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingLog;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.tileentities.machines.IDualInputHatch;

@Pseudo
@Mixin(targets = "com.science.gtnl.common.machine.multiMachineBase.MultiMachineBase", remap = false)
public abstract class MixinGTNLMultiMachineBaseAssignmentRefresh {

    @Shadow
    protected boolean mMachine;

    @Shadow
    public ArrayList<IDualInputHatch> mDualInputHatches;

    @Inject(
        method = "checkStructure(ZLgregtech/api/interfaces/tileentity/IGregTechTileEntity;)Z",
        at = @At("RETURN"),
        remap = false)
    private void nhaeutilities$refreshAssignmentsAfterGTNLCheckStructure(boolean aForceReset,
        IGregTechTileEntity aBaseMetaTileEntity, CallbackInfoReturnable<Boolean> cir) {
        if (!PatternRoutingRuntime.isEnabled()) {
            return;
        }
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][assignment] multiblock refresh trigger source=gtnl-checkStructure result=%s forceReset=%s mMachine=%s controller=%s dualInputCount=%s",
            cir.getReturnValue(),
            aForceReset,
            this.mMachine,
            this.getClass()
                .getName(),
            this.mDualInputHatches != null ? this.mDualInputHatches.size() : 0);
        if (Boolean.TRUE.equals(cir.getReturnValue()) && this.mMachine) {
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][assignment] controller refresh start source=gtnl-checkStructure controller=%s",
                this.getClass()
                    .getName());
            HatchAssignmentService.refreshAssignments(this);
        } else {
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][assignment] controller refresh clear source=gtnl-checkStructure controller=%s",
                this.getClass()
                    .getName());
            HatchAssignmentService.clearAssignments(this.mDualInputHatches);
        }
    }

    @Inject(method = "onMachineModeSwitchClick()V", at = @At("TAIL"), remap = false)
    private void nhaeutilities$refreshAssignmentsAfterGTNLModeButton(CallbackInfo ci) {
        if (!PatternRoutingRuntime.isEnabled() || !this.mMachine) {
            return;
        }
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][assignment] controller refresh start source=gtnl-mode-button controller=%s dualInputCount=%s",
            this.getClass()
                .getName(),
            this.mDualInputHatches != null ? this.mDualInputHatches.size() : 0);
        HatchAssignmentService.refreshAssignments(this);
    }

    @Inject(
        method = "onScrewdriverRightClick(Lnet/minecraftforge/common/util/ForgeDirection;Lnet/minecraft/entity/player/EntityPlayer;FFFLnet/minecraft/item/ItemStack;)V",
        at = @At("TAIL"),
        remap = false)
    private void nhaeutilities$refreshAssignmentsAfterGTNLScrewdriver(ForgeDirection side, EntityPlayer aPlayer,
        float aX, float aY, float aZ, ItemStack aTool, CallbackInfo ci) {
        if (!PatternRoutingRuntime.isEnabled() || !this.mMachine) {
            return;
        }
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][assignment] controller refresh start source=gtnl-mode-screwdriver controller=%s dualInputCount=%s",
            this.getClass()
                .getName(),
            this.mDualInputHatches != null ? this.mDualInputHatches.size() : 0);
        HatchAssignmentService.refreshAssignments(this);
    }
}
