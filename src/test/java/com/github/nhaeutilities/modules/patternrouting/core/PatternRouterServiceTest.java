package com.github.nhaeutilities.modules.patternrouting.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.Test;

public class PatternRouterServiceTest {

    @Test
    public void selectCandidateRejectsDifferentRecipeCategory() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe.canner",
            "",
            "",
            "gt.integrated_circuit@5",
            "minecraft:bucket@0",
            PatternRoutingKeys.SOURCE_NEI,
            false);

        HatchRoutingCandidate selected = PatternRouterService.selectCandidate(
            metadata,
            Arrays.asList(
                HatchRoutingCandidate.empty(
                    new HatchAssignmentData(
                        "a",
                        "gt.recipe.assembler",
                        "gt.integrated_circuit@5",
                        "minecraft:bucket@0"))));

        assertNull(selected);
    }

    @Test
    public void selectCandidateRejectsDifferentCircuitKey() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe.canner",
            "",
            "",
            "gt.integrated_circuit@5",
            "minecraft:bucket@0",
            PatternRoutingKeys.SOURCE_NEI,
            false);

        HatchRoutingCandidate selected = PatternRouterService.selectCandidate(
            metadata,
            Arrays.asList(
                HatchRoutingCandidate.empty(
                    new HatchAssignmentData(
                        "a",
                        "gt.recipe.canner",
                        "gt.integrated_circuit@1",
                        "minecraft:bucket@0"))));

        assertNull(selected);
    }

    @Test
    public void selectCandidateRejectsDifferentManualItemsKey() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe.canner",
            "",
            "",
            "gt.integrated_circuit@5",
            "minecraft:bucket@0|minecraft:cell@0",
            PatternRoutingKeys.SOURCE_NEI,
            false);

        HatchRoutingCandidate selected = PatternRouterService.selectCandidate(
            metadata,
            Arrays.asList(
                HatchRoutingCandidate.empty(
                    new HatchAssignmentData(
                        "a",
                        "gt.recipe.canner",
                        "gt.integrated_circuit@5",
                        "minecraft:bucket@0"))));

        assertNull(selected);
    }

    @Test
    public void matchingHatchesWithExistingPatternsArePreferredOverEmptyHatches() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe.canner",
            "",
            "",
            "gt.integrated_circuit@5",
            "minecraft:bucket@0",
            PatternRoutingKeys.SOURCE_NEI,
            false);

        HatchRoutingCandidate empty = HatchRoutingCandidate
            .empty(new HatchAssignmentData("a", "gt.recipe.canner", "gt.integrated_circuit@5", "minecraft:bucket@0"));
        HatchRoutingCandidate populated = HatchRoutingCandidate.withPatterns(
            new HatchAssignmentData("b", "gt.recipe.canner", "gt.integrated_circuit@5", "minecraft:bucket@0"));

        HatchRoutingCandidate selected = PatternRouterService
            .selectCandidate(metadata, Arrays.asList(empty, populated));

        assertEquals("b", selected.assignment.assignmentKey);
    }

    @Test
    public void fullHatchesAreExcludedBeforeSelection() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe.canner",
            "",
            "",
            "gt.integrated_circuit@5",
            "minecraft:bucket@0",
            PatternRoutingKeys.SOURCE_NEI,
            false);

        HatchRoutingCandidate full = HatchRoutingCandidate
            .full(new HatchAssignmentData("a", "gt.recipe.canner", "gt.integrated_circuit@5", "minecraft:bucket@0"));
        HatchRoutingCandidate empty = HatchRoutingCandidate
            .empty(new HatchAssignmentData("b", "gt.recipe.canner", "gt.integrated_circuit@5", "minecraft:bucket@0"));

        HatchRoutingCandidate selected = PatternRouterService.selectCandidate(metadata, Arrays.asList(full, empty));

        assertEquals("b", selected.assignment.assignmentKey);
    }

    @Test
    public void firstEmptyMatchIsSelectedWhenNoCandidateHasPatterns() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe.canner",
            "",
            "",
            "gt.integrated_circuit@5",
            "minecraft:bucket@0|minecraft:cell@0",
            PatternRoutingKeys.SOURCE_NEI,
            false);

        HatchRoutingCandidate first = HatchRoutingCandidate.empty(
            new HatchAssignmentData(
                "a",
                "gt.recipe.canner",
                "gt.integrated_circuit@5",
                "minecraft:bucket@0|minecraft:cell@0"));
        HatchRoutingCandidate second = HatchRoutingCandidate.empty(
            new HatchAssignmentData(
                "b",
                "gt.recipe.canner",
                "gt.integrated_circuit@5",
                "minecraft:bucket@0|minecraft:cell@0"));

        HatchRoutingCandidate selected = PatternRouterService.selectCandidate(metadata, Arrays.asList(first, second));

        assertEquals("a", selected.assignment.assignmentKey);
    }

    @Test
    public void resolveAssignmentDoesNotFallbackWhenAssignmentKeyIsExplicitButMissing() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "gt.recipe.canner",
            "",
            "missing-key",
            "gt.integrated_circuit@5",
            "minecraft:bucket@0",
            PatternRoutingKeys.SOURCE_NEI,
            false);

        HatchAssignmentData resolved = PatternRouterService.resolveAssignment(
            metadata,
            Arrays.asList(
                new HatchAssignmentData("key-b", "gt.recipe.canner", "gt.integrated_circuit@5", "minecraft:bucket@0")));

        assertNull(resolved);
    }

    @Test
    public void resolveAssignmentReturnsNullWhenMetadataIsNotResolvable() {
        PatternRoutingNbt.RoutingMetadata metadata = PatternRoutingNbt.RoutingMetadata.EMPTY;

        HatchAssignmentData resolved = PatternRouterService.resolveAssignment(
            metadata,
            Arrays.asList(
                new HatchAssignmentData("key-b", "gt.recipe.canner", "gt.integrated_circuit@5", "minecraft:bucket@0")));

        assertNull(resolved);
    }
}
