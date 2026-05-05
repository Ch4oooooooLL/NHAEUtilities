package com.github.nhaeutilities.modules.patternrouting.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.minecraft.util.EnumChatFormatting;

import com.github.nhaeutilities.NHAEUtilities;
import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;
import com.github.nhaeutilities.modules.patternrouting.network.NetworkHandler;
import com.github.nhaeutilities.modules.patternrouting.network.PacketUpdateFilterRules;
import com.github.nhaeutilities.modules.patternrouting.service.FilterRule;
import com.github.nhaeutilities.modules.patternrouting.service.FilterRule.RuleType;
import com.gtnewhorizons.modularui.api.ModularUITextures;
import com.gtnewhorizons.modularui.api.drawable.shapes.Rectangle;
import com.gtnewhorizons.modularui.api.math.Alignment;
import com.gtnewhorizons.modularui.api.screen.ModularWindow;
import com.gtnewhorizons.modularui.api.screen.UIBuildContext;
import com.gtnewhorizons.modularui.common.widget.ButtonWidget;
import com.gtnewhorizons.modularui.common.widget.Scrollable;
import com.gtnewhorizons.modularui.common.widget.TextWidget;
import com.gtnewhorizons.modularui.common.widget.textfield.TextFieldWidget;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.recipe.RecipeMap;

@SideOnly(Side.CLIENT)
public final class GuiAddFilterRule {

    private static final int GUI_W = 260;
    private static final int GUI_H = 240;

    private GuiAddFilterRule() {}

    public static ModularWindow createWindow(UIBuildContext buildContext) {
        return createWindow(buildContext, null, -1);
    }

