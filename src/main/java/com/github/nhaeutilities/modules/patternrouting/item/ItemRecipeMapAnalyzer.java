package com.github.nhaeutilities.modules.patternrouting.item;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

import com.github.nhaeutilities.NHAEUtilities;
import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;
import com.github.nhaeutilities.modules.patternrouting.util.MachineRecipeMapResolver;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.RecipeMap;

public class ItemRecipeMapAnalyzer extends Item {

    public static final int GUI_ID_ANALYSIS = 201;
    public static final String NBT_RECIPE_MAP = "recipeMap";

    public ItemRecipeMapAnalyzer() {
        setUnlocalizedName("nhaeutilities.recipe_map_analyzer");
        setTextureName("nhaeutilities:recipe_map_analyzer");
        setMaxStackSize(1);
        setCreativeTab(CreativeTabs.tabTools);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!player.isSneaking() && !world.isRemote) {
            player.openGui(
                NHAEUtilities.instance,
                GUI_ID_ANALYSIS,
                world,
                (int) player.posX,
                (int) player.posY,
                (int) player.posZ);
        }
        return stack;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (!player.isSneaking() || world.isRemote) {
            return false;
        }

        TileEntity tileEntity = world.getTileEntity(x, y, z);
        if (tileEntity == null) {
            player.addChatMessage(msg(EnumChatFormatting.RED, "nhaeutilities.msg.item.block_not_detectable"));
            return true;
        }

        if (!(tileEntity instanceof IGregTechTileEntity)) {
            player.addChatMessage(msg(EnumChatFormatting.RED, "nhaeutilities.msg.item.machine_part_unsupported"));
            return true;
        }

        RecipeMap<?> recipeMap = resolveRecipeMap((IGregTechTileEntity) tileEntity);
        if (recipeMap != null) {
            saveRecipeMap(stack, recipeMap.unlocalizedName);
            player.addChatMessage(
                msg(EnumChatFormatting.GREEN, "nhaeutilities.msg.item.detected_recipe_map", recipeMap.unlocalizedName));
            return true;
        }

        player.addChatMessage(msg(EnumChatFormatting.RED, "nhaeutilities.msg.item.machine_part_unsupported"));
        return true;
    }

    @Override
    public boolean doesSneakBypassUse(World world, int x, int y, int z, EntityPlayer player) {
        return true;
    }

    private static RecipeMap<?> resolveRecipeMap(IGregTechTileEntity tileEntity) {
        if (tileEntity == null) {
            return null;
        }
        return MachineRecipeMapResolver.resolve(tileEntity.getMetaTileEntity());
    }

    public static String getStoredRecipeMap(ItemStack stack) {
        if (stack == null || !(stack.getItem() instanceof ItemRecipeMapAnalyzer)) {
            return "";
        }
        NBTTagCompound tag = stack.getTagCompound();
        if (tag == null) {
            return "";
        }
        String recipeMapId = tag.getString(NBT_RECIPE_MAP);
        return recipeMapId == null ? "" : recipeMapId.trim();
    }

    private static void saveRecipeMap(ItemStack stack, String recipeMapId) {
        if (stack == null) {
            return;
        }
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound()
            .setString(NBT_RECIPE_MAP, recipeMapId == null ? "" : recipeMapId);
    }

    private static ChatComponentText msg(EnumChatFormatting color, String key, Object... args) {
        return new ChatComponentText(color + I18nUtil.tr(key, args));
    }
}
