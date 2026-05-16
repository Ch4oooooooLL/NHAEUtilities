package com.github.nhaeutilities.modules.shared.nei;

import com.github.nhaeutilities.modules.patternrouting.core.RecipeTransferMetadataExtractor;

import codechicken.nei.recipe.IRecipeHandler;

public final class NeiRecipeData {

    public final String recipeMapId;
    public final String recipeName;
    public final IRecipeHandler recipeHandler;
    public final int recipeIndex;
    public final RecipeTransferMetadataExtractor.Metadata snapshot;

    NeiRecipeData(String recipeMapId, String recipeName, IRecipeHandler recipeHandler, int recipeIndex,
        RecipeTransferMetadataExtractor.Metadata snapshot) {
        this.recipeMapId = recipeMapId;
        this.recipeName = recipeName;
        this.recipeHandler = recipeHandler;
        this.recipeIndex = recipeIndex;
        this.snapshot = snapshot;
    }
}
