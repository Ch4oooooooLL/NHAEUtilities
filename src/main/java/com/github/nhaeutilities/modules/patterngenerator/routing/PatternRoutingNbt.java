package com.github.nhaeutilities.modules.patterngenerator.routing;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

public final class PatternRoutingNbt {

    private PatternRoutingNbt() {}

    public static void writeRoutingData(ItemStack pattern, RoutingMetadata metadata) {
        if (pattern == null || metadata == null) {
            return;
        }
        NBTTagCompound stackTag = getOrCreateStackTag(pattern);
        NBTTagCompound routingTag = getOrCreateRoutingTag(stackTag);
        routingTag.setInteger(PatternRoutingKeys.VERSION_KEY, metadata.version);
        setString(routingTag, PatternRoutingKeys.RECIPE_ID_KEY, metadata.recipeId);
        setString(routingTag, PatternRoutingKeys.ASSIGNMENT_KEY, metadata.assignmentKey);
        setString(routingTag, PatternRoutingKeys.CIRCUIT_KEY, metadata.circuitKey);
        setString(routingTag, PatternRoutingKeys.MANUAL_ITEMS_KEY, metadata.manualItemsKey);
        setString(routingTag, PatternRoutingKeys.SOURCE_KEY, metadata.source);
        routingTag.setBoolean(PatternRoutingKeys.HAS_DIRECT_ROUTE_KEY, metadata.hasDirectRoute);
        setString(routingTag, PatternRoutingKeys.OVERLAY_IDENTIFIER_KEY, metadata.overlayIdentifier);
        pattern.setTagCompound(stackTag);
    }

    public static RoutingMetadata readRoutingData(ItemStack pattern) {
        if (pattern == null || !pattern.hasTagCompound()) {
            return RoutingMetadata.EMPTY;
        }

        NBTTagCompound root = pattern.getTagCompound()
            .getCompoundTag(PatternRoutingKeys.ROOT_KEY);
        if (root == null || !root.hasKey(PatternRoutingKeys.ROUTING_KEY)) {
            return RoutingMetadata.EMPTY;
        }

        NBTTagCompound routingTag = root.getCompoundTag(PatternRoutingKeys.ROUTING_KEY);
        return new RoutingMetadata(
            routingTag.hasKey(PatternRoutingKeys.VERSION_KEY) ? routingTag.getInteger(PatternRoutingKeys.VERSION_KEY)
                : PatternRoutingKeys.CURRENT_VERSION,
            getString(routingTag, PatternRoutingKeys.RECIPE_ID_KEY),
            getString(routingTag, PatternRoutingKeys.ASSIGNMENT_KEY),
            getString(routingTag, PatternRoutingKeys.CIRCUIT_KEY),
            getString(routingTag, PatternRoutingKeys.MANUAL_ITEMS_KEY),
            getString(routingTag, PatternRoutingKeys.SOURCE_KEY),
            routingTag.getBoolean(PatternRoutingKeys.HAS_DIRECT_ROUTE_KEY),
            getString(routingTag, PatternRoutingKeys.OVERLAY_IDENTIFIER_KEY));
    }

    public static boolean hasRoutingData(ItemStack pattern) {
        return !readRoutingData(pattern).isEmpty();
    }

    public static String itemSignature(ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return "";
        }

        StringBuilder signature = new StringBuilder();
        Object registryName = Item.itemRegistry.getNameForObject(stack.getItem());
        signature.append(registryName != null ? registryName : Item.getIdFromItem(stack.getItem()))
            .append("@")
            .append(stack.getItemDamage());

        if (stack.hasTagCompound()) {
            NBTTagCompound tagCopy = (NBTTagCompound) stack.getTagCompound()
                .copy();
            tagCopy.removeTag("Count");
            tagCopy.removeTag("Cnt");
            if (!tagCopy.hasNoTags()) {
                signature.append("@")
                    .append(tagCopy);
            }
        }

