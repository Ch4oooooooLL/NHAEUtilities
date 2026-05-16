package com.github.nhaeutilities.modules.patternrouting.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumChatFormatting;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.drawable.GuiTextures;
import com.cleanroommc.modularui.drawable.Rectangle;
import com.cleanroommc.modularui.factory.ClientGUI;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.cleanroommc.modularui.widget.scroll.VerticalScrollData;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.TextWidget;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.github.nhaeutilities.NHAEUtilities;
import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;
import com.github.nhaeutilities.modules.patternrouting.network.NetworkHandler;
import com.github.nhaeutilities.modules.patternrouting.network.PacketUpdateFilterRules;
import com.github.nhaeutilities.modules.patternrouting.service.FilterRule;
import com.github.nhaeutilities.modules.patternrouting.service.FilterRule.RuleType;
import com.github.nhaeutilities.modules.shared.NHTextures;
import com.github.nhaeutilities.modules.shared.nei.NeiRecipeExtractionContext;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.recipe.RecipeMap;

@SideOnly(Side.CLIENT)
public final class GuiAddFilterRule {

    private static final int GUI_W = 260;
    private static final int GUI_H = 240;

    private GuiAddFilterRule() {}

    public static ModularPanel createWindow(EntityPlayer player) {
        return createWindow(player, null, -1);
    }

