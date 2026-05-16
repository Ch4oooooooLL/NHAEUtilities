package com.github.nhaeutilities.modules.patternrouting.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.github.nhaeutilities.NHAEUtilities;
import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;
import com.github.nhaeutilities.modules.patternrouting.core.RecipeMapAnalysisResult;
import com.github.nhaeutilities.modules.patternrouting.item.ItemRecipeMapAnalyzer;
import com.github.nhaeutilities.modules.patternrouting.network.NetworkHandler;
import com.github.nhaeutilities.modules.patternrouting.network.PacketRequestRecipeMapAnalysis;
import com.github.nhaeutilities.modules.shared.NHTextures;

import codechicken.nei.LayoutManager;

public final class GuiPatternRoutingAnalysis {

    private static final int GUI_W = 260;
    private static final int GUI_H = 240;
    private static final String ERROR_NO_RECIPE_MAP = "nhaeutilities.gui.pattern_routing_analysis.error.no_recipe_map";

    private GuiPatternRoutingAnalysis() {}

    public static ModularPanel createWindow(EntityPlayer player) {
        ModularPanel panel = ModularPanel.defaultPanel("pattern_routing_analysis", GUI_W, GUI_H)
            .background(GuiTextures.MC_BACKGROUND);

        GuiPatternRoutingAnalysisState.Snapshot snapshot = GuiPatternRoutingAnalysisState.snapshot();

        panel.child(
            new TextWidget(
                IKey.str(t("nhaeutilities.gui.pattern_routing_analysis.title", "RecipeMap Analysis"))
                    .style(EnumChatFormatting.BOLD)).scale(1.2f)
                        .pos(8, 8)
                        .size(GUI_W - 16, 20));

        panel.child(
            new TextWidget(IKey.dynamic(() -> summaryText(GuiPatternRoutingAnalysisState.snapshot()))).pos(8, 28));

        panel.child(
            new TextWidget(IKey.dynamic(() -> statusMessage(GuiPatternRoutingAnalysisState.snapshot()))).pos(8, 42));

        ScrollWidget<?> scrollable = new ScrollWidget<>(new VerticalScrollData()).pos(8, 58)
            .size(GUI_W - 16, 142);

        int totalContentHeight = populateContent(scrollable, snapshot);
        scrollable.getScrollArea()
            .getScrollY()
            .setScrollSize(totalContentHeight);
        panel.child(scrollable);

        int buttonY = GUI_H - 30;
        int numButtons = 3;
        int buttonW = 64;
        int buttonH = 16;
        int gap = 4;
        int totalWidth = numButtons * buttonW + (numButtons - 1) * gap;
        int startX = (GUI_W - totalWidth) / 2;

        panel.child(
            NHTextures.createButton()
                .pos(startX, buttonY)
                .size(buttonW, buttonH)
                .overlay(
                    IKey.str(t("nhaeutilities.gui.pattern_routing_analysis.button.refresh", "Refresh"))
                        .shadow(false))
                .onMousePressed(mb -> {
                    requestAnalysis(player);
                    return true;
                }));

        panel.child(
            NHTextures.createButton()
                .pos(startX + buttonW + gap, buttonY)
                .size(buttonW, buttonH)
                .overlay(
                    IKey.str(t("nhaeutilities.gui.pattern_routing_analysis.button.add_bookmark", "Add Bookmark"))
                        .shadow(false))
                .onMousePressed(mb -> {
                    addBookmarkFromCurrentResult();
                    return true;
                }));

        panel.child(
            NHTextures.createButton()
                .pos(startX + 2 * (buttonW + gap), buttonY)
                .size(buttonW, buttonH)
                .overlay(
                    IKey.str(t("nhaeutilities.gui.common.close", "Close"))
                        .shadow(false))
                .onMousePressed(mb -> {
                    NHAEUtilities.proxy.closeCurrentScreen();
                    return true;
                }));

        return panel;
    }

    static void requestAnalysis(EntityPlayer player) {
        if (player == null) {
            prepareInitialState("");
            return;
        }
        prepareInitialStateAndRequest(
            readStoredRecipeMap(player),
            () -> NetworkHandler.sendToServer(new PacketRequestRecipeMapAnalysis()));
    }

