package com.github.nhaeutilities.modules.patterngenerator.recipe;

import java.util.Collections;
import java.util.List;

public class RecipeAnalysisSnapshot {

    public enum Status {
        COMPLETE,
        PARTIAL,
        UNRESOLVED
    }

    public final Status status;
    public final String recipeMapId;
    public final String recipeId;
    public final List<RecipeInputSemanticSnapshot> inputSnapshots;
    public final String groupingKey;
    public final String programmingCircuit;
    public final int circuitNumber;
    public final List<String> nonConsumableSignatures;
    public final int consumableCount;

    public RecipeAnalysisSnapshot(Status status, String recipeMapId, String recipeId,
        List<RecipeInputSemanticSnapshot> inputSnapshots, String groupingKey, String programmingCircuit,
        int circuitNumber, List<String> nonConsumableSignatures, int consumableCount) {
        this.status = status;
        this.recipeMapId = recipeMapId != null ? recipeMapId : "";
        this.recipeId = recipeId != null ? recipeId : "";
        this.inputSnapshots = inputSnapshots != null ? Collections.unmodifiableList(inputSnapshots)
            : Collections.<RecipeInputSemanticSnapshot>emptyList();
        this.groupingKey = groupingKey != null ? groupingKey : "";
        this.programmingCircuit = programmingCircuit != null ? programmingCircuit : "";
        this.circuitNumber = circuitNumber;
        this.nonConsumableSignatures = nonConsumableSignatures != null
            ? Collections.unmodifiableList(nonConsumableSignatures)
            : Collections.<String>emptyList();
        this.consumableCount = consumableCount;
    }
}
