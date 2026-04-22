package com.github.nhaeutilities.modules.patternrouting.core;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import cpw.mods.fml.common.registry.GameRegistry;

public final class PatternRoutingNbt {

    private PatternRoutingNbt() {}

    public static void writeRoutingData(ItemStack pattern, RoutingMetadata metadata) {
        if (pattern == null || metadata == null) {
            return;
        }
        NBTTagCompound stackTag = getOrCreateStackTag(pattern);
        NBTTagCompound routingTag = getOrCreateRoutingTag(stackTag);
        routingTag.setInteger(PatternRoutingKeys.VERSION_KEY, metadata.version);
        setString(routingTag, PatternRoutingKeys.RECIPE_CATEGORY_KEY, metadata.recipeCategory);
        setString(routingTag, PatternRoutingKeys.RECIPE_ID_KEY, metadata.recipeId);
        setString(routingTag, PatternRoutingKeys.ASSIGNMENT_KEY, metadata.assignmentKey);
        setString(routingTag, PatternRoutingKeys.CIRCUIT_KEY, metadata.circuitKey);
        setString(routingTag, PatternRoutingKeys.MANUAL_ITEMS_KEY, metadata.manualItemsKey);
        setString(routingTag, PatternRoutingKeys.SOURCE_KEY, metadata.source);
        routingTag.setBoolean(PatternRoutingKeys.HAS_DIRECT_ROUTE_KEY, metadata.hasDirectRoute);
        setString(routingTag, PatternRoutingKeys.PROGRAMMING_CIRCUIT_KEY, metadata.programmingCircuit);
        setString(routingTag, PatternRoutingKeys.NON_CONSUMABLES_KEY, metadata.nonConsumables);
        setString(routingTag, PatternRoutingKeys.RECIPE_SNAPSHOT_KEY, metadata.recipeSnapshot);
        pattern.setTagCompound(stackTag);
        PatternRoutingLog.debug(
            "[NHAEUtilities][patternrouting][nbt] write routing nbt item=%s recipeCategory=%s recipeId=%s assignment=%s circuit=%s manual=%s source=%s direct=%s nc=%s",
            itemSignature(pattern),
            metadata.recipeCategory,
            metadata.recipeId,
            metadata.assignmentKey,
            metadata.circuitKey,
            metadata.manualItemsKey,
            metadata.source,
            metadata.hasDirectRoute,
            metadata.nonConsumables);
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
            getString(routingTag, PatternRoutingKeys.RECIPE_CATEGORY_KEY),
            getString(routingTag, PatternRoutingKeys.RECIPE_ID_KEY),
            getString(routingTag, PatternRoutingKeys.ASSIGNMENT_KEY),
            getString(routingTag, PatternRoutingKeys.CIRCUIT_KEY),
            getString(routingTag, PatternRoutingKeys.MANUAL_ITEMS_KEY),
            getString(routingTag, PatternRoutingKeys.SOURCE_KEY),
            routingTag.getBoolean(PatternRoutingKeys.HAS_DIRECT_ROUTE_KEY),
            getString(routingTag, PatternRoutingKeys.PROGRAMMING_CIRCUIT_KEY),
            getString(routingTag, PatternRoutingKeys.NON_CONSUMABLES_KEY),
            getString(routingTag, PatternRoutingKeys.RECIPE_SNAPSHOT_KEY));
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

    public static String manualItemsKeyFromJson(String nonConsumablesJson) {
        return manualItemsKeyFromJson(nonConsumablesJson, "");
    }

    public static String manualItemsKeyFromJson(String nonConsumablesJson, String programmingCircuitKey) {
        if (nonConsumablesJson == null || nonConsumablesJson.trim()
            .isEmpty() || "[]".equals(nonConsumablesJson.trim())) {
            return "";
        }

        StringBuilder key = new StringBuilder();
        int index = 0;
        while (index >= 0 && index < nonConsumablesJson.length()) {
            index = nonConsumablesJson.indexOf("\"item\":\"", index);
            if (index < 0) {
                break;
            }
            index += 8;
            int end = nonConsumablesJson.indexOf('"', index);
            if (end < 0) {
                break;
            }
            String signature = nonConsumablesJson.substring(index, end)
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
            if (!signature.isEmpty() && !signature.equals(programmingCircuitKey)) {
                if (key.length() > 0) {
                    key.append("|");
                }
                key.append(signature);
            }
            index = end + 1;
        }
        return key.toString();
    }

    public static String buildAssignmentKey(String recipeCategory, String circuitKey, String manualItemsKey) {
        return nullToEmpty(recipeCategory) + "|" + nullToEmpty(circuitKey) + "|" + nullToEmpty(manualItemsKey);
    }

    public static String buildAssignmentKey(String recipeFamily, String controllerKey, String circuitKey,
        String manualItemsKey) {
        return buildAssignmentKey(recipeFamily, circuitKey, manualItemsKey);
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
        return nullToEmpty(metadata.recipeCategory) + "|"
            + nullToEmpty(metadata.circuitKey)
            + "|"
            + nullToEmpty(metadata.manualItemsKey);
    }

