package com.github.nhaeutilities.modules.patternrouting.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.DimensionManager;

import com.github.nhaeutilities.modules.patterngenerator.config.ForgeConfig;
import com.github.nhaeutilities.modules.patternrouting.service.FilterRule.RuleType;

public final class FilterRuleConfig {

    private static final String CONFIG_DIR = "pattern_routing";
    private static final String CONFIG_FILE = "filter_rules.dat";
    private static final String KEY_RULES = "Rules";
    private static final String KEY_TYPE = "Type";
    private static final String KEY_ITEM_PATTERN = "ItemPattern";
    private static final String KEY_RECIPE_MAP = "RecipeMap";

    private List<FilterRule> rules;

    FilterRuleConfig() {
        this.rules = new ArrayList<>();
    }

    public static FilterRuleConfig load() {
        FilterRuleConfig config = new FilterRuleConfig();
        File file = getConfigFile();
        if (!file.exists()) {
            return config;
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(fis);
            NBTTagList list = root.getTagList(KEY_RULES, 10);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound tag = list.getCompoundTagAt(i);
                String typeName = tag.getString(KEY_TYPE);
                RuleType type;
                try {
                    type = RuleType.valueOf(typeName);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                String itemPattern = tag.getString(KEY_ITEM_PATTERN);
                String recipeMap = tag.getString(KEY_RECIPE_MAP);
                FilterRule rule = new FilterRule(type, itemPattern, recipeMap);
                if (rule.isValid()) {
                    config.rules.add(rule);
                }
            }
        } catch (Exception e) {
            System.err.println("[NHAEUtilities] Failed to load filter rules: " + e.getMessage());
        }
        return config;
    }

    public boolean save() {
        File tmpFile = null;
        try {
            File file = getConfigFile();
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                return false;
            }

            NBTTagList list = new NBTTagList();
            for (FilterRule rule : rules) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setString(KEY_TYPE, rule.type.name());
                tag.setString(KEY_ITEM_PATTERN, rule.itemPattern);
                tag.setString(KEY_RECIPE_MAP, rule.recipeMapId);
                list.appendTag(tag);
            }

            NBTTagCompound root = new NBTTagCompound();
            root.setTag(KEY_RULES, list);

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
            System.err.println("[NHAEUtilities] Failed to save filter rules: " + e.getMessage());
            if (tmpFile != null && tmpFile.exists()) {
                tmpFile.delete();
            }
            return false;
        }
    }

    public List<FilterRule> getRules() {
        return Collections.unmodifiableList(rules);
    }

    public void addRule(FilterRule rule) {
        if (rule != null && rule.isValid()) {
            rules.add(rule);
        }
    }

    public void updateRule(int index, FilterRule rule) {
        if (index >= 0 && index < rules.size() && rule != null && rule.isValid()) {
            rules.set(index, rule);
        }
    }

    public void removeRule(int index) {
        if (index >= 0 && index < rules.size()) {
            rules.remove(index);
        }
    }

    public int size() {
        return rules.size();
    }

    public FilterRule getRule(int index) {
        if (index >= 0 && index < rules.size()) {
            return rules.get(index);
        }
        return null;
    }

    private static File getConfigFile() {
        File worldDir = DimensionManager.getCurrentSaveRootDirectory();
        File configDir = new File(new File(worldDir, ForgeConfig.getStorageDirectoryName()), CONFIG_DIR);
        return new File(configDir, CONFIG_FILE);
    }
}
