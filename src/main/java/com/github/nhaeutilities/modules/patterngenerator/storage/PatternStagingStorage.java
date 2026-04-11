package com.github.nhaeutilities.modules.patterngenerator.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.DimensionManager;

import com.github.nhaeutilities.modules.patterngenerator.config.ForgeConfig;
import com.github.nhaeutilities.modules.patterngenerator.routing.PatternRoutingNbt;

public final class PatternStagingStorage {

    private static final String STAGING_DIRECTORY = "pattern_routing_staging";
    private static final String KEY_GROUPS = "Groups";
    private static final String KEY_TOTAL_PATTERNS = "TotalPatterns";
    private static final String KEY_UPDATED_AT = "UpdatedAt";

    private static volatile File storageRootForTests;

    private PatternStagingStorage() {}

    public static boolean append(UUID playerId, ItemStack pattern, long timestamp) {
        if (playerId == null || pattern == null) {
            return false;
        }

        PatternRoutingNbt.RoutingMetadata metadata = PatternRoutingNbt.readRoutingData(pattern);
        String groupKey = PatternRoutingNbt.buildStagingGroupKey(metadata);
        List<PatternStagingGroup> groups = loadGroups(playerId);
        List<PatternStagingGroup> updated = new ArrayList<PatternStagingGroup>(groups.size() + 1);
        boolean appended = false;

        for (PatternStagingGroup group : groups) {
            if (group.groupKey.equals(groupKey)) {
                updated.add(group.append(pattern.copy(), timestamp));
                appended = true;
            } else {
                updated.add(group);
            }
        }

        if (!appended) {
            updated.add(
                PatternStagingGroup.create(
                    groupKey,
                    metadata.recipeId,
                    metadata.circuitKey,
                    metadata.manualItemsKey,
                    timestamp,
                    pattern.copy()));
        }

        return saveGroups(playerId, updated, timestamp);
    }

