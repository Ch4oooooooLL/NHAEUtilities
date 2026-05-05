package com.github.nhaeutilities.modules.patternrouting.gui;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;

import com.github.nhaeutilities.NHAEUtilities;
import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;
import com.github.nhaeutilities.modules.patternrouting.network.NetworkHandler;
import com.github.nhaeutilities.modules.patternrouting.network.PacketBatchEncodeBookmarks;
import com.github.nhaeutilities.modules.patternrouting.network.PacketRequestFilterRules;
import com.github.nhaeutilities.modules.patternrouting.network.PacketUpdateFilterRules;
import com.github.nhaeutilities.modules.patternrouting.service.FilterRule;
import com.github.nhaeutilities.modules.patternrouting.service.FilterRule.RuleType;
import com.gtnewhorizons.modularui.api.ModularUITextures;
import com.gtnewhorizons.modularui.api.drawable.shapes.Rectangle;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.Scrollable;
import com.gtnewhorizons.modularui.common.widget.TextWidget;

import codechicken.nei.LayoutManager;
import codechicken.nei.bookmark.BookmarkGrid;
import codechicken.nei.bookmark.BookmarkItem;

public final class GuiPatternIndex {

    private static final int GUI_W = 240;
    private static final int GUI_H = 220;

    private GuiPatternIndex() {}

    public static ModularWindow createWindow(UIBuildContext buildContext) {
        ModularWindow.Builder builder = ModularWindow.builder(GUI_W, GUI_H);
        builder.setBackground(ModularUITextures.VANILLA_BACKGROUND);

        TextWidget title = new TextWidget(
            EnumChatFormatting.BOLD + t("nhaeutilities.gui.pattern_index.title", "Pattern Index"));
        title.setScale(1.2f);
        title.setSize(GUI_W - 16, 20);
        title.setPos(8, 8);
        builder.widget(title);

        List<FilterRule> rules = PatternIndexConfigState.getRules();
        List<ItemStack> bookmarks = readBookmarkItems();
        int bookmarkCount = bookmarks != null ? bookmarks.size() : 0;

        TextWidget info = new TextWidget(
            t(
                "nhaeutilities.gui.pattern_index.bookmark_count",
                "Bookmarks: %s  |  Rules: %s",
                bookmarkCount,
                rules.size()));
        info.setPos(8, 30);
        builder.widget(info);

        Scrollable ruleScrollable = new Scrollable().setVerticalScroll();
        ruleScrollable.setPos(8, 48);
        ruleScrollable.setSize(GUI_W - 16, 120);

        int ry = 0;
        for (int i = 0; i < rules.size(); i++) {
            final int ruleIndex = i;
            FilterRule rule = rules.get(i);
            String ruleText = formatRule(rule);

            ButtonWidget row = new ButtonWidget();
            row.setPos(2, ry);
            row.setSize(GUI_W - 56, 42);
            row.setBackground(new Rectangle().setColor(0xFF1E1E30));
            final int clickIndex = i;
            row.setOnClick((cd, w) -> handleEditRule(clickIndex));
            ruleScrollable.widget(row);

            TextWidget ruleLine = new TextWidget(ruleText);
            ruleLine.setPos(6, ry + 4);
            ruleScrollable.widget(ruleLine);

            ButtonWidget delBtn = new ButtonWidget();
            delBtn.setPos(GUI_W - 48, ry + 4);
            delBtn.setSize(28, 14);
            delBtn.setBackground(ModularUITextures.VANILLA_BUTTON_NORMAL);
            delBtn.setSynced(false, false);
            final int delIndex = i;
            delBtn.setOnClick((cd, w) -> handleDeleteRule(delIndex));
            ruleScrollable.widget(delBtn);

            TextWidget delText = new TextWidget(EnumChatFormatting.RED + "X");
            delText.setPos(GUI_W - 40, ry + 6);
            ruleScrollable.widget(delText);

            ButtonWidget editBtn = new ButtonWidget();
            editBtn.setPos(GUI_W - 48, ry + 22);
            editBtn.setSize(28, 14);
            editBtn.setBackground(ModularUITextures.VANILLA_BUTTON_NORMAL);
            editBtn.setSynced(false, false);
            final int editIndex = i;
            editBtn.setOnClick((cd, w) -> handleEditRule(editIndex));
            ruleScrollable.widget(editBtn);

            TextWidget editText = new TextWidget("...");
            editText.setPos(GUI_W - 38, ry + 24);
            ruleScrollable.widget(editText);

            ry += 46;
        }

        if (rules.isEmpty()) {
            TextWidget empty = new TextWidget(
                EnumChatFormatting.GRAY + t("nhaeutilities.gui.pattern_index.no_rules", "No filter rules defined."));
            empty.setPos(4, 0);
            ruleScrollable.widget(empty);
        }

        builder.widget(ruleScrollable);

        int btnY = GUI_H - 38;
        int numBtns = 3;
        int btnW = 70;
        int gap = 4;
        int totalW = numBtns * btnW + (numBtns - 1) * gap;
        int startX = (GUI_W - totalW) / 2;

        ButtonWidget encodeBtn = new ButtonWidget();
        encodeBtn.setPos(startX, btnY);
        encodeBtn.setSize(btnW, 18);
        encodeBtn.setBackground(ModularUITextures.VANILLA_BUTTON_NORMAL);
        encodeBtn.setSynced(false, false);
        encodeBtn.setOnClick((cd, w) -> doBatchEncode(bookmarks));
        builder.widget(encodeBtn);

        TextWidget encodeText = new TextWidget(
            t("nhaeutilities.gui.pattern_index.button.batch_encode", "Batch Encode"));
        encodeText.setPos(startX + 8, btnY + 3);
        builder.widget(encodeText);

        ButtonWidget addBtn = new ButtonWidget();
        addBtn.setPos(startX + btnW + gap, btnY);
        addBtn.setSize(btnW, 18);
        addBtn.setBackground(ModularUITextures.VANILLA_BUTTON_NORMAL);
        addBtn.setSynced(false, false);
        addBtn.setOnClick((cd, w) -> handleAddRule());
        builder.widget(addBtn);

        TextWidget addText = new TextWidget(t("nhaeutilities.gui.pattern_index.button.add_filter", "Add Filter"));
        addText.setPos(startX + btnW + gap + 12, btnY + 3);
        builder.widget(addText);

        ButtonWidget refreshBtn = new ButtonWidget();
        refreshBtn.setPos(startX + 2 * (btnW + gap), btnY);
        refreshBtn.setSize(btnW, 18);
        refreshBtn.setBackground(ModularUITextures.VANILLA_BUTTON_NORMAL);
        refreshBtn.setSynced(false, false);
        refreshBtn.setOnClick((cd, w) -> NetworkHandler.sendToServer(new PacketRequestFilterRules()));
        builder.widget(refreshBtn);

        TextWidget refreshText = new TextWidget(t("nhaeutilities.gui.pattern_index.button.refresh", "Refresh Rules"));
        refreshText.setPos(startX + 2 * (btnW + gap) + 6, btnY + 3);
        builder.widget(refreshText);

        return builder.build();
    }

