package com.github.nhaeutilities.modules.patterngenerator.filter;

import java.util.ArrayList;
import java.util.List;

import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;

/**
 * ?????????????????????????????(AND)
 */
public class CompositeFilter implements IRecipeFilter {

    private final List<IRecipeFilter> filters = new ArrayList<>();

    public CompositeFilter() {}

    public void addFilter(IRecipeFilter filter) {
        if (filter != null) {
            filters.add(filter);
        }
    }

    public void clearFilters() {
        filters.clear();
    }

    public List<IRecipeFilter> getFilters() {
        return filters;
    }

    @Override
    public boolean matches(RecipeEntry recipe) {
        for (IRecipeFilter filter : filters) {
            if (!filter.matches(recipe)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String getDescription() {
        if (filters.isEmpty()) return "no filters";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < filters.size(); i++) {
            if (i > 0) sb.append(" AND ");
            sb.append(
                filters.get(i)
                    .getDescription());
        }
        return sb.toString();
    }
}

