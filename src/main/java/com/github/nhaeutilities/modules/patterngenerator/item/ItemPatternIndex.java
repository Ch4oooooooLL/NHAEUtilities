package com.github.nhaeutilities.modules.patterngenerator.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.github.nhaeutilities.NHAEUtilities;
import com.github.nhaeutilities.modules.patterngenerator.storage.PatternStagingStorage;

import appeng.api.features.IWirelessTermHandler;
import appeng.api.util.IConfigManager;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemPatternIndex extends Item implements IWirelessTermHandler {

    public static final int GUI_ID_PATTERN_INDEX = 202;
    public static final int GUI_ID_STAGING_STORAGE = 203;

    @SideOnly(Side.CLIENT)
    private IIcon encodedPatternIcon;

    public ItemPatternIndex() {
        setUnlocalizedName("nhaeutilities.pattern_index");
        setMaxStackSize(1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        encodedPatternIcon = register.registerIcon("appliedenergistics2:ItemEncodedPattern");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return encodedPatternIcon;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int renderPass) {
        return 0x8888FF;
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            if (player.isSneaking()) {
                player.openGui(
                    NHAEUtilities.instance,
                    GUI_ID_STAGING_STORAGE,
                    world,
                    (int) player.posX,
                    (int) player.posY,
                    (int) player.posZ);
            } else {
                player.openGui(
                    NHAEUtilities.instance,
                    GUI_ID_PATTERN_INDEX,
                    world,
                    (int) player.posX,
                    (int) player.posY,
                    (int) player.posZ);
            }
        }
        return stack;
    }

    @Override
    public boolean doesSneakBypassUse(World world, int x, int y, int z, EntityPlayer player) {
        return true;
    }

    @Override
    @SideOnly(Side.CLIENT)
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        list.add(
            EnumChatFormatting.GRAY
                + StatCollector.translateToLocal("nhaeutilities.tooltip.pattern_index.enable_staging"));
        list.add(
            EnumChatFormatting.GRAY
                + StatCollector.translateToLocal("nhaeutilities.tooltip.pattern_index.player_bound"));
        list.add("");
        list.add(
            EnumChatFormatting.GRAY
                + StatCollector.translateToLocal("nhaeutilities.tooltip.pattern_index.right_click"));
        list.add(
            EnumChatFormatting.GRAY
                + StatCollector.translateToLocal("nhaeutilities.tooltip.pattern_index.shift_right_click"));
    }

    static List<String> buildSummaryLines(PatternStagingStorage.StorageSummary summary, int maxGroups) {
        List<String> lines = new ArrayList<String>();
        if (summary == null || summary.isEmpty()) {
            lines.add("nhaeutilities.msg.pattern_index.empty");
            return lines;
        }

        lines.add("nhaeutilities.msg.pattern_index.summary|" + summary.groupCount + "|" + summary.totalPatterns);

        int limit = Math.max(0, maxGroups);
        int count = 0;
        for (PatternStagingStorage.GroupSummary group : summary.groups) {
            if (count >= limit) {
                break;
            }
            lines.add(
                "nhaeutilities.msg.pattern_index.group_entry|" + group.groupKey
                    + "|"
                    + group.patternCount
                    + "|"
                    + group.preview);
            count++;
        }
        return lines;
    }

    private static void sendSummaryLine(EntityPlayer player, String encodedLine) {
        if (player == null || encodedLine == null || encodedLine.isEmpty()) {
            return;
        }

        String[] parts = encodedLine.split("\\|", -1);
        String key = parts[0];
        Object[] args = new Object[Math.max(0, parts.length - 1)];
        for (int i = 1; i < parts.length; i++) {
            args[i - 1] = parts[i];
        }
        player.addChatMessage(
            new ChatComponentText(EnumChatFormatting.GRAY + StatCollector.translateToLocalFormatted(key, args)));
    }

    @Override
    public String getEncryptionKey(ItemStack item) {
        if (item != null && item.hasTagCompound()) {
            return item.getTagCompound()
                .getString("encryptionKey");
        }
        return "";
    }

    @Override
    public void setEncryptionKey(ItemStack item, String encKey, String name) {
        if (item != null) {
            if (!item.hasTagCompound()) {
                item.setTagCompound(new NBTTagCompound());
            }
            item.getTagCompound()
                .setString("encryptionKey", encKey);
        }
    }

    @Override
    public boolean canHandle(ItemStack is) {
        return is != null && is.getItem() == this;
    }

    @Override
    public boolean usePower(EntityPlayer player, double amount, ItemStack is) {
        return true;
    }

    @Override
    public boolean hasPower(EntityPlayer player, double amount, ItemStack is) {
        return true;
    }

    @Override
    public IConfigManager getConfigManager(ItemStack is) {
        return null;
    }
}
