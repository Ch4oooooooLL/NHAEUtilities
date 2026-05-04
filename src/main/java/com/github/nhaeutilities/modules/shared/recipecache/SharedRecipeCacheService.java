package com.github.nhaeutilities.modules.shared.recipecache;

import java.util.List;

import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;
import com.github.nhaeutilities.modules.patterngenerator.storage.CacheQueryResult;
import com.github.nhaeutilities.modules.patterngenerator.storage.RecipeCacheService;

public final class SharedRecipeCacheService {

    private SharedRecipeCacheService() {}

    public static List<RecipeEntry> loadRecipesEnsuringLatest(String recipeMapId) {
        String normalized = requireRecipeMapId(recipeMapId);

        CacheQueryResult result = RecipeCacheService.loadExactRecipeMap(normalized);
        if (!result.cacheValid) {
            RecipeCacheService.createOrRefreshCacheNow();
            result = RecipeCacheService.loadExactRecipeMap(normalized);
        }
        if (!result.cacheValid) {
            throw new IllegalStateException("Failed to load recipe cache for " + normalized);
        }
        if (result.matchedMapIds.isEmpty()) {
            throw new IllegalArgumentException("Unknown recipe map: " + normalized);
        }
        return result.recipes;
    }

    private static String requireRecipeMapId(String recipeMapId) {
        String normalized = recipeMapId != null ? recipeMapId.trim() : "";
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("recipeMapId is blank");
        }
        return normalized;
    }
}
