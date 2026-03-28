package com.github.nhaeutilities.modules.patterngenerator.filter;

import net.minecraft.item.ItemStack;

import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;

/**
 * ??????????????????????????????
 */
public class OutputOreDictFilter implements IRecipeFilter {

    private final String matchSource;
    private final ExplicitStackMatcher matcher;

    public OutputOreDictFilter(String matchSource) {
        this(matchSource, new ExplicitStackMatcher.StackMatchCache());
    }

    OutputOreDictFilter(String matchSource, ExplicitStackMatcher.StackMatchCache stackMatchCache) {
        this.matchSource = matchSource;
        this.matcher = new ExplicitStackMatcher(matchSource, stackMatchCache);
    }

    @Override
    public boolean matches(RecipeEntry recipe) {
        if (matcher.isDisabled()) {
            return true;
        }

        for (ItemStack output : recipe.outputs) {
            if (output != null && matcher.matches(output)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "output filter " + matchSource;
    }

    public String getRegexPattern() {
        return matchSource;
    }
}

