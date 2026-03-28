package com.github.nhaeutilities.modules.patterngenerator.encoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

public class OreDictReplacerTest {

    @After
    public void tearDown() {
        OreDictReplacer.resetOreDictionaryAccess();
    }

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
    public void constructorKeepsOnlyWellFormedReplacementRules() throws Exception {
        OreDictReplacer replacer = new OreDictReplacer(
            " ingotCopper = dustCopper ; broken ; =missing ; dustTin=dustSmallTin ");

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

    @Test
    public void applyReplacesMatchingOreDictItemsAndPreservesStackSize() {
        String sourceOre = "task4_source_" + System.nanoTime();
        String targetOre = "task4_target_" + System.nanoTime();
        Item sourceItem = new Item();
        Item targetItem = new Item();
        ItemStack sourceStack = new ItemStack(sourceItem, 7, 0);
        ItemStack targetStack = new ItemStack(targetItem, 1, 0);
        FakeOreDictionaryAccess oreDictionaryAccess = new FakeOreDictionaryAccess();
        oreDictionaryAccess.registerSource(sourceStack, sourceOre);
        oreDictionaryAccess.registerTarget(targetOre, targetStack.copy());
        OreDictReplacer.setOreDictionaryAccess(oreDictionaryAccess);
        OreDictReplacer replacer = new OreDictReplacer(sourceOre + "=" + targetOre);

        ItemStack[] replaced = replacer.apply(new ItemStack[] { sourceStack.copy() });

        assertSame(targetItem, replaced[0].getItem());
        assertEquals(7, replaced[0].stackSize);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> readRules(OreDictReplacer replacer) throws Exception {
        Field field = OreDictReplacer.class.getDeclaredField("rules");
        field.setAccessible(true);
        return (Map<String, String>) field.get(replacer);
    }

    private static final class FakeOreDictionaryAccess implements OreDictReplacer.OreDictionaryAccess {

        private final Map<String, List<ItemStack>> oresByName = new LinkedHashMap<String, List<ItemStack>>();
        private final Map<String, String[]> oreNamesByStack = new LinkedHashMap<String, String[]>();

        void registerSource(ItemStack stack, String oreName) {
            oreNamesByStack.put(stackKey(stack), new String[] { oreName });
        }

        void registerTarget(String oreName, ItemStack stack) {
            List<ItemStack> ores = new ArrayList<ItemStack>();
            ores.add(stack);
            oresByName.put(oreName, ores);
        }

        @Override
        public String[] getOreNames(ItemStack stack) {
            String[] oreNames = oreNamesByStack.get(stackKey(stack));
            return oreNames != null ? oreNames : new String[0];
        }

        @Override
        public List<ItemStack> getOres(String oreName) {
            List<ItemStack> ores = oresByName.get(oreName);
            return ores != null ? ores : new ArrayList<ItemStack>();
        }

        private String stackKey(ItemStack stack) {
            return stack.getItem() + "@" + stack.getItemDamage();
        }
    }
}
