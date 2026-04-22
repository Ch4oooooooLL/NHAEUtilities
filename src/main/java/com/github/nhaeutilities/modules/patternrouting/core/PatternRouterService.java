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
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][match] route start item=%s node=%s assignment=%s overlay=%s recipeId=%s circuit=%s manual=%s",
            PatternRoutingNbt.itemSignature(pattern),
            node != null,
            metadata != null ? metadata.assignmentKey : "",
            metadata != null ? metadata.overlayIdentifier : "",
            metadata != null ? metadata.recipeId : "",
            metadata != null ? metadata.circuitKey : "",
            metadata != null ? metadata.manualItemsKey : "");
        if (pattern == null || !hasResolvableRouting(metadata)) {
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][match] route result status=%s reason=%s",
                RouteStatus.NO_METADATA,
                pattern == null ? "null-pattern" : "unresolvable-metadata");
            return RouteResult.noMetadata();
        }
        if (node == null) {
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][match] route result status=%s reason=node-null",
                RouteStatus.NO_MATCHING_HATCH);
            return RouteResult.noMatchingHatch();
        }

        IGrid grid = node.getGrid();
        if (grid == null) {
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][match] route result status=%s reason=grid-null",
                RouteStatus.NO_MATCHING_HATCH);
            return RouteResult.noMatchingHatch();
        }

        List<HatchRoutingCandidate> candidates = new ArrayList<HatchRoutingCandidate>();
        int scannedNodes = 0;
        int hatchCandidates = 0;
        int matchingCount = 0;
        for (IGridNode gridNode : grid.getNodes()) {
            scannedNodes++;
            Object machine = gridNode != null ? gridNode.getMachine() : null;
            if (!CraftingInputHatchAccess.isCraftingInputHatch(machine)) {
                continue;
            }
            hatchCandidates++;
            if (!(machine instanceof HatchAssignmentHolder)) {
                PatternRoutingLog.debug(
                    "[NHAEUtilities][patternrouting][match] candidate skip hatch=%s reason=missing-assignment-holder",
                    machine != null ? machine.getClass()
                        .getName() : "null");
                continue;
            }
            HatchAssignmentData assignment = ((HatchAssignmentHolder) machine).nhaeutilities$getAssignmentData();
            boolean matched = matchesAssignment(metadata, assignment);
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][match] candidate evaluate hatch=%s assignment=%s matched=%s recipeCategory=%s recipeId=%s",
                machine != null ? machine.getClass()
                    .getName() : "null",
                assignment != null ? assignment.assignmentKey : "",
                matched,
                assignment != null ? assignment.recipeCategory : "",
                assignment != null ? assignment.recipeId : "");
            if (!matched) {
                continue;
            }
            matchingCount++;
            IInventory inventory = CraftingInputHatchAccess.getPatterns(machine);
            int limit = resolvePatternSlotLimit(machine, inventory);
            candidates.add(
                new HatchRoutingCandidate(
                    machine,
                    assignment,
                    hasPatterns(inventory, limit),
                    isFull(inventory, limit)));
        }

        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][match] candidate summary scanned=%s hatchCandidates=%s matched=%s",
            scannedNodes,
            hatchCandidates,
            matchingCount);
        if (matchingCount == 0) {
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][match] route result status=%s reason=no-candidate-match",
                RouteStatus.NO_MATCHING_HATCH);
            return RouteResult.noMatchingHatch();
        }

        HatchRoutingCandidate selected = selectCandidate(metadata, candidates);
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][match] selection resolved assignment=%s",
            selected != null ? selected.assignment.assignmentKey : "null");
        if (selected == null) {
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][match] route result status=%s reason=assignment-unresolved",
                RouteStatus.TARGET_FULL);
            return RouteResult.targetFull();
        }
        if (tryInsertIntoHatch(selected.hatch, pattern, selected.assignment)) {
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][match] route result status=%s target=%s assignment=%s",
                RouteStatus.ROUTED,
                selected.hatch != null ? selected.hatch.getClass()
                    .getName() : "null",
                selected.assignment.assignmentKey);
            return RouteResult.routed(selected.hatch);
        }
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][match] insertion failed hatch=%s assignment=%s",
            selected.hatch != null ? selected.hatch.getClass()
                .getName() : "null",
            selected.assignment.assignmentKey);
        return RouteResult.insertionFailed();
    }

    static HatchAssignmentData resolveAssignment(PatternRoutingNbt.RoutingMetadata metadata,
        Iterable<HatchAssignmentData> assignments) {
        if (!hasResolvableRouting(metadata) || assignments == null) {
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][match] selection skipped reason=%s",
                assignments == null ? "null-assignments" : "unresolvable-metadata");
            return null;
        }

        if (!metadata.assignmentKey.isEmpty()) {
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][match] selection mode=direct-key assignment=%s",
                metadata.assignmentKey);
            for (HatchAssignmentData assignment : assignments) {
                if (matchesAssignment(metadata, assignment)) {
                    PatternRoutingLog.debug(
                        "[NHAEUtilities][patternrouting][match] selection direct-key hit assignment=%s",
                        assignment.assignmentKey);
                    return assignment;
                }
            }
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][match] selection direct-key miss assignment=%s",
                metadata.assignmentKey);
            return null;
        }

        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][match] selection mode=fallback overlay=%s circuit=%s manual=%s recipeId=%s",
            metadata.overlayIdentifier,
            metadata.circuitKey,
            metadata.manualItemsKey,
            metadata.recipeId);
        HatchRoutingCandidate matched = null;
        for (HatchAssignmentData assignment : assignments) {
            if (!matchesAssignment(metadata, assignment)) {
                continue;
            }
            if (matched != null) {
                PatternRoutingLog.debug(
                    "[NHAEUtilities][patternrouting][match] selection fallback ambiguous existing=%s incoming=%s",
                    matched.assignment.assignmentKey,
                    assignment.assignmentKey);
                return null;
            }
            matched = HatchRoutingCandidate.empty(assignment);
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][match] selection fallback candidate assignment=%s",
                assignment.assignmentKey);
        }
        return matched != null ? matched.assignment : null;
    }

    static HatchRoutingCandidate selectCandidate(PatternRoutingNbt.RoutingMetadata metadata,
        List<HatchRoutingCandidate> candidates) {
        if (!hasResolvableRouting(metadata) || candidates == null) {
            return null;
        }
        HatchRoutingCandidate firstEmpty = null;
        for (HatchRoutingCandidate candidate : candidates) {
            if (candidate == null || candidate.full || !matchesAssignment(metadata, candidate.assignment)) {
                continue;
            }
            if (candidate.hasPatterns) {
                return candidate;
            }
            if (firstEmpty == null) {
                firstEmpty = candidate;
            }
        }
        return firstEmpty;
    }

    private static boolean tryInsertIntoHatch(Object hatch, ItemStack pattern, HatchAssignmentData assignment) {
        IInventory inventory = CraftingInputHatchAccess.getPatterns(hatch);
        if (inventory == null || pattern == null || assignment == null || assignment.assignmentKey.isEmpty()) {
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][match] insertion skipped hatch=%s reason=%s",
                hatch != null ? hatch.getClass()
                    .getName() : "null",
                inventory == null ? "null-inventory"
                    : pattern == null ? "null-pattern"
                        : assignment == null ? "null-assignment" : "empty-assignment-key");
            return false;
        }

        int limit = resolvePatternSlotLimit(hatch, inventory);
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][match] insertion start hatch=%s assignment=%s slots=%s",
            hatch != null ? hatch.getClass()
                .getName() : "null",
            assignment.assignmentKey,
            limit);
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
                    metadata.recipeCategory,
                    metadata.recipeId,
                    assignment.assignmentKey,
                    metadata.circuitKey,
                    metadata.manualItemsKey,
                    metadata.source,
                    true,
                    metadata.programmingCircuit,
                    metadata.nonConsumables,
                    metadata.recipeSnapshot));
            inventory.setInventorySlotContents(slot, copy);
            ItemStack inserted = inventory.getStackInSlot(slot);
            boolean insertedOk = inserted != null && ItemStack.areItemStacksEqual(inserted, copy);
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][match] insertion attempt hatch=%s slot=%s assignment=%s success=%s",
                hatch.getClass()
                    .getName(),
                slot,
                assignment.assignmentKey,
                insertedOk);
            if (insertedOk) {
                return true;
            }
        }
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][match] insertion exhausted hatch=%s assignment=%s",
            hatch != null ? hatch.getClass()
                .getName() : "null",
            assignment.assignmentKey);
        return false;
    }

    private static boolean hasResolvableRouting(PatternRoutingNbt.RoutingMetadata metadata) {
        if (metadata == null) {
            return false;
        }
        if (!metadata.assignmentKey.isEmpty()) {
            return true;
        }
        return !metadata.recipeCategory.isEmpty();
    }

    private static boolean matchesAssignment(PatternRoutingNbt.RoutingMetadata metadata,
        HatchAssignmentData assignment) {
        if (assignment == null || assignment.assignmentKey.isEmpty()) {
            return false;
        }

        if (!metadata.assignmentKey.isEmpty()) {
            return metadata.assignmentKey.equals(assignment.assignmentKey);
        }

        if (!metadata.recipeCategory.equals(assignment.recipeCategory)) {
            return false;
        }
        if (!metadata.circuitKey.equals(assignment.circuitKey)) {
            return false;
        }
        return metadata.manualItemsKey.equals(assignment.manualItemsKey);
    }

    private static boolean hasPatterns(IInventory inventory, int limit) {
        if (inventory == null || limit <= 0) {
            return false;
        }
        for (int slot = 0; slot < limit; slot++) {
            if (inventory.getStackInSlot(slot) != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFull(IInventory inventory, int limit) {
        if (inventory == null || limit <= 0) {
            return true;
        }
        for (int slot = 0; slot < limit; slot++) {
            if (inventory.getStackInSlot(slot) == null) {
                return false;
            }
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

        public static RouteResult insertionFailed() {
            return new RouteResult(RouteStatus.INSERTION_FAILED, null);
        }
    }

    public enum RouteStatus {
        ROUTED,
        NO_METADATA,
        NO_MATCHING_HATCH,
        TARGET_FULL,
        INSERTION_FAILED
    }
}
