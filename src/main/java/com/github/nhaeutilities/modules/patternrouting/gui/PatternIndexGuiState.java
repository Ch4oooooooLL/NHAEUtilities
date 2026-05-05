package com.github.nhaeutilities.modules.patternrouting.gui;

import com.github.nhaeutilities.modules.patternrouting.service.FilterRule.RuleType;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PatternIndexGuiState {

    private static final PatternIndexGuiState INSTANCE = new PatternIndexGuiState();

    public RuleType ruleType = RuleType.BLACKLIST;
    public String itemPattern = "";
    public String recipeMapId = "";
    public String recipeMapSearch = "";

    private PatternIndexGuiState() {}

    public static PatternIndexGuiState instance() {
        return INSTANCE;
    }

    public static void clearTemp() {
        INSTANCE.ruleType = RuleType.BLACKLIST;
        INSTANCE.itemPattern = "";
        INSTANCE.recipeMapId = "";
        INSTANCE.recipeMapSearch = "";
    }

    public static void prepareEdit(int index) {
        clearTemp();
        java.util.List<com.github.nhaeutilities.modules.patternrouting.service.FilterRule> rules = PatternIndexConfigState
            .getRules();
        if (index < 0 || index >= rules.size()) return;
        com.github.nhaeutilities.modules.patternrouting.service.FilterRule rule = rules.get(index);
        if (rule == null) return;
        INSTANCE.ruleType = rule.type;
        INSTANCE.itemPattern = rule.itemPattern;
        INSTANCE.recipeMapId = rule.recipeMapId;
    }
}
