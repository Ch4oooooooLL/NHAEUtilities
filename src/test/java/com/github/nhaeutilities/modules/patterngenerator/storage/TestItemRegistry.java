package com.github.nhaeutilities.modules.patterngenerator.storage;

import java.lang.reflect.Method;

import net.minecraft.item.Item;

import cpw.mods.fml.common.registry.GameData;

final class TestItemRegistry {

    private static final int TEST_PATTERN_ITEM_ID = 5000;
    private static final String TEST_PATTERN_ITEM_NAME = "nhaeutilities:test_pattern";

    private TestItemRegistry() {}

    static Item getOrCreatePatternItem() {
        Item existing = (Item) GameData.getItemRegistry()
            .getObject(TEST_PATTERN_ITEM_NAME);
        if (existing != null) {
            return existing;
        }

        Item created = new Item();
        try {
            Method addObjectRaw = GameData.getItemRegistry()
                .getClass()
                .getDeclaredMethod("addObjectRaw", int.class, String.class, Object.class);
            addObjectRaw.setAccessible(true);
            addObjectRaw.invoke(GameData.getItemRegistry(), TEST_PATTERN_ITEM_ID, TEST_PATTERN_ITEM_NAME, created);
            return created;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to register test pattern item", e);
        }
    }
}