    static void addBookmarkFromCurrentResult() {
        GuiPatternRoutingAnalysisState.Snapshot snapshot = GuiPatternRoutingAnalysisState.snapshot();
        if (snapshot == null || snapshot.result == null) {
            return;
        }

        List<ItemStack> allNcStacks = new ArrayList<ItemStack>();
        Set<String> seen = new HashSet<String>();
        for (RecipeMapAnalysisResult.RecipeTypeGroup group : snapshot.result.repeatedTypes) {
            for (ItemStack stack : group.ncItemStacks) {
                String sig = signatureKey(stack);
                if (seen.add(sig)) {
                    allNcStacks.add(stack.copy());
                }
            }
        }
        for (RecipeMapAnalysisResult.RecipeTypeGroup group : snapshot.result.singleOccurrenceTypes) {
            for (ItemStack stack : group.ncItemStacks) {
                String sig = signatureKey(stack);
                if (seen.add(sig)) {
                    allNcStacks.add(stack.copy());
                }
            }
        }

        if (allNcStacks.isEmpty()) {
            return;
        }

        try {
            if (LayoutManager.bookmarkPanel != null) {
                LayoutManager.bookmarkPanel
                    .addGroup(allNcStacks, codechicken.nei.BookmarkPanel.BookmarkViewMode.DEFAULT, false);
                LayoutManager.bookmarkPanel.save();
            }
        } catch (Exception ignored) {}
    }

