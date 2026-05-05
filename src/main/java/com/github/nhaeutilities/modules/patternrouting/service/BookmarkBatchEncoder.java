package com.github.nhaeutilities.modules.patternrouting.service;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraftforge.fluids.FluidStack;

import com.github.nhaeutilities.modules.patterngenerator.encoder.PatternEncoder;
import com.github.nhaeutilities.modules.patterngenerator.recipe.RecipeEntry;
import com.github.nhaeutilities.modules.patterngenerator.storage.PatternStagingStorage;
import com.github.nhaeutilities.modules.patterngenerator.util.AE2Util;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRouterService;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingKeys;
import com.github.nhaeutilities.modules.patternrouting.core.PatternRoutingNbt;

import appeng.api.AEApi;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.PlayerSource;

public final class BookmarkBatchEncoder {

    private BookmarkBatchEncoder() {}

    public static BatchEncodeResult encodeBatch(EntityPlayerMP player, List<ItemStack> bookmarkItems) {
        FilterRuleConfig ruleConfig = FilterRuleConfig.load();
        List<FilterRule> rules = ruleConfig.getRules();
        RecipeOutputIndex.IndexResult index = RecipeOutputIndex.buildIndex(bookmarkItems, rules);

        IGridNode gridNode = resolveGridNode(player);
        if (gridNode == null) {
            player.addChatMessage(
                new ChatComponentText(
                    EnumChatFormatting.RED + "[NHAEUtilities] "
                        + net.minecraft.util.StatCollector
                            .translateToLocal("nhaeutilities.msg.batch_encode.no_wireless_terminal")));
            return new BatchEncodeResult(0, 0, bookmarkItems.size(), 0);
        }

        int encoded = 0;
        int skippedMultiSource = 0;
        int skippedNoRecipe = 0;
        int failedBlank = 0;
        List<ItemStack> encodedItems = new ArrayList<>();

        for (ItemStack bookmarkItem : bookmarkItems) {
            if (bookmarkItem == null || bookmarkItem.getItem() == null) {
                skippedNoRecipe++;
                continue;
            }

            RecipeOutputIndex.RecipeInfo recipeInfo = index.getUniqueRecipe(bookmarkItem);
            if (recipeInfo == null) {
                if (index.recipeCounts.containsKey(RecipeOutputIndex.itemMatchKey(bookmarkItem))) {
                    skippedMultiSource++;
                } else {
                    skippedNoRecipe++;
                }
                continue;
            }

            RecipeEntry entry = toRecipeEntry(recipeInfo);
            ItemStack pattern = PatternEncoder.encode(entry);
            if (pattern == null) {
                skippedNoRecipe++;
                continue;
            }

            boolean consumed = AE2Util.tryWirelessConsume(player, 1, new ItemStack(pattern.getItem(), 1, 0));
            if (!consumed) {
                failedBlank++;
                continue;
            }

            PatternRoutingNbt.RoutingMetadata metadata = buildMetadata(recipeInfo);
            PatternRoutingNbt.RoutingMetadata derived = PatternRoutingNbt.withDerivedDescriptor(metadata);
            PatternRoutingNbt.writeRoutingData(pattern, derived);

            PatternRouterService.RouteResult routeResult = PatternRouterService
                .tryRoute(pattern, gridNode, new PlayerSource(player, null));

            if (routeResult.status != PatternRouterService.RouteStatus.ROUTED) {
                PatternStagingStorage.append(player.getUniqueID(), pattern, System.currentTimeMillis());
            }

            encoded++;
            encodedItems.add(bookmarkItem);
        }

        return new BatchEncodeResult(encoded, skippedMultiSource, skippedNoRecipe, failedBlank, encodedItems);
    }

