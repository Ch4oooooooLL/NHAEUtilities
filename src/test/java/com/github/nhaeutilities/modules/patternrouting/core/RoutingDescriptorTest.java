package com.github.nhaeutilities.modules.patternrouting.core;

import static org.junit.Assert.assertEquals;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.junit.BeforeClass;
import org.junit.Test;

public class RoutingDescriptorTest {

    @BeforeClass
    public static void initializeMinecraftBootstrap() {
        try {
            Class<?> bootstrap = Class.forName("net.minecraft.init.Bootstrap");
            try {
                bootstrap.getMethod("register")
                    .invoke(null);
                return;
            } catch (NoSuchMethodException ignored) {}

            bootstrap.getMethod("func_151354_b")
                .invoke(null);
        } catch (Exception ignored) {}
    }

    @Test
    public void descriptorNormalizesNullsToEmptyStrings() {
        RoutingDescriptor descriptor = new RoutingDescriptor(null, null, null);

        assertEquals("", descriptor.recipeCategory);
        assertEquals("", descriptor.circuitKey);
        assertEquals("", descriptor.manualItemsKey);
    }

    @Test
    public void routingMetadataRoundTripsRecipeCategoryCircuitAndManualItems() {
        ItemStack pattern = new ItemStack(Items.paper, 1, 0);
        PatternRoutingNbt.writeRoutingData(
            pattern,
            PatternRoutingNbt.RoutingMetadata.forDescriptor(
                new RoutingDescriptor("gt.recipe.canner", "gt.integrated_circuit@5", "minecraft:bucket@0"),
                PatternRoutingKeys.SOURCE_NEI,
                "{\"snapshot\":true}"));

        PatternRoutingNbt.RoutingMetadata restored = PatternRoutingNbt.readRoutingData(pattern);

        assertEquals("gt.recipe.canner", restored.recipeCategory);
        assertEquals("gt.integrated_circuit@5", restored.circuitKey);
        assertEquals("minecraft:bucket@0", restored.manualItemsKey);
    }
}
