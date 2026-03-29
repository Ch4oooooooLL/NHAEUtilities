package com.github.nhaeutilities.modules.patterngenerator.filter;

import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;

/**
 * ???????????
 */
public class TierFilter implements IRecipeFilter {

    private final int targetTier;

    /**
     * @param targetTier ????????? (-1=Any, 0=ULV, 1=LV...)
     */
    public TierFilter(int targetTier) {
        this.targetTier = targetTier;
    }

    @Override
    public boolean matches(RecipeEntry recipe) {
        if (targetTier < 0) return true; // Any

        // ???????????????????????????????????????????????????
        return getTier(recipe.euPerTick) == targetTier;
    }

    @Override
    public String getDescription() {
        return "Tier=" + targetTier;
    }

    private int getTier(long euPerTick) {
        if (euPerTick <= 0) return -1;
        long threshold = 8;
        int tier = 0;
        while (euPerTick > threshold) {
            threshold *= 4;
            tier++;
        }
        return tier;
    }
}