    private static String signatureKey(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return "";
        }
        Object registryName = net.minecraft.item.Item.itemRegistry.getNameForObject(stack.getItem());
        return registryName + "@" + stack.getItemDamage();
    }

    private static void prepareInitialStateAndRequest(String recipeMapId, Runnable requestSender) {
        boolean hasRecipeMap = prepareInitialState(recipeMapId);
        if (requestSender != null && hasRecipeMap) {
            requestSender.run();
        }
    }

    private static boolean prepareInitialState(String recipeMapId) {
        String normalizedRecipeMapId = normalize(recipeMapId);
        if (normalizedRecipeMapId.isEmpty()) {
            GuiPatternRoutingAnalysisState.setError("", ERROR_NO_RECIPE_MAP);
            return false;
        }
        GuiPatternRoutingAnalysisState.setLoading(normalizedRecipeMapId);
        return true;
    }

    static String summaryText(GuiPatternRoutingAnalysisState.Snapshot snapshot) {
        String recipeMapId = snapshot == null ? "" : normalize(snapshot.recipeMapId);
        if (recipeMapId.isEmpty()) {
            return t("nhaeutilities.gui.pattern_routing_analysis.summary.empty", "RecipeMap: none");
        }

        RecipeMapAnalysisResult result = snapshot.result;
        int totalTypeCount = result == null ? 0 : result.totalTypeCount;
        return t(
            "nhaeutilities.gui.pattern_routing_analysis.summary.result",
            "RecipeMap: %s | Total types: %s",
            recipeMapId,
            totalTypeCount);
    }

    static String statusMessage(GuiPatternRoutingAnalysisState.Snapshot snapshot) {
        if (snapshot == null) {
            return emptyStateMessage();
        }
        if (snapshot.loading) {
            return t("nhaeutilities.gui.pattern_routing_analysis.status.loading", "Loading recipe-map analysis...");
        }
        if (!normalize(snapshot.errorMessage).isEmpty()) {
            return snapshot.errorMessage;
        }
        if (normalize(snapshot.recipeMapId).isEmpty()) {
            return emptyStateMessage();
        }
        if (snapshot.result == null) {
            return t("nhaeutilities.gui.pattern_routing_analysis.status.ready", "Press Refresh to request analysis.");
        }
        if (snapshot.result.hasIncompleteAnalysis) {
            return t(
                "nhaeutilities.gui.pattern_routing_analysis.status.incomplete",
                "Analysis incomplete; counts may be partial.");
        }
        return t("nhaeutilities.gui.pattern_routing_analysis.status.loaded", "Analysis loaded.");
    }

    static String repeatedSectionTitle(GuiPatternRoutingAnalysisState.Snapshot snapshot) {
        int count = snapshot == null || snapshot.result == null ? 0 : snapshot.result.repeatedTypes.size();
        return t("nhaeutilities.gui.pattern_routing_analysis.section.repeated", "Repeated types (%s)", count);
    }

    static String singleSectionTitle(GuiPatternRoutingAnalysisState.Snapshot snapshot) {
        int count = snapshot == null || snapshot.result == null ? 0 : snapshot.result.singleOccurrenceTypes.size();
        return t("nhaeutilities.gui.pattern_routing_analysis.section.single", "Single-occurrence types (%s)", count);
    }

    static boolean shouldRefreshCurrentAnalyzerScreen(String recipeMapId, boolean isAnalyzerScreen,
        boolean isHoldingAnalyzer, String heldRecipeMapId) {
        String normalizedRecipeMapId = normalize(recipeMapId);
        String normalizedHeldRecipeMapId = normalize(heldRecipeMapId);
        return isAnalyzerScreen && isHoldingAnalyzer
            && !normalizedRecipeMapId.isEmpty()
            && (normalizedHeldRecipeMapId.isEmpty() || normalizedRecipeMapId.equals(normalizedHeldRecipeMapId));
    }

    private static int populateContent(ScrollWidget<?> scrollable, GuiPatternRoutingAnalysisState.Snapshot snapshot) {
        int y = 0;

        if (snapshot == null || normalize(snapshot.recipeMapId).isEmpty()) {
            scrollable.child(new TextWidget(IKey.str(emptyStateMessage())).pos(2, y));
            return 12;
        }

        if (snapshot.result == null) {
            scrollable.child(new TextWidget(IKey.str(statusMessage(snapshot))).pos(2, y));
            return 12;
        }

        y = addSection(scrollable, y, repeatedSectionTitle(snapshot), snapshot.result.repeatedTypes, true);
        y = addSection(scrollable, y, singleSectionTitle(snapshot), snapshot.result.singleOccurrenceTypes, false);
        return y;
    }

    private static int addSection(ScrollWidget<?> scrollable, int startY, String title,
        List<RecipeMapAnalysisResult.RecipeTypeGroup> groups, boolean highlighted) {
        int y = startY;
        scrollable
            .child(new TextWidget(IKey.str((highlighted ? EnumChatFormatting.BOLD.toString() : "") + title)).pos(2, y));
        y += 12;

        if (groups == null || groups.isEmpty()) {
            scrollable.child(
                new TextWidget(
                    IKey.str(t("nhaeutilities.gui.pattern_routing_analysis.section.none", "None"))
                        .style(EnumChatFormatting.GRAY)).pos(8, y));
            y += 12;
            return y + 4;
        }

        for (RecipeMapAnalysisResult.RecipeTypeGroup group : groups) {
            scrollable.child(
                new ButtonWidget<>().pos(2, y - 1)
                    .size(GUI_W - 34, 12)
                    .background(new Rectangle().setColor(0xFF1E1E30))
                    .onMousePressed(mb -> true));

            scrollable.child(new TextWidget(IKey.str(formatGroupLine(group))).pos(6, y + 2));
            y += 14;
        }

        return y + 4;
    }

    private static String formatGroupLine(RecipeMapAnalysisResult.RecipeTypeGroup group) {
        if (group == null) {
            return "";
        }
        String summary = normalize(group.displaySummary);
        if (summary.isEmpty()) {
            summary = t("nhaeutilities.gui.pattern_routing_analysis.group.unnamed", "Unnamed type");
        }
        if (group.matchCount <= 1) {
            return summary;
        }
        return summary + " x" + group.matchCount;
    }

    static String readStoredRecipeMap(EntityPlayer player) {
        if (player == null) {
            return "";
        }
        ItemStack heldItem = player.getCurrentEquippedItem();
        return normalize(ItemRecipeMapAnalyzer.getStoredRecipeMap(heldItem));
    }

    static boolean isHoldingAnalyzer(EntityPlayer player) {
        if (player == null) {
            return false;
        }
        ItemStack heldItem = player.getCurrentEquippedItem();
        return heldItem != null && heldItem.getItem() instanceof ItemRecipeMapAnalyzer;
    }

    private static String emptyStateMessage() {
        return t(ERROR_NO_RECIPE_MAP, "No RecipeMap stored. Sneak-right-click a GregTech machine to capture one.");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private static String t(String key, String fallback, Object... args) {
        return I18nUtil.trOr(key, fallback, args);
    }
}
