package com.github.nhaeutilities.modules.patterngenerator.encoder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.Map;

import org.junit.Test;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

public class OreDictReplacerTest {

    @Test
    public void constructorKeepsOnlyWellFormedReplacementRules() throws Exception {
        OreDictReplacer replacer = new OreDictReplacer(" ingotCopper = dustCopper ; broken ; =missing ; dustTin=dustSmallTin ");

        Map<?, ?> rules = readRules(replacer);

        assertTrue(replacer.hasRules());
        assertTrue(rules.containsKey("ingotCopper"));
        assertTrue(rules.containsKey("dustTin"));
        assertFalse(rules.containsKey("broken"));
        assertFalse(rules.containsKey(""));
    }

    @Test
    public void applyReturnsOriginalArrayWhenNoRulesExist() {
        OreDictReplacer replacer = new OreDictReplacer("   ");
        ItemStack[] items = new ItemStack[] { new ItemStack(Items.iron_ingot, 2, 0) };

        assertFalse(replacer.hasRules());
        assertSame(items, replacer.apply(items));
    }

    @Test
    public void applyReturnsNullWhenItemsAreNull() {
        OreDictReplacer replacer = new OreDictReplacer("ingotCopper=dustCopper");

        assertNull(replacer.apply(null));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> readRules(OreDictReplacer replacer) throws Exception {
        Field field = OreDictReplacer.class.getDeclaredField("rules");
        field.setAccessible(true);
        return (Map<String, String>) field.get(replacer);
    }
}
