package com.github.nhaeutilities.modules.patterngenerator.network;

import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.nhaeutilities.modules.patterngenerator.encoder.PatternEncoder;
import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;
import com.github.nhaeutilities.modules.patterngenerator.storage.PatternStorage;
import com.github.nhaeutilities.modules.patterngenerator.util.AE2Util;
import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;
import com.github.nhaeutilities.modules.patterngenerator.util.InventoryUtil;

/**
 * Encodes patterns, consumes blank patterns, and persists generated output.
 */
public final class PatternGenerationService {

    private PatternGenerationService() {}

    public static boolean generateAndStore(EntityPlayerMP player, String source, List<RecipeEntry> recipes) {
        if (player == null || recipes == null || recipes.isEmpty()) {
            return false;
        }

        List<ItemStack> patterns = PatternEncoder.encodeBatch(recipes);
        if (patterns.isEmpty()) {
            send(player, EnumChatFormatting.YELLOW, "nhaeutilities.msg.pattern.no_valid_after_encode");
            return false;
        }

        int requiredCount = patterns.size();
        ItemStack blankPattern = InventoryUtil.getBlankPattern();
        if (!consumeBlankPatterns(player, requiredCount, blankPattern)) {
            return false;
        }

        UUID uuid = player.getUniqueID();
        if (!PatternStorage.save(uuid, patterns, source)) {
            send(player, EnumChatFormatting.RED, "nhaeutilities.msg.pattern.storage_write_failed");
            return false;
        }
        send(player, EnumChatFormatting.GREEN, "nhaeutilities.msg.pattern.generated_and_consumed", requiredCount);
        send(player, EnumChatFormatting.GRAY, "nhaeutilities.msg.pattern.stored_hint");
        return true;
    }

    private static boolean consumeBlankPatterns(EntityPlayerMP player, int requiredCount, ItemStack blankPattern) {
        if (AE2Util.tryWirelessConsume(player, requiredCount, blankPattern)) {
            return true;
        }

        if (!InventoryUtil.consumeItem(player, blankPattern, requiredCount)) {
            int currentHas = InventoryUtil.countItem(player, blankPattern);
            send(
                player,
                EnumChatFormatting.RED,
                "nhaeutilities.msg.pattern.insufficient_blank_pattern",
                requiredCount,
                currentHas);
            return false;
        }

        return true;
    }

    private static void send(EntityPlayerMP player, EnumChatFormatting color, String key, Object... args) {
        player.addChatMessage(new ChatComponentText(color + I18nUtil.tr(key, args)));
    }
}
