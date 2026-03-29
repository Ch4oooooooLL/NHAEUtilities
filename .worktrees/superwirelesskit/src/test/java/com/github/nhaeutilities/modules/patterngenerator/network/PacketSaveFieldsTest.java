package com.github.nhaeutilities.modules.patterngenerator.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import org.junit.Test;

public class PacketSaveFieldsTest {

    @Test
    public void doesNotMutateNonPatternGeneratorItems() {
        Item item = new Item().setUnlocalizedName("minecraft.test_item");
        ItemStack held = new ItemStack(item);

        boolean written = PacketSaveFields.writeFieldsIfPatternGenerator(
            held,
            new PacketSaveFields("map", "out", "in", "nc", "bin", "bout", "rep", 2));

        assertFalse(written);
        assertNull(held.getTagCompound());
    }

    @Test
    public void persistsFieldsForPatternGeneratorLikeItem() {
        Item item = new Item().setUnlocalizedName("nhaeutilities.pattern_generator");
        ItemStack held = new ItemStack(item);

        boolean written = PacketSaveFields.writeFieldsIfPatternGenerator(
            held,
            new PacketSaveFields("map", "out", "in", "nc", "bin", "bout", "rep", 2));

        assertTrue(written);
        NBTTagCompound tag = held.getTagCompound();
        assertNotNull(tag);
        assertEquals("map", tag.getString(PacketSaveFields.NBT_RECIPE_MAP));
        assertEquals("out", tag.getString(PacketSaveFields.NBT_OUTPUT_ORE));
        assertEquals("in", tag.getString(PacketSaveFields.NBT_INPUT_ORE));
        assertEquals("nc", tag.getString(PacketSaveFields.NBT_NC_ITEM));
        assertEquals("bin", tag.getString(PacketSaveFields.NBT_BLACKLIST_INPUT));
        assertEquals("bout", tag.getString(PacketSaveFields.NBT_BLACKLIST_OUTPUT));
        assertEquals("rep", tag.getString(PacketSaveFields.NBT_REPLACEMENTS));
        assertEquals(2, tag.getInteger(PacketSaveFields.NBT_TARGET_TIER));
    }
}
