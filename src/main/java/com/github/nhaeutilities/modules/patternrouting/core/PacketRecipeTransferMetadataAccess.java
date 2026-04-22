package com.github.nhaeutilities.modules.patternrouting.core;

public interface PacketRecipeTransferMetadataAccess {

    String nhaeutilities$getRecipeId();

    void nhaeutilities$setRecipeId(String recipeId);

    String nhaeutilities$getOverlayIdentifier();

    void nhaeutilities$setOverlayIdentifier(String overlayIdentifier);

    String nhaeutilities$getProgrammingCircuit();

    void nhaeutilities$setProgrammingCircuit(String programmingCircuit);

    String nhaeutilities$getNonConsumables();

    void nhaeutilities$setNonConsumables(String nonConsumables);

    String nhaeutilities$getRecipeSnapshot();

    void nhaeutilities$setRecipeSnapshot(String recipeSnapshot);
}
