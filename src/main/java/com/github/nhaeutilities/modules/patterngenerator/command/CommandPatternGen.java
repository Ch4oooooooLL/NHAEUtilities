package com.github.nhaeutilities.modules.patterngenerator.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.nhaeutilities.modules.patterngenerator.encoder.PatternEncoder;
import com.github.nhaeutilities.modules.patterngenerator.filter.CompositeFilter;
import com.github.nhaeutilities.modules.patterngenerator.filter.RecipeFilterFactory;
import com.github.nhaeutilities.modules.patterngenerator.recipe.GTRecipeSource;
import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;
import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;
import com.github.nhaeutilities.modules.patterngenerator.util.InventoryUtil;

/**
 * /patterngen command support kept in the migrated workflow.
 */
public class CommandPatternGen extends CommandBase {

    @Override
    public String getCommandName() {
        return "patterngen";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/patterngen <list|generate|count> [recipeMapId] [outputFilter] [inputFilter] [ncFilter] [blacklistInput] [blacklistOutput]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length == 0) {
            sendHelp(sender);
            return;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "list":
                handleList(sender);
                break;
            case "generate":
                handleGenerate(sender, args);
                break;
            case "count":
                handleCount(sender, args);
                break;
            default:
                sendHelp(sender);
                break;
        }
    }

    private void sendHelp(ICommandSender sender) {
        send(sender, EnumChatFormatting.GOLD, "nhaeutilities.command.help.title");
        send(sender, EnumChatFormatting.YELLOW, "nhaeutilities.command.help.list");
        send(sender, EnumChatFormatting.YELLOW, "nhaeutilities.command.help.count");
        send(sender, EnumChatFormatting.YELLOW, "nhaeutilities.command.help.generate");
    }

    private void handleList(ICommandSender sender) {
        Map<String, String> maps = GTRecipeSource.getAvailableRecipeMaps();
        send(sender, EnumChatFormatting.GOLD, "nhaeutilities.command.list.available_maps", maps.size());

        for (Map.Entry<String, String> entry : maps.entrySet()) {
            send(
                sender,
                EnumChatFormatting.GREEN,
                "nhaeutilities.command.list.entry",
                entry.getKey(),
                entry.getValue());
        }
    }

    private void handleGenerate(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, EnumChatFormatting.RED, "nhaeutilities.command.generate.usage");
            return;
        }

        if (!(sender instanceof EntityPlayerMP)) {
            send(sender, EnumChatFormatting.RED, "nhaeutilities.command.only_player");
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender;
        List<RecipeEntry> filtered = collectAndFilter(sender, args);

        if (filtered.isEmpty()) {
            send(sender, EnumChatFormatting.YELLOW, "nhaeutilities.command.no_matching_recipe");
            return;
        }

        List<ItemStack> patterns = PatternEncoder.encodeBatch(filtered);

        int requiredCount = patterns.size();
        ItemStack blankPattern = InventoryUtil.getBlankPattern();

        if (!InventoryUtil.consumeItem(player, blankPattern, requiredCount)) {
            int currentHas = InventoryUtil.countItem(player, blankPattern);
            send(sender, EnumChatFormatting.RED, "nhaeutilities.command.generate.insufficient_blank_pattern");
            send(
                sender,
                EnumChatFormatting.RED,
                "nhaeutilities.command.generate.required_vs_owned",
                requiredCount,
                currentHas);
            return;
        }

        send(sender, EnumChatFormatting.GREEN, "nhaeutilities.command.generate.start", patterns.size());

        int givenToInventory = 0;
        int droppedOnGround = 0;

        for (ItemStack pattern : patterns) {
            if (!player.inventory.addItemStackToInventory(pattern)) {
                EntityItem entityItem = player.entityDropItem(pattern, 0.5F);
                if (entityItem != null) {
                    entityItem.delayBeforeCanPickup = 0;
                }
                droppedOnGround++;
            } else {
                givenToInventory++;
            }
        }

        player.inventoryContainer.detectAndSendChanges();

        send(
            sender,
            EnumChatFormatting.GREEN,
            "nhaeutilities.command.generate.done",
            givenToInventory,
            droppedOnGround);
    }

    private void handleCount(ICommandSender sender, String[] args) {
        if (args.length < 2) {
            send(sender, EnumChatFormatting.RED, "nhaeutilities.command.count.usage");
            return;
        }

        List<RecipeEntry> filtered = collectAndFilter(sender, args);
        send(sender, EnumChatFormatting.GREEN, "nhaeutilities.command.count.result", filtered.size());
    }

    private List<RecipeEntry> collectAndFilter(ICommandSender sender, String[] args) {
        String recipeMapIdInput = args[1];
        String outputOreDict = args.length > 2 ? args[2] : null;
        String inputOreDict = args.length > 3 ? args[3] : null;
        String ncItem = args.length > 4 ? args[4] : null;

        List<String> matchedMaps = GTRecipeSource.findMatchingRecipeMaps(recipeMapIdInput);
        if (matchedMaps.isEmpty()) {
            send(sender, EnumChatFormatting.RED, "nhaeutilities.command.map_not_found", recipeMapIdInput);
            return new ArrayList<RecipeEntry>();
        }
        send(sender, EnumChatFormatting.GRAY, "nhaeutilities.command.matched_maps", String.join(", ", matchedMaps));

        List<RecipeEntry> recipes = GTRecipeSource.collectRecipes(recipeMapIdInput);
        int totalBefore = recipes.size();

        CompositeFilter filter = RecipeFilterFactory.build(
            outputOreDict,
            inputOreDict,
            ncItem,
            args.length > 5 ? args[5] : null,
            args.length > 6 ? args[6] : null,
            -1);

        List<RecipeEntry> filtered = new ArrayList<RecipeEntry>();
        for (RecipeEntry recipe : recipes) {
            if (filter.matches(recipe)) {
                filtered.add(recipe);
            }
        }

        send(sender, EnumChatFormatting.GRAY, "nhaeutilities.command.filter_result", totalBefore, filtered.size());
        return filtered;
    }

    private void send(ICommandSender sender, EnumChatFormatting color, String key, Object... args) {
        sender.addChatMessage(new ChatComponentText(color + I18nUtil.tr(key, args)));
    }
}
