package com.github.nhaeutilities.modules.patternrouting.core;

import static org.junit.Assert.assertEquals;

import java.util.UUID;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class PatternRoutingDeliveryServiceTest {

    @BeforeClass
    public static void initializeMinecraftBootstrap() {
        try {
            Class<?> bootstrap = Class.forName("net.minecraft.init.Bootstrap");
            try {
                bootstrap.getMethod("register")
                    .invoke(null);
                return;
            } catch (NoSuchMethodException ignored) {}

            bootstrap.getMethod("func_151354_b")
                .invoke(null);
        } catch (Exception ignored) {}
    }

    @After
    public void tearDown() {
        PendingRecipeTransferContext.clearAll();
    }

    @Test
    public void buildRoutingMetadataUsesRecipeCategoryCircuitAndManualItems() {
        UUID playerId = UUID.randomUUID();
        PendingRecipeTransferContext.store(
            playerId,
            "gt.recipe.canner",
            "gt.integrated_circuit@5",
            "[{\"item\":\"minecraft:bucket@0\",\"count\":0,\"nc\":true}]",
            "{\"recipeCategory\":\"gt.recipe.canner\"}",
            PatternRoutingKeys.SOURCE_NEI,
            1L);

        PendingRecipeTransferContext.PendingTransfer transfer = PendingRecipeTransferContext.consume(playerId, 2L);
        PatternRoutingNbt.RoutingMetadata metadata = PatternRoutingDeliveryService
            .buildRoutingMetadata(new ItemStack(Items.paper, 1, 0), transfer);

        assertEquals("gt.recipe.canner", metadata.recipeCategory);
        assertEquals("", metadata.assignmentKey);
        assertEquals(PatternRoutingKeys.SOURCE_NEI, metadata.source);
        assertEquals("gt.integrated_circuit@5", metadata.circuitKey);
        assertEquals("minecraft:bucket@0", metadata.manualItemsKey);
        assertEquals("gt.integrated_circuit@5", metadata.programmingCircuit);
        assertEquals("[{\"item\":\"minecraft:bucket@0\",\"count\":0,\"nc\":true}]", metadata.nonConsumables);
    }

    @Test
    public void buildRoutingMetadataDoesNotDuplicateProgrammingCircuitIntoManualItems() {
        UUID playerId = UUID.randomUUID();
        PendingRecipeTransferContext.store(
            playerId,
            "gt.recipe.metalbender",
            "gregtech:gt.integrated_circuit@1",
            "[{\"item\":\"gregtech:gt.integrated_circuit@1\",\"count\":0,\"nc\":true}]",
            "{\"recipeCategory\":\"gt.recipe.metalbender\"}",
            PatternRoutingKeys.SOURCE_NEI,
            1L);

        PendingRecipeTransferContext.PendingTransfer transfer = PendingRecipeTransferContext.consume(playerId, 2L);
        PatternRoutingNbt.RoutingMetadata metadata = PatternRoutingDeliveryService
            .buildRoutingMetadata(new ItemStack(Items.paper, 1, 0), transfer);

        assertEquals("gregtech:gt.integrated_circuit@1", metadata.circuitKey);
        assertEquals("", metadata.manualItemsKey);
    }

    @Test
    public void warningMessageKeyExistsForTargetFullAndMissingManualItems() {
        assertEquals(
            "",
            PatternRoutingDeliveryService.warningMessageKeyFor(PatternRouterService.RouteStatus.NO_METADATA));
        assertEquals(
            "",
            PatternRoutingDeliveryService.warningMessageKeyFor(PatternRouterService.RouteStatus.NO_MATCHING_HATCH));
        assertEquals("", PatternRoutingDeliveryService.warningMessageKeyFor(PatternRouterService.RouteStatus.ROUTED));
        assertEquals(
            "",
            PatternRoutingDeliveryService.warningMessageKeyFor(PatternRouterService.RouteStatus.INSERTION_FAILED));
        assertEquals(
            "nhaeutilities.msg.pattern.route_target_full",
            PatternRoutingDeliveryService.warningMessageKeyFor(PatternRouterService.RouteStatus.TARGET_FULL));
        assertEquals(
            "nhaeutilities.msg.pattern.route_missing_manual_items",
            PatternRoutingDeliveryService.warningMessageKeyFor(PatternRouterService.RouteStatus.MISSING_MANUAL_ITEMS));
    }
}
