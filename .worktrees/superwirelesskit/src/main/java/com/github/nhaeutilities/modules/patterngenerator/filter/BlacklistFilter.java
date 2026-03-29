package com.github.nhaeutilities.modules.patterngenerator.filter;

import net.minecraft.item.ItemStack;

import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;

/**
 * ????????? ????????????????????????
 */
public class BlacklistFilter implements IRecipeFilter {

    private final String keyword;
    private final boolean checkInputs;
    private final boolean checkOutputs;
    private final ExplicitStackMatcher matcher;

    public BlacklistFilter(String keyword, boolean checkInputs, boolean checkOutputs) {
        this(keyword, checkInputs, checkOutputs, new ExplicitStackMatcher.StackMatchCache());
    }

    BlacklistFilter(String keyword, boolean checkInputs, boolean checkOutputs,
        ExplicitStackMatcher.StackMatchCache stackMatchCache) {
        this.keyword = keyword;
        this.checkInputs = checkInputs;
        this.checkOutputs = checkOutputs;
        this.matcher = new ExplicitStackMatcher(keyword, stackMatchCache);
    }

    @Override
    public boolean matches(RecipeEntry recipe) {
        if (matcher.isDisabled()) {
            return true;
        }

        if (checkInputs && containsMatch(recipe.inputs)) {
            return false;
        }

        if (checkOutputs && containsMatch(recipe.outputs)) {
            return false;
        }

        return true;
    }

    private boolean containsMatch(ItemStack[] stacks) {
        if (stacks == null || stacks.length == 0) {
            return false;
        }

        for (ItemStack stack : stacks) {
            if (matcher.matches(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getDescription() {
        return "blacklist(" + (checkInputs ? "in" : "") + (checkOutputs ? "out" : "") + "): " + keyword;
    }
}
