package com.github.nhaeutilities.item;

import net.minecraft.item.ItemEditableBook;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.StatCollector;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemBookTutorial extends ItemEditableBook {

    @SideOnly(Side.CLIENT)
    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (stack.hasTagCompound()) {
            NBTTagCompound nbt = stack.getTagCompound();
            String title = nbt.getString("title");
            if (title != null && !title.isEmpty()) {
                return StatCollector.translateToLocal(title);
            }
        }
        return super.getItemStackDisplayName(stack);
    }
}
