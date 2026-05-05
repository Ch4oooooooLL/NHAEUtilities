package com.github.nhaeutilities.modules.patternrouting.service;

public final class FilterRule {

    public enum RuleType {
        BLACKLIST,
        MANUAL_MATCH
    }

    public final RuleType type;
    public final String itemPattern;
    public final String recipeMapId;

    public FilterRule(RuleType type, String recipeMapId) {
        this(type, "", recipeMapId);
    }

    public FilterRule(RuleType type, String itemPattern, String recipeMapId) {
        this.type = type;
        this.itemPattern = itemPattern != null ? itemPattern.trim() : "";
        this.recipeMapId = recipeMapId != null ? recipeMapId.trim() : "";
    }

    public boolean isValid() {
        if (recipeMapId.isEmpty()) return false;
        if (type == RuleType.MANUAL_MATCH && itemPattern.isEmpty()) return false;
        return true;
    }

    @Override
    public String toString() {
        if (type == RuleType.BLACKLIST) {
            return "BLACKLIST:" + recipeMapId;
        }
        return "MANUAL:" + itemPattern + " -> " + recipeMapId;
    }
}
