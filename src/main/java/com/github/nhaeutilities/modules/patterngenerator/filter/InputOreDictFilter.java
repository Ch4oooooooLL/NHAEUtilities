package com.github.nhaeutilities.modules.patterngenerator.filter;

import net.minecraft.item.ItemStack;

import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;

/**
 * ??????????????????????????????
 */
public class InputOreDictFilter implements IRecipeFilter {

    private final String matchSource;
    private final ExplicitStackMatcher matcher;

    public InputOreDictFilter(String matchSource) {
        this(matchSource, new ExplicitStackMatcher.StackMatchCache());
    }

    InputOreDictFilter(String matchSource, ExplicitStackMatcher.StackMatchCache stackMatchCache) {
        this.matchSource = matchSource;
        this.matcher = new ExplicitStackMatcher(matchSource, stackMatchCache);
    }

    @Override
    public boolean matches(RecipeEntry recipe) {
        if (matcher.isDisabled()) {
            return true;
        }

        for (ItemStack input : recipe.inputs) {
            if (input != null && matcher.matches(input)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "input filter " + matchSource;
    }

    public String getRegexPattern() {
        return matchSource;
    }
}

