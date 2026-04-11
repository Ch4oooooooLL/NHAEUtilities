package com.github.nhaeutilities.modules.patternrouting.core;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;

import com.github.nhaeutilities.accessor.patternrouting.HatchAssignmentHolder;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;

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

        List<Object> matches = new ArrayList<Object>();
        List<HatchAssignmentData> assignments = new ArrayList<HatchAssignmentData>();
        for (IGridNode gridNode : grid.getNodes()) {
            Object machine = gridNode != null ? gridNode.getMachine() : null;
            if (!CraftingInputHatchAccess.isCraftingInputHatch(machine)) {
                continue;
            }
            if (!(machine instanceof HatchAssignmentHolder)) {
                continue;
            }
            HatchAssignmentData assignment = ((HatchAssignmentHolder) machine).nhaeutilities$getAssignmentData();
            if (matchesAssignment(metadata, assignment)) {
                matches.add(machine);
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
            Object hatch = matches.get(i);
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

    private static boolean tryInsertIntoHatch(Object hatch, ItemStack pattern, HatchAssignmentData assignment) {
        IInventory inventory = CraftingInputHatchAccess.getPatterns(hatch);
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

    private static boolean matchesAssignment(PatternRoutingNbt.RoutingMetadata metadata,
        HatchAssignmentData assignment) {
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

    private static int resolvePatternSlotLimit(Object hatch, IInventory inventory) {
        return CraftingInputHatchAccess.getPatternSlotLimit(hatch, inventory, FALLBACK_PATTERN_SLOT_LIMIT);
    }

    public static final class RouteResult {

        public final RouteStatus status;
        public final Object target;

        private RouteResult(RouteStatus status, Object target) {
            this.status = status;
            this.target = target;
        }

        public boolean isRouted() {
            return status == RouteStatus.ROUTED;
        }

        public static RouteResult routed(Object target) {
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
