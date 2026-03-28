package com.github.nhaeutilities.modules.patterngenerator.config;

import java.util.Objects;

import net.minecraftforge.common.config.Configuration;

import cpw.mods.fml.common.FMLLog;

public final class ForgeConfig {

    private static final String MODULE_PREFIX = "patternGenerator.";

    private static final String CATEGORY_CONFLICT = MODULE_PREFIX + "conflict";
    private static final String CATEGORY_DUPLICATE = MODULE_PREFIX + "duplicate";
    private static final String CATEGORY_UI_PATTERN_GEN = MODULE_PREFIX + "ui.patternGen";
    private static final String CATEGORY_UI_RECIPE_PICKER = MODULE_PREFIX + "ui.recipePicker";
    private static final String CATEGORY_STORAGE = MODULE_PREFIX + "storage";
    private static final String CATEGORY_ITEMS = MODULE_PREFIX + "items";

    private static final int MIN_CONFLICT_BATCH_SIZE = 1;
    private static final int MAX_CONFLICT_BATCH_SIZE = 64;
    public static final int DEFAULT_CONFLICT_BATCH_SIZE = 6;

    private static final int DEFAULT_MAX_FILTERED_RECIPES = 4096;
    private static final int DEFAULT_MAX_CONFLICT_GROUPS = 256;

    private static volatile int conflictBatchSize = DEFAULT_CONFLICT_BATCH_SIZE;
    private static volatile int maxFilteredRecipes = DEFAULT_MAX_FILTERED_RECIPES;
    private static volatile int maxConflictGroups = DEFAULT_MAX_CONFLICT_GROUPS;

    private static final int DEFAULT_DUPLICATE_WINDOW_MS = 500;

    private static volatile long duplicateWindowMs = DEFAULT_DUPLICATE_WINDOW_MS;

    private static final int DEFAULT_PATTERN_GEN_GUI_WIDTH = 260;
    private static final int DEFAULT_PATTERN_GEN_GUI_HEIGHT = 315;

    private static volatile int patternGenGuiWidth = DEFAULT_PATTERN_GEN_GUI_WIDTH;
    private static volatile int patternGenGuiHeight = DEFAULT_PATTERN_GEN_GUI_HEIGHT;

    private static final int DEFAULT_RECIPE_PICKER_GUI_WIDTH = 404;
    private static final int DEFAULT_RECIPE_PICKER_MIN_HEIGHT = 250;
    private static final int DEFAULT_RECIPE_PICKER_IDEAL_HEIGHT = 296;
    private static final int DEFAULT_RECIPE_PICKER_ROW_HEIGHT = 30;
    private static final int DEFAULT_RECIPE_PICKER_ROW_GAP = 1;
    private static final int DEFAULT_RECIPE_PICKER_MAX_DETAIL_LINES = 90;

    private static volatile int recipePickerGuiWidth = DEFAULT_RECIPE_PICKER_GUI_WIDTH;
    private static volatile int recipePickerMinHeight = DEFAULT_RECIPE_PICKER_MIN_HEIGHT;
    private static volatile int recipePickerIdealHeight = DEFAULT_RECIPE_PICKER_IDEAL_HEIGHT;
    private static volatile int recipePickerRowHeight = DEFAULT_RECIPE_PICKER_ROW_HEIGHT;
    private static volatile int recipePickerRowGap = DEFAULT_RECIPE_PICKER_ROW_GAP;
    private static volatile int recipePickerMaxDetailLines = DEFAULT_RECIPE_PICKER_MAX_DETAIL_LINES;

    private static final String DEFAULT_STORAGE_DIRECTORY_NAME = "nhaeutilities";
    private static final String DEFAULT_RECIPE_CACHE_DIRECTORY_NAME = "recipe_cache";

    private static volatile String storageDirectoryName = DEFAULT_STORAGE_DIRECTORY_NAME;
    private static volatile String recipeCacheDirectoryName = DEFAULT_RECIPE_CACHE_DIRECTORY_NAME;

    private static final String DEFAULT_ENCODED_PATTERN_ID = "appliedenergistics2:item.ItemEncodedPattern";

    private static volatile String encodedPatternId = DEFAULT_ENCODED_PATTERN_ID;

    private ForgeConfig() {}

