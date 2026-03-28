package com.github.nhaeutilities.modules.patterngenerator.encoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.junit.BeforeClass;
import org.junit.Test;

import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

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
    public void appendStackTagPreservesLargeStackCountsInPatternNbt() {
        NBTTagList inputList = new NBTTagList();
        invokeAppendStackTag(inputList, new ItemStack(Items.iron_ingot, 300, 0));

        assertEquals(1, inputList.tagCount());
        NBTTagCompound inputTag = inputList.getCompoundTagAt(0);
        assertEquals(300, inputTag.getInteger("Count"));
        assertEquals(300L, inputTag.getLong("Cnt"));
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
