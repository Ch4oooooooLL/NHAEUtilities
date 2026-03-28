package com.github.nhaeutilities.modules.patterngenerator.storage;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.nhaeutilities.modules.patterngenerator.filter.CompositeFilter;
import com.github.nhaeutilities.modules.patterngenerator.recipe.GTRecipeSource;
import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;

/**
 * Coordinates recipe cache validation, rebuilds, and query access.
 */
public final class RecipeCacheService {

    private static final ExecutorService CACHE_EXECUTOR = Executors
        .newSingleThreadExecutor(runnable -> new Thread(runnable, "NHAEUtilities-RecipeCache"));

    private static volatile boolean caching = false;
    private static volatile StorageBackend storageBackend = new DefaultStorageBackend();
    private static volatile RecipeCollector recipeCollector = new DefaultRecipeCollector();
    private static volatile EnvironmentInspector environmentInspector = new DefaultEnvironmentInspector();
    private static volatile SourceCacheInvalidator sourceCacheInvalidator = new DefaultSourceCacheInvalidator();

    private RecipeCacheService() {}

    public static boolean createOrRefreshCache(ProgressNotifier notifier) {
        storageBackend.prepareAccessContext();
        synchronized (RecipeCacheService.class) {
            if (caching) {
                return false;
            }
            caching = true;
        }

        CACHE_EXECUTOR.execute(() -> {
            try {
                CacheStatistics stats = rebuildNow(notifier);
                if (notifier != null) {
                    notifier.onComplete(stats);
                }
            } catch (RuntimeException e) {
                if (notifier != null) {
                    notifier.onError(e.getMessage() != null ? e.getMessage() : "recipe_cache_build_failed");
                }
            } finally {
                synchronized (RecipeCacheService.class) {
                    caching = false;
                }
            }
        });
        return true;
    }

    public static boolean isCaching() {
        return caching;
    }

    public static boolean validateCache() {
        return loadValidatedMetadata(true) != null;
    }

    public static CacheQueryResult loadRecipes(String recipeMapKeyword) {
        return loadAndFilterRecipes(recipeMapKeyword, null);
    }

    public static CacheQueryResult loadAndFilterRecipes(String recipeMapKeyword, CompositeFilter filter) {
        if (loadValidatedMetadata(false) == null) {
            return CacheQueryResult.invalid("cache_missing_or_invalid");
        }

        List<String> matchedMapIds = recipeCollector.findMatchingRecipeMaps(recipeMapKeyword);
        if (matchedMapIds.isEmpty()) {
            return CacheQueryResult.valid(matchedMapIds, new ArrayList<RecipeEntry>(), 0, 0);
        }

        List<RecipeEntry> filtered = new ArrayList<RecipeEntry>();
        int totalLoaded = 0;
        for (String mapId : matchedMapIds) {
            List<RecipeEntry> recipes = storageBackend.loadRecipeMap(mapId);
            if (recipes == null) {
                return CacheQueryResult.invalid("cache_missing_or_invalid");
            }
            totalLoaded += recipes.size();
            if (filter == null) {
                filtered.addAll(recipes);
                continue;
            }

            for (RecipeEntry recipe : recipes) {
                if (filter.matches(recipe)) {
                    filtered.add(recipe);
                }
            }
        }

        return CacheQueryResult.valid(matchedMapIds, filtered, totalLoaded, filtered.size());
    }

    public static CacheStatistics getStatistics() {
        storageBackend.prepareAccessContext();
        RecipeCacheMetadata metadata = storageBackend.loadMetadata();
        if (metadata == null || metadata.recipeMaps.isEmpty()) {
            return CacheStatistics.EMPTY;
        }
        return buildStatistics(metadata);
    }

    public static void clearCache() {
        storageBackend.prepareAccessContext();
        sourceCacheInvalidator.invalidate();
        storageBackend.clearAll();
    }

