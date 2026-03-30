package com.github.nhaeutilities.modules.patterngenerator.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.github.nhaeutilities.modules.patterngenerator.storage.PatternStagingStorage;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class ItemPatternIndex extends Item {

    private static final int MAX_SUMMARY_GROUPS = 5;

    public ItemPatternIndex() {
        setUnlocalizedName("nhaeutilities.pattern_index");
        setMaxStackSize(1);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world == null || world.isRemote || player == null) {
            return stack;
        }

        List<String> lines = buildSummaryLines(PatternStagingStorage.getSummary(player.getUniqueID()), MAX_SUMMARY_GROUPS);
        for (String line : lines) {
            sendSummaryLine(player, line);
        }
        return stack;
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
    }

    static List<String> buildSummaryLines(PatternStagingStorage.StorageSummary summary, int maxGroups) {
        List<String> lines = new ArrayList<String>();
        if (summary == null || summary.isEmpty()) {
            lines.add("nhaeutilities.msg.pattern_index.empty");
            return lines;
        }

        lines.add(
            "nhaeutilities.msg.pattern_index.summary|" + summary.groupCount + "|" + summary.totalPatterns);

        int limit = Math.max(0, maxGroups);
        int count = 0;
        for (PatternStagingStorage.GroupSummary group : summary.groups) {
            if (count >= limit) {
                break;
            }
            lines.add(
                "nhaeutilities.msg.pattern_index.group_entry|" + group.groupKey + "|" + group.patternCount + "|"
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
}
