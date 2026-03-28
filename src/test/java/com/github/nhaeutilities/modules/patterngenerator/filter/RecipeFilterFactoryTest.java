package com.github.nhaeutilities.modules.patterngenerator.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

public class RecipeFilterFactoryTest {

    @Test
    public void buildAddsEnabledFiltersInPriorityOrder() {
        CompositeFilter filter = RecipeFilterFactory.build(
            "(dustCopper)",
            "(ingotCopper)",
            "{NC Catalyst}",
            "{Bad Input}",
            "{Bad Output}",
            4);

        List<IRecipeFilter> filters = filter.getFilters();
        assertEquals(6, filters.size());
        assertEquals(TierFilter.class, filters.get(0).getClass());
        assertEquals(BlacklistFilter.class, filters.get(1).getClass());
        assertEquals(BlacklistFilter.class, filters.get(2).getClass());
        assertEquals(NCItemFilter.class, filters.get(3).getClass());
        assertEquals(OutputOreDictFilter.class, filters.get(4).getClass());
        assertEquals(InputOreDictFilter.class, filters.get(5).getClass());
    }

    @Test
    public void buildSkipsDisabledFilters() {
        CompositeFilter filter = RecipeFilterFactory.build("*", "", null, "*", "*", -1);

        assertTrue(filter.getFilters().isEmpty());
    }
}
