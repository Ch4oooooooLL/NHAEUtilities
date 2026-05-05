package com.github.nhaeutilities.modules.patternrouting.gui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.github.nhaeutilities.modules.patternrouting.service.FilterRule;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class PatternIndexConfigState {

    private static final PatternIndexConfigState INSTANCE = new PatternIndexConfigState();

    public volatile List<FilterRule> rules;

    private PatternIndexConfigState() {
        rules = new ArrayList<>();
    }

    public static PatternIndexConfigState instance() {
        return INSTANCE;
    }

    public static void setRules(List<FilterRule> newRules) {
        INSTANCE.rules = newRules != null ? new ArrayList<>(newRules) : new ArrayList<FilterRule>();
    }

    public static List<FilterRule> getRules() {
        return Collections.unmodifiableList(INSTANCE.rules);
    }
}
