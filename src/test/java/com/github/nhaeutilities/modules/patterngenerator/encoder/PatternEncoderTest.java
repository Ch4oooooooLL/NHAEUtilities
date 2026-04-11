package com.github.nhaeutilities.modules.patterngenerator.encoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;
import com.github.nhaeutilities.modules.patterngenerator.routing.PatternRoutingKeys;

public class PatternEncoderTest {

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
    public void encodeBuildsPatternNbtWithFullStackCounts() {
        final Item patternItem = new Item();
        PatternEncoder.setPatternItemResolver(() -> patternItem);
        RecipeEntry recipe = new RecipeEntry(
            "gt",
            "gt.recipe.assembler",
            "Assembler",
            new ItemStack[] { new ItemStack(Items.iron_ingot, 300, 0) },
            new ItemStack[] { new ItemStack(Items.gold_ingot, 2, 0) },
            new net.minecraftforge.fluids.FluidStack[0],
            new net.minecraftforge.fluids.FluidStack[0],
            new ItemStack[0],
            120,
            30,
            "generator-recipe-1");

        ItemStack encoded = PatternEncoder.encode(recipe);

        assertNotNull(encoded);
        assertEquals(patternItem, encoded.getItem());
        assertNotNull(encoded.getTagCompound());
        NBTTagList inputList = encoded.getTagCompound()
            .getTagList("in", 10);
        assertEquals(1, inputList.tagCount());
        NBTTagCompound inputTag = inputList.getCompoundTagAt(0);
        assertEquals(300, inputTag.getInteger("Count"));
        assertEquals(300L, inputTag.getLong("Cnt"));
        assertEquals(
            1,
            encoded.getTagCompound()
                .getTagList("out", 10)
                .tagCount());
        assertEquals(
            "generator-recipe-1",
            encoded.getTagCompound()
                .getCompoundTag(PatternRoutingKeys.ROOT_KEY)
                .getCompoundTag(PatternRoutingKeys.ROUTING_KEY)
                .getString(PatternRoutingKeys.RECIPE_ID_KEY));
        assertEquals(
            PatternRoutingKeys.SOURCE_GENERATOR,
            encoded.getTagCompound()
                .getCompoundTag(PatternRoutingKeys.ROOT_KEY)
                .getCompoundTag(PatternRoutingKeys.ROUTING_KEY)
                .getString(PatternRoutingKeys.SOURCE_KEY));
    }

    @Test
    public void appendStackTagPreservesLargeStackCountsInPatternNbt() {
        NBTTagList inputList = new NBTTagList();
        invokeAppendStackTag(inputList, new ItemStack(Items.iron_ingot, 300, 0));

        assertEquals(1, inputList.tagCount());
        NBTTagCompound inputTag = inputList.getCompoundTagAt(0);
        assertEquals(300, inputTag.getInteger("Count"));
        assertEquals(300L, inputTag.getLong("Cnt"));
    }

    @Test
    public void encodeWritesUniformRoutingNbtForEmptyRecipeId() {
        final Item patternItem = new Item();
        PatternEncoder.setPatternItemResolver(() -> patternItem);
        RecipeEntry recipe = new RecipeEntry(
            "gt",
            "gt.recipe.assembler",
            "Assembler",
            new ItemStack[] { new ItemStack(Items.iron_ingot, 1, 0) },
            new ItemStack[] { new ItemStack(Items.gold_ingot, 1, 0) },
            new net.minecraftforge.fluids.FluidStack[0],
            new net.minecraftforge.fluids.FluidStack[0],
            new ItemStack[0],
            120,
            30);

        ItemStack encoded = PatternEncoder.encode(recipe);

        assertNotNull(encoded);
        assertNotNull(encoded.getTagCompound());
        assertTrue(
            encoded.getTagCompound()
                .hasKey(PatternRoutingKeys.ROOT_KEY));
        assertTrue(
            encoded.getTagCompound()
                .getCompoundTag(PatternRoutingKeys.ROOT_KEY)
                .hasKey(PatternRoutingKeys.ROUTING_KEY));
        assertEquals(
            "",
            encoded.getTagCompound()
                .getCompoundTag(PatternRoutingKeys.ROOT_KEY)
                .getCompoundTag(PatternRoutingKeys.ROUTING_KEY)
                .getString(PatternRoutingKeys.RECIPE_ID_KEY));
        assertEquals(
            PatternRoutingKeys.SOURCE_GENERATOR,
            encoded.getTagCompound()
                .getCompoundTag(PatternRoutingKeys.ROOT_KEY)
                .getCompoundTag(PatternRoutingKeys.ROUTING_KEY)
                .getString(PatternRoutingKeys.SOURCE_KEY));
    }

    @After
    public void tearDown() {
        PatternEncoder.resetPatternItemResolver();
    }

    private static void invokeAppendStackTag(NBTTagList targetList, ItemStack stack) {
        try {
            Method method = PatternEncoder.class.getDeclaredMethod("appendStackTag", NBTTagList.class, ItemStack.class);
            method.setAccessible(true);
            method.invoke(null, targetList, stack);
        } catch (NoSuchMethodException e) {
            fail("Expected appendStackTag helper to exist");
        } catch (IllegalAccessException e) {
            fail("Unable to access appendStackTag helper: " + e.getMessage());
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            fail("appendStackTag helper threw unexpectedly: " + cause.getMessage());
        }
    }
}