    public static ModularPanel createWindow(EntityPlayer player, FilterRule existingRule, int editIndex) {
        boolean isEdit = existingRule != null;
        PatternIndexGuiState state = PatternIndexGuiState.instance();

        final int capturedEditIndex = editIndex;
        NeiRecipeExtractionContext.instance()
            .activate(data -> {
                if (data.recipeMapId != null && !data.recipeMapId.isEmpty()
                    && RecipeMap.ALL_RECIPE_MAPS.containsKey(data.recipeMapId)) {
                    state.recipeMapId = data.recipeMapId;
                    state.recipeMapSearch = "";
                }
                ClientGUI.close();
                PatternIndexClientScreen.openAddFilterRuleGui(null, capturedEditIndex);
            });

        ModularPanel panel = ModularPanel.defaultPanel("add_filter_rule", GUI_W, GUI_H)
            .background(GuiTextures.MC_BACKGROUND);

        String titleKey = isEdit ? "nhaeutilities.gui.filter_rule.title_edit"
            : "nhaeutilities.gui.filter_rule.title_add";
        panel.child(
            new TextWidget(
                IKey.str(t(titleKey, isEdit ? "Edit Filter Rule" : "Add Filter Rule"))
                    .style(EnumChatFormatting.BOLD)).scale(1.2f)
                        .pos(8, 8)
                        .size(GUI_W - 16, 20));

        panel.child(new TextWidget(IKey.str(t("nhaeutilities.gui.filter_rule.label_type", "Rule Type:"))).pos(8, 32));

        boolean isBlacklist = state.ruleType == RuleType.BLACKLIST;

        final int editIdx = editIndex;
        int btnWType = 64;
        int btnWManual = 72;
        ButtonWidget<?> btnBlacklist = new ButtonWidget<>();
        btnBlacklist.pos(8, 48)
            .size(btnWType, 16)
            .background(isBlacklist ? new Rectangle().setColor(0xFF3366CC) : NHTextures.BUTTON)
            .overlay(
                IKey.str(t("nhaeutilities.gui.filter_rule.type_blacklist", "Blacklist"))
                    .shadow(false));
        btnBlacklist.disableHoverBackground();
        btnBlacklist.onMousePressed(mb -> {
            if (!isBlacklist) {
                state.ruleType = RuleType.BLACKLIST;
                reloadWindow(state.recipeMapId.isEmpty() ? "" : state.recipeMapId, state.itemPattern, editIdx);
            }
            return true;
        });
        panel.child(btnBlacklist);

        ButtonWidget<?> btnManual = new ButtonWidget<>();
        btnManual.pos(8 + btnWType + 4, 48)
            .size(btnWManual, 16)
            .background(!isBlacklist ? new Rectangle().setColor(0xFF3366CC) : NHTextures.BUTTON)
            .overlay(
                IKey.str(t("nhaeutilities.gui.filter_rule.type_manual", "Manual Match"))
                    .shadow(false));
        btnManual.disableHoverBackground();
        btnManual.onMousePressed(mb -> {
            if (isBlacklist) {
                state.ruleType = RuleType.MANUAL_MATCH;
                reloadWindow(state.recipeMapId.isEmpty() ? "" : state.recipeMapId, state.itemPattern, editIdx);
            }
            return true;
        });
        panel.child(btnManual);

        panel.child(
            new TextWidget(
                IKey.str(t("nhaeutilities.gui.filter_rule.label_item", "Item Pattern ([ID]/(ore)/{display}):")))
                    .pos(8, 72));

        String initItemText = state.itemPattern;
        TextFieldWidget itemField = new TextFieldWidget();
        itemField.pos(8, 88);
        itemField.size(GUI_W - 16, 14);
        itemField.setTextColor(isBlacklist ? 0x888888 : 0xFFFFFF);
        itemField.background(new Rectangle().setColor(0xFF222244));
        itemField.setTextAlignment(Alignment.CenterLeft);
        itemField.setText(initItemText);
        panel.child(itemField);

        if (isBlacklist) {
            panel.child(
                new TextWidget(
                    IKey.str(t("nhaeutilities.gui.filter_rule.item_disabled", "(disabled for blacklist)"))
                        .style(EnumChatFormatting.GRAY)).pos(8, 88)
                            .size(GUI_W - 16, 14));
        }

        panel.child(new TextWidget(IKey.str(t("nhaeutilities.gui.filter_rule.label_map", "RecipeMap:"))).pos(8, 110));

        panel.child(
            new TextWidget(
                IKey.str(t("nhaeutilities.gui.filter_rule.map_hint", "Press R/U on NEI item to pick"))
                    .style(EnumChatFormatting.GRAY)).pos(80, 111));

        String initMapText = state.recipeMapSearch.isEmpty() ? state.recipeMapId : state.recipeMapSearch;
        TextFieldWidget mapField = new TextFieldWidget();
        mapField.pos(8, 126);
        mapField.size(GUI_W - 16 - 40, 14);
        mapField.setTextColor(0xFFFFFF);
        mapField.background(new Rectangle().setColor(0xFF222244));
        mapField.setTextAlignment(Alignment.CenterLeft);
        mapField.setText(initMapText);
        panel.child(mapField);

        panel.child(
            NHTextures.createButton()
                .pos(GUI_W - 8 - 36, 126)
                .size(36, 14)
                .overlay(
                    IKey.str("...")
                        .shadow(false))
                .onMousePressed(mb -> {
                    String searchText = mapField.getText();
                    state.recipeMapSearch = searchText;
                    state.recipeMapId = "";
                    state.itemPattern = itemField.getText();
                    reloadWindow("", state.itemPattern, editIdx);
                    return true;
                }));

        String searchKeyword = state.recipeMapSearch.isEmpty() ? state.recipeMapId : state.recipeMapSearch;
        List<String> matchedMaps = findMatchingMaps(searchKeyword);
        if (!matchedMaps.isEmpty()) {
            ScrollWidget<?> mapList = new ScrollWidget<>(new VerticalScrollData()).pos(8, 144)
                .size(GUI_W - 16, GUI_H - 144 - 40);
            int ly = 0;
            for (String mapId : matchedMaps) {
                final String finalMapId = mapId;
                mapList.child(
                    NHTextures.createButton()
                        .pos(2, ly)
                        .size(GUI_W - 36, 14)
                        .overlay(
                            IKey.str(finalMapId)
                                .shadow(false))
                        .onMousePressed(mb -> {
                            state.recipeMapId = finalMapId;
                            state.recipeMapSearch = "";
                            state.itemPattern = itemField.getText();
                            reloadWindow(finalMapId, state.itemPattern, editIdx);
                            return true;
                        }));
                ly += 16;
            }
            panel.child(mapList);
        }

        int btnY = GUI_H - 22;
        int btnW = 64;
        int btnH = 16;
        int startX = (GUI_W - (btnW * 2 + 4)) / 2;

        panel.child(
            NHTextures.createButton()
                .pos(startX, btnY)
                .size(btnW, btnH)
                .overlay(
                    IKey.str(t("nhaeutilities.gui.filter_rule.button_save", "Save"))
                        .shadow(false))
                .onMousePressed(mb -> {
                    state.itemPattern = itemField.getText();
                    String mapText = mapField.getText();
                    if (!mapText.isEmpty() && RecipeMap.ALL_RECIPE_MAPS.containsKey(mapText.trim())) {
                        state.recipeMapId = mapText.trim();
                    }
                    if (validateAndSave(isEdit, editIndex)) {
                        NHAEUtilities.proxy.closeCurrentScreen();
                    }
                    return true;
                }));

        panel.child(
            NHTextures.createButton()
                .pos(startX + btnW + 4, btnY)
                .size(btnW, btnH)
                .overlay(
                    IKey.str(t("nhaeutilities.gui.common.cancel", "Cancel"))
                        .shadow(false))
                .onMousePressed(mb -> {
                    PatternIndexGuiState.clearTemp();
                    NHAEUtilities.proxy.closeCurrentScreen();
                    PatternIndexClientScreen.openPatternIndexGui(null);
                    return true;
                }));

        return panel;
    }