    private static String formatRule(FilterRule rule) {
        if (rule.type == RuleType.BLACKLIST) {
            return EnumChatFormatting.RED + t("nhaeutilities.gui.filter_rule.type_blacklist", "Blacklist")
                + EnumChatFormatting.RESET
                + "\n"
                + EnumChatFormatting.GRAY
                + rule.recipeMapId;
        }
        return EnumChatFormatting.GREEN + t("nhaeutilities.gui.filter_rule.type_manual", "Manual Match")
            + EnumChatFormatting.RESET
            + "\n"
            + EnumChatFormatting.GRAY
            + rule.itemPattern
            + " \u2192 "
            + rule.recipeMapId;
    }

    private static void handleAddRule() {
        PatternIndexGuiState.clearTemp();
        NHAEUtilities.proxy.closeCurrentScreen();
        PatternIndexClientScreen.openAddFilterRuleGui(null, -1);
    }

    private static void handleEditRule(int index) {
        PatternIndexGuiState.prepareEdit(index);
        NHAEUtilities.proxy.closeCurrentScreen();
        PatternIndexClientScreen.openAddFilterRuleGui(null, index);
    }

    private static void handleDeleteRule(int index) {
        NetworkHandler.sendToServer(PacketUpdateFilterRules.delete(index));
    }

    private static void doBatchEncode(List<ItemStack> bookmarks) {
        if (bookmarks == null || bookmarks.isEmpty()) return;
        NetworkHandler.sendToServer(new PacketBatchEncodeBookmarks(bookmarks));
    }

    private static List<ItemStack> readBookmarkItems() {
        if (LayoutManager.bookmarkPanel == null) return null;

        try {
            BookmarkGrid grid = LayoutManager.bookmarkPanel.getGrid();
            int firstGroupId = findFirstGroupId(grid);
            if (firstGroupId < 0) return null;

            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < grid.size(); i++) {
                BookmarkItem item = grid.getBookmarkItem(i);
                if (item != null && item.groupId == firstGroupId) {
                    ItemStack stack = item.getItemStack();
                    if (stack != null) {
                        items.add(stack.copy());
                    }
                }
            }
            return items;
        } catch (Exception e) {
            return null;
        }
    }

    private static int findFirstGroupId(BookmarkGrid grid) {
        int firstGroupId = Integer.MAX_VALUE;
        for (int i = 0; i < grid.size(); i++) {
            BookmarkItem item = grid.getBookmarkItem(i);
            if (item != null && item.groupId >= 0 && item.groupId < firstGroupId) {
                firstGroupId = item.groupId;
            }
        }
        return firstGroupId == Integer.MAX_VALUE ? -1 : firstGroupId;
    }

    private static String t(String key, String fallback, Object... args) {
        return I18nUtil.trOr(key, fallback, args);
    }
}
