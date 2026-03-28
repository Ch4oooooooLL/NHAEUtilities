package com.github.nhaeutilities.modules.patterngenerator.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.github.nhaeutilities.modules.patterngenerator.filter.CompositeFilter;
import com.github.nhaeutilities.modules.patterngenerator.filter.IRecipeFilter;
import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;

public class RecipeCacheServiceTest {

    private FakeStorageBackend storage;
    private FakeRecipeCollector collector;
    private FakeEnvironmentInspector inspector;
    private AtomicInteger sourceInvalidations;

    @Before
    public void setUp() throws Exception {
        storage = new FakeStorageBackend(
            Files.createTempDirectory("nhaeutilities-cache-service")
                .toFile());
        collector = new FakeRecipeCollector();
        inspector = new FakeEnvironmentInspector();
        sourceInvalidations = new AtomicInteger();
        RecipeCacheService.setStorageBackend(storage);
        RecipeCacheService.setRecipeCollector(collector);
        RecipeCacheService.setEnvironmentInspector(inspector);
        RecipeCacheService.setSourceCacheInvalidator(() -> sourceInvalidations.incrementAndGet());
    }

    @After
    public void tearDown() {
        RecipeCacheService.resetTestHooks();
    }

    @Test
    public void validateCacheFailsWhenMetadataMissing() {
        assertFalse(RecipeCacheService.validateCache());
    }

    @Test
    public void loadAndFilterRecipesRejectsInvalidCache() {
        CacheQueryResult result = RecipeCacheService.loadAndFilterRecipes("assembler", new CompositeFilter());

        assertFalse(result.cacheValid);
        assertEquals("cache_missing_or_invalid", result.failureReason);
    }

    @Test
    public void rebuildNowReusesExistingEntriesWhenEnvironmentIsUnchanged() {
        RecipeCacheMetadata metadata = new RecipeCacheMetadata();
        RecipeCacheMetadata.RecipeMapInfo info = new RecipeCacheMetadata.RecipeMapInfo(
            "gt.recipe.assembler",
            "gregtech");
        info.recipeCount = 2;
        info.contentHash = "hash-a";
        info.cacheFileName = "gt.recipe.assembler.dat";
        metadata.putRecipeMapInfo(info);
        metadata.updateModInfo("gregtech", "5.0.0", 1, 2);
        metadata.setConfigHash("config/nhaeutilities.cfg", "cfg-hash");
        storage.metadata = metadata;
        storage.persistedRecipeMaps.put("gt.recipe.assembler", Arrays.asList(sampleRecipe(20), sampleRecipe(40)));
        collector.availableMapIds = Collections.singletonList("gt.recipe.assembler");
        inspector.modVersions.put("gregtech", "5.0.0");
        inspector.configHashes.put("config/nhaeutilities.cfg", "cfg-hash");

        CacheStatistics stats = RecipeCacheService.rebuildNow(null);

        assertEquals(2, stats.totalRecipeCount);
        assertEquals(0, collector.collectCalls);
        assertEquals(0, storage.saveRecipeMapCalls);
    }

    @Test
    public void rebuildNowRegeneratesCorruptPersistedMapWhenEnvironmentIsUnchanged() {
        RecipeCacheMetadata metadata = new RecipeCacheMetadata();
        RecipeCacheMetadata.RecipeMapInfo info = new RecipeCacheMetadata.RecipeMapInfo(
            "gt.recipe.assembler",
            "gregtech");
        info.recipeCount = 1;
        info.contentHash = "hash-a";
        info.cacheFileName = "gt.recipe.assembler.dat";
        metadata.putRecipeMapInfo(info);
        metadata.updateModInfo("gregtech", "5.0.0", 1, 1);
        metadata.setConfigHash("config/nhaeutilities.cfg", "cfg-hash");
        storage.metadata = metadata;
        storage.persistedRecipeMaps.put("gt.recipe.assembler", Collections.singletonList(sampleRecipe(20)));
        storage.invalidRecipeMaps.add("gt.recipe.assembler");
        collector.availableMapIds = Collections.singletonList("gt.recipe.assembler");
        collector.recipeMapRecipes.put("gt.recipe.assembler", Collections.singletonList(sampleRecipe(120)));
        inspector.modVersions.put("gregtech", "5.0.0");
        inspector.configHashes.put("config/nhaeutilities.cfg", "cfg-hash");

        CacheStatistics stats = RecipeCacheService.rebuildNow(null);

        assertEquals(1, stats.totalRecipeCount);
        assertEquals(1, collector.collectCalls);
        assertEquals(1, storage.saveRecipeMapCalls);
        assertEquals(120, storage.persistedRecipeMaps.get("gt.recipe.assembler").get(0).duration);
        assertTrue(RecipeCacheService.validateCache());
    }