    static CacheStatistics rebuildNow(ProgressNotifier notifier) {
        sourceCacheInvalidator.invalidate();
        RecipeCacheMetadata existing = storageBackend.loadMetadata();
        Map<String, String> currentModVersions = environmentInspector.getLoadedModVersions();
        Map<String, String> currentConfigHashes = environmentInspector.getConfigHashes();
        boolean canReuseExisting = existing != null && existing.cacheVersion == RecipeCacheMetadata.CURRENT_VERSION
            && !ModVersionHelper.isModVersionChanged(existing, currentModVersions)
            && !ModVersionHelper.isConfigHashChanged(existing, currentConfigHashes);

        RecipeCacheMetadata metadata = new RecipeCacheMetadata();
        metadata.createdAt = existing != null && existing.createdAt > 0 ? existing.createdAt : metadata.createdAt;
        metadata.configHashes.putAll(currentConfigHashes);

        List<String> availableMapIds = recipeCollector.getAvailableRecipeMapIds();
        java.util.Collections.sort(availableMapIds);

        Map<String, int[]> modCounters = new LinkedHashMap<String, int[]>();
        int total = availableMapIds.size();
        for (int index = 0; index < availableMapIds.size(); index++) {
            String mapId = availableMapIds.get(index);
            if (notifier != null) {
                notifier.onProgress("Caching " + mapId, index + 1, total);
            }

            RecipeCacheMetadata.RecipeMapInfo info;
            String resolvedModId = resolveSnapshotModId(mapId, currentModVersions, canReuseExisting
                && existing.recipeMaps.containsKey(mapId) ? existing.recipeMaps.get(mapId).modId : null);
            if (canReuseRecipeMap(existing, mapId)) {
                RecipeCacheMetadata.RecipeMapInfo oldInfo = existing.recipeMaps.get(mapId);
                info = new RecipeCacheMetadata.RecipeMapInfo(oldInfo.mapId, oldInfo.modId);
                info.recipeCount = oldInfo.recipeCount;
                info.cachedAt = oldInfo.cachedAt;
                info.contentHash = oldInfo.contentHash;
                info.cacheFileName = oldInfo.cacheFileName;
            } else {
                List<RecipeEntry> recipes = recipeCollector.collectRecipes(mapId);
                info = new RecipeCacheMetadata.RecipeMapInfo(mapId, resolvedModId);
                info.recipeCount = recipes.size();
                info.cachedAt = System.currentTimeMillis();
                info.contentHash = environmentInspector.calculateRecipeMapHash(mapId, recipes);
                info.cacheFileName = RecipeCacheStorage.getRecipeMapFile(mapId)
                    .getName();
                if (!storageBackend.saveRecipeMap(mapId, recipes, info)) {
                    throw new IllegalStateException("failed_to_save_recipe_map:" + mapId);
                }
            }

            info.modId = resolvedModId;
            metadata.putRecipeMapInfo(info);
            incrementModCounter(modCounters, info.modId, info.recipeCount);
        }

        if (existing != null) {
            for (String staleMapId : existing.recipeMaps.keySet()) {
                if (!metadata.recipeMaps.containsKey(staleMapId)) {
                    storageBackend.deleteRecipeMap(staleMapId);
                }
            }
        }

        for (Map.Entry<String, String> entry : currentModVersions.entrySet()) {
            int[] counts = modCounters.get(entry.getKey());
            metadata.updateModInfo(
                entry.getKey(),
                entry.getValue(),
                counts != null ? counts[0] : 0,
                counts != null ? counts[1] : 0);
        }

        metadata.recalculateTotals();
        metadata.touch();
        if (!storageBackend.saveMetadata(metadata)) {
            throw new IllegalStateException("failed_to_save_recipe_cache_metadata");
        }

        return buildStatistics(metadata);
    }

    static void setStorageBackend(StorageBackend backend) {
        storageBackend = backend != null ? backend : storageBackend;
    }

    static void setRecipeCollector(RecipeCollector collector) {
        recipeCollector = collector != null ? collector : recipeCollector;
    }

    static void setEnvironmentInspector(EnvironmentInspector inspector) {
        environmentInspector = inspector != null ? inspector : environmentInspector;
    }

    static void setSourceCacheInvalidator(SourceCacheInvalidator invalidator) {
        sourceCacheInvalidator = invalidator != null ? invalidator : sourceCacheInvalidator;
    }

    static void resetTestHooks() {
        storageBackend = new DefaultStorageBackend();
        recipeCollector = new DefaultRecipeCollector();
        environmentInspector = new DefaultEnvironmentInspector();
        sourceCacheInvalidator = new DefaultSourceCacheInvalidator();
    }