    private static void reloadWindow(String recipeMapId, String itemPattern, int editIndex) {
        PatternIndexGuiState state = PatternIndexGuiState.instance();
        if (!recipeMapId.isEmpty()) state.recipeMapId = recipeMapId;
        state.itemPattern = itemPattern;
        PatternIndexClientScreen.openAddFilterRuleGui(null, editIndex);
    }

    private static boolean validateAndSave(boolean isEdit, int editIndex) {
        PatternIndexGuiState state = PatternIndexGuiState.instance();

        String normalizedMap = state.recipeMapId != null ? state.recipeMapId.trim() : "";
        if (normalizedMap.isEmpty()) {
            return false;
        }

        if (!RecipeMap.ALL_RECIPE_MAPS.containsKey(normalizedMap)) {
            return false;
        }

        String pattern = state.ruleType == RuleType.MANUAL_MATCH
            ? (state.itemPattern != null ? state.itemPattern.trim() : "")
            : "";

        FilterRule rule = new FilterRule(state.ruleType, pattern, normalizedMap);
        if (!rule.isValid()) {
            return false;
        }

        if (isEdit && editIndex >= 0) {
            NetworkHandler.sendToServer(PacketUpdateFilterRules.edit(editIndex, rule));
        } else {
            NetworkHandler.sendToServer(PacketUpdateFilterRules.add(rule));
        }
        PatternIndexGuiState.clearTemp();

        return true;
    }

    private static List<String> findMatchingMaps(String keyword) {
        List<String> matches = new ArrayList<>();
        String lower = keyword != null ? keyword.trim()
            .toLowerCase(Locale.ROOT) : "";
        if (lower.isEmpty()) return matches;

        for (String mapId : RecipeMap.ALL_RECIPE_MAPS.keySet()) {
            if (mapId.toLowerCase(Locale.ROOT)
                .contains(lower)) {
                matches.add(mapId);
                if (matches.size() >= 20) break;
            }
        }
        return matches;
    }

    private static String t(String key, String fallback, Object... args) {
        return I18nUtil.trOr(key, fallback, args);
    }
}
