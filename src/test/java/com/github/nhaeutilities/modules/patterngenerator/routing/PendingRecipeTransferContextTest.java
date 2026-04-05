package com.github.nhaeutilities.modules.patterngenerator.routing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.UUID;

import org.junit.After;
import org.junit.Test;

public class PendingRecipeTransferContextTest {

    @After
    public void tearDown() {
        PendingRecipeTransferContext.clearAll();
    }

    @Test
    public void consumeReturnsPendingTransferWhenFresh() {
        UUID playerId = UUID.randomUUID();

        PendingRecipeTransferContext.store(
            playerId,
            "{\"handlerName\":\"gt.recipe.assembler\"}",
            "gt.recipe.assembler",
            PatternRoutingKeys.SOURCE_NEI,
            1_000L);

        PendingRecipeTransferContext.PendingTransfer transfer = PendingRecipeTransferContext.consume(playerId, 2_000L);

        assertNotNull(transfer);
        assertEquals("{\"handlerName\":\"gt.recipe.assembler\"}", transfer.recipeId);
        assertEquals("gt.recipe.assembler", transfer.overlayIdentifier);
        assertEquals(PatternRoutingKeys.SOURCE_NEI, transfer.source);
        assertNull(PendingRecipeTransferContext.consume(playerId, 2_001L));
    }

    @Test
    public void consumeDropsStaleTransfers() {
        UUID playerId = UUID.randomUUID();

        PendingRecipeTransferContext.store(playerId, "recipe-a", "gt.recipe.assembler", PatternRoutingKeys.SOURCE_NEI, 0L);

        assertNull(
            PendingRecipeTransferContext.consume(
                playerId,
                PendingRecipeTransferContext.DEFAULT_EXPIRY_MS + 1L));
    }
}
