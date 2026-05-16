package com.github.nhaeutilities.modules.patternrouting.gui;

import java.util.ArrayList;
import java.util.List;

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
import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;
import com.github.nhaeutilities.modules.patternrouting.network.NetworkHandler;
import com.github.nhaeutilities.modules.patternrouting.network.PacketBatchEncodeBookmarks;
import com.github.nhaeutilities.modules.patternrouting.network.PacketRequestFilterRules;
import com.github.nhaeutilities.modules.patternrouting.network.PacketUpdateFilterRules;
import com.github.nhaeutilities.modules.patternrouting.service.FilterRule;
import com.github.nhaeutilities.modules.patternrouting.service.FilterRule.RuleType;
import com.github.nhaeutilities.modules.shared.NHTextures;
import com.github.nhaeutilities.modules.shared.nei.NeiRecipeExtractionContext;

import codechicken.nei.LayoutManager;
import codechicken.nei.bookmark.BookmarkGrid;
import codechicken.nei.bookmark.BookmarkItem;

public final class GuiPatternIndex {

    private static final int GUI_W = 240;
    private static final int GUI_H = 220;

    private static final int PADDING = 8;
    private static final int CONTENT_W = GUI_W - PADDING * 2;
    private static final int BTN_X = CONTENT_W - 30;
    private static final int BG_W = BTN_X - 4;

    private GuiPatternIndex() {}

    public static ModularPanel createWindow(EntityPlayer player) {
        NeiRecipeExtractionContext.instance()
            .deactivate();

        ModularPanel panel = ModularPanel.defaultPanel("pattern_index", GUI_W, GUI_H)
            .background(GuiTextures.MC_BACKGROUND);

        panel.child(
            new TextWidget(
                IKey.str(t("nhaeutilities.gui.pattern_index.title", "Pattern Index"))
                    .style(EnumChatFormatting.BOLD)).scale(1.2f)
                        .pos(PADDING, 8)
                        .size(CONTENT_W, 20));

        List<FilterRule> rules = PatternIndexConfigState.getRules();
        List<ItemStack> bookmarks = readBookmarkItems();
        int bookmarkCount = bookmarks != null ? bookmarks.size() : 0;

        panel.child(
            new TextWidget(
                IKey.str(
                    t(
                        "nhaeutilities.gui.pattern_index.bookmark_count",
                        "Bookmarks: %s  |  Rules: %s",
                        bookmarkCount,
                        rules.size()))).pos(PADDING, 30));

        panel.child(
            new ButtonWidget<>().pos(PADDING, 44)
                .size(CONTENT_W, 1)
                .background(new Rectangle().setColor(0xFF444444)));

        ScrollWidget<?> ruleScrollable = new ScrollWidget<>(new VerticalScrollData()).pos(PADDING, 49)
            .size(CONTENT_W, 130);

        int ry = 0;
        for (int i = 0; i < rules.size(); i++) {
            FilterRule rule = rules.get(i);
            String ruleText = formatRule(rule);
            final int idx = i;

            ButtonWidget<?> row = new ButtonWidget<>().pos(2, ry)
                .size(BG_W, 30)
                .background(new Rectangle().setColor(0xFF1E1E30))
                .onMousePressed(mb -> {
                    handleEditRule(idx);
                    return true;
                });
            ruleScrollable.child(row);

            ruleScrollable.child(new TextWidget(IKey.str(ruleText)).pos(6, ry + 4));

            ruleScrollable.child(
                NHTextures.createButton()
                    .pos(BTN_X, ry + 3)
                    .size(18, 12)
                    .overlay(
                        IKey.str("X")
                            .style(EnumChatFormatting.RED)
                            .shadow(false))
                    .onMousePressed(mb -> {
                        handleDeleteRule(idx);
                        return true;
                    }));

            ruleScrollable.child(
                NHTextures.createButton()
                    .pos(BTN_X, ry + 18)
                    .size(18, 12)
                    .overlay(
                        IKey.str("...")
                            .shadow(false))
                    .onMousePressed(mb -> {
                        handleEditRule(idx);
                        return true;
                    }));

            ry += 34;
        }

        if (rules.isEmpty()) {
            ruleScrollable.child(
                new TextWidget(
                    IKey.str(t("nhaeutilities.gui.pattern_index.no_rules", "No filter rules defined."))
                        .style(EnumChatFormatting.GRAY)).pos(4, 0));
        }

        ruleScrollable.getScrollArea()
            .getScrollY()
            .setScrollSize(rules.isEmpty() ? 12 : ry);
        panel.child(ruleScrollable);

        int btnY = GUI_H - 34;
        int numBtns = 3;
        int btnW = 64;
        int btnH = 16;
        int gap = 4;
        int totalW = numBtns * btnW + (numBtns - 1) * gap;
        int startX = (GUI_W - totalW) / 2;

        panel.child(
            NHTextures.createButton()
                .pos(startX, btnY)
                .size(btnW, btnH)
                .overlay(
                    IKey.str(t("nhaeutilities.gui.pattern_index.button.batch_encode", "Batch Encode"))
                        .shadow(false))
                .onMousePressed(mb -> {
                    doBatchEncode();
                    return true;
                }));

        panel.child(
            NHTextures.createButton()
                .pos(startX + btnW + gap, btnY)
                .size(btnW, btnH)
                .overlay(
                    IKey.str(t("nhaeutilities.gui.pattern_index.button.add_filter", "Add Filter"))
                        .shadow(false))
                .onMousePressed(mb -> {
                    handleAddRule();
                    return true;
                }));

        panel.child(
            NHTextures.createButton()
                .pos(startX + 2 * (btnW + gap), btnY)
                .size(btnW, btnH)
                .overlay(
                    IKey.str(t("nhaeutilities.gui.pattern_index.button.refresh", "Refresh Rules"))
                        .shadow(false))
                .onMousePressed(mb -> {
                    NetworkHandler.sendToServer(new PacketRequestFilterRules());
                    return true;
                }));

        return panel;
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
        PatternIndexClientScreen.openAddFilterRuleGui(null, -1);
    }

    private static void handleEditRule(int index) {
        PatternIndexGuiState.prepareEdit(index);
        PatternIndexClientScreen.openAddFilterRuleGui(null, index);
    }

    private static void handleDeleteRule(int index) {
        NetworkHandler.sendToServer(PacketUpdateFilterRules.delete(index));
    }

    private static void doBatchEncode() {
        List<ItemStack> bookmarks = readBookmarkItems();
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
