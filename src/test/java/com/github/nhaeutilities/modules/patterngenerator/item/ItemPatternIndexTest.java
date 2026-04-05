package com.github.nhaeutilities.modules.patterngenerator.item;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.github.nhaeutilities.modules.patterngenerator.storage.PatternStagingStorage;

public class ItemPatternIndexTest {

    @Test
    public void buildSummaryLinesFormatsOverviewAndGroupPreviews() {
        PatternStagingStorage.StorageSummary summary = new PatternStagingStorage.StorageSummary(
            2,
            3,
            20L,
            Arrays.asList(
                new PatternStagingStorage.GroupSummary("recipe-a|circuit-a|manual-a", "recipe-a", "circuit-a",
                    "manual-a", 2, 20L, "Output B"),
                new PatternStagingStorage.GroupSummary("recipe-b||", "recipe-b", "", "", 1, 30L, "Output C")));

        List<String> lines = ItemPatternIndex.buildSummaryLines(summary, 5);

        assertEquals(3, lines.size());
        assertEquals("nhaeutilities.msg.pattern_index.summary|2|3", lines.get(0));
        assertEquals("nhaeutilities.msg.pattern_index.group_entry|recipe-a|circuit-a|manual-a|2|Output B", lines.get(1));
        assertEquals("nhaeutilities.msg.pattern_index.group_entry|recipe-b|||1|Output C", lines.get(2));
    }

    @Test
    public void buildSummaryLinesReturnsEmptyMessageWhenStorageIsEmpty() {
        List<String> lines = ItemPatternIndex.buildSummaryLines(PatternStagingStorage.StorageSummary.EMPTY, 5);

        assertEquals(1, lines.size());
        assertTrue(lines.get(0).startsWith("nhaeutilities.msg.pattern_index.empty"));
    }
}
