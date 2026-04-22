package com.github.nhaeutilities.modules.patternrouting.mixin;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;
import com.github.nhaeutilities.modules.patternrouting.core.HatchAssignmentService;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingLog;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;

@Pseudo
@Mixin(targets = "com.science.gtnl.common.machine.multiMachineBase.MultiMachineBase", remap = false)
public abstract class MixinGTNLMultiMachineBaseAssignmentRefresh {

    @Inject(
        method = "checkStructure(ZLgregtech/api/interfaces/tileentity/IGregTechTileEntity;)Z",
        at = @At("RETURN"),
        remap = false)
    private void nhaeutilities$refreshAssignmentsAfterGTNLCheckStructure(boolean aForceReset,
        IGregTechTileEntity aBaseMetaTileEntity, CallbackInfoReturnable<Boolean> cir) {
        if (!PatternRoutingRuntime.isEnabled()) {
            return;
        }
        MTEMultiBlockBase base = nhaeutilities$asBase();
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][assignment] multiblock refresh trigger source=gtnl-checkStructure result=%s forceReset=%s mMachine=%s controller=%s dualInputCount=%s",
            cir.getReturnValue(),
            aForceReset,
            base.mMachine,
            this.getClass()
                .getName(),
            base.mDualInputHatches != null ? base.mDualInputHatches.size() : 0);
        if (Boolean.TRUE.equals(cir.getReturnValue()) && base.mMachine) {
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
            HatchAssignmentService.clearAssignments(base.mDualInputHatches);
        }
    }

    @Inject(method = "onMachineModeSwitchClick()V", at = @At("TAIL"), remap = false)
    private void nhaeutilities$refreshAssignmentsAfterGTNLModeButton(CallbackInfo ci) {
        MTEMultiBlockBase base = nhaeutilities$asBase();
        if (!PatternRoutingRuntime.isEnabled() || !base.mMachine) {
            return;
        }
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][assignment] controller refresh start source=gtnl-mode-button controller=%s dualInputCount=%s",
            this.getClass()
                .getName(),
            base.mDualInputHatches != null ? base.mDualInputHatches.size() : 0);
        HatchAssignmentService.refreshAssignments(this);
    }

    @Inject(
        method = "onScrewdriverRightClick(Lnet/minecraftforge/common/util/ForgeDirection;Lnet/minecraft/entity/player/EntityPlayer;FFFLnet/minecraft/item/ItemStack;)V",
        at = @At("TAIL"),
        remap = false)
    private void nhaeutilities$refreshAssignmentsAfterGTNLScrewdriver(ForgeDirection side, EntityPlayer aPlayer,
        float aX, float aY, float aZ, ItemStack aTool, CallbackInfo ci) {
        MTEMultiBlockBase base = nhaeutilities$asBase();
        if (!PatternRoutingRuntime.isEnabled() || !base.mMachine) {
            return;
        }
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][assignment] controller refresh start source=gtnl-mode-screwdriver controller=%s dualInputCount=%s",
            this.getClass()
                .getName(),
            base.mDualInputHatches != null ? base.mDualInputHatches.size() : 0);
        HatchAssignmentService.refreshAssignments(this);
    }

    private MTEMultiBlockBase nhaeutilities$asBase() {
        return (MTEMultiBlockBase) (Object) this;
    }
}
