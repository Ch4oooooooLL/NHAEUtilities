package com.github.nhaeutilities.modules.patterngenerator.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingKeys;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingNbt;

public class PatternStagingStorageTest {

    @BeforeClass
    public static void initializeMinecraftBootstrap() {
        try {
            Class<?> bootstrap = Class.forName("net.minecraft.init.Bootstrap");
            try {
                bootstrap.getMethod("register")
                    .invoke(null);
                return;
            } catch (NoSuchMethodException ignored) {}

            bootstrap.getMethod("func_151354_b")
                .invoke(null);
        } catch (Exception ignored) {}
    }

    @After
    public void tearDown() {
        PatternStagingStorage.resetStorageRootForTests();
    }

    @Test
    public void appendCreatesGroupsByRecipeAndKeys() throws Exception {
        File tempRoot = Files.createTempDirectory("pattern-staging-test")
            .toFile();
        PatternStagingStorage.setStorageRootForTests(tempRoot);
        UUID playerId = UUID.randomUUID();

        assertTrue(
            PatternStagingStorage
                .append(playerId, stagedPattern("recipe-a", "circuit-a", "manual-a", "Output A"), 10L));
        assertTrue(
            PatternStagingStorage
                .append(playerId, stagedPattern("recipe-a", "circuit-a", "manual-a", "Output B"), 20L));
        assertTrue(PatternStagingStorage.append(playerId, stagedPattern("recipe-b", "", "", "Output C"), 30L));

        List<PatternStagingGroup> groups = PatternStagingStorage.loadGroups(playerId);

        assertEquals(2, groups.size());
        assertEquals("recipe-a|circuit-a|manual-a", groups.get(0).groupKey);
        assertEquals(2, groups.get(0).patterns.size());
        assertEquals("recipe-b||", groups.get(1).groupKey);
        assertEquals(1, groups.get(1).patterns.size());
        PatternStagingStorage.StorageSummary summary = PatternStagingStorage.getSummary(playerId);
        assertFalse(summary.isEmpty());
        assertEquals(2, summary.groups.size());
        assertEquals("recipe-a|circuit-a|manual-a", summary.groups.get(0).groupKey);
        assertEquals(2, summary.groups.get(0).patternCount);
        assertEquals(20L, summary.groups.get(0).updatedAt);
        assertEquals("Output B", summary.groups.get(0).preview);
        assertEquals("recipe-b||", summary.groups.get(1).groupKey);
        assertEquals(1, summary.groups.get(1).patternCount);
        assertEquals("Output C", summary.groups.get(1).preview);
    }

    @Test
    public void summaryIsEmptyWhenPlayerHasNoStagedPatterns() throws Exception {
        File tempRoot = Files.createTempDirectory("pattern-staging-empty")
            .toFile();
        PatternStagingStorage.setStorageRootForTests(tempRoot);

        assertTrue(
            PatternStagingStorage.getSummary(UUID.randomUUID())
                .isEmpty());
    }

    private static ItemStack stagedPattern(String recipeId, String circuitKey, String manualItemsKey, String label) {
        ItemStack stack = new ItemStack(TestItemRegistry.getOrCreatePatternItem(), 1, 0);
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", label);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag("display", display);
        stack.setTagCompound(tag);
        PatternRoutingNbt.writeRoutingData(
            stack,
            new PatternRoutingNbt.RoutingMetadata(
                1,
                recipeId,
                "",
                circuitKey,
                manualItemsKey,
                PatternRoutingKeys.SOURCE_NEI,
                false));
        return stack;
    }
}