    private static RecipeCacheMetadata loadValidatedMetadata(boolean validatePayloads) {
        storageBackend.prepareAccessContext();
        RecipeCacheMetadata metadata = storageBackend.loadMetadata();
        if (metadata == null || metadata.cacheVersion != RecipeCacheMetadata.CURRENT_VERSION
            || metadata.recipeMaps.isEmpty()) {
            return null;
        }

        if (ModVersionHelper.isModVersionChanged(metadata, environmentInspector.getLoadedModVersions())) {
            return null;
        }
        if (ModVersionHelper.isConfigHashChanged(metadata, environmentInspector.getConfigHashes())) {
            return null;
        }

        for (Map.Entry<String, RecipeCacheMetadata.RecipeMapInfo> entry : metadata.recipeMaps.entrySet()) {
            String mapId = entry.getKey();
            if (!storageBackend.recipeMapExists(mapId)) {
                return null;
            }
            if (!validatePayloads) {
                continue;
            }

            List<RecipeEntry> recipes = storageBackend.loadRecipeMap(mapId);
            if (!isLoadedPayloadReusable(entry.getValue(), recipes)) {
                return null;
            }
        }
        return metadata;
    }

    private static boolean canReuseRecipeMap(RecipeCacheMetadata existing, String mapId) {
        if (existing == null || !existing.recipeMaps.containsKey(mapId) || !storageBackend.recipeMapExists(mapId)) {
            return false;
        }
        return isLoadedPayloadReusable(existing.recipeMaps.get(mapId), storageBackend.loadRecipeMap(mapId));
    }

    private static boolean isLoadedPayloadReusable(RecipeCacheMetadata.RecipeMapInfo info, List<RecipeEntry> recipes) {
        return info != null && recipes != null && info.recipeCount == recipes.size();
    }

    private static CacheStatistics buildStatistics(RecipeCacheMetadata metadata) {
        return new CacheStatistics(
            true,
            metadata.totalRecipeCount,
            metadata.totalRecipeMaps,
            metadata.mods.size(),
            calculateDirectoryBytes(storageBackend.getCacheDirectory()),
            metadata.createdAt,
            metadata.lastUpdated);
    }

    private static long calculateDirectoryBytes(File file) {
        if (file == null || !file.exists()) {
            return 0L;
        }
        if (file.isFile()) {
            return file.length();
        }

        long total = 0L;
        File[] children = file.listFiles();
        if (children == null) {
            return 0L;
        }
        for (File child : children) {
            total += calculateDirectoryBytes(child);
        }
        return total;
    }

    private static void incrementModCounter(Map<String, int[]> counters, String modId, int recipeCount) {
        String key = modId != null && !modId.isEmpty() ? modId : "unknown";
        int[] counts = counters.get(key);
        if (counts == null) {
            counts = new int[] { 0, 0 };
            counters.put(key, counts);
        }
        counts[0]++;
        counts[1] += Math.max(0, recipeCount);
    }

    private static String resolveSnapshotModId(String mapId, Map<String, String> currentModVersions, String fallbackModId) {
        String resolved = canonicalizeLoadedModId(environmentInspector.resolveModId(mapId), currentModVersions);
        if (!isBlank(resolved)) {
            return resolved;
        }

        String fallback = canonicalizeLoadedModId(fallbackModId, currentModVersions);
        if (!isBlank(fallback)) {
            return fallback;
        }

        String direct = environmentInspector.resolveModId(mapId);
        if (!isBlank(direct)) {
            return direct;
        }
        return !isBlank(fallbackModId) ? fallbackModId : "unknown";
    }

    private static String canonicalizeLoadedModId(String candidate, Map<String, String> currentModVersions) {
        if (isBlank(candidate) || currentModVersions == null || currentModVersions.isEmpty()) {
            return null;
        }
        if (currentModVersions.containsKey(candidate)) {
            return candidate;
        }

        List<String> loadedModIds = new ArrayList<String>(currentModVersions.keySet());
        Collections.sort(loadedModIds);
        for (String loadedModId : loadedModIds) {
            if (loadedModId.equalsIgnoreCase(candidate)) {
                return loadedModId;
            }
        }
        return null;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim()
            .isEmpty();
    }

