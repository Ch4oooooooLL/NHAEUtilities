package com.github.nhaeutilities.modules.patterngenerator.routing;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import com.github.nhaeutilities.accessor.patterngenerator.HatchAssignmentHolder;
import gregtech.common.tileentities.machines.MTEHatchCraftingInputME;

public final class PatternRouterService {

    private static final int FALLBACK_PATTERN_SLOT_LIMIT = 36;

    private PatternRouterService() {}

    public static RouteResult tryRoute(ItemStack pattern, IGridNode node) {
        PatternRoutingNbt.RoutingMetadata metadata = PatternRoutingNbt.readRoutingData(pattern);
        if (pattern == null || !hasResolvableRouting(metadata)) {
            return RouteResult.noMetadata();
        }
        if (node == null) {
            return RouteResult.noMatchingHatch();
        }

        IGrid grid = node.getGrid();
        if (grid == null) {
            return RouteResult.noMatchingHatch();
        }

        List<MTEHatchCraftingInputME> matches = new ArrayList<MTEHatchCraftingInputME>();
        List<HatchAssignmentData> assignments = new ArrayList<HatchAssignmentData>();
        for (IGridNode gridNode : grid.getNodes()) {
            Object machine = gridNode != null ? gridNode.getMachine() : null;
            if (!(machine instanceof MTEHatchCraftingInputME)) {
                continue;
            }
            MTEHatchCraftingInputME hatch = (MTEHatchCraftingInputME) machine;
            if (!(hatch instanceof HatchAssignmentHolder)) {
                continue;
            }
            HatchAssignmentData assignment = ((HatchAssignmentHolder) hatch).nhaeutilities$getAssignmentData();
            if (matchesAssignment(metadata, assignment)) {
                matches.add(hatch);
                assignments.add(assignment);
            }
        }

        if (matches.isEmpty()) {
            return RouteResult.noMatchingHatch();
        }

        HatchAssignmentData resolvedAssignment = resolveAssignment(metadata, assignments);
        if (resolvedAssignment == null) {
            return RouteResult.noMatchingHatch();
        }

        boolean targetFull = false;
        for (int i = 0; i < matches.size(); i++) {
            MTEHatchCraftingInputME hatch = matches.get(i);
            HatchAssignmentData assignment = assignments.get(i);
            if (!resolvedAssignment.assignmentKey.equals(assignment.assignmentKey)) {
                continue;
            }
            if (tryInsertIntoHatch(hatch, pattern, assignment)) {
                return RouteResult.routed(hatch);
            }
            targetFull = true;
        }

        return targetFull ? RouteResult.targetFull() : RouteResult.noMatchingHatch();
    }

    static HatchAssignmentData resolveAssignment(PatternRoutingNbt.RoutingMetadata metadata,
        Iterable<HatchAssignmentData> assignments) {
        if (!hasResolvableRouting(metadata) || assignments == null) {
            return null;
        }

        if (!metadata.assignmentKey.isEmpty()) {
            for (HatchAssignmentData assignment : assignments) {
                if (matchesAssignment(metadata, assignment)) {
                    return assignment;
                }
            }
            return null;
        }

        HatchAssignmentData matchedAssignment = null;
        for (HatchAssignmentData assignment : assignments) {
            if (matchesAssignment(metadata, assignment)) {
                if (matchedAssignment != null) {
                    return null;
                }
                matchedAssignment = assignment;
            }
        }
        return matchedAssignment;
    }

    private static boolean tryInsertIntoHatch(MTEHatchCraftingInputME hatch, ItemStack pattern,
        HatchAssignmentData assignment) {
        IInventory inventory = hatch.getPatterns();
        if (inventory == null || pattern == null || assignment == null || assignment.assignmentKey.isEmpty()) {
            return false;
        }

        int limit = resolvePatternSlotLimit(hatch, inventory);
        for (int slot = 0; slot < limit; slot++) {
            ItemStack existing = inventory.getStackInSlot(slot);
            if (existing != null) {
                continue;
            }

            ItemStack copy = pattern.copy();
            copy.stackSize = 1;
            PatternRoutingNbt.RoutingMetadata metadata = PatternRoutingNbt.readRoutingData(copy);
            PatternRoutingNbt.writeRoutingData(
                copy,
                new PatternRoutingNbt.RoutingMetadata(
                    metadata.version,
                    metadata.recipeId,
                    assignment.assignmentKey,
                    metadata.circuitKey,
                    metadata.manualItemsKey,
                    metadata.source,
                    true,
                    metadata.overlayIdentifier));
            inventory.setInventorySlotContents(slot, copy);
            ItemStack inserted = inventory.getStackInSlot(slot);
            if (inserted != null && ItemStack.areItemStacksEqual(inserted, copy)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasResolvableRouting(PatternRoutingNbt.RoutingMetadata metadata) {
        if (metadata == null) {
            return false;
        }
        if (!metadata.assignmentKey.isEmpty()) {
            return true;
        }
        return !metadata.overlayIdentifier.isEmpty();
    }

    private static boolean matchesAssignment(PatternRoutingNbt.RoutingMetadata metadata, HatchAssignmentData assignment) {
        if (assignment == null || assignment.assignmentKey.isEmpty()) {
            return false;
        }

        if (!metadata.assignmentKey.isEmpty()) {
            return metadata.assignmentKey.equals(assignment.assignmentKey);
        }

        if (!metadata.overlayIdentifier.equals(assignment.recipeFamily)) {
            return false;
        }
        if (!metadata.circuitKey.equals(assignment.circuitKey)) {
            return false;
        }
        if (!metadata.manualItemsKey.equals(assignment.manualItemsKey)) {
            return false;
        }
        if (!assignment.recipeId.isEmpty() && !metadata.recipeId.isEmpty()
            && !metadata.recipeId.equals(assignment.recipeId)) {
            return false;
        }
        return true;
    }

    private static int resolvePatternSlotLimit(MTEHatchCraftingInputME hatch, IInventory inventory) {
        int reflected = readStaticIntField(hatch.getClass(), "MAX_PATTERN_COUNT");
        if (reflected > 0) {
            return Math.min(reflected, inventory.getSizeInventory());
        }
        return Math.min(FALLBACK_PATTERN_SLOT_LIMIT, inventory.getSizeInventory());
    }

    private static int readStaticIntField(Class<?> type, String fieldName) {
        Class<?> current = type;
        while (current != null) {
            try {
                Field field = current.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.getInt(null);
            } catch (Exception ignored) {
                current = current.getSuperclass();
            }
        }
        return -1;
    }

    public static final class RouteResult {

        public final RouteStatus status;
        public final MTEHatchCraftingInputME target;

        private RouteResult(RouteStatus status, MTEHatchCraftingInputME target) {
            this.status = status;
            this.target = target;
        }

        public boolean isRouted() {
            return status == RouteStatus.ROUTED;
        }

        public static RouteResult routed(MTEHatchCraftingInputME target) {
            return new RouteResult(RouteStatus.ROUTED, target);
        }

        public static RouteResult noMetadata() {
            return new RouteResult(RouteStatus.NO_METADATA, null);
        }

        public static RouteResult noMatchingHatch() {
            return new RouteResult(RouteStatus.NO_MATCHING_HATCH, null);
        }

        public static RouteResult targetFull() {
            return new RouteResult(RouteStatus.TARGET_FULL, null);
        }
    }

    public enum RouteStatus {
        ROUTED,
        NO_METADATA,
        NO_MATCHING_HATCH,
        TARGET_FULL
    }
}