    /**
     * Loads all pattern-generator configuration properties from the given shared
     * {@link Configuration}. This method both declares the properties (so they
     * appear in the config GUI) and reads their current values into the static
     * fields.
     *
     * <p>
     * Safe to call multiple times (e.g. on config-GUI reload).
     */
    public static void load(Configuration cfg) {
        Objects.requireNonNull(cfg, "cfg");
        try {
            loadConflictConfig(cfg);
            loadDuplicateConfig(cfg);
            loadUIConfig(cfg);
            loadStorageConfig(cfg);
            loadItemsConfig(cfg);
            applyCategoryMetadata(cfg);
        } catch (RuntimeException e) {
            FMLLog.warning("[NHAEUtilities] Failed to load PatternGenerator config: %s", e.getMessage());
        }
    }

    private static void loadConflictConfig(Configuration cfg) {
        int configuredBatchSize = cfg.getInt(
            "batchSize",
            CATEGORY_CONFLICT,
            DEFAULT_CONFLICT_BATCH_SIZE,
            MIN_CONFLICT_BATCH_SIZE,
            MAX_CONFLICT_BATCH_SIZE,
            "How many conflict groups are sent to client per batch. Larger values may cause network lag.");
        conflictBatchSize = normalizeConflictBatchSize(configuredBatchSize);

        int configuredMaxFiltered = cfg.getInt(
            "maxFilteredRecipes",
            CATEGORY_CONFLICT,
            DEFAULT_MAX_FILTERED_RECIPES,
            512,
            65536,
            "Maximum number of filtered recipes allowed for interactive conflict selection. Exceeding this aborts interactive selection.");
        maxFilteredRecipes = configuredMaxFiltered;

        int configuredMaxGroups = cfg.getInt(
            "maxConflictGroups",
            CATEGORY_CONFLICT,
            DEFAULT_MAX_CONFLICT_GROUPS,
            64,
            1024,
            "Maximum number of conflict groups allowed for interactive conflict selection. Exceeding this aborts interactive selection.");
        maxConflictGroups = configuredMaxGroups;
    }

    private static void loadDuplicateConfig(Configuration cfg) {
        int configuredWindowMs = cfg.getInt(
            "windowMs",
            CATEGORY_DUPLICATE,
            DEFAULT_DUPLICATE_WINDOW_MS,
            100,
            5000,
            "Time window in milliseconds to collapse duplicate generation requests from the same player. Prevents rapid-fire duplicate submissions.");
        duplicateWindowMs = configuredWindowMs;
    }

    private static void loadUIConfig(Configuration cfg) {
        int configuredPatternGenWidth = cfg.getInt(
            "guiWidth",
            CATEGORY_UI_PATTERN_GEN,
            DEFAULT_PATTERN_GEN_GUI_WIDTH,
            200,
            500,
            "Width of the main Pattern Generator GUI.");
        patternGenGuiWidth = configuredPatternGenWidth;

        int configuredPatternGenHeight = cfg.getInt(
            "guiHeight",
            CATEGORY_UI_PATTERN_GEN,
            DEFAULT_PATTERN_GEN_GUI_HEIGHT,
            200,
            600,
            "Height of the main Pattern Generator GUI.");
        patternGenGuiHeight = configuredPatternGenHeight;

        int configuredPickerWidth = cfg.getInt(
            "guiWidth",
            CATEGORY_UI_RECIPE_PICKER,
            DEFAULT_RECIPE_PICKER_GUI_WIDTH,
            300,
            600,
            "Width of the Recipe Picker GUI.");
        recipePickerGuiWidth = configuredPickerWidth;

        int configuredPickerMinHeight = cfg.getInt(
            "minHeight",
            CATEGORY_UI_RECIPE_PICKER,
            DEFAULT_RECIPE_PICKER_MIN_HEIGHT,
            150,
            400,
            "Minimum height of the Recipe Picker GUI.");
        recipePickerMinHeight = configuredPickerMinHeight;

        int configuredPickerIdealHeight = cfg.getInt(
            "idealHeight",
            CATEGORY_UI_RECIPE_PICKER,
            DEFAULT_RECIPE_PICKER_IDEAL_HEIGHT,
            200,
            500,
            "Ideal height of the Recipe Picker GUI (used for initial sizing).");
        recipePickerIdealHeight = configuredPickerIdealHeight;

        int configuredPickerRowHeight = cfg.getInt(
            "rowHeight",
            CATEGORY_UI_RECIPE_PICKER,
            DEFAULT_RECIPE_PICKER_ROW_HEIGHT,
            20,
            50,
            "Height of each row in the Recipe Picker list.");
        recipePickerRowHeight = configuredPickerRowHeight;

        int configuredPickerRowGap = cfg.getInt(
            "rowGap",
            CATEGORY_UI_RECIPE_PICKER,
            DEFAULT_RECIPE_PICKER_ROW_GAP,
            0,
            5,
            "Gap between rows in the Recipe Picker list.");
        recipePickerRowGap = configuredPickerRowGap;

        int configuredPickerMaxDetailLines = cfg.getInt(
            "maxDetailLines",
            CATEGORY_UI_RECIPE_PICKER,
            DEFAULT_RECIPE_PICKER_MAX_DETAIL_LINES,
            20,
            200,
            "Maximum number of lines displayed in the recipe detail panel.");
        recipePickerMaxDetailLines = configuredPickerMaxDetailLines;
    }

