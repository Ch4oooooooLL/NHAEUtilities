package com.github.nhaeutilities.modules.patternrouting.gui;

import com.github.nhaeutilities.modules.patterngenerator.util.I18nUtil;
import com.github.nhaeutilities.modules.patternrouting.core.RecipeMapAnalysisResult;

public final class GuiPatternRoutingAnalysisState {

    private static volatile Snapshot snapshot = Snapshot.empty();

    private GuiPatternRoutingAnalysisState() {}

    public static void clear() {
        snapshot = Snapshot.empty();
    }

    public static void setLoading(String recipeMapId) {
        snapshot = new Snapshot(normalize(recipeMapId), true, "", "", null);
    }

    public static void setError(String recipeMapId, String messageKey) {
        String normalizedMessageKey = normalize(messageKey);
        snapshot = new Snapshot(
            normalize(recipeMapId),
            false,
            normalizedMessageKey,
            normalizedMessageKey.isEmpty() ? "" : I18nUtil.tr(normalizedMessageKey),
            null);
    }

    public static void setResult(String recipeMapId, RecipeMapAnalysisResult result) {
        snapshot = new Snapshot(normalize(recipeMapId), false, "", "", result);
    }

    public static Snapshot snapshot() {
        return snapshot;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Snapshot {

        public final String recipeMapId;
        public final boolean loading;
        public final String errorKey;
        public final String errorMessage;
        public final RecipeMapAnalysisResult result;

        private Snapshot(String recipeMapId, boolean loading, String errorKey, String errorMessage,
            RecipeMapAnalysisResult result) {
            this.recipeMapId = normalize(recipeMapId);
            this.loading = loading;
            this.errorKey = normalize(errorKey);
            this.errorMessage = normalize(errorMessage);
            this.result = result;
        }

        private static Snapshot empty() {
            return new Snapshot("", false, "", "", null);
        }
    }
}
