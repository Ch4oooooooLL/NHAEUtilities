package com.github.nhaeutilities.modules.patterngenerator.storage;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Collections;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.BeforeClass;
import org.junit.Test;

public class PatternStagingGroupTest {

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

    @Test
    public void fromNbtRestoresPatternWhenVanillaItemIdIsInvalid() {
        ItemStack pattern = new ItemStack(TestItemRegistry.getOrCreatePatternItem(), 1, 0);
        NBTTagCompound display = new NBTTagCompound();
        display.setString("Name", "Staged Pattern");
        NBTTagCompound stackTag = new NBTTagCompound();
        stackTag.setTag("display", display);
        pattern.setTagCompound(stackTag);

        PatternStagingGroup group = new PatternStagingGroup(
            "recipe-a|circuit-a|manual-a",
            "recipe-a",
            "circuit-a",
            "manual-a",
            10L,
            Collections.singletonList(pattern));

        NBTTagCompound groupTag = group.toNbt();
        NBTTagList patterns = groupTag.getTagList("Patterns", 10);
        patterns.getCompoundTagAt(0)
            .setShort("id", (short) 0);

        PatternStagingGroup restored = PatternStagingGroup.fromNbt(groupTag);

        assertEquals(1, restored.patterns.size());
        assertNotNull(
            restored.patterns.get(0)
                .getItem());
        assertEquals(
            "Staged Pattern",
            restored.patterns.get(0)
                .getDisplayName());
    }
}
