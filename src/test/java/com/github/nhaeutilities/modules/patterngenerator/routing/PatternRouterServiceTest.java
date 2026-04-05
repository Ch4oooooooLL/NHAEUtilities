package com.github.nhaeutilities.modules.patterngenerator.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.Test;

public class PatternRouterServiceTest {

    @Test
    public void resolveAssignmentPrefersExactAssignmentKeyWhenPresent() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "recipe-a",
            "key-b",
            "circuit-a",
            "manual-a",
            PatternRoutingKeys.SOURCE_NEI,
            false,
            "overlay-a");

        HatchAssignmentData resolved = PatternRouterService.resolveAssignment(
            metadata,
            Arrays.asList(
                new HatchAssignmentData("key-a", "overlay-a", "", "circuit-a", "manual-a"),
                new HatchAssignmentData("key-b", "overlay-b", "", "", "")));

        assertEquals("key-b", resolved.assignmentKey);
    }

    @Test
    public void resolveAssignmentFallsBackToOverlayCircuitAndManualKeys() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "recipe-a",
            "",
            "circuit-a",
            "manual-a",
            PatternRoutingKeys.SOURCE_NEI,
            false,
            "gt.recipe.assembler");

        HatchAssignmentData resolved = PatternRouterService.resolveAssignment(
            metadata,
            Arrays.asList(
                new HatchAssignmentData("key-a", "gt.recipe.cutter", "", "circuit-a", "manual-a"),
                new HatchAssignmentData("key-b", "gt.recipe.assembler", "", "circuit-a", "manual-a")));

        assertEquals("key-b", resolved.assignmentKey);
    }

    @Test
    public void resolveAssignmentRejectsAmbiguousFallbackMatches() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "recipe-a",
            "",
            "circuit-a",
            "manual-a",
            PatternRoutingKeys.SOURCE_NEI,
            false,
            "gt.recipe.assembler");

        HatchAssignmentData resolved = PatternRouterService.resolveAssignment(
            metadata,
            Arrays.asList(
                new HatchAssignmentData("key-a", "gt.recipe.assembler", "", "circuit-a", "manual-a"),
                new HatchAssignmentData("key-b", "gt.recipe.assembler", "", "circuit-a", "manual-a")));

        assertNull(resolved);
    }

    @Test
    public void resolveAssignmentRejectsRecipeIdMismatchWhenHatchDeclaresSpecificRecipeId() {
        PatternRoutingNbt.RoutingMetadata metadata = new PatternRoutingNbt.RoutingMetadata(
            1,
            "recipe-a",
            "",
            "circuit-a",
            "manual-a",
            PatternRoutingKeys.SOURCE_NEI,
            false,
            "gt.recipe.assembler");

        HatchAssignmentData resolved = PatternRouterService.resolveAssignment(
            metadata,
            Arrays.asList(new HatchAssignmentData("key-b", "gt.recipe.assembler", "recipe-b", "circuit-a", "manual-a")));

        assertNull(resolved);
    }
}
