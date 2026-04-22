package com.github.nhaeutilities.modules.patternrouting.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

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
            "gt.recipe.assembler",
            "gt.integrated_circuit@1",
            "[{\"item\":\"minecraft:bucket@0\",\"count\":0,\"nc\":true}]",
            "{\"recipeCategory\":\"gt.recipe.assembler\"}",
            PatternRoutingKeys.SOURCE_NEI,
            1_000L);

        PendingRecipeTransferContext.PendingTransfer transfer = PendingRecipeTransferContext.consume(playerId, 2_000L);

        assertNotNull(transfer);
        assertEquals("", transfer.recipeId);
        assertEquals("gt.recipe.assembler", transfer.recipeCategory);
        assertEquals("gt.recipe.assembler", transfer.overlayIdentifier);
        assertEquals(PatternRoutingKeys.SOURCE_NEI, transfer.source);
        assertEquals("gt.integrated_circuit@1", transfer.programmingCircuit);
        assertEquals("[{\"item\":\"minecraft:bucket@0\",\"count\":0,\"nc\":true}]", transfer.nonConsumables);
        assertEquals("{\"recipeCategory\":\"gt.recipe.assembler\"}", transfer.recipeSnapshot);
        assertNull(PendingRecipeTransferContext.consume(playerId, 2_001L));
    }

    @Test
    public void consumePreservesRecipeIdAndOverlayIdentifierForExtendedStore() {
        UUID playerId = UUID.randomUUID();

        PendingRecipeTransferContext.store(
            playerId,
            "{\"id\":\"recipe-1\"}",
            "gt.recipe.assembler",
            "gt.integrated_circuit@24",
            "[{\"item\":\"minecraft:bucket@0\",\"count\":0,\"nc\":true}]",
            "{\"recipeCategory\":\"gt.recipe.assembler\",\"inputs\":[]}",
            PatternRoutingKeys.SOURCE_AE2FC,
            1_000L);

        PendingRecipeTransferContext.PendingTransfer transfer = PendingRecipeTransferContext.consume(playerId, 2_000L);

        assertNotNull(transfer);
        assertEquals("{\"id\":\"recipe-1\"}", transfer.recipeId);
        assertEquals("gt.recipe.assembler", transfer.recipeCategory);
        assertEquals("gt.recipe.assembler", transfer.overlayIdentifier);
        assertEquals(PatternRoutingKeys.SOURCE_AE2FC, transfer.source);
        assertEquals("gt.integrated_circuit@24", transfer.programmingCircuit);
        assertEquals("[{\"item\":\"minecraft:bucket@0\",\"count\":0,\"nc\":true}]", transfer.nonConsumables);
        assertEquals("{\"recipeCategory\":\"gt.recipe.assembler\",\"inputs\":[]}", transfer.recipeSnapshot);
    }

    @Test
    public void consumeDropsStaleTransfers() {
        UUID playerId = UUID.randomUUID();

        PendingRecipeTransferContext
            .store(playerId, "gt.recipe.assembler", "", "[]", "{}", PatternRoutingKeys.SOURCE_NEI, 0L);

        assertNull(PendingRecipeTransferContext.consume(playerId, PendingRecipeTransferContext.DEFAULT_EXPIRY_MS + 1L));
    }

    @Test
    public void consumeCannotBeAppliedTwiceConcurrently() throws InterruptedException {
        UUID playerId = UUID.randomUUID();

        PendingRecipeTransferContext
            .store(playerId, "gt.recipe.assembler", "", "[]", "{}", PatternRoutingKeys.SOURCE_NEI, 1_000L);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger nonNullResults = new AtomicInteger(0);

        Runnable consume = new Runnable() {

            @Override
            public void run() {
                ready.countDown();
                try {
                    start.await();
                } catch (InterruptedException e) {
                    Thread.currentThread()
                        .interrupt();
                    return;
                }
                PendingRecipeTransferContext.PendingTransfer transfer = PendingRecipeTransferContext
                    .consume(playerId, 2_000L);
                if (transfer != null) {
                    nonNullResults.incrementAndGet();
                }
            }
        };

        Thread first = new Thread(consume, "consume-first");
        Thread second = new Thread(consume, "consume-second");
        first.start();
        second.start();

        ready.await();
        start.countDown();
        first.join();
        second.join();

        assertEquals(1, nonNullResults.get());
        assertNull(PendingRecipeTransferContext.consume(playerId, 2_001L));
    }
}
