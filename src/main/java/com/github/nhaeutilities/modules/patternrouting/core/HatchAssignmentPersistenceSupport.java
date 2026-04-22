package com.github.nhaeutilities.modules.patternrouting.core;

import net.minecraft.nbt.NBTTagCompound;

import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;

public final class HatchAssignmentPersistenceSupport {

    private HatchAssignmentPersistenceSupport() {}

    public static void save(NBTTagCompound aNBT, HatchAssignmentData assignmentData, Class<?> hatchClass) {
        if (!PatternRoutingRuntime.isEnabled()) {
            return;
        }
        NBTTagCompound root = aNBT.getCompoundTag(PatternRoutingKeys.ROOT_KEY);
        if (!aNBT.hasKey(PatternRoutingKeys.ROOT_KEY)) {
            root = new NBTTagCompound();
            aNBT.setTag(PatternRoutingKeys.ROOT_KEY, root);
        }

        HatchAssignmentData normalized = normalize(assignmentData);
        root.setTag(PatternRoutingKeys.HATCH_ASSIGNMENT_KEY, normalized.toNbt());
        PatternRoutingLog.info(
            "[NHAEUtilities][patternrouting] save hatch assignment hatch=%s assignment=%s recipeFamily=%s recipeId=%s circuit=%s manual=%s",
            hatchClass != null ? hatchClass.getName() : "null",
            normalized.assignmentKey,
            normalized.recipeFamily,
            normalized.recipeId,
            normalized.circuitKey,
            normalized.manualItemsKey);
    }

    public static HatchAssignmentData load(NBTTagCompound aNBT, Class<?> hatchClass) {
        if (!PatternRoutingRuntime.isEnabled()) {
            return HatchAssignmentData.EMPTY;
        }
        if (!aNBT.hasKey(PatternRoutingKeys.ROOT_KEY)) {
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] load hatch assignment missing root hatch=%s",
                hatchClass != null ? hatchClass.getName() : "null");
            return HatchAssignmentData.EMPTY;
        }

        NBTTagCompound root = aNBT.getCompoundTag(PatternRoutingKeys.ROOT_KEY);
        HatchAssignmentData loaded = HatchAssignmentData
            .fromNbt(root.getCompoundTag(PatternRoutingKeys.HATCH_ASSIGNMENT_KEY));
        PatternRoutingLog.info(
            "[NHAEUtilities][patternrouting] load hatch assignment hatch=%s assignment=%s recipeFamily=%s recipeId=%s circuit=%s manual=%s",
            hatchClass != null ? hatchClass.getName() : "null",
            loaded.assignmentKey,
            loaded.recipeFamily,
            loaded.recipeId,
            loaded.circuitKey,
            loaded.manualItemsKey);
        return loaded;
    }

    public static HatchAssignmentData set(HatchAssignmentData assignmentData, Class<?> hatchClass) {
        HatchAssignmentData normalized = normalize(assignmentData);
        PatternRoutingLog.info(
            "[NHAEUtilities][patternrouting] set hatch assignment hatch=%s assignment=%s recipeFamily=%s recipeId=%s circuit=%s manual=%s",
            hatchClass != null ? hatchClass.getName() : "null",
            normalized.assignmentKey,
            normalized.recipeFamily,
            normalized.recipeId,
            normalized.circuitKey,
            normalized.manualItemsKey);
        return normalized;
    }

    public static HatchAssignmentData clear(Class<?> hatchClass) {
        PatternRoutingLog.info(
            "[NHAEUtilities][patternrouting] clear hatch assignment hatch=%s",
            hatchClass != null ? hatchClass.getName() : "null");
        return HatchAssignmentData.EMPTY;
    }

    private static HatchAssignmentData normalize(HatchAssignmentData assignmentData) {
        return assignmentData != null ? assignmentData : HatchAssignmentData.EMPTY;
    }
}