    public static List<PatternStagingGroup> loadGroups(UUID playerId) {
        if (playerId == null) {
            return Collections.emptyList();
        }

        File file = getStorageFile(playerId);
        if (!file.exists()) {
            return Collections.emptyList();
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(fis);
            NBTTagList list = root.getTagList(KEY_GROUPS, 10);
            List<PatternStagingGroup> groups = new ArrayList<PatternStagingGroup>(list.tagCount());
            for (int i = 0; i < list.tagCount(); i++) {
                groups.add(PatternStagingGroup.fromNbt(list.getCompoundTagAt(i)));
            }
            return groups;
        } catch (Exception e) {
            System.err.println("[NHAEUtilities] Failed to load pattern staging storage: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    public static StorageSummary getSummary(UUID playerId) {
        List<PatternStagingGroup> groups = loadGroups(playerId);
        if (groups.isEmpty()) {
            return StorageSummary.EMPTY;
        }

        int totalPatterns = 0;
        long updatedAt = 0L;
        List<GroupSummary> summaries = new ArrayList<GroupSummary>(groups.size());
        for (PatternStagingGroup group : groups) {
            totalPatterns += group.patterns.size();
            updatedAt = Math.max(updatedAt, group.timestamp);
            summaries.add(
                new GroupSummary(
                    group.groupKey,
                    group.recipeId,
                    group.circuitKey,
                    group.manualItemsKey,
                    group.patterns.size(),
                    group.timestamp,
                    extractPreview(group)));
        }
        return new StorageSummary(groups.size(), totalPatterns, updatedAt, summaries);
    }

    public static void clear(UUID playerId) {
        if (playerId == null) {
            return;
        }

        File file = getStorageFile(playerId);
        if (file.exists()) {
            file.delete();
        }
    }

    static void setStorageRootForTests(File root) {
        storageRootForTests = root;
    }

    static void resetStorageRootForTests() {
        storageRootForTests = null;
    }

    private static boolean saveGroups(UUID playerId, List<PatternStagingGroup> groups, long updatedAt) {
        File tmpFile = null;
        try {
            File file = getStorageFile(playerId);
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return false;
            }

            int totalPatterns = 0;
            NBTTagList list = new NBTTagList();
            for (PatternStagingGroup group : groups) {
                totalPatterns += group.patterns.size();
                list.appendTag(group.toNbt());
            }

            NBTTagCompound root = new NBTTagCompound();
            root.setTag(KEY_GROUPS, list);
            root.setInteger(KEY_TOTAL_PATTERNS, totalPatterns);
            root.setLong(KEY_UPDATED_AT, updatedAt);

            tmpFile = new File(file.getParentFile(), file.getName() + ".tmp");
            try (FileOutputStream fos = new FileOutputStream(tmpFile)) {
                CompressedStreamTools.writeCompressed(root, fos);
            }

            try {
                Files.move(
                    tmpFile.toPath(),
                    file.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (Exception e) {
            System.err.println("[NHAEUtilities] Failed to save pattern staging storage: " + e.getMessage());
            if (tmpFile != null && tmpFile.exists()) {
                tmpFile.delete();
            }
            return false;
        }
    }

    private static File getStorageFile(UUID playerId) {
        File worldDir = storageRootForTests != null ? storageRootForTests
            : DimensionManager.getCurrentSaveRootDirectory();
        File storageDir = new File(new File(worldDir, ForgeConfig.getStorageDirectoryName()), STAGING_DIRECTORY);
        return new File(storageDir, playerId.toString() + ".dat");
    }

    public static final class StorageSummary {

        public static final StorageSummary EMPTY = new StorageSummary(0, 0, 0L, Collections.<GroupSummary>emptyList());

        public final int groupCount;
        public final int totalPatterns;
        public final long updatedAt;
        public final List<GroupSummary> groups;

        public StorageSummary(int groupCount, int totalPatterns, long updatedAt, List<GroupSummary> groups) {
            this.groupCount = groupCount;
            this.totalPatterns = totalPatterns;
            this.updatedAt = updatedAt;
            this.groups = groups != null ? new ArrayList<GroupSummary>(groups) : new ArrayList<GroupSummary>();
        }

        public boolean isEmpty() {
            return groupCount == 0 || totalPatterns == 0;
        }
    }

    public static final class GroupSummary {

        public final String groupKey;
        public final String recipeId;
        public final String circuitKey;
        public final String manualItemsKey;
        public final int patternCount;
        public final long updatedAt;
        public final String preview;

        public GroupSummary(String groupKey, String recipeId, String circuitKey, String manualItemsKey,
            int patternCount, long updatedAt, String preview) {
            this.groupKey = groupKey != null ? groupKey : "";
            this.recipeId = recipeId != null ? recipeId : "";
            this.circuitKey = circuitKey != null ? circuitKey : "";
            this.manualItemsKey = manualItemsKey != null ? manualItemsKey : "";
            this.patternCount = patternCount;
            this.updatedAt = updatedAt;
            this.preview = preview != null ? preview : "";
        }
    }

    private static String extractPreview(PatternStagingGroup group) {
        if (group == null || group.patterns.isEmpty()) {
            return "";
        }
        ItemStack latest = group.patterns.get(group.patterns.size() - 1);
        return extractPreview(latest);
    }

    private static String extractPreview(ItemStack pattern) {
        if (pattern == null) {
            return "";
        }
        if (!pattern.hasTagCompound()) {
            return pattern.getDisplayName();
        }

        NBTTagCompound tag = pattern.getTagCompound();
        NBTTagList outList = tag.getTagList("out", 10);
        if (outList.tagCount() == 0) {
            return pattern.getDisplayName();
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < outList.tagCount(); i++) {
            ItemStack output = ItemStack.loadItemStackFromNBT(outList.getCompoundTagAt(i));
            if (output == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(", ");
            }
            long count = outList.getCompoundTagAt(i)
                .hasKey("Cnt")
                    ? outList.getCompoundTagAt(i)
                        .getLong("Cnt")
                    : output.stackSize;
            if (count <= 0) {
                count = 1;
            }
            builder.append(output.getDisplayName());
            if (count > 1) {
                builder.append(" x")
                    .append(count);
            }
        }
        return builder.length() > 0 ? builder.toString() : pattern.getDisplayName();
    }
}
