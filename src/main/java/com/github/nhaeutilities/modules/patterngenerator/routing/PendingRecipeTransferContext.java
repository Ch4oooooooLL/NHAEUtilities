package com.github.nhaeutilities.modules.patterngenerator.routing;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingRecipeTransferContext {

    public static final long DEFAULT_EXPIRY_MS = 300_000L;

    private static final Map<UUID, PendingTransfer> PENDING_TRANSFERS = new ConcurrentHashMap<UUID, PendingTransfer>();

    private PendingRecipeTransferContext() {}

    public static void store(UUID playerId, String recipeId, String overlayIdentifier, String source, long timestamp) {
        if (playerId == null || isBlank(recipeId)) {
            return;
        }
        PENDING_TRANSFERS.put(
            playerId,
            new PendingTransfer(recipeId, overlayIdentifier, source, timestamp >= 0L ? timestamp : System.currentTimeMillis()));
    }

    public static PendingTransfer peek(UUID playerId, long now) {
        if (playerId == null) {
            return null;
        }
        PendingTransfer transfer = PENDING_TRANSFERS.get(playerId);
        if (transfer == null) {
            return null;
        }
        if (isExpired(transfer, now)) {
            PENDING_TRANSFERS.remove(playerId);
            return null;
        }
        return transfer;
    }

    public static PendingTransfer consume(UUID playerId, long now) {
        PendingTransfer transfer = peek(playerId, now);
        if (transfer != null) {
            PENDING_TRANSFERS.remove(playerId);
        }
        return transfer;
    }

    public static void clear(UUID playerId) {
        if (playerId != null) {
            PENDING_TRANSFERS.remove(playerId);
        }
    }

    public static void clearAll() {
        PENDING_TRANSFERS.clear();
    }

    private static boolean isExpired(PendingTransfer transfer, long now) {
        long currentTime = now > 0L ? now : System.currentTimeMillis();
        return currentTime - transfer.timestamp > DEFAULT_EXPIRY_MS;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim()
            .isEmpty();
    }

    public static final class PendingTransfer {

        public final String recipeId;
        public final String overlayIdentifier;
        public final String source;
        public final long timestamp;

        private PendingTransfer(String recipeId, String overlayIdentifier, String source, long timestamp) {
            this.recipeId = recipeId != null ? recipeId : "";
            this.overlayIdentifier = overlayIdentifier != null ? overlayIdentifier : "";
            this.source = source != null ? source : "";
            this.timestamp = timestamp;
        }
    }
}
