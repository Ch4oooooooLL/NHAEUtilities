package com.github.nhaeutilities.modules.patternrouting.gui;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.github.nhaeutilities.NHAEUtilities;
import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;
import com.github.nhaeutilities.modules.patternrouting.core.RecipeMapAnalysisResult;
import com.github.nhaeutilities.modules.patternrouting.item.ItemRecipeMapAnalyzer;
import com.github.nhaeutilities.modules.patternrouting.network.NetworkHandler;
import com.github.nhaeutilities.modules.patternrouting.network.PacketRequestRecipeMapAnalysis;
import com.gtnewhorizons.modularui.api.drawable.shapes.Rectangle;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.Scrollable;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

import codechicken.nei.LayoutManager;

public final class GuiPatternRoutingAnalysis {

    private static final int GUI_W = 260;
    private static final int GUI_H = 240;
    private static final String ERROR_NO_RECIPE_MAP = "nhaeutilities.gui.pattern_routing_analysis.error.no_recipe_map";

    private GuiPatternRoutingAnalysis() {}

    public static ModularWindow createWindow(UIBuildContext buildContext, EntityPlayer player) {
        ModularWindow.Builder builder = ModularWindow.builder(GUI_W, GUI_H);
        builder.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BACKGROUND);

        GuiPatternRoutingAnalysisState.Snapshot snapshot = GuiPatternRoutingAnalysisState.snapshot();

        TextWidget title = new TextWidget(
            EnumChatFormatting.BOLD + t("nhaeutilities.gui.pattern_routing_analysis.title", "RecipeMap Analysis"));
        title.setScale(1.2f);
        title.setSize(GUI_W - 16, 20);
        title.setPos(8, 8);
        builder.widget(title);

        TextWidget summary = new TextWidget(summaryText(snapshot));
        summary.setPos(8, 28);
        builder.widget(summary);

        TextWidget status = new TextWidget(statusMessage(snapshot));
        status.setPos(8, 42);
        builder.widget(status);

        Scrollable scrollable = new Scrollable().setVerticalScroll();
        scrollable.setPos(8, 58);
        scrollable.setSize(GUI_W - 16, 142);

        populateContent(scrollable, snapshot);
        builder.widget(scrollable);

        int buttonY = GUI_H - 30;
        int numButtons = 3;
        int buttonW = 64;
        int buttonH = 18;
        int gap = 4;
        int totalWidth = numButtons * buttonW + (numButtons - 1) * gap;
        int startX = (GUI_W - totalWidth) / 2;

        ButtonWidget refreshButton = new ButtonWidget();
        refreshButton.setPos(startX, buttonY);
        refreshButton.setSize(buttonW, buttonH);
        refreshButton.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
        refreshButton.setSynced(false, false);
        refreshButton.setOnClick((cd, w) -> requestAnalysis(player));
        builder.widget(refreshButton);

        TextWidget refreshText = new TextWidget(
            t("nhaeutilities.gui.pattern_routing_analysis.button.refresh", "Refresh"));
        refreshText.setPos(startX + 23, buttonY + 5);
        builder.widget(refreshText);

        ButtonWidget bookmarkButton = new ButtonWidget();
        bookmarkButton.setPos(startX + buttonW + gap, buttonY);
        bookmarkButton.setSize(buttonW, buttonH);
        bookmarkButton.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
        bookmarkButton.setSynced(false, false);
        bookmarkButton.setOnClick((cd, w) -> addBookmarkFromCurrentResult());
        builder.widget(bookmarkButton);

        TextWidget bookmarkText = new TextWidget(
            t("nhaeutilities.gui.pattern_routing_analysis.button.add_bookmark", "Add Bookmark"));
        bookmarkText.setPos(startX + buttonW + gap + 14, buttonY + 5);
        builder.widget(bookmarkText);

        ButtonWidget closeButton = new ButtonWidget();
        closeButton.setPos(startX + 2 * (buttonW + gap), buttonY);
        closeButton.setSize(buttonW, buttonH);
        closeButton.setBackground(com.gtnewhorizons.modularui.api.ModularUITextures.VANILLA_BUTTON_NORMAL);
        closeButton.setSynced(false, false);
        closeButton.setOnClick((cd, w) -> NHAEUtilities.proxy.closeCurrentScreen());
        builder.widget(closeButton);

        TextWidget closeText = new TextWidget(t("nhaeutilities.gui.pattern_routing_analysis.button.close", "Close"));
        closeText.setPos(startX + 2 * (buttonW + gap) + 23, buttonY + 5);
        builder.widget(closeText);

        return builder.build();
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

    private static void populateContent(Scrollable scrollable, GuiPatternRoutingAnalysisState.Snapshot snapshot) {
        int y = 0;

        if (snapshot == null || normalize(snapshot.recipeMapId).isEmpty()) {
            TextWidget empty = new TextWidget(emptyStateMessage());
            empty.setPos(2, y);
            scrollable.widget(empty);
            return;
        }

        if (snapshot.result == null) {
            TextWidget status = new TextWidget(statusMessage(snapshot));
            status.setPos(2, y);
            scrollable.widget(status);
            return;
        }

        y = addSection(scrollable, y, repeatedSectionTitle(snapshot), snapshot.result.repeatedTypes, true);
        addSection(scrollable, y, singleSectionTitle(snapshot), snapshot.result.singleOccurrenceTypes, false);
    }

    private static int addSection(Scrollable scrollable, int startY, String title,
        List<RecipeMapAnalysisResult.RecipeTypeGroup> groups, boolean highlighted) {
        int y = startY;
        TextWidget titleWidget = new TextWidget((highlighted ? EnumChatFormatting.BOLD : "") + title);
        titleWidget.setPos(2, y);
        scrollable.widget(titleWidget);
        y += 12;

        if (groups == null || groups.isEmpty()) {
            TextWidget empty = new TextWidget(
                EnumChatFormatting.GRAY + t("nhaeutilities.gui.pattern_routing_analysis.section.none", "None"));
            empty.setPos(8, y);
            scrollable.widget(empty);
            y += 12;
            return y + 4;
        }

        for (RecipeMapAnalysisResult.RecipeTypeGroup group : groups) {
            ButtonWidget row = new ButtonWidget();
            row.setPos(2, y - 1);
            row.setSize(GUI_W - 34, 12);
            row.setBackground(new Rectangle().setColor(0xFF1E1E30));
            row.setOnClick((cd, w) -> {});
            scrollable.widget(row);

            TextWidget line = new TextWidget(formatGroupLine(group));
            line.setPos(6, y + 2);
            scrollable.widget(line);
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