    @Test
    public void validateCacheSucceedsAfterRebuildWithFullModSnapshot() {
        collector.availableMapIds = Collections.singletonList("gt.recipe.assembler");
        inspector.modVersions.put("gregtech", "5.0.0");
        inspector.modVersions.put("minecraft", "1.7.10");
        inspector.configHashes.put("config/nhaeutilities.cfg", "cfg-hash");

        RecipeCacheService.rebuildNow(null);

        assertTrue(RecipeCacheService.validateCache());
        assertEquals(2, storage.metadata.mods.size());
        assertTrue(storage.metadata.mods.containsKey("gregtech"));
        assertTrue(storage.metadata.mods.containsKey("minecraft"));
    }

    @Test
    public void validateCacheIgnoresRecipePrefixesThatAreNotRealModIds() {
        collector.availableMapIds = Collections.singletonList("ic.recipe.recycler");
        collector.recipeMapRecipes.put("ic.recipe.recycler", Arrays.asList(sampleRecipe(20), sampleRecipe(40)));
        inspector.resolvedModIds.put("ic.recipe.recycler", "IC2");
        inspector.modVersions.put("IC2", "2.2.828");
        inspector.modVersions.put("minecraft", "1.7.10");
        inspector.configHashes.put("config/nhaeutilities.cfg", "cfg-hash");

        RecipeCacheService.rebuildNow(null);

        assertTrue(RecipeCacheService.validateCache());
        assertEquals(2, storage.metadata.mods.size());
        assertTrue(storage.metadata.mods.containsKey("IC2"));
        assertTrue(storage.metadata.mods.containsKey("minecraft"));
        assertEquals(1, storage.metadata.mods.get("IC2").recipeMapCount);
        assertEquals(2, storage.metadata.mods.get("IC2").recipeCount);
    }

    @Test
    public void validateCacheFailsWhenRecipeMapPayloadCannotBeLoaded() {
        RecipeCacheMetadata metadata = new RecipeCacheMetadata();
        metadata.updateRecipeMapInfo("gt.recipe.assembler", "gregtech", 1, "hash-a", "gt.recipe.assembler.dat");
        metadata.updateModInfo("gregtech", "5.0.0", 1, 1);
        metadata.setConfigHash("config/nhaeutilities.cfg", "cfg-hash");
        storage.metadata = metadata;
        storage.persistedRecipeMaps.put("gt.recipe.assembler", Collections.singletonList(sampleRecipe(20)));
        storage.invalidRecipeMaps.add("gt.recipe.assembler");
        inspector.modVersions.put("gregtech", "5.0.0");
        inspector.configHashes.put("config/nhaeutilities.cfg", "cfg-hash");

        assertFalse(RecipeCacheService.validateCache());
    }

    @Test
    public void rebuildNowReassignsReusedRecipeMapCountsToResolvedLoadedModId() {
        RecipeCacheMetadata metadata = new RecipeCacheMetadata();
        metadata.updateRecipeMapInfo("ic.recipe.recycler", "ic", 2, "hash-a", "ic.recipe.recycler.dat");
        metadata.updateModInfo("ic", "legacy", 1, 2);
        metadata.setConfigHash("config/nhaeutilities.cfg", "cfg-hash");
        storage.metadata = metadata;
        storage.persistedRecipeMaps.put("ic.recipe.recycler", Arrays.asList(sampleRecipe(20), sampleRecipe(40)));
        collector.availableMapIds = Collections.singletonList("ic.recipe.recycler");
        inspector.resolvedModIds.put("ic.recipe.recycler", "IC2");
        inspector.modVersions.put("IC2", "2.2.828");
        inspector.modVersions.put("minecraft", "1.7.10");
        inspector.configHashes.put("config/nhaeutilities.cfg", "cfg-hash");

        RecipeCacheService.rebuildNow(null);

        assertTrue(storage.metadata.mods.containsKey("IC2"));
        assertEquals(1, storage.metadata.mods.get("IC2").recipeMapCount);
        assertEquals(2, storage.metadata.mods.get("IC2").recipeCount);
        assertEquals("IC2", storage.metadata.recipeMaps.get("ic.recipe.recycler").modId);
    }

