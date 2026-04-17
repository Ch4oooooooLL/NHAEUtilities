package com.github.nhaeutilities.modules.patternrouting.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingRecipeTransferContext {

    public static final long DEFAULT_EXPIRY_MS = 300_000L;

    private static final Map<UUID, PendingTransfer> PENDING_TRANSFERS = new ConcurrentHashMap<UUID, PendingTransfer>();

    private PendingRecipeTransferContext() {}

    public static void store(UUID playerId, String recipeId, String overlayIdentifier, String programmingCircuit,
        String nonConsumables, String recipeSnapshot, String source, long timestamp) {
        if (playerId == null || isBlank(recipeId)) {
            return;
        }
        PendingTransfer previous = PENDING_TRANSFERS.put(
            playerId,
            new PendingTransfer(
                recipeId,
                overlayIdentifier,
                programmingCircuit,
                nonConsumables,
                recipeSnapshot,
                source,
                timestamp >= 0L ? timestamp : System.currentTimeMillis()));
        PatternRoutingLog.info(
            "[NHAEUtilities][patternrouting] pending store player=%s recipeId=%s overlay=%s circuit=%s nc=%s snapshotSize=%s replaced=%s",
            playerId,
            recipeId,
            overlayIdentifier,
            programmingCircuit,
            nonConsumables,
            recipeSnapshot != null ? recipeSnapshot.length() : 0,
            previous != null);
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
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] pending expired player=%s recipeId=%s overlay=%s",
                playerId,
                transfer.recipeId,
                transfer.overlayIdentifier);
            return null;
        }
        return transfer;
    }

    public static PendingTransfer consume(UUID playerId, long now) {
        PendingTransfer transfer = peek(playerId, now);
        if (transfer != null) {
            PENDING_TRANSFERS.remove(playerId);
            PatternRoutingLog.info(
                "[NHAEUtilities][patternrouting] pending consume player=%s recipeId=%s overlay=%s circuit=%s nc=%s snapshotSize=%s",
                playerId,
                transfer.recipeId,
                transfer.overlayIdentifier,
                transfer.programmingCircuit,
                transfer.nonConsumables,
                transfer.recipeSnapshot.length());
        }
        return transfer;
    }

    public static void clear(UUID playerId) {
        if (playerId != null) {
            PENDING_TRANSFERS.remove(playerId);
            PatternRoutingLog.info("[NHAEUtilities][patternrouting] pending clear player=%s", playerId);
        }
    }

    public static void clearAll() {
        PENDING_TRANSFERS.clear();
        PatternRoutingLog.info("[NHAEUtilities][patternrouting] pending clearAll");
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
        public final String programmingCircuit;
        public final String nonConsumables;
        public final String recipeSnapshot;
        public final String source;
        public final long timestamp;

        private PendingTransfer(String recipeId, String overlayIdentifier, String programmingCircuit,
            String nonConsumables, String recipeSnapshot, String source, long timestamp) {
            this.recipeId = recipeId != null ? recipeId : "";
            this.overlayIdentifier = overlayIdentifier != null ? overlayIdentifier : "";
            this.programmingCircuit = programmingCircuit != null ? programmingCircuit : "";
            this.nonConsumables = nonConsumables != null ? nonConsumables : "[]";
            this.recipeSnapshot = recipeSnapshot != null ? recipeSnapshot : "{}";
            this.source = source != null ? source : "";
            this.timestamp = timestamp;
        }
    }
}
