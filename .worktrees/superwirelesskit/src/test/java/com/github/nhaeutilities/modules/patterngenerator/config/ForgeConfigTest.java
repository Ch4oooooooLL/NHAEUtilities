package com.github.nhaeutilities.modules.patterngenerator.config;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class ForgeConfigTest {

    @Test
    public void conflictBatchSizeUsesExpectedDefault() {
        assertEquals(6, ForgeConfig.DEFAULT_CONFLICT_BATCH_SIZE);
    }

    @Test
    public void storageDirectoryUsesExpectedDefault() {
        assertEquals("nhaeutilities", ForgeConfig.getStorageDirectoryName());
    }

    @Test
    public void recipeCacheDirectoryUsesExpectedDefault() {
        assertEquals("recipe_cache", ForgeConfig.getRecipeCacheDirectoryName());
    }

    @Test
    public void defaultConfigFileUsesExpectedPathAndName() {
        assertEquals(
            new File("config", "nhaeutilities.cfg").getPath(),
            ForgeConfig.resolveConfigFile(null)
                .getPath());
    }

    @Test
    public void normalizeConflictBatchSizeClampsOutOfRangeValues() {
        assertEquals(1, ForgeConfig.normalizeConflictBatchSize(0));
        assertEquals(1, ForgeConfig.normalizeConflictBatchSize(-5));
        assertEquals(64, ForgeConfig.normalizeConflictBatchSize(999));
    }

    @Test
    public void normalizeConflictBatchSizeKeepsValidValue() {
        assertEquals(12, ForgeConfig.normalizeConflictBatchSize(12));
    }
}
