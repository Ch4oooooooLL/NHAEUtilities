package com.github.nhaeutilities.modules.patterngenerator.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;

import com.github.nhaeutilities.modules.patterngenerator.config.ReplacementConfig;
import com.github.nhaeutilities.modules.patterngenerator.encoder.OreDictReplacer;
import com.github.nhaeutilities.modules.patterngenerator.filter.CompositeFilter;
import com.github.nhaeutilities.modules.patterngenerator.filter.RecipeFilterFactory;
import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;
import com.github.nhaeutilities.modules.patterngenerator.storage.CacheQueryResult;
import com.github.nhaeutilities.modules.patterngenerator.storage.PatternStorage;
import com.github.nhaeutilities.modules.patterngenerator.storage.RecipeCacheService;
import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;
import com.github.nhaeutilities.modules.patterngenerator.util.ItemStackUtil;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Client -> Server: request pattern generation from cached recipes.
 */
public class PacketGeneratePatterns implements IMessage {

    private String recipeMapId;
    private String outputOreDict;
    private String inputOreDict;
    private String ncItem;
    private String blacklistInput;
    private String blacklistOutput;
    private String replacements;
    private String outputSlots;
    private int targetTier;

    public PacketGeneratePatterns() {}

    public PacketGeneratePatterns(String recipeMapId, String outputOreDict, String inputOreDict, String ncItem,
        String blacklistInput, String blacklistOutput, String replacements, String outputSlots, int targetTier) {
        this.recipeMapId = recipeMapId;
        this.outputOreDict = outputOreDict;
        this.inputOreDict = inputOreDict;
        this.ncItem = ncItem;
        this.blacklistInput = blacklistInput;
        this.blacklistOutput = blacklistOutput;
        this.replacements = replacements;
        this.outputSlots = outputSlots;
        this.targetTier = targetTier;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        recipeMapId = ByteBufUtils.readUTF8String(buf);
        outputOreDict = ByteBufUtils.readUTF8String(buf);
        inputOreDict = ByteBufUtils.readUTF8String(buf);
        ncItem = ByteBufUtils.readUTF8String(buf);
        blacklistInput = ByteBufUtils.readUTF8String(buf);
        blacklistOutput = ByteBufUtils.readUTF8String(buf);
        replacements = ByteBufUtils.readUTF8String(buf);
        outputSlots = ByteBufUtils.readUTF8String(buf);
        targetTier = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, recipeMapId != null ? recipeMapId : "");
        ByteBufUtils.writeUTF8String(buf, outputOreDict != null ? outputOreDict : "");
        ByteBufUtils.writeUTF8String(buf, inputOreDict != null ? inputOreDict : "");
        ByteBufUtils.writeUTF8String(buf, ncItem != null ? ncItem : "");
        ByteBufUtils.writeUTF8String(buf, blacklistInput != null ? blacklistInput : "");
        ByteBufUtils.writeUTF8String(buf, blacklistOutput != null ? blacklistOutput : "");
        ByteBufUtils.writeUTF8String(buf, replacements != null ? replacements : "");
        ByteBufUtils.writeUTF8String(buf, outputSlots != null ? outputSlots : "");
        buf.writeInt(targetTier);
    }

    public static class Handler implements IMessageHandler<PacketGeneratePatterns, IMessage> {

        @Override
        public IMessage onMessage(PacketGeneratePatterns message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            UUID uuid = player.getUniqueID();
            try {
                String requestFingerprint = PatternGenerationRequestGate.fingerprint(
                    message.recipeMapId,
                    message.outputOreDict,
                    message.inputOreDict,
                    message.ncItem,
                    message.blacklistInput,
                    message.blacklistOutput,
                    message.replacements,
                    message.outputSlots,
                    message.targetTier);
                if (!PatternGenerationRequestGate.shouldProcess(uuid, requestFingerprint, System.currentTimeMillis())) {
                    return null;
                }

                if (!PatternStorage.isEmpty(uuid)) {
                    PatternStorage.StorageSummary existing = PatternStorage.getSummary(uuid);
                    send(
                        player,
                        EnumChatFormatting.RED,
                        "nhaeutilities.msg.generate.storage_not_empty",
                        existing.count,
                        existing.source);
                    return null;
                }

                CompositeFilter filter = RecipeFilterFactory.build(
                    message.outputOreDict,
                    message.inputOreDict,
                    message.ncItem,
                    message.blacklistInput,
                    message.blacklistOutput,
                    message.targetTier);
                CacheQueryResult queryResult = RecipeCacheService.loadAndFilterRecipes(message.recipeMapId, filter);
                if (!queryResult.cacheValid) {
                    send(player, EnumChatFormatting.RED, "nhaeutilities.msg.cache.missing_or_invalid");
                    return null;
                }

                List<String> matchedMaps = queryResult.matchedMapIds;
                if (matchedMaps.isEmpty()) {
                    send(
                        player,
                        EnumChatFormatting.RED,
                        "nhaeutilities.msg.generate.no_matching_map",
                        message.recipeMapId);
                    return null;
                }

                send(
                    player,
                    EnumChatFormatting.GRAY,
                    "nhaeutilities.msg.generate.matched_maps",
                    String.join(", ", matchedMaps));

                List<RecipeEntry> filtered = new ArrayList<RecipeEntry>(queryResult.recipes);

                send(
                    player,
                    EnumChatFormatting.GRAY,
                    "nhaeutilities.msg.generate.filter_result",
                    queryResult.totalLoadedCount,
                    queryResult.totalFilteredCount);

                if (filtered.isEmpty()) {
                    send(player, EnumChatFormatting.YELLOW, "nhaeutilities.msg.generate.no_match_after_filter");
                    return null;
                }

                OutputSlotSelection outputSlotSelection;
                try {
                    outputSlotSelection = OutputSlotSelection.parse(message.outputSlots);
                } catch (OutputSlotSelection.OutputSlotSelectionException e) {
                    send(player, EnumChatFormatting.RED, e.getTranslationKey());
                    return null;
                }

                List<RecipeEntry> outputFiltered = new ArrayList<RecipeEntry>(filtered.size());
                try {
                    for (RecipeEntry recipe : filtered) {
                        outputFiltered.add(outputSlotSelection.apply(recipe));
                    }
                } catch (OutputSlotSelection.OutputSlotSelectionException e) {
                    send(player, EnumChatFormatting.RED, e.getTranslationKey());
                    return null;
                }
                filtered = outputFiltered;

                OreDictReplacer replacer = new OreDictReplacer(ReplacementConfig.getRulesString());
                if (replacer.hasRules()) {
                    List<RecipeEntry> replaced = new ArrayList<RecipeEntry>();
                    for (RecipeEntry recipe : filtered) {
                        replaced.add(
                            new RecipeEntry(
                                recipe.sourceType,
                                recipe.recipeMapId,
                                recipe.machineDisplayName,
                                replacer.apply(recipe.inputs),
                                replacer.apply(recipe.outputs),
                                recipe.fluidInputs,
                                recipe.fluidOutputs,
                                recipe.specialItems,
                                recipe.duration,
                                recipe.euPerTick));
                    }
                    filtered = replaced;
                    send(player, EnumChatFormatting.GRAY, "nhaeutilities.msg.generate.replacement_applied");
                }

                Map<String, List<RecipeEntry>> groups = new LinkedHashMap<String, List<RecipeEntry>>();
                for (RecipeEntry recipe : filtered) {
                    String key = I18nUtil.tr("nhaeutilities.msg.common.unknown_item");
                    if (recipe.outputs != null && recipe.outputs.length > 0 && recipe.outputs[0] != null) {
                        key = ItemStackUtil.getSafeDisplayName(recipe.outputs[0]);
                    } else if (recipe.fluidOutputs != null && recipe.fluidOutputs.length > 0
                        && recipe.fluidOutputs[0] != null) {
                            key = recipe.fluidOutputs[0].getLocalizedName();
                        }
                    List<RecipeEntry> recipes = groups.get(key);
                    if (recipes == null) {
                        recipes = new ArrayList<RecipeEntry>();
                        groups.put(key, recipes);
                    }
                    recipes.add(recipe);
                }

                List<RecipeEntry> nonConflicts = new ArrayList<RecipeEntry>();
                Map<String, List<RecipeEntry>> conflicts = new LinkedHashMap<String, List<RecipeEntry>>();
                for (Map.Entry<String, List<RecipeEntry>> entry : groups.entrySet()) {
                    if (entry.getValue()
                        .size() > 1) {
                        conflicts.put(entry.getKey(), entry.getValue());
                    } else {
                        nonConflicts.addAll(entry.getValue());
                    }
                }

                if (!conflicts.isEmpty()) {
                    if (ConflictSelectionPolicy.shouldAbortInteractiveSelection(filtered.size(), conflicts.size())) {
                        send(
                            player,
                            EnumChatFormatting.RED,
                            "nhaeutilities.msg.generate.conflicts_too_large",
                            filtered.size(),
                            conflicts.size(),
                            ConflictSelectionPolicy.getMaxInteractiveFilteredRecipes(),
                            ConflictSelectionPolicy.getMaxInteractiveConflictGroups());
                        return null;
                    }

                    ConflictSession.start(uuid, message.recipeMapId, nonConflicts, conflicts);
                    send(
                        player,
                        EnumChatFormatting.YELLOW,
                        "nhaeutilities.msg.generate.conflicts_detected",
                        conflicts.size());

                    ConflictSession session = ConflictSession.get(uuid);
                    ConflictResolutionService.sendCurrentBatch(player, session);
                    return null;
                }

                PatternGenerationService.generateAndStore(player, message.recipeMapId, filtered);
            } catch (RuntimeException e) {
                FMLLog.severe(
                    "[NHAEUtilities] Generation request failed for player %s: %s",
                    player != null ? player.getCommandSenderName() : "unknown",
                    e.getMessage());
                if (player != null) {
                    send(player, EnumChatFormatting.RED, "nhaeutilities.msg.generate.internal_error");
                }
            }
            return null;
        }

        private void send(EntityPlayerMP player, EnumChatFormatting color, String key, Object... args) {
            player.addChatMessage(new ChatComponentText(color + I18nUtil.tr(key, args)));
        }
    }
}
