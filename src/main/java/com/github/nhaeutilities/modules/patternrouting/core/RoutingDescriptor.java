package com.github.nhaeutilities.modules.patternrouting.core;

public final class RoutingDescriptor {

    public final String recipeCategory;
    public final String circuitKey;
    public final String manualItemsKey;

    public RoutingDescriptor(String recipeCategory, String circuitKey, String manualItemsKey) {
        this.recipeCategory = normalize(recipeCategory);
        this.circuitKey = normalize(circuitKey);
        this.manualItemsKey = normalize(manualItemsKey);
    }

    public boolean isEmpty() {
        return recipeCategory.isEmpty() && circuitKey.isEmpty() && manualItemsKey.isEmpty();
    }

    private static String normalize(String value) {
        return value != null ? value : "";
    }
}