    private static IGridNode resolveGridNode(EntityPlayerMP player) {
        ItemStack heldItem = player.getHeldItem();
        if (heldItem == null || !(heldItem.getItem() instanceof IWirelessTermHandler)) {
            return null;
        }

        try {
            IWirelessTermHandler handler = (IWirelessTermHandler) heldItem.getItem();
            String key = handler.getEncryptionKey(heldItem);
            if (key == null || key.isEmpty()) return null;

            long serial = Long.parseLong(key);
            Object obj = AEApi.instance()
                .registries()
                .locatable()
                .getLocatableBy(serial);
            if (obj instanceof IActionHost) {
                IGrid grid = ((IActionHost) obj).getActionableNode()
                    .getGrid();
                if (grid != null) {
                    for (IGridNode node : grid.getNodes()) {
                        if (node.getMachine() instanceof IActionHost) {
                            return node;
                        }
                    }
                    if (grid.getPivot() != null) {
                        return grid.getPivot();
                    }
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    private static PatternRoutingNbt.RoutingMetadata buildMetadata(RecipeOutputIndex.RecipeInfo info) {
        String recipeCategory = info.recipeMapId;

        String programmingCircuit = "";
        String nonConsumablesJson = "[]";

        Object specialItems = info.recipe.mSpecialItems;
        if (specialItems != null) {
            ItemStack[] specials;
            if (specialItems instanceof ItemStack[]) {
                specials = (ItemStack[]) specialItems;
            } else if (specialItems instanceof ItemStack) {
                specials = new ItemStack[] { (ItemStack) specialItems };
            } else {
                specials = new ItemStack[0];
            }

            List<ItemStack> circuits = new ArrayList<>();
            List<ItemStack> ncItems = new ArrayList<>();

            try {
                java.lang.reflect.Method circuitMethod = null;
                try {
                    Class<?> gtUtility = Class.forName("gregtech.api.util.GTUtility");
                    circuitMethod = gtUtility.getMethod("isAnyIntegratedCircuit", ItemStack.class);
                } catch (Exception ignored) {}

                for (ItemStack sp : specials) {
                    if (sp == null || sp.getItem() == null) continue;
                    boolean isCircuit = false;
                    if (circuitMethod != null) {
                        try {
                            isCircuit = Boolean.TRUE.equals(circuitMethod.invoke(null, sp));
                        } catch (Exception ignored) {}
                    }
                    if (isCircuit) {
                        circuits.add(sp.copy());
                    } else {
                        ncItems.add(sp.copy());
                    }
                }
            } catch (Throwable ignored) {}

            if (!circuits.isEmpty()) {
                programmingCircuit = PatternRoutingNbt.itemSignature(circuits.get(0));
            }
            if (!ncItems.isEmpty()) {
                nonConsumablesJson = toJsonArray(ncItems);
            }
        }

        return new PatternRoutingNbt.RoutingMetadata(
            PatternRoutingKeys.CURRENT_VERSION,
            recipeCategory,
            "",
            "",
            "",
            "",
            PatternRoutingKeys.SOURCE_GENERATOR,
            false,
            programmingCircuit,
            nonConsumablesJson,
            "{}");
    }

    private static String toJsonArray(List<ItemStack> stacks) {
        if (stacks == null || stacks.isEmpty()) return "[]";
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (ItemStack stack : stacks) {
            if (stack == null) continue;
            if (!first) sb.append(",");
            first = false;
            sb.append("{\"item\":\"")
                .append(escapeJson(PatternRoutingNbt.itemSignature(stack)))
                .append("\",\"count\":")
                .append(stack.stackSize)
                .append(",\"nc\":true}");
        }
        sb.append("]");
        return sb.toString();
    }

    private static String escapeJson(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"");
    }

    private static RecipeEntry toRecipeEntry(RecipeOutputIndex.RecipeInfo info) {
        ItemStack[] specials;
        Object specialItems = info.recipe.mSpecialItems;
        if (specialItems instanceof ItemStack[]) {
            specials = (ItemStack[]) specialItems;
        } else if (specialItems instanceof ItemStack) {
            specials = new ItemStack[] { (ItemStack) specialItems };
        } else {
            specials = new ItemStack[0];
        }

        return new RecipeEntry(
            "gt",
            info.recipeMapId,
            info.recipeMapId,
            info.recipe.mInputs != null ? info.recipe.mInputs : new ItemStack[0],
            info.recipe.mOutputs != null ? info.recipe.mOutputs : new ItemStack[0],
            info.recipe.mFluidInputs != null ? info.recipe.mFluidInputs : new FluidStack[0],
            info.recipe.mFluidOutputs != null ? info.recipe.mFluidOutputs : new FluidStack[0],
            specials,
            info.recipe.mDuration,
            info.recipe.mEUt);
    }

    public static final class BatchEncodeResult {

        public final int encoded;
        public final int skippedMultiSource;
        public final int skippedNoRecipe;
        public final int failedBlank;
        public final List<ItemStack> encodedItems;

        BatchEncodeResult(int encoded, int skippedMultiSource, int skippedNoRecipe, int failedBlank) {
            this(encoded, skippedMultiSource, skippedNoRecipe, failedBlank, new ArrayList<ItemStack>());
        }

        BatchEncodeResult(int encoded, int skippedMultiSource, int skippedNoRecipe, int failedBlank,
            List<ItemStack> encodedItems) {
            this.encoded = encoded;
            this.skippedMultiSource = skippedMultiSource;
            this.skippedNoRecipe = skippedNoRecipe;
            this.failedBlank = failedBlank;
            this.encodedItems = encodedItems;
        }
    }
}
