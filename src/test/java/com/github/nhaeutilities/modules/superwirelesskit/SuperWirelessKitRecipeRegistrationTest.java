package com.github.nhaeutilities.modules.superwirelesskit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.junit.After;
import org.junit.Test;

import com.github.nhaeutilities.core.config.CoreConfig;
import com.github.nhaeutilities.proxy.CommonProxy;

public class SuperWirelessKitRecipeRegistrationTest {

    @After
    public void tearDown() {
        com.github.nhaeutilities.modules.superwirelesskit.item.ModItems.itemSuperWirelessKit = null;
    }

    @Test
    public void moduleInitRegistersSuperWirelessKitRecipeThroughProxy() {
        RecordingProxy proxy = new RecordingProxy();
        SuperWirelessKitModule module = new SuperWirelessKitModule(new CoreConfig(true, true, true), proxy);

        module.init(null, new Object());

        assertTrue(proxy.integrationRegistered);
    }

    @Test
    public void proxyRegistersRecipeUsingAe2StuffAdvancedWirelessKit() {
        RecordingProxy proxy = new RecordingProxy();
        Item outputItem = new Item();
        Item ingredientItem = new Item();
        com.github.nhaeutilities.modules.superwirelesskit.item.ModItems.itemSuperWirelessKit = outputItem;
        proxy.itemToFind = ingredientItem;

        proxy.registerSuperWirelessKitIntegration();

        assertEquals("ae2stuff", proxy.lastFindItemModId);
        assertEquals("AdvWirelessKit", proxy.lastFindItemName);
        assertNotNull(proxy.recipeOutput);
        assertSame(outputItem, proxy.recipeOutput.getItem());
        assertEquals(1, proxy.recipeOutput.stackSize);
        assertNotNull(proxy.recipeInputs);
        assertEquals("AAA", proxy.recipeInputs[0]);
        assertEquals("AAA", proxy.recipeInputs[1]);
        assertEquals("AAA", proxy.recipeInputs[2]);
        assertEquals(Character.valueOf('A'), proxy.recipeInputs[3]);
        assertTrue(proxy.recipeInputs[4] instanceof ItemStack);
        assertSame(ingredientItem, ((ItemStack) proxy.recipeInputs[4]).getItem());
        assertEquals(1, ((ItemStack) proxy.recipeInputs[4]).stackSize);
    }

    private static final class RecordingProxy extends CommonProxy {

        private boolean integrationRegistered;
        private Item itemToFind;
        private String lastFindItemModId;
        private String lastFindItemName;
        private ItemStack recipeOutput;
        private Object[] recipeInputs;

        @Override
        public void registerSuperWirelessKitIntegration() {
            integrationRegistered = true;
            super.registerSuperWirelessKitIntegration();
        }

        @Override
        protected Item findItem(String modId, String itemName) {
            lastFindItemModId = modId;
            lastFindItemName = itemName;
            return itemToFind;
        }

        @Override
        protected void addShapedRecipe(ItemStack output, Object... inputs) {
            recipeOutput = output;
            recipeInputs = inputs;
        }
    }
}
