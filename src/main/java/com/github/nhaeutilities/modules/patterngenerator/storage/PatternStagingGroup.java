package com.github.nhaeutilities.modules.patterngenerator.storage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public final class PatternStagingGroup {

    private static final String KEY_FALLBACK_ITEM_ID = "NhaeItemId";
    private static final String KEY_GROUP_KEY = "GroupKey";
    private static final String KEY_RECIPE_ID = "RecipeId";
    private static final String KEY_CIRCUIT_KEY = "CircuitKey";
    private static final String KEY_MANUAL_ITEMS_KEY = "ManualItemsKey";
    private static final String KEY_TIMESTAMP = "Timestamp";
    private static final String KEY_PATTERNS = "Patterns";

    public final String groupKey;
    public final String recipeId;
    public final String circuitKey;
    public final String manualItemsKey;
    public final long timestamp;
    public final List<ItemStack> patterns;

    public PatternStagingGroup(String groupKey, String recipeId, String circuitKey, String manualItemsKey,
        long timestamp, List<ItemStack> patterns) {
        this.groupKey = normalize(groupKey);
        this.recipeId = normalize(recipeId);
        this.circuitKey = normalize(circuitKey);
        this.manualItemsKey = normalize(manualItemsKey);
        this.timestamp = timestamp;
        this.patterns = patterns != null ? new ArrayList<ItemStack>(patterns) : new ArrayList<ItemStack>();
    }

    public NBTTagCompound toNbt() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(KEY_GROUP_KEY, groupKey);
        tag.setString(KEY_RECIPE_ID, recipeId);
        tag.setString(KEY_CIRCUIT_KEY, circuitKey);
        tag.setString(KEY_MANUAL_ITEMS_KEY, manualItemsKey);
        tag.setLong(KEY_TIMESTAMP, timestamp);

        NBTTagList patternList = new NBTTagList();
        for (ItemStack pattern : patterns) {
            if (pattern == null) {
                continue;
            }
            NBTTagCompound patternTag = new NBTTagCompound();
            pattern.writeToNBT(patternTag);
            Object registryName = pattern.getItem() != null ? Item.itemRegistry.getNameForObject(pattern.getItem()) : null;
            if (registryName != null) {
                patternTag.setString(KEY_FALLBACK_ITEM_ID, registryName.toString());
            }
            patternList.appendTag(patternTag);
        }
        tag.setTag(KEY_PATTERNS, patternList);
        return tag;
    }

    public static PatternStagingGroup fromNbt(NBTTagCompound tag) {
        if (tag == null) {
            return new PatternStagingGroup("", "", "", "", 0L, Collections.<ItemStack>emptyList());
        }

        NBTTagList patternList = tag.getTagList(KEY_PATTERNS, 10);
        List<ItemStack> patterns = new ArrayList<ItemStack>(patternList.tagCount());
        for (int i = 0; i < patternList.tagCount(); i++) {
            NBTTagCompound patternTag = patternList.getCompoundTagAt(i);
            ItemStack stack = ItemStack.loadItemStackFromNBT(patternTag);
            if (stack == null) {
                stack = loadItemStackCompat(patternTag);
            }
            if (stack != null) {
                patterns.add(stack);
            }
        }

        return new PatternStagingGroup(
            tag.getString(KEY_GROUP_KEY),
            tag.getString(KEY_RECIPE_ID),
            tag.getString(KEY_CIRCUIT_KEY),
            tag.getString(KEY_MANUAL_ITEMS_KEY),
            tag.getLong(KEY_TIMESTAMP),
            patterns);
    }

    public static PatternStagingGroup create(String groupKey, String recipeId, String circuitKey, String manualItemsKey,
        long timestamp, ItemStack firstPattern) {
        List<ItemStack> patterns = new ArrayList<ItemStack>(1);
        if (firstPattern != null) {
            patterns.add(firstPattern);
        }
        return new PatternStagingGroup(groupKey, recipeId, circuitKey, manualItemsKey, timestamp, patterns);
    }

    public PatternStagingGroup append(ItemStack pattern, long updatedAt) {
        List<ItemStack> updated = new ArrayList<ItemStack>(patterns);
        if (pattern != null) {
            updated.add(pattern);
        }
        return new PatternStagingGroup(groupKey, recipeId, circuitKey, manualItemsKey, updatedAt, updated);
    }

    private static String normalize(String value) {
        return value != null ? value : "";
    }

    private static ItemStack loadItemStackCompat(NBTTagCompound tag) {
        if (tag == null) {
            return null;
        }

        Item item = null;
        if (tag.hasKey(KEY_FALLBACK_ITEM_ID, 8)) {
            item = (Item) Item.itemRegistry.getObject(tag.getString(KEY_FALLBACK_ITEM_ID));
        }
        if (item == null && tag.hasKey("id", 8)) {
            item = (Item) Item.itemRegistry.getObject(tag.getString("id"));
        } else if (item == null && tag.hasKey("id")) {
            item = Item.getItemById(tag.getShort("id"));
        }
        if (item == null) {
            return null;
        }

        int stackSize = tag.hasKey("Count") ? Math.max(1, tag.getByte("Count")) : 1;
        int damage = tag.hasKey("Damage") ? tag.getShort("Damage") : 0;
        ItemStack stack = new ItemStack(item, stackSize, damage);
        if (tag.hasKey("tag", 10)) {
            stack.setTagCompound(tag.getCompoundTag("tag"));
        }
        return stack;
    }
}
