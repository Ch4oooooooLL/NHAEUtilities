package com.github.nhaeutilities.mixin;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;

@LateMixin
public class PatternRoutingLateMixinLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.nhaeutilities.patternrouting.late.json";
    }

    @Override
    public List<String> getMixins(Set<String> loadedMods) {
        return Arrays.asList(
            "MixinNEEPatternTerminalHandler",
            "MixinPacketNEIPatternRecipe",
            "MixinPacketNEIPatternRecipeHandler");
    }
}