    public interface ProgressNotifier {

        void onProgress(String message, int current, int total);

        void onComplete(CacheStatistics statistics);

        void onError(String message);
    }

    interface StorageBackend {

        default void prepareAccessContext() {}

        boolean saveRecipeMap(String mapId, List<RecipeEntry> recipes, RecipeCacheMetadata.RecipeMapInfo info);

        List<RecipeEntry> loadRecipeMap(String mapId);

        boolean saveMetadata(RecipeCacheMetadata metadata);

        RecipeCacheMetadata loadMetadata();

        boolean deleteRecipeMap(String mapId);

        void clearAll();

        File getCacheDirectory();

        boolean recipeMapExists(String mapId);
    }

    interface RecipeCollector {

        List<String> getAvailableRecipeMapIds();

        List<String> findMatchingRecipeMaps(String keyword);

        List<RecipeEntry> collectRecipes(String mapId);
    }

    interface EnvironmentInspector {

        Map<String, String> getLoadedModVersions();

        Map<String, String> getConfigHashes();

        String calculateRecipeMapHash(String mapId, List<RecipeEntry> recipes);

        String resolveModId(String mapId);
    }

    interface SourceCacheInvalidator {

        void invalidate();
    }

    private static final class DefaultStorageBackend implements StorageBackend {

        @Override
        public void prepareAccessContext() {
            RecipeCacheStorage.captureCurrentWorldSaveRoot();
        }

        @Override
        public boolean saveRecipeMap(String mapId, List<RecipeEntry> recipes, RecipeCacheMetadata.RecipeMapInfo info) {
            return RecipeCacheStorage.saveRecipeMap(mapId, recipes, info);
        }

        @Override
        public List<RecipeEntry> loadRecipeMap(String mapId) {
            return RecipeCacheStorage.loadRecipeMap(mapId);
        }

        @Override
        public boolean saveMetadata(RecipeCacheMetadata metadata) {
            return RecipeCacheStorage.saveMetadata(metadata);
        }

        @Override
        public RecipeCacheMetadata loadMetadata() {
            return RecipeCacheStorage.loadMetadata();
        }

        @Override
        public boolean deleteRecipeMap(String mapId) {
            return RecipeCacheStorage.deleteRecipeMap(mapId);
        }

        @Override
        public void clearAll() {
            RecipeCacheStorage.clearAll();
        }

        @Override
        public File getCacheDirectory() {
            return RecipeCacheStorage.getCacheDirectory();
        }

        @Override
        public boolean recipeMapExists(String mapId) {
            return RecipeCacheStorage.getRecipeMapFile(mapId)
                .exists();
        }
    }

    private static final class DefaultRecipeCollector implements RecipeCollector {

        @Override
        public List<String> getAvailableRecipeMapIds() {
            return new ArrayList<String>(
                GTRecipeSource.getAvailableRecipeMaps()
                    .keySet());
        }

        @Override
        public List<String> findMatchingRecipeMaps(String keyword) {
            return GTRecipeSource.findMatchingRecipeMaps(keyword);
        }

        @Override
        public List<RecipeEntry> collectRecipes(String mapId) {
            return GTRecipeSource.collectRecipes(mapId);
        }
    }

    private static final class DefaultEnvironmentInspector implements EnvironmentInspector {

        @Override
        public Map<String, String> getLoadedModVersions() {
            return ModVersionHelper.getLoadedModVersions();
        }

        @Override
        public Map<String, String> getConfigHashes() {
            return ModVersionHelper.calculateConfigHashes();
        }

        @Override
        public String calculateRecipeMapHash(String mapId, List<RecipeEntry> recipes) {
            return ModVersionHelper.calculateRecipeMapHash(mapId, recipes);
        }

        @Override
        public String resolveModId(String mapId) {
            return ModVersionHelper.resolveModId(mapId);
        }
    }

    private static final class DefaultSourceCacheInvalidator implements SourceCacheInvalidator {

        @Override
        public void invalidate() {
            GTRecipeSource.invalidateCollectionCache();
        }
    }
}

