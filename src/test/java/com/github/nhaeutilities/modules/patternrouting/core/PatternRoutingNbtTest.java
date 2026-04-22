package com.github.nhaeutilities.modules.patternrouting.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class PatternRoutingNbtTest {

    @Test
    public void writeRoutingDataCreatesNamespacedCompoundAndRoundTrips() {
        ItemStack pattern = new ItemStack(new Item(), 1, 0);

        PatternRoutingNbt.writeRoutingData(
            pattern,
            new PatternRoutingNbt.RoutingMetadata(
                1,
                "{\"handlerName\":\"gt.recipe.assembler\"}",
                "gt.recipe.assembler|gregtech.machine.assembler||",
                "",
                "",
                PatternRoutingKeys.SOURCE_NEI,
                false));

        NBTTagCompound tag = pattern.getTagCompound();
        assertNotNull(tag);
        assertTrue(tag.hasKey(PatternRoutingKeys.ROOT_KEY));
        PatternRoutingNbt.RoutingMetadata metadata = PatternRoutingNbt.readRoutingData(pattern);
        assertEquals("{\"handlerName\":\"gt.recipe.assembler\"}", metadata.recipeId);
        assertEquals("gt.recipe.assembler|gregtech.machine.assembler||", metadata.assignmentKey);
        assertEquals(PatternRoutingKeys.SOURCE_NEI, metadata.source);
        assertFalse(metadata.hasDirectRoute);
    }

    @Test
    public void itemSignatureIncludesRegistryMetaAndNestedTag() {
        ItemStack stack = new ItemStack(new Item(), 4, 0);
        NBTTagCompound nested = new NBTTagCompound();
        nested.setString("foo", "bar");
        stack.setTagCompound(nested);

        String signature = PatternRoutingNbt.itemSignature(stack);

        assertFalse(signature.isEmpty());
        assertTrue(signature.contains("@0"));
        assertTrue(signature.contains("foo"));
    }

    @Test
    public void circuitAndManualKeysAreEmptySafeAndOrderStable() {
        ItemStack circuit = new ItemStack(new Item(), 1, 0);
        ItemStack first = new ItemStack(new Item(), 2, 0);
        ItemStack second = new ItemStack(new Item(), 1, 0);

        assertEquals("", PatternRoutingNbt.circuitKey(null));
        assertEquals("", PatternRoutingNbt.manualItemsKey(null));
        assertEquals(PatternRoutingNbt.itemSignature(circuit), PatternRoutingNbt.circuitKey(circuit));
        String manualKey = PatternRoutingNbt.manualItemsKey(new ItemStack[] { first, null, second });
        assertFalse(manualKey.isEmpty());
        assertTrue(manualKey.contains(PatternRoutingNbt.itemSignature(first)));
        assertTrue(manualKey.contains(PatternRoutingNbt.itemSignature(second)));
    }

    @Test
    public void manualItemsKeyFromJsonExcludesProgrammingCircuitSignature() {
        String programmingCircuit = "testmod:circuit@5";
        String nonConsumables = "[{\"item\":\"testmod:circuit@5\",\"count\":0,\"nc\":true},{\"item\":\"testmod:mold@0\",\"count\":0,\"nc\":true}]";

        String manualKey = PatternRoutingNbt.manualItemsKeyFromJson(nonConsumables, programmingCircuit);

        assertEquals("testmod:mold@0", manualKey);
    }
}
