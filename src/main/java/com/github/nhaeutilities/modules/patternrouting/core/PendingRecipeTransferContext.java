package com.github.nhaeutilities.modules.patternrouting.core;

import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

public final class PendingRecipeTransferContext {

    public static final long DEFAULT_EXPIRY_MS = 300_000L;

    private static final Map<UUID, Deque<PendingTransfer>> PENDING_TRANSFERS = new ConcurrentHashMap<UUID, Deque<PendingTransfer>>();

    private PendingRecipeTransferContext() {}

    public static void store(UUID playerId, String recipeCategory, String programmingCircuit, String nonConsumables,
        String recipeSnapshot, String source, long timestamp) {
        if (playerId == null || isBlank(recipeCategory)) {
            return;
        }
        Deque<PendingTransfer> transfers = queueFor(playerId);
        transfers.addLast(
            createLegacyTransfer(
                "",
                recipeCategory,
                programmingCircuit,
                nonConsumables,
                recipeSnapshot,
                source,
                timestamp));
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][nbt] pending store player=%s recipeCategory=%s circuit=%s nc=%s snapshotSize=%s queued=%s",
            playerId,
            recipeCategory,
            programmingCircuit,
            nonConsumables,
            recipeSnapshot != null ? recipeSnapshot.length() : 0,
            transfers.size());
    }

    public static void store(UUID playerId, String recipeId, String overlayIdentifier, String programmingCircuit,
        String nonConsumables, String recipeSnapshot, String source, long timestamp) {
        if (playerId == null || isBlank(overlayIdentifier)) {
            return;
        }
        Deque<PendingTransfer> transfers = queueFor(playerId);
        transfers.addLast(
            createLegacyTransfer(
                recipeId,
                overlayIdentifier,
                programmingCircuit,
                nonConsumables,
                recipeSnapshot,
                source,
                timestamp));
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][nbt] pending store player=%s recipeId=%s recipeCategory=%s circuit=%s nc=%s snapshotSize=%s queued=%s",
            playerId,
            recipeId,
            overlayIdentifier,
            programmingCircuit,
            nonConsumables,
            recipeSnapshot != null ? recipeSnapshot.length() : 0,
            transfers.size());
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
        Deque<PendingTransfer> transfers = PENDING_TRANSFERS.get(playerId);
        if (transfers == null) {
            return null;
        }
        pruneExpired(playerId, transfers, now);
        PendingTransfer transfer = transfers.peekLast();
        if (transfer == null) {
            PENDING_TRANSFERS.remove(playerId);
            return null;
        }
        return transfer;
    }

    public static PendingTransfer consume(UUID playerId, String source, String recipeId, String overlayIdentifier,
        long now) {
        Deque<PendingTransfer> transfers = PENDING_TRANSFERS.get(playerId);
        if (transfers == null) {
            return null;
        }
        pruneExpired(playerId, transfers, now);
        PendingTransfer fallbackCandidate = transfers.peekLast();
        if (fallbackCandidate == null) {
            PENDING_TRANSFERS.remove(playerId);
            return null;
        }

        for (PendingTransfer transfer : transfers) {
            if (!matches(transfer, source, recipeId, overlayIdentifier)) {
                continue;
            }
            if (transfers.remove(transfer)) {
                clearIfEmpty(playerId, transfers);
                PatternRoutingLog.debug(
                    "[NHAEUtilities][patternrouting][nbt] pending consume exact player=%s recipeId=%s overlay=%s source=%s circuit=%s nc=%s snapshotSize=%s",
                    playerId,
                    transfer.recipeId,
                    transfer.overlayIdentifier,
                    transfer.source,
                    transfer.programmingCircuit,
                    transfer.nonConsumables,
                    transfer.recipeSnapshot.length());
                return transfer;
            }
        }

        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][nbt] pending fallback player=%s requestedSource=%s requestedRecipeId=%s requestedOverlay=%s actualSource=%s actualRecipeId=%s actualOverlay=%s",
            playerId,
            source,
            recipeId,
            overlayIdentifier,
            fallbackCandidate.source,
            fallbackCandidate.recipeId,
            fallbackCandidate.overlayIdentifier);
        return consume(playerId, now);
    }

    public static PendingTransfer consume(UUID playerId, long now) {
        if (playerId == null) {
            return null;
        }
        Deque<PendingTransfer> transfers = PENDING_TRANSFERS.get(playerId);
        if (transfers == null) {
            return null;
        }
        pruneExpired(playerId, transfers, now);
        PendingTransfer transfer = transfers.pollLast();
        if (transfer == null) {
            PENDING_TRANSFERS.remove(playerId);
            return null;
        }
        clearIfEmpty(playerId, transfers);
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

    private static boolean matches(PendingTransfer transfer, String source, String recipeId, String overlayIdentifier) {
        if (transfer == null) {
            return false;
        }
        if (!isBlank(source) && !source.equals(transfer.source)) {
            return false;
        }
        if (!isBlank(recipeId) && !recipeId.equals(transfer.recipeId)) {
            return false;
        }
        if (!isBlank(overlayIdentifier) && !overlayIdentifier.equals(transfer.overlayIdentifier)) {
            return false;
        }
        return true;
    }

    public static void clear(UUID playerId) {
        if (playerId != null) {
            PENDING_TRANSFERS.remove(playerId);
            PatternRoutingLog.debug("[NHAEUtilities][patternrouting][nbt] pending clear player=%s", playerId);
        }
    }

    private static Deque<PendingTransfer> queueFor(UUID playerId) {
        return PENDING_TRANSFERS.computeIfAbsent(playerId, ignored -> new ConcurrentLinkedDeque<PendingTransfer>());
    }

    private static void pruneExpired(UUID playerId, Deque<PendingTransfer> transfers, long now) {
        for (PendingTransfer transfer : transfers) {
            if (!isExpired(transfer, now)) {
                continue;
            }
            if (transfers.remove(transfer)) {
                PatternRoutingLog.debug(
                    "[NHAEUtilities][patternrouting][nbt] pending expire player=%s recipeId=%s overlay=%s",
                    playerId,
                    transfer.recipeId,
                    transfer.overlayIdentifier);
            }
        }
        clearIfEmpty(playerId, transfers);
    }

    private static void clearIfEmpty(UUID playerId, Deque<PendingTransfer> transfers) {
        if (transfers != null && transfers.isEmpty()) {
            PENDING_TRANSFERS.remove(playerId);
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