    private static void loadStorageConfig(Configuration cfg) {
        String configuredDirectoryName = cfg.getString(
            "directoryName",
            CATEGORY_STORAGE,
            DEFAULT_STORAGE_DIRECTORY_NAME,
            "Name of the directory where generated patterns are stored (in the save folder).");
        storageDirectoryName = configuredDirectoryName;

        String configuredRecipeCacheDirectoryName = cfg.getString(
            "recipeCacheDirectoryName",
            CATEGORY_STORAGE,
            DEFAULT_RECIPE_CACHE_DIRECTORY_NAME,
            "Name of the subdirectory used for persisted recipe cache files (under the storage directory).");
        recipeCacheDirectoryName = configuredRecipeCacheDirectoryName;
    }

    private static void loadItemsConfig(Configuration cfg) {
        String configuredPatternId = cfg.getString(
            "encodedPatternId",
            CATEGORY_ITEMS,
            DEFAULT_ENCODED_PATTERN_ID,
            "Item ID of the AE2 encoded pattern item. Used for compatibility with different AE2 versions.");
        encodedPatternId = configuredPatternId;
    }

    private static void applyCategoryMetadata(Configuration cfg) {
        String langPrefix = "nhaeutilities.config.";
        cfg.getCategory("patternGenerator")
            .setLanguageKey(langPrefix + "patternGenerator");
        cfg.getCategory(CATEGORY_CONFLICT)
            .setLanguageKey(langPrefix + CATEGORY_CONFLICT);
        cfg.getCategory(CATEGORY_DUPLICATE)
            .setLanguageKey(langPrefix + CATEGORY_DUPLICATE);
        cfg.getCategory(MODULE_PREFIX + "ui")
            .setLanguageKey(langPrefix + MODULE_PREFIX + "ui");
        cfg.getCategory(CATEGORY_UI_PATTERN_GEN)
            .setLanguageKey(langPrefix + CATEGORY_UI_PATTERN_GEN);
        cfg.getCategory(CATEGORY_UI_RECIPE_PICKER)
            .setLanguageKey(langPrefix + CATEGORY_UI_RECIPE_PICKER);
        cfg.getCategory(CATEGORY_STORAGE)
            .setLanguageKey(langPrefix + CATEGORY_STORAGE);
        cfg.getCategory(CATEGORY_ITEMS)
            .setLanguageKey(langPrefix + CATEGORY_ITEMS);
    }

    public static int getConflictBatchSize() {
        return conflictBatchSize;
    }

    public static int getMaxFilteredRecipes() {
        return maxFilteredRecipes;
    }

    public static int getMaxConflictGroups() {
        return maxConflictGroups;
    }

    public static long getDuplicateWindowMs() {
        return duplicateWindowMs;
    }

    public static int getPatternGenGuiWidth() {
        return patternGenGuiWidth;
    }

    public static int getPatternGenGuiHeight() {
        return patternGenGuiHeight;
    }

    public static int getRecipePickerGuiWidth() {
        return recipePickerGuiWidth;
    }

    public static int getRecipePickerMinHeight() {
        return recipePickerMinHeight;
    }

    public static int getRecipePickerIdealHeight() {
        return recipePickerIdealHeight;
    }

    public static int getRecipePickerRowHeight() {
        return recipePickerRowHeight;
    }

    public static int getRecipePickerRowGap() {
        return recipePickerRowGap;
    }

    public static int getRecipePickerMaxDetailLines() {
        return recipePickerMaxDetailLines;
    }

    public static String getStorageDirectoryName() {
        return storageDirectoryName;
    }

    public static String getRecipeCacheDirectoryName() {
        return recipeCacheDirectoryName;
    }

    public static String getEncodedPatternId() {
        return encodedPatternId;
    }

    static int normalizeConflictBatchSize(int value) {
        if (value < MIN_CONFLICT_BATCH_SIZE) {
            return MIN_CONFLICT_BATCH_SIZE;
        }
        if (value > MAX_CONFLICT_BATCH_SIZE) {
            return MAX_CONFLICT_BATCH_SIZE;
        }
        return value;
    }
}
