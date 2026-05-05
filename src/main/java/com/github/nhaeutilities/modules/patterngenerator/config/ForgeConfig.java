package com.github.nhaeutilities.modules.patterngenerator.config;

import java.util.Objects;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

import cpw.mods.fml.common.FMLLog;

public final class ForgeConfig {

    private static final String MODULE_CATEGORY = "modules.patternGenerator";
    private static final String MODULE_PREFIX = MODULE_CATEGORY + ".";
    private static final String LANG_PREFIX = "nhaeutilities.config.";

    private static final String CATEGORY_CONFLICT = MODULE_PREFIX + "conflict";
    private static final String CATEGORY_REQUEST_PROTECTION = MODULE_PREFIX + "requestProtection";
    private static final String CATEGORY_UI_PATTERN_GEN = MODULE_PREFIX + "ui.patternGen";
    private static final String CATEGORY_UI_RECIPE_PICKER = MODULE_PREFIX + "ui.recipePicker";
    private static final String CATEGORY_STORAGE = MODULE_PREFIX + "storage";
    private static final String CATEGORY_ADVANCED = MODULE_PREFIX + "advanced";

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
            loadRequestProtectionConfig(cfg);
            loadUIConfig(cfg);
            loadStorageConfig(cfg);
            loadAdvancedConfig(cfg);
            applyCategoryMetadata(cfg);
        } catch (RuntimeException e) {
            FMLLog.warning("[NHAEUtilities] Failed to load PatternGenerator config: %s", e.getMessage());
        }
    }

    private static void loadConflictConfig(Configuration cfg) {
        Property batchSizeProp = cfg.get(
            CATEGORY_CONFLICT,
            "batchSize",
            DEFAULT_CONFLICT_BATCH_SIZE,
            "How many conflict groups are sent to client per batch. Larger values may cause network lag.",
            MIN_CONFLICT_BATCH_SIZE,
            MAX_CONFLICT_BATCH_SIZE);
        batchSizeProp.setLanguageKey(LANG_PREFIX + CATEGORY_CONFLICT + ".batchSize");
        conflictBatchSize = normalizeConflictBatchSize(batchSizeProp.getInt());

        Property maxFilteredProp = cfg.get(
            CATEGORY_CONFLICT,
            "maxFilteredRecipes",
            DEFAULT_MAX_FILTERED_RECIPES,
            "Maximum number of filtered recipes allowed for interactive conflict selection. Exceeding this aborts interactive selection.",
            512,
            65536);
        maxFilteredProp.setLanguageKey(LANG_PREFIX + CATEGORY_CONFLICT + ".maxFilteredRecipes");
        maxFilteredRecipes = maxFilteredProp.getInt();

        Property maxGroupsProp = cfg.get(
            CATEGORY_CONFLICT,
            "maxConflictGroups",
            DEFAULT_MAX_CONFLICT_GROUPS,
            "Maximum number of conflict groups allowed for interactive conflict selection. Exceeding this aborts interactive selection.",
            64,
            1024);
        maxGroupsProp.setLanguageKey(LANG_PREFIX + CATEGORY_CONFLICT + ".maxConflictGroups");
        maxConflictGroups = maxGroupsProp.getInt();
    }

    private static void loadRequestProtectionConfig(Configuration cfg) {
        Property windowMsProp = cfg.get(
            CATEGORY_REQUEST_PROTECTION,
            "windowMs",
            DEFAULT_DUPLICATE_WINDOW_MS,
            "Time window in milliseconds to collapse duplicate generation requests from the same player. Prevents rapid-fire duplicate submissions.",
            100,
            5000);
        windowMsProp.setLanguageKey(LANG_PREFIX + CATEGORY_REQUEST_PROTECTION + ".windowMs");
        duplicateWindowMs = windowMsProp.getInt();
    }

    private static void loadUIConfig(Configuration cfg) {
        Property patternGenWidthProp = cfg.get(
            CATEGORY_UI_PATTERN_GEN,
            "guiWidth",
            DEFAULT_PATTERN_GEN_GUI_WIDTH,
            "Width of the main Pattern Generator GUI.",
            200,
            500);
        patternGenWidthProp.setLanguageKey(LANG_PREFIX + CATEGORY_UI_PATTERN_GEN + ".guiWidth");
        patternGenGuiWidth = patternGenWidthProp.getInt();

        Property patternGenHeightProp = cfg.get(
            CATEGORY_UI_PATTERN_GEN,
            "guiHeight",
            DEFAULT_PATTERN_GEN_GUI_HEIGHT,
            "Height of the main Pattern Generator GUI.",
            200,
            600);
        patternGenHeightProp.setLanguageKey(LANG_PREFIX + CATEGORY_UI_PATTERN_GEN + ".guiHeight");
        patternGenGuiHeight = patternGenHeightProp.getInt();

        Property pickerWidthProp = cfg.get(
            CATEGORY_UI_RECIPE_PICKER,
            "guiWidth",
            DEFAULT_RECIPE_PICKER_GUI_WIDTH,
            "Width of the Recipe Picker GUI.",
            300,
            600);
        pickerWidthProp.setLanguageKey(LANG_PREFIX + CATEGORY_UI_RECIPE_PICKER + ".guiWidth");
        recipePickerGuiWidth = pickerWidthProp.getInt();

        Property pickerMinHeightProp = cfg.get(
            CATEGORY_UI_RECIPE_PICKER,
            "minHeight",
            DEFAULT_RECIPE_PICKER_MIN_HEIGHT,
            "Minimum height of the Recipe Picker GUI.",
            150,
            400);
        pickerMinHeightProp.setLanguageKey(LANG_PREFIX + CATEGORY_UI_RECIPE_PICKER + ".minHeight");
        recipePickerMinHeight = pickerMinHeightProp.getInt();

        Property pickerIdealHeightProp = cfg.get(
            CATEGORY_UI_RECIPE_PICKER,
            "idealHeight",
            DEFAULT_RECIPE_PICKER_IDEAL_HEIGHT,
            "Ideal height of the Recipe Picker GUI (used for initial sizing).",
            200,
            500);
        pickerIdealHeightProp.setLanguageKey(LANG_PREFIX + CATEGORY_UI_RECIPE_PICKER + ".idealHeight");
        recipePickerIdealHeight = pickerIdealHeightProp.getInt();

        Property pickerRowHeightProp = cfg.get(
            CATEGORY_UI_RECIPE_PICKER,
            "rowHeight",
            DEFAULT_RECIPE_PICKER_ROW_HEIGHT,
            "Height of each row in the Recipe Picker list.",
            20,
            50);
        pickerRowHeightProp.setLanguageKey(LANG_PREFIX + CATEGORY_UI_RECIPE_PICKER + ".rowHeight");
        recipePickerRowHeight = pickerRowHeightProp.getInt();

        Property pickerRowGapProp = cfg.get(
            CATEGORY_UI_RECIPE_PICKER,
            "rowGap",
            DEFAULT_RECIPE_PICKER_ROW_GAP,
            "Gap between rows in the Recipe Picker list.",
            0,
            5);
        pickerRowGapProp.setLanguageKey(LANG_PREFIX + CATEGORY_UI_RECIPE_PICKER + ".rowGap");
        recipePickerRowGap = pickerRowGapProp.getInt();

        Property pickerMaxDetailLinesProp = cfg.get(
            CATEGORY_UI_RECIPE_PICKER,
            "maxDetailLines",
            DEFAULT_RECIPE_PICKER_MAX_DETAIL_LINES,
            "Maximum number of lines displayed in the recipe detail panel.",
            20,
            200);
        pickerMaxDetailLinesProp.setLanguageKey(LANG_PREFIX + CATEGORY_UI_RECIPE_PICKER + ".maxDetailLines");
        recipePickerMaxDetailLines = pickerMaxDetailLinesProp.getInt();
    }

    private static void loadStorageConfig(Configuration cfg) {
        Property directoryNameProp = cfg.get(
            CATEGORY_STORAGE,
            "directoryName",
            DEFAULT_STORAGE_DIRECTORY_NAME,
            "Name of the directory where generated patterns are stored (in the save folder).");
        directoryNameProp.setLanguageKey(LANG_PREFIX + CATEGORY_STORAGE + ".directoryName");
        storageDirectoryName = directoryNameProp.getString();

        Property recipeCacheDirProp = cfg.get(
            CATEGORY_STORAGE,
            "recipeCacheDirectoryName",
            DEFAULT_RECIPE_CACHE_DIRECTORY_NAME,
            "Name of the subdirectory used for persisted recipe cache files (under the storage directory).");
        recipeCacheDirProp.setLanguageKey(LANG_PREFIX + CATEGORY_STORAGE + ".recipeCacheDirectoryName");
        recipeCacheDirectoryName = recipeCacheDirProp.getString();
    }

    private static void loadAdvancedConfig(Configuration cfg) {
        Property patternIdProp = cfg.get(
            CATEGORY_ADVANCED,
            "encodedPatternId",
            DEFAULT_ENCODED_PATTERN_ID,
            "Item ID of the AE2 encoded pattern item. Used for compatibility with different AE2 versions.");
        patternIdProp.setLanguageKey(LANG_PREFIX + CATEGORY_ADVANCED + ".encodedPatternId");
        encodedPatternId = patternIdProp.getString();
    }

    private static void applyCategoryMetadata(Configuration cfg) {
        cfg.getCategory(MODULE_CATEGORY)
            .setLanguageKey(LANG_PREFIX + MODULE_CATEGORY)
            .setRequiresMcRestart(true);
        cfg.getCategory(CATEGORY_CONFLICT)
            .setLanguageKey(LANG_PREFIX + CATEGORY_CONFLICT);
        cfg.getCategory(CATEGORY_REQUEST_PROTECTION)
            .setLanguageKey(LANG_PREFIX + CATEGORY_REQUEST_PROTECTION);
        cfg.getCategory(MODULE_PREFIX + "ui")
            .setLanguageKey(LANG_PREFIX + MODULE_PREFIX + "ui");
        cfg.getCategory(CATEGORY_UI_PATTERN_GEN)
            .setLanguageKey(LANG_PREFIX + CATEGORY_UI_PATTERN_GEN);
        cfg.getCategory(CATEGORY_UI_RECIPE_PICKER)
            .setLanguageKey(LANG_PREFIX + CATEGORY_UI_RECIPE_PICKER);
        cfg.getCategory(CATEGORY_STORAGE)
            .setLanguageKey(LANG_PREFIX + CATEGORY_STORAGE);
        cfg.getCategory(CATEGORY_ADVANCED)
            .setLanguageKey(LANG_PREFIX + CATEGORY_ADVANCED);
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
