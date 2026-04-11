package com.github.nhaeutilities.modules.patternrouting.core;

import net.minecraft.nbt.NBTTagCompound;

public final class HatchAssignmentData {

    public static final HatchAssignmentData EMPTY = new HatchAssignmentData("", "", "", "", "");

    public final String assignmentKey;
    public final String recipeFamily;
    public final String recipeId;
    public final String circuitKey;
    public final String manualItemsKey;

    public HatchAssignmentData(String assignmentKey, String recipeFamily, String recipeId, String circuitKey,
        String manualItemsKey) {
        this.assignmentKey = normalize(assignmentKey);
        this.recipeFamily = normalize(recipeFamily);
        this.recipeId = normalize(recipeId);
        this.circuitKey = normalize(circuitKey);
        this.manualItemsKey = normalize(manualItemsKey);
    }

    public boolean isAssigned() {
        return !assignmentKey.isEmpty();
    }

    public NBTTagCompound toNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(PatternRoutingKeys.ASSIGNMENT_KEY, assignmentKey);
        tag.setString(PatternRoutingKeys.OVERLAY_IDENTIFIER_KEY, recipeFamily);
        tag.setString(PatternRoutingKeys.RECIPE_ID_KEY, recipeId);
        tag.setString(PatternRoutingKeys.CIRCUIT_KEY, circuitKey);
        tag.setString(PatternRoutingKeys.MANUAL_ITEMS_KEY, manualItemsKey);
        return tag;
    }

    public static HatchAssignmentData fromNbt(NBTTagCompound tag) {
        if (tag == null || tag.hasNoTags()) {
            return EMPTY;
        }
        return new HatchAssignmentData(
            tag.getString(PatternRoutingKeys.ASSIGNMENT_KEY),
            tag.getString(PatternRoutingKeys.OVERLAY_IDENTIFIER_KEY),
            tag.getString(PatternRoutingKeys.RECIPE_ID_KEY),
            tag.getString(PatternRoutingKeys.CIRCUIT_KEY),
            tag.getString(PatternRoutingKeys.MANUAL_ITEMS_KEY));
    }

    private static String normalize(String value) {
        return value != null ? value : "";
    }
}
