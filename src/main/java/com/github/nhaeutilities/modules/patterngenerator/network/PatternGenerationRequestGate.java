package com.github.nhaeutilities.modules.patterngenerator.network;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import com.github.nhaeutilities.modules.patterngenerator.config.ForgeConfig;

/**
 * Collapses duplicate generate requests emitted by a single GUI interaction.
 */
final class PatternGenerationRequestGate {

    private static final char FINGERPRINT_SEPARATOR = 0x1F;
    private static final Map<UUID, RecentRequest> RECENT_REQUESTS = new HashMap<UUID, RecentRequest>();

    private PatternGenerationRequestGate() {}

    static synchronized boolean shouldProcess(UUID playerUUID, String fingerprint, long nowMillis) {
        cleanupExpired(nowMillis);

        long duplicateWindow = ForgeConfig.getDuplicateWindowMs();
        RecentRequest previous = RECENT_REQUESTS.get(playerUUID);
        if (previous != null && previous.fingerprint.equals(fingerprint)
            && nowMillis - previous.timestampMillis <= duplicateWindow) {
            return false;
        }

        RECENT_REQUESTS.put(playerUUID, new RecentRequest(fingerprint, nowMillis));
        return true;
    }

    static String fingerprint(String recipeMapId, String outputOreDict, String inputOreDict, String ncItem,
        String blacklistInput, String blacklistOutput, String replacements, String outputSlots, int targetTier) {
        return normalize(recipeMapId) + FINGERPRINT_SEPARATOR
            + normalize(outputOreDict)
            + FINGERPRINT_SEPARATOR
            + normalize(inputOreDict)
            + FINGERPRINT_SEPARATOR
            + normalize(ncItem)
            + FINGERPRINT_SEPARATOR
            + normalize(blacklistInput)
            + FINGERPRINT_SEPARATOR
            + normalize(blacklistOutput)
            + FINGERPRINT_SEPARATOR
            + normalize(replacements)
            + FINGERPRINT_SEPARATOR
            + normalize(outputSlots)
            + FINGERPRINT_SEPARATOR
            + targetTier;
    }

    static synchronized void reset() {
        RECENT_REQUESTS.clear();
    }

    private static void cleanupExpired(long nowMillis) {
        long duplicateWindow = ForgeConfig.getDuplicateWindowMs();
        Iterator<Map.Entry<UUID, RecentRequest>> iterator = RECENT_REQUESTS.entrySet()
            .iterator();
        while (iterator.hasNext()) {
            RecentRequest request = iterator.next()
                .getValue();
            if (nowMillis - request.timestampMillis > duplicateWindow) {
                iterator.remove();
            }
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class RecentRequest {

        private final String fingerprint;
        private final long timestampMillis;

        private RecentRequest(String fingerprint, long timestampMillis) {
            this.fingerprint = fingerprint;
            this.timestampMillis = timestampMillis;
        }
    }
}