    @Test
    public void loadAndFilterRecipesUsesStoredPayloadAndFilter() {
        RecipeCacheMetadata metadata = new RecipeCacheMetadata();
        metadata.updateRecipeMapInfo("gt.recipe.assembler", "gregtech", 2, "hash-a", "gt.recipe.assembler.dat");
        metadata.updateModInfo("gregtech", "5.0.0", 1, 2);
        metadata.setConfigHash("config/nhaeutilities.cfg", "cfg-hash");
        storage.metadata = metadata;
        storage.persistedRecipeMaps.put("gt.recipe.assembler", Arrays.asList(sampleRecipe(20), sampleRecipe(120)));
        collector.availableMapIds = Collections.singletonList("gt.recipe.assembler");
        collector.matches.put("assembler", Collections.singletonList("gt.recipe.assembler"));
        inspector.modVersions.put("gregtech", "5.0.0");
        inspector.configHashes.put("config/nhaeutilities.cfg", "cfg-hash");

        CompositeFilter filter = new CompositeFilter();
        filter.addFilter(new IRecipeFilter() {

            @Override
            public boolean matches(RecipeEntry recipe) {
                return recipe.duration >= 100;
            }

            @Override
            public String getDescription() {
                return "duration >= 100";
            }
        });

        CacheQueryResult result = RecipeCacheService.loadAndFilterRecipes("assembler", filter);

        assertTrue(result.cacheValid);
        assertEquals(2, result.totalLoadedCount);
        assertEquals(1, result.totalFilteredCount);
        assertEquals(120, result.recipes.get(0).duration);
    }

    @Test
    public void loadAndFilterRecipesSkipsDeepValidationOfUnmatchedCorruptMaps() {
        RecipeCacheMetadata metadata = new RecipeCacheMetadata();
        metadata.updateRecipeMapInfo("gt.recipe.assembler", "gregtech", 1, "hash-a", "gt.recipe.assembler.dat");
        metadata.updateRecipeMapInfo("gt.recipe.circuit", "gregtech", 1, "hash-b", "gt.recipe.circuit.dat");
        metadata.updateModInfo("gregtech", "5.0.0", 2, 2);
        metadata.setConfigHash("config/nhaeutilities.cfg", "cfg-hash");
        storage.metadata = metadata;
        storage.persistedRecipeMaps.put("gt.recipe.assembler", Collections.singletonList(sampleRecipe(20)));
        storage.persistedRecipeMaps.put("gt.recipe.circuit", Collections.singletonList(sampleRecipe(40)));
        storage.invalidRecipeMaps.add("gt.recipe.circuit");
        collector.availableMapIds = Arrays.asList("gt.recipe.assembler", "gt.recipe.circuit");
        collector.matches.put("assembler", Collections.singletonList("gt.recipe.assembler"));
        inspector.modVersions.put("gregtech", "5.0.0");
        inspector.configHashes.put("config/nhaeutilities.cfg", "cfg-hash");

        CacheQueryResult result = RecipeCacheService.loadAndFilterRecipes("assembler", null);

        assertTrue(result.cacheValid);
        assertEquals(1, result.totalLoadedCount);
        assertEquals(1, result.recipes.size());
        assertEquals(1, storage.loadRecipeMapCalls);
    }

    @Test
    public void rebuildNowInvalidatesRecipeSourceCaches() {
        collector.availableMapIds = Collections.singletonList("gt.recipe.assembler");
        inspector.modVersions.put("gregtech", "5.0.0");
        inspector.configHashes.put("config/nhaeutilities.cfg", "cfg-hash");

        RecipeCacheService.rebuildNow(null);

        assertEquals(1, sourceInvalidations.get());
    }

    @Test
    public void clearCacheInvalidatesRecipeSourceCaches() {
        RecipeCacheService.clearCache();

        assertEquals(1, sourceInvalidations.get());
    }