    public static ItemStack itemStackFromSignature(String signature) {
        if (signature == null || signature.isEmpty()) {
            return null;
        }

        int firstSeparator = signature.indexOf('@');
        if (firstSeparator <= 0 || firstSeparator >= signature.length() - 1) {
            return null;
        }

        int secondSeparator = signature.indexOf('@', firstSeparator + 1);
        String itemId = signature.substring(0, firstSeparator);
        String damageToken = secondSeparator >= 0 ? signature.substring(firstSeparator + 1, secondSeparator)
            : signature.substring(firstSeparator + 1);
        Item item = resolveItem(itemId);
        if (item == null) {
            return null;
        }

        int damage;
        try {
            damage = Integer.parseInt(damageToken);
        } catch (NumberFormatException ignored) {
            return null;
        }

        ItemStack stack = new ItemStack(item, 1, damage);
        if (secondSeparator >= 0 && secondSeparator < signature.length() - 1) {
            try {
                stack
                    .setTagCompound((NBTTagCompound) JsonToNBT.func_150315_a(signature.substring(secondSeparator + 1)));
            } catch (Exception ignored) {
                return null;
            }
        }
        return stack;
    }

    public static ItemStack[] itemStacksFromManualItemsKey(String manualItemsKey) {
        if (manualItemsKey == null || manualItemsKey.isEmpty()) {
            return new ItemStack[0];
        }

        java.util.List<ItemStack> stacks = new java.util.ArrayList<ItemStack>();
        for (String token : manualItemsKey.split("\\|")) {
            ItemStack stack = itemStackFromSignature(token);
            if (stack != null) {
                stacks.add(stack);
            }
        }
        return stacks.toArray(new ItemStack[stacks.size()]);
    }

    public static ItemStack[] itemStacksFromNonConsumablesJson(String nonConsumablesJson,
        String programmingCircuitKey) {
        String manualItemsKey = manualItemsKeyFromJson(nonConsumablesJson, programmingCircuitKey);
        return itemStacksFromManualItemsKey(manualItemsKey);
    }

    public static ItemStack programmingCircuitStack(RoutingMetadata metadata) {
        if (metadata == null) {
            return null;
        }
        String signature = !metadata.programmingCircuit.isEmpty() ? metadata.programmingCircuit : metadata.circuitKey;
        return itemStackFromSignature(signature);
    }

    public static ItemStack[] manualItemStacks(RoutingMetadata metadata) {
        if (metadata == null) {
            return new ItemStack[0];
        }
        if (!metadata.nonConsumables.isEmpty() && !"[]".equals(metadata.nonConsumables)) {
            return itemStacksFromNonConsumablesJson(metadata.nonConsumables, metadata.programmingCircuit);
        }
        return itemStacksFromManualItemsKey(metadata.manualItemsKey);
    }

    public static HatchAssignmentData assignmentDataFor(RoutingMetadata metadata) {
        if (metadata == null || metadata.recipeCategory.isEmpty()) {
            return HatchAssignmentData.EMPTY;
        }
        String assignmentKey = buildAssignmentKey(
            metadata.recipeCategory,
            metadata.circuitKey,
            metadata.manualItemsKey);
        return new HatchAssignmentData(
            assignmentKey,
            metadata.recipeCategory,
            metadata.circuitKey,
            metadata.manualItemsKey);
    }

    public static RoutingMetadata withDerivedAssignment(RoutingMetadata metadata) {
        if (metadata == null) {
            return RoutingMetadata.EMPTY;
        }
        return metadata.withAssignmentKey(
            buildAssignmentKey(metadata.recipeCategory, metadata.circuitKey, metadata.manualItemsKey));
    }

    public static RoutingMetadata withDerivedDescriptor(RoutingMetadata metadata) {
        if (metadata == null) {
            return RoutingMetadata.EMPTY;
        }
        String circuitKey = !metadata.circuitKey.isEmpty() ? metadata.circuitKey
            : circuitKey(programmingCircuitStack(metadata));
        String manualItemsKey = !metadata.manualItemsKey.isEmpty() ? metadata.manualItemsKey
            : manualItemsKey(manualItemStacks(metadata));
        return new RoutingMetadata(
            metadata.version,
            metadata.recipeCategory,
            metadata.recipeId,
            metadata.assignmentKey,
            circuitKey,
            manualItemsKey,
            metadata.source,
            metadata.hasDirectRoute,
            metadata.programmingCircuit,
            metadata.nonConsumables,
            metadata.recipeSnapshot);
    }

    public static RoutingMetadata withConfiguredAssignment(RoutingMetadata metadata) {
        return withDerivedAssignment(withDerivedDescriptor(metadata));
    }

