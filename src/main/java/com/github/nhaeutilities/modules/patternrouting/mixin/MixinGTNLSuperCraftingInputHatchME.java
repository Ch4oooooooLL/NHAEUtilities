package com.github.nhaeutilities.modules.patternrouting.mixin;

import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.nhaeutilities.accessor.patternrouting.HatchAssignmentHolder;
import com.github.nhaeutilities.modules.patternrouting.core.HatchAssignmentData;
import com.github.nhaeutilities.modules.patternrouting.core.HatchAssignmentPersistenceSupport;

@Pseudo
@Mixin(targets = "com.science.gtnl.common.machine.hatch.SuperCraftingInputHatchME", remap = false)
public abstract class MixinGTNLSuperCraftingInputHatchME implements HatchAssignmentHolder {

    @Unique
    private HatchAssignmentData nhaeutilities$assignmentData = HatchAssignmentData.EMPTY;

    @Inject(method = "saveNBTData", at = @At("TAIL"), remap = false)
    private void nhaeutilities$saveAssignmentData(NBTTagCompound aNBT, CallbackInfo ci) {
        HatchAssignmentPersistenceSupport.save(aNBT, nhaeutilities$assignmentData, this.getClass());
    }

    @Inject(method = "loadNBTData", at = @At("TAIL"), remap = false)
    private void nhaeutilities$loadAssignmentData(NBTTagCompound aNBT, CallbackInfo ci) {
        nhaeutilities$assignmentData = HatchAssignmentPersistenceSupport.load(aNBT, this.getClass());
    }

    @Override
    public HatchAssignmentData nhaeutilities$getAssignmentData() {
        return nhaeutilities$assignmentData;
    }

    @Override
    public void nhaeutilities$setAssignmentData(HatchAssignmentData assignmentData) {
        nhaeutilities$assignmentData = HatchAssignmentPersistenceSupport.set(assignmentData, this.getClass());
    }

    @Override
    public void nhaeutilities$clearAssignmentData() {
        nhaeutilities$assignmentData = HatchAssignmentPersistenceSupport.clear(this.getClass());
    }
}