        return signature.toString();
    }

    public static String circuitKey(ItemStack circuitStack) {
        return itemSignature(circuitStack);
    }

    public static String manualItemsKey(ItemStack[] manualItems) {
        if (manualItems == null || manualItems.length == 0) {
            return "";
        }

        StringBuilder key = new StringBuilder();
        for (ItemStack stack : manualItems) {
            String signature = itemSignature(stack);
            if (signature.isEmpty()) {
                continue;
            }
            if (key.length() > 0) {
                key.append("|");
            }
            key.append(signature);
        }
        return key.toString();
    }

    public static String buildAssignmentKey(String recipeFamily, String controllerKey, String circuitKey,
        String manualItemsKey) {
        StringBuilder builder = new StringBuilder();
        builder.append(nullToEmpty(recipeFamily))
            .append("|")
            .append(nullToEmpty(controllerKey))
            .append("|")
            .append(nullToEmpty(circuitKey))
            .append("|")
            .append(nullToEmpty(manualItemsKey));
        return builder.toString();
    }

    public static String inferCircuitKeyFromEncodedPattern(ItemStack pattern) {
        if (pattern == null || !pattern.hasTagCompound()) {
            return "";
        }

        NBTTagList inputs = pattern.getTagCompound()
            .getTagList("in", 10);
        for (int i = 0; i < inputs.tagCount(); i++) {
            ItemStack input = ItemStack.loadItemStackFromNBT(inputs.getCompoundTagAt(i));
            if (input == null || input.getItem() == null) {
                continue;
            }
            String unlocalizedName = input.getUnlocalizedName();
            if (unlocalizedName != null && unlocalizedName.startsWith("gt.integrated_circuit")) {
                return circuitKey(input);
            }
        }
        return "";
    }

    public static String buildStagingGroupKey(RoutingMetadata metadata) {
        if (metadata == null) {
            return "||";
        }
        return nullToEmpty(metadata.recipeId) + "|"
            + nullToEmpty(metadata.circuitKey)
            + "|"
            + nullToEmpty(metadata.manualItemsKey);
    }

    private static NBTTagCompound getOrCreateStackTag(ItemStack stack) {
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        return stack.getTagCompound();
    }

    private static NBTTagCompound getOrCreateRoutingTag(NBTTagCompound stackTag) {
        NBTTagCompound rootTag = stackTag.getCompoundTag(PatternRoutingKeys.ROOT_KEY);
        if (!stackTag.hasKey(PatternRoutingKeys.ROOT_KEY)) {
            rootTag = new NBTTagCompound();
            stackTag.setTag(PatternRoutingKeys.ROOT_KEY, rootTag);
        }

        NBTTagCompound routingTag = rootTag.getCompoundTag(PatternRoutingKeys.ROUTING_KEY);
        if (!rootTag.hasKey(PatternRoutingKeys.ROUTING_KEY)) {
            routingTag = new NBTTagCompound();
            rootTag.setTag(PatternRoutingKeys.ROUTING_KEY, routingTag);
        }
        return routingTag;
    }

    private static void setString(NBTTagCompound tag, String key, String value) {
        tag.setString(key, nullToEmpty(value));
    }

    private static String getString(NBTTagCompound tag, String key) {
        return tag != null && tag.hasKey(key) ? nullToEmpty(tag.getString(key)) : "";
    }

    private static String nullToEmpty(String value) {
        return value != null ? value : "";
    }

    public static final class RoutingMetadata {

        public static final RoutingMetadata EMPTY = new RoutingMetadata(
            PatternRoutingKeys.CURRENT_VERSION,
            "",
            "",
            "",
            "",
            "",
            false,
            "");

        public final int version;
        public final String recipeId;
        public final String assignmentKey;
        public final String circuitKey;
        public final String manualItemsKey;
        public final String source;
        public final boolean hasDirectRoute;
        public final String overlayIdentifier;

        public RoutingMetadata(int version, String recipeId, String assignmentKey, String circuitKey,
            String manualItemsKey, String source, boolean hasDirectRoute) {
            this(version, recipeId, assignmentKey, circuitKey, manualItemsKey, source, hasDirectRoute, "");
        }

        public RoutingMetadata(int version, String recipeId, String assignmentKey, String circuitKey,
            String manualItemsKey, String source, boolean hasDirectRoute, String overlayIdentifier) {
            this.version = version;
            this.recipeId = nullToEmpty(recipeId);
            this.assignmentKey = nullToEmpty(assignmentKey);
            this.circuitKey = nullToEmpty(circuitKey);
            this.manualItemsKey = nullToEmpty(manualItemsKey);
            this.source = nullToEmpty(source);
            this.hasDirectRoute = hasDirectRoute;
            this.overlayIdentifier = nullToEmpty(overlayIdentifier);
        }

        public boolean isEmpty() {
            return recipeId.isEmpty() && assignmentKey.isEmpty()
                && circuitKey.isEmpty()
                && manualItemsKey.isEmpty()
                && source.isEmpty()
                && overlayIdentifier.isEmpty()
                && !hasDirectRoute;
        }
    }
}