    private static Item resolveItem(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            return null;
        }
        Object direct = Item.itemRegistry.getObject(itemId);
        if (direct instanceof Item) {
            return (Item) direct;
        }
        int separator = itemId.indexOf(':');
        if (separator > 0 && separator < itemId.length() - 1) {
            Item namespaced = GameRegistry.findItem(itemId.substring(0, separator), itemId.substring(separator + 1));
            if (namespaced != null) {
                return namespaced;
            }
        }
        try {
            return Item.getItemById(Integer.parseInt(itemId));
        } catch (NumberFormatException ignored) {
            return null;
        }
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
            "",
            false,
            "",
            "[]",
            "{}");

        public final int version;
        public final String recipeCategory;
        public final String recipeId;
        public final String assignmentKey;
        public final String circuitKey;
        public final String manualItemsKey;
        public final String source;
        public final boolean hasDirectRoute;
        public final String programmingCircuit;
        public final String nonConsumables;
        public final String recipeSnapshot;
        public final String overlayIdentifier;

        public static RoutingMetadata forDescriptor(RoutingDescriptor descriptor, String source,
            String recipeSnapshot) {
            return new RoutingMetadata(
                PatternRoutingKeys.CURRENT_VERSION,
                descriptor != null ? descriptor.recipeCategory : "",
                "",
                "",
                descriptor != null ? descriptor.circuitKey : "",
                descriptor != null ? descriptor.manualItemsKey : "",
                source,
                false,
                descriptor != null ? descriptor.circuitKey : "",
                "[]",
                recipeSnapshot);
        }

        public RoutingMetadata(int version, String recipeId, String assignmentKey, String circuitKey,
            String manualItemsKey, String source, boolean hasDirectRoute) {
            this(version, "", recipeId, assignmentKey, circuitKey, manualItemsKey, source, hasDirectRoute);
        }

        public RoutingMetadata(int version, String recipeId, String assignmentKey, String circuitKey,
            String manualItemsKey, String source, boolean hasDirectRoute, String overlayIdentifier) {
            this(
                version,
                overlayIdentifier,
                recipeId,
                assignmentKey,
                circuitKey,
                manualItemsKey,
                source,
                hasDirectRoute,
                "",
                "[]",
                "{}");
        }

        public RoutingMetadata(int version, String recipeId, String assignmentKey, String circuitKey,
            String manualItemsKey, String source, boolean hasDirectRoute, String overlayIdentifier,
            String programmingCircuit, String nonConsumables, String recipeSnapshot) {
            this(
                version,
                overlayIdentifier,
                recipeId,
                assignmentKey,
                circuitKey,
                manualItemsKey,
                source,
                hasDirectRoute,
                programmingCircuit,
                nonConsumables,
                recipeSnapshot);
        }

        public RoutingMetadata(int version, String recipeCategory, String recipeId, String assignmentKey,
            String circuitKey, String manualItemsKey, String source, boolean hasDirectRoute) {
            this(
                version,
                recipeCategory,
                recipeId,
                assignmentKey,
                circuitKey,
                manualItemsKey,
                source,
                hasDirectRoute,
                "",
                "[]",
                "{}");
        }

        public RoutingMetadata(int version, String recipeCategory, String recipeId, String assignmentKey,
            String circuitKey, String manualItemsKey, String source, boolean hasDirectRoute, String programmingCircuit,
            String nonConsumables, String recipeSnapshot) {
            this.version = version;
            this.recipeCategory = nullToEmpty(recipeCategory);
            this.recipeId = nullToEmpty(recipeId);
            this.assignmentKey = nullToEmpty(assignmentKey);
            this.circuitKey = nullToEmpty(circuitKey);
            this.manualItemsKey = nullToEmpty(manualItemsKey);
            this.source = nullToEmpty(source);
            this.hasDirectRoute = hasDirectRoute;
            this.programmingCircuit = nullToEmpty(programmingCircuit);
            this.nonConsumables = nullToEmpty(nonConsumables);
            this.recipeSnapshot = nullToEmpty(recipeSnapshot);
            this.overlayIdentifier = this.recipeCategory;
        }

        public RoutingMetadata withAssignmentKey(String assignmentKey) {
            return new RoutingMetadata(
                version,
                recipeCategory,
                recipeId,
                assignmentKey,
                circuitKey,
                manualItemsKey,
                source,
                hasDirectRoute,
                programmingCircuit,
                nonConsumables,
                recipeSnapshot);
        }

        public RoutingMetadata withDirectRoute(boolean hasDirectRoute) {
            return new RoutingMetadata(
                version,
                recipeCategory,
                recipeId,
                assignmentKey,
                circuitKey,
                manualItemsKey,
                source,
                hasDirectRoute,
                programmingCircuit,
                nonConsumables,
                recipeSnapshot);
        }

        public RoutingDescriptor toDescriptor() {
            return new RoutingDescriptor(recipeCategory, circuitKey, manualItemsKey);
        }

        public boolean isEmpty() {
            return recipeCategory.isEmpty() && assignmentKey.isEmpty()
                && circuitKey.isEmpty()
                && manualItemsKey.isEmpty()
                && source.isEmpty()
                && programmingCircuit.isEmpty()
                && (nonConsumables.isEmpty() || "[]".equals(nonConsumables))
                && (recipeSnapshot.isEmpty() || "{}".equals(recipeSnapshot))
                && !hasDirectRoute;
        }
    }
}
