package com.github.nhaeutilities.modules.patternrouting.mixin;

import net.minecraft.nbt.NBTTagCompound;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.github.nhaeutilities.accessor.patternrouting.HatchAssignmentHolder;
import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;
import com.github.nhaeutilities.modules.patternrouting.core.HatchAssignmentData;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingKeys;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingLog;

@Pseudo
@Mixin(targets = "gregtech.common.tileentities.machines.MTEHatchCraftingInputME", remap = false)
public abstract class MixinMTEHatchCraftingInputME implements HatchAssignmentHolder {

    @Unique
    private HatchAssignmentData nhaeutilities$assignmentData = HatchAssignmentData.EMPTY;

    @Inject(method = "saveNBTData", at = @At("TAIL"), remap = false)
    private void nhaeutilities$saveAssignmentData(NBTTagCompound aNBT, CallbackInfo ci) {
        if (!PatternRoutingRuntime.isEnabled()) {
            return;
        }
        NBTTagCompound root = aNBT.getCompoundTag(PatternRoutingKeys.ROOT_KEY);
        if (!aNBT.hasKey(PatternRoutingKeys.ROOT_KEY)) {
            root = new NBTTagCompound();
            aNBT.setTag(PatternRoutingKeys.ROOT_KEY, root);
        }
        root.setTag(PatternRoutingKeys.HATCH_ASSIGNMENT_KEY, nhaeutilities$assignmentData.toNbt());
        PatternRoutingLog.info(
            "[NHAEUtilities][patternrouting] save hatch assignment hatch=%s assignment=%s recipeFamily=%s recipeId=%s circuit=%s manual=%s",
            this.getClass()
                .getName(),
            nhaeutilities$assignmentData.assignmentKey,
            nhaeutilities$assignmentData.recipeFamily,
            nhaeutilities$assignmentData.recipeId,
            nhaeutilities$assignmentData.circuitKey,
            nhaeutilities$assignmentData.manualItemsKey);
    }

    @Inject(method = "loadNBTData", at = @At("TAIL"), remap = false)
    private void nhaeutilities$loadAssignmentData(NBTTagCompound aNBT, CallbackInfo ci) {
        if (!PatternRoutingRuntime.isEnabled()) {
            nhaeutilities$assignmentData = HatchAssignmentData.EMPTY;
            return;
        }
        if (!aNBT.hasKey(PatternRoutingKeys.ROOT_KEY)) {
            nhaeutilities$assignmentData = HatchAssignmentData.EMPTY;
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] load hatch assignment missing root hatch=%s",
                this.getClass()
                    .getName());
            return;
        }
        NBTTagCompound root = aNBT.getCompoundTag(PatternRoutingKeys.ROOT_KEY);
        nhaeutilities$assignmentData = HatchAssignmentData
            .fromNbt(root.getCompoundTag(PatternRoutingKeys.HATCH_ASSIGNMENT_KEY));
        PatternRoutingLog.info(
            "[NHAEUtilities][patternrouting] load hatch assignment hatch=%s assignment=%s recipeFamily=%s recipeId=%s circuit=%s manual=%s",
            this.getClass()
                .getName(),
            nhaeutilities$assignmentData.assignmentKey,
            nhaeutilities$assignmentData.recipeFamily,
            nhaeutilities$assignmentData.recipeId,
            nhaeutilities$assignmentData.circuitKey,
            nhaeutilities$assignmentData.manualItemsKey);
    }

    @Override
    public HatchAssignmentData nhaeutilities$getAssignmentData() {
        return nhaeutilities$assignmentData;
    }

    @Override
    public void nhaeutilities$setAssignmentData(HatchAssignmentData assignmentData) {
        nhaeutilities$assignmentData = assignmentData != null ? assignmentData : HatchAssignmentData.EMPTY;
        PatternRoutingLog.info(
            "[NHAEUtilities][patternrouting] set hatch assignment hatch=%s assignment=%s recipeFamily=%s recipeId=%s circuit=%s manual=%s",
            this.getClass()
                .getName(),
            nhaeutilities$assignmentData.assignmentKey,
            nhaeutilities$assignmentData.recipeFamily,
            nhaeutilities$assignmentData.recipeId,
            nhaeutilities$assignmentData.circuitKey,
            nhaeutilities$assignmentData.manualItemsKey);
    }

    @Override
    public void nhaeutilities$clearAssignmentData() {
        nhaeutilities$assignmentData = HatchAssignmentData.EMPTY;
        PatternRoutingLog.info(
            "[NHAEUtilities][patternrouting] clear hatch assignment hatch=%s",
            this.getClass()
                .getName());
    }
}