    private static RecipeEntry sampleRecipe(int duration) {
        return new RecipeEntry("gt", "gt.recipe.assembler", "Assembler", null, null, null, null, null, duration, 30);
    }

    private static final class FakeStorageBackend implements RecipeCacheService.StorageBackend {

        private final File cacheDirectory;
        private RecipeCacheMetadata metadata;
        private final Map<String, List<RecipeEntry>> persistedRecipeMaps = new LinkedHashMap<String, List<RecipeEntry>>();
        private final List<String> invalidRecipeMaps = new ArrayList<String>();
        private int loadRecipeMapCalls;
        private int saveRecipeMapCalls;

        private FakeStorageBackend(File cacheDirectory) {
            this.cacheDirectory = cacheDirectory;
        }

        @Override
        public boolean saveRecipeMap(String mapId, List<RecipeEntry> recipes, RecipeCacheMetadata.RecipeMapInfo info) {
            saveRecipeMapCalls++;
            invalidRecipeMaps.remove(mapId);
            persistedRecipeMaps.put(mapId, new ArrayList<RecipeEntry>(recipes));
            return true;
        }

        @Override
        public List<RecipeEntry> loadRecipeMap(String mapId) {
            loadRecipeMapCalls++;
            if (invalidRecipeMaps.contains(mapId)) {
                return null;
            }
            List<RecipeEntry> recipes = persistedRecipeMaps.get(mapId);
            return recipes != null ? new ArrayList<RecipeEntry>(recipes) : new ArrayList<RecipeEntry>();
        }

        @Override
        public boolean saveMetadata(RecipeCacheMetadata metadata) {
            this.metadata = metadata;
            return true;
        }

        @Override
        public RecipeCacheMetadata loadMetadata() {
            return metadata;
        }

        @Override
        public boolean deleteRecipeMap(String mapId) {
            persistedRecipeMaps.remove(mapId);
            return true;
        }

        @Override
        public void clearAll() {
            persistedRecipeMaps.clear();
            metadata = null;
        }

        @Override
        public File getCacheDirectory() {
            return cacheDirectory;
        }

        @Override
        public boolean recipeMapExists(String mapId) {
            return persistedRecipeMaps.containsKey(mapId);
        }
    }

    private static final class FakeRecipeCollector implements RecipeCacheService.RecipeCollector {

        private List<String> availableMapIds = new ArrayList<String>();
        private Map<String, List<String>> matches = new LinkedHashMap<String, List<String>>();
        private Map<String, List<RecipeEntry>> recipeMapRecipes = new LinkedHashMap<String, List<RecipeEntry>>();
        private int collectCalls;

        @Override
        public List<String> getAvailableRecipeMapIds() {
            return new ArrayList<String>(availableMapIds);
        }

        @Override
        public List<String> findMatchingRecipeMaps(String keyword) {
            List<String> found = matches.get(keyword);
            return found != null ? new ArrayList<String>(found) : new ArrayList<String>();
        }

        @Override
        public List<RecipeEntry> collectRecipes(String mapId) {
            collectCalls++;
            List<RecipeEntry> recipes = recipeMapRecipes.get(mapId);
            return recipes != null ? new ArrayList<RecipeEntry>(recipes) : Arrays.asList(sampleRecipe(20), sampleRecipe(40));
        }
    }

    private static final class FakeEnvironmentInspector implements RecipeCacheService.EnvironmentInspector {

        private final Map<String, String> modVersions = new LinkedHashMap<String, String>();
        private final Map<String, String> configHashes = new LinkedHashMap<String, String>();
        private final Map<String, String> resolvedModIds = new LinkedHashMap<String, String>();

        @Override
        public Map<String, String> getLoadedModVersions() {
            return new LinkedHashMap<String, String>(modVersions);
        }

        @Override
        public Map<String, String> getConfigHashes() {
            return new LinkedHashMap<String, String>(configHashes);
        }

        @Override
        public String calculateRecipeMapHash(String mapId, List<RecipeEntry> recipes) {
            return mapId + ":" + recipes.size();
        }

        @Override
        public String resolveModId(String mapId) {
            String resolved = resolvedModIds.get(mapId);
            return resolved != null ? resolved : "gregtech";
        }
    }
}