    public static ModularWindow createWindow(UIBuildContext buildContext, FilterRule existingRule, int editIndex) {
        ModularWindow.Builder builder = ModularWindow.builder(GUI_W, GUI_H);
        builder.setBackground(ModularUITextures.VANILLA_BACKGROUND);

        boolean isEdit = existingRule != null;
        PatternIndexGuiState state = PatternIndexGuiState.instance();

        String titleKey = isEdit ? "nhaeutilities.gui.filter_rule.title_edit"
            : "nhaeutilities.gui.filter_rule.title_add";
        TextWidget title = new TextWidget(
            EnumChatFormatting.BOLD + t(titleKey, isEdit ? "Edit Filter Rule" : "Add Filter Rule"));
        title.setScale(1.2f);
        title.setSize(GUI_W - 16, 20);
        title.setPos(8, 8);
        builder.widget(title);

        TextWidget typeLabel = new TextWidget(t("nhaeutilities.gui.filter_rule.label_type", "Rule Type:"));
        typeLabel.setPos(8, 34);
        builder.widget(typeLabel);

        boolean isBlacklist = state.ruleType == RuleType.BLACKLIST;

        ButtonWidget blacklistBtn = new ButtonWidget();
        blacklistBtn.setPos(8, 50);
        blacklistBtn.setSize(64, 18);
        blacklistBtn.setBackground(
            isBlacklist ? new Rectangle().setColor(0xFF3366CC) : ModularUITextures.VANILLA_BUTTON_NORMAL);
        blacklistBtn.setSynced(false, false);
        final int editIdx = editIndex;
        blacklistBtn.setOnClick((cd, w) -> {
            if (!isBlacklist) {
                state.ruleType = RuleType.BLACKLIST;
                reloadWindow(state.recipeMapId.isEmpty() ? "" : state.recipeMapId, state.itemPattern, editIdx);
            }
        });
        builder.widget(blacklistBtn);

        TextWidget blacklistText = new TextWidget(t("nhaeutilities.gui.filter_rule.type_blacklist", "Blacklist"));
        blacklistText.setPos(13, 53);
        builder.widget(blacklistText);

        ButtonWidget manualBtn = new ButtonWidget();
        manualBtn.setPos(76, 50);
        manualBtn.setSize(72, 18);
        manualBtn.setBackground(
            !isBlacklist ? new Rectangle().setColor(0xFF3366CC) : ModularUITextures.VANILLA_BUTTON_NORMAL);
        manualBtn.setSynced(false, false);
        manualBtn.setOnClick((cd, w) -> {
            if (isBlacklist) {
                state.ruleType = RuleType.MANUAL_MATCH;
                reloadWindow(state.recipeMapId.isEmpty() ? "" : state.recipeMapId, state.itemPattern, editIdx);
            }
        });
        builder.widget(manualBtn);

        TextWidget manualText = new TextWidget(t("nhaeutilities.gui.filter_rule.type_manual", "Manual Match"));
        manualText.setPos(80, 53);
        builder.widget(manualText);

        TextWidget itemLabel = new TextWidget(
            t("nhaeutilities.gui.filter_rule.label_item", "Item Pattern ([ID]/(ore)/{display}):"));
        itemLabel.setPos(8, 76);
        builder.widget(itemLabel);

        String initItemText = state.itemPattern;
        TextFieldWidget itemField = new TextFieldWidget();
        itemField.setPos(8, 92);
        itemField.setSize(GUI_W - 16, 14);
        itemField.setTextColor(isBlacklist ? 0x888888 : 0xFFFFFF);
        itemField.setBackground(new Rectangle().setColor(0xFF222244));
        itemField.setTextAlignment(Alignment.CenterLeft);
        itemField.setText(initItemText);
        builder.widget(itemField);

        if (isBlacklist) {
            TextWidget disabledHint = new TextWidget(
                EnumChatFormatting.GRAY + t("nhaeutilities.gui.filter_rule.item_disabled", "(disabled for blacklist)"));
            disabledHint.setPos(8, 92);
            disabledHint.setSize(GUI_W - 16, 14);
            builder.widget(disabledHint);
        }

        TextWidget mapLabel = new TextWidget(t("nhaeutilities.gui.filter_rule.label_map", "RecipeMap:"));
        mapLabel.setPos(8, 114);
        builder.widget(mapLabel);

        String initMapText = state.recipeMapSearch.isEmpty() ? state.recipeMapId : state.recipeMapSearch;
        TextFieldWidget mapField = new TextFieldWidget();
        mapField.setPos(8, 130);
        mapField.setSize(GUI_W - 16 - 40, 14);
        mapField.setTextColor(0xFFFFFF);
        mapField.setBackground(new Rectangle().setColor(0xFF222244));
        mapField.setTextAlignment(Alignment.CenterLeft);
        mapField.setText(initMapText);
        builder.widget(mapField);

        ButtonWidget searchBtn = new ButtonWidget();
        searchBtn.setPos(GUI_W - 8 - 36, 130);
        searchBtn.setSize(36, 14);
        searchBtn.setBackground(ModularUITextures.VANILLA_BUTTON_NORMAL);
        searchBtn.setSynced(false, false);
        searchBtn.setOnClick((cd, w) -> {
            String searchText = mapField.getText();
            state.recipeMapSearch = searchText;
            state.recipeMapId = "";
            state.itemPattern = itemField.getText();
            reloadWindow("", state.itemPattern, editIdx);
        });
        builder.widget(searchBtn);

        TextWidget searchText = new TextWidget("...");
        searchText.setPos(GUI_W - 8 - 30, 132);
        builder.widget(searchText);

        String searchKeyword = state.recipeMapSearch.isEmpty() ? state.recipeMapId : state.recipeMapSearch;
        List<String> matchedMaps = findMatchingMaps(searchKeyword);
        if (!matchedMaps.isEmpty()) {
            Scrollable mapList = new Scrollable().setVerticalScroll();
            mapList.setPos(8, 148);
            mapList.setSize(GUI_W - 16, GUI_H - 148 - 40);
            int ly = 0;
            for (String mapId : matchedMaps) {
                final String finalMapId = mapId;
                ButtonWidget mapRow = new ButtonWidget();
                mapRow.setPos(2, ly);
                mapRow.setSize(GUI_W - 36, 14);
                mapRow.setBackground(ModularUITextures.VANILLA_BUTTON_NORMAL);
                mapRow.setSynced(false, false);
                mapRow.setOnClick((cd, w) -> {
                    state.recipeMapId = finalMapId;
                    state.recipeMapSearch = "";
                    state.itemPattern = itemField.getText();
                    reloadWindow(finalMapId, state.itemPattern, editIdx);
                });
                mapList.widget(mapRow);

                TextWidget mapRowText = new TextWidget(finalMapId);
                mapRowText.setPos(6, ly + 2);
                mapList.widget(mapRowText);
                ly += 16;
            }
            builder.widget(mapList);
        }

        int btnY = GUI_H - 22;
        ButtonWidget saveBtn = new ButtonWidget();
        saveBtn.setPos(60, btnY);
        saveBtn.setSize(64, 18);
        saveBtn.setBackground(ModularUITextures.VANILLA_BUTTON_NORMAL);
        saveBtn.setSynced(false, false);
        saveBtn.setOnClick((cd, w) -> {
            state.itemPattern = itemField.getText();
            String mapText = mapField.getText();
            if (!mapText.isEmpty() && RecipeMap.ALL_RECIPE_MAPS.containsKey(mapText.trim())) {
                state.recipeMapId = mapText.trim();
            }
            if (validateAndSave(isEdit, editIndex)) {
                NHAEUtilities.proxy.closeCurrentScreen();
                PatternIndexClientScreen.openPatternIndexGui(null);
            }
        });
        builder.widget(saveBtn);

        TextWidget saveText = new TextWidget(t("nhaeutilities.gui.filter_rule.button_save", "Save"));
        saveText.setPos(80, btnY + 3);
        builder.widget(saveText);

        ButtonWidget cancelBtn = new ButtonWidget();
        cancelBtn.setPos(140, btnY);
        cancelBtn.setSize(64, 18);
        cancelBtn.setBackground(ModularUITextures.VANILLA_BUTTON_NORMAL);
        cancelBtn.setSynced(false, false);
        cancelBtn.setOnClick((cd, w) -> {
            PatternIndexGuiState.clearTemp();
            NHAEUtilities.proxy.closeCurrentScreen();
            PatternIndexClientScreen.openPatternIndexGui(null);
        });
        builder.widget(cancelBtn);

        TextWidget cancelText = new TextWidget(t("nhaeutilities.gui.pattern_routing_analysis.button.close", "Cancel"));
        cancelText.setPos(155, btnY + 3);
        builder.widget(cancelText);

        return builder.build();
    }

    private static void reloadWindow(String recipeMapId, String itemPattern, int editIndex) {
        PatternIndexGuiState state = PatternIndexGuiState.instance();
        if (!recipeMapId.isEmpty()) state.recipeMapId = recipeMapId;
        state.itemPattern = itemPattern;
        NHAEUtilities.proxy.closeCurrentScreen();
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
