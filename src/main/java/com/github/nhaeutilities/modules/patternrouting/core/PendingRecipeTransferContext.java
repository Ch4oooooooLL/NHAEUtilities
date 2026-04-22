package com.github.nhaeutilities.modules.patternrouting.core;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class PendingRecipeTransferContext {

    public static final long DEFAULT_EXPIRY_MS = 300_000L;

    private static final Map<UUID, PendingTransfer> PENDING_TRANSFERS = new ConcurrentHashMap<UUID, PendingTransfer>();

    private PendingRecipeTransferContext() {}

    public static void store(UUID playerId, String recipeCategory, String programmingCircuit, String nonConsumables,
        String recipeSnapshot, String source, long timestamp) {
        if (playerId == null || isBlank(recipeCategory)) {
            return;
        }
        PendingTransfer previous = PENDING_TRANSFERS.put(
            playerId,
            createLegacyTransfer(
                "",
                recipeCategory,
                programmingCircuit,
                nonConsumables,
                recipeSnapshot,
                source,
                timestamp));
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][nbt] pending store player=%s recipeCategory=%s circuit=%s nc=%s snapshotSize=%s replaced=%s",
            playerId,
            recipeCategory,
            programmingCircuit,
            nonConsumables,
            recipeSnapshot != null ? recipeSnapshot.length() : 0,
            previous != null);
    }

    public static void store(UUID playerId, String recipeId, String overlayIdentifier, String programmingCircuit,
        String nonConsumables, String recipeSnapshot, String source, long timestamp) {
        if (playerId == null || isBlank(overlayIdentifier)) {
            return;
        }
        PendingTransfer previous = PENDING_TRANSFERS.put(
            playerId,
            createLegacyTransfer(
                recipeId,
                overlayIdentifier,
                programmingCircuit,
                nonConsumables,
                recipeSnapshot,
                source,
                timestamp));
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][nbt] pending store player=%s recipeId=%s recipeCategory=%s circuit=%s nc=%s snapshotSize=%s replaced=%s",
            playerId,
            recipeId,
            overlayIdentifier,
            programmingCircuit,
            nonConsumables,
            recipeSnapshot != null ? recipeSnapshot.length() : 0,
            previous != null);
    }

    private static PendingTransfer createLegacyTransfer(String recipeId, String recipeCategory,
        String programmingCircuit, String nonConsumables, String recipeSnapshot, String source, long timestamp) {
        return new PendingTransfer(
            recipeId,
            recipeCategory,
            programmingCircuit,
            nonConsumables,
            recipeSnapshot,
            source,
            timestamp >= 0L ? timestamp : System.currentTimeMillis());
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
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][nbt] pending expire player=%s recipeId=%s overlay=%s",
                playerId,
                transfer.recipeId,
                transfer.overlayIdentifier);
            return null;
        }
        return transfer;
    }

    public static PendingTransfer consume(UUID playerId, long now) {
        if (playerId == null) {
            return null;
        }
        PendingTransfer transfer = PENDING_TRANSFERS.remove(playerId);
        if (transfer == null) {
            return null;
        }
        if (isExpired(transfer, now)) {
            PatternRoutingLog.debug(
                "[NHAEUtilities][patternrouting][nbt] pending expire player=%s recipeId=%s overlay=%s",
                playerId,
                transfer.recipeId,
                transfer.overlayIdentifier);
            return null;
        }
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][nbt] pending consume player=%s recipeId=%s overlay=%s circuit=%s nc=%s snapshotSize=%s",
            playerId,
            transfer.recipeId,
            transfer.overlayIdentifier,
            transfer.programmingCircuit,
            transfer.nonConsumables,
            transfer.recipeSnapshot.length());
        return transfer;
    }

    public static void clear(UUID playerId) {
        if (playerId != null) {
            PENDING_TRANSFERS.remove(playerId);
            PatternRoutingLog.debug("[NHAEUtilities][patternrouting][nbt] pending clear player=%s", playerId);
        }
    }

    public static void clearAll() {
        PENDING_TRANSFERS.clear();
        PatternRoutingLog.debug("[NHAEUtilities][patternrouting][nbt] pending clearAll");
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

        public final String recipeCategory;
        public final String recipeId;
        public final String overlayIdentifier;
        public final String programmingCircuit;
        public final String nonConsumables;
        public final String recipeSnapshot;
        public final String source;
        public final long timestamp;

        private PendingTransfer(String recipeId, String recipeCategory, String programmingCircuit,
            String nonConsumables, String recipeSnapshot, String source, long timestamp) {
            this.recipeCategory = recipeCategory != null ? recipeCategory : "";
            this.recipeId = recipeId != null ? recipeId : "";
            this.overlayIdentifier = this.recipeCategory;
            this.programmingCircuit = programmingCircuit != null ? programmingCircuit : "";
            this.nonConsumables = nonConsumables != null ? nonConsumables : "[]";
            this.recipeSnapshot = recipeSnapshot != null ? recipeSnapshot : "{}";
            this.source = source != null ? source : "";
            this.timestamp = timestamp;
        }
    }
}
