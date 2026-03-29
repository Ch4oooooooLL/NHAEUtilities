package com.github.nhaeutilities.core.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;

public final class CoreConfig {

    private static final String MODULES_CATEGORY = "modules";
    private static final String PATTERN_GENERATOR_CATEGORY = "modules.patternGenerator";
    private static final String PATTERN_GENERATOR_BASIC_CATEGORY = PATTERN_GENERATOR_CATEGORY + ".basic";
    private static final String SUPER_WIRELESS_KIT_CATEGORY = "modules.superWirelessKit";
    private static final String SUPER_WIRELESS_KIT_BASIC_CATEGORY = SUPER_WIRELESS_KIT_CATEGORY + ".basic";
    private static final String ENABLED_PROPERTY = "enabled";
    private static final String PATTERN_GENERATOR_ENABLED_COMMENT = "Enable the pattern generator module. [Requires MC restart]";
    private static final String SUPER_WIRELESS_KIT_ENABLED_COMMENT = "Enable the super wireless kit module. [Requires MC restart]";

    private static Configuration configuration;

    private boolean patternGeneratorEnabled;
    private boolean superWirelessKitEnabled;

    public CoreConfig(boolean patternGeneratorEnabled, boolean superWirelessKitEnabled) {
        this.patternGeneratorEnabled = patternGeneratorEnabled;
        this.superWirelessKitEnabled = superWirelessKitEnabled;
    }

    /**
     * Loads the core configuration from the given file. Creates and retains
     * the shared {@link Configuration} instance so that modules can later
     * declare their own properties via
     * {@link com.github.nhaeutilities.core.module.ModuleRegistry#loadAllConfigs(Configuration)}.
     *
     * <p>
     * The caller is responsible for calling {@link #saveIfChanged()} after
     * all modules have finished loading their config properties.
     */
    public static CoreConfig load(File configFile) {
        configuration = new Configuration(configFile, true);
        configuration.load();

        boolean patternGeneratorEnabled = readPatternGeneratorEnabled(configuration);
        boolean superWirelessKitEnabled = readSuperWirelessKitEnabled(configuration);
        applyCategoryMetadata(configuration);

        return new CoreConfig(patternGeneratorEnabled, superWirelessKitEnabled);
    }

    /**
     * Re-reads all core config values from the retained {@link Configuration}.
     * Called when the in-game config GUI is closed.
     *
     * <p>
     * Note: Module enable/disable changes require an MC restart to take
     * effect because the module registry is frozen at bootstrap.
     */
    public void reload() {
        if (configuration == null) {
            return;
        }
        patternGeneratorEnabled = readPatternGeneratorEnabled(configuration);
        superWirelessKitEnabled = readSuperWirelessKitEnabled(configuration);
    }

    /**
     * Returns the shared {@link Configuration} instance, or {@code null} if
     * {@link #load(File)} has not been called yet.
     */
    public static Configuration getConfiguration() {
        return configuration;
    }

    /**
     * Saves the shared configuration file if any property has changed.
     */
    public static void saveIfChanged() {
        if (configuration != null && configuration.hasChanged()) {
            configuration.save();
        }
    }

    public boolean isPatternGeneratorEnabled() {
        return patternGeneratorEnabled;
    }

    public boolean isSuperWirelessKitEnabled() {
        return superWirelessKitEnabled;
    }

    private static boolean readPatternGeneratorEnabled(Configuration cfg) {
        Property property = cfg
            .get(PATTERN_GENERATOR_BASIC_CATEGORY, ENABLED_PROPERTY, true, PATTERN_GENERATOR_ENABLED_COMMENT);
        property.setRequiresMcRestart(true);
        property.setLanguageKey("nhaeutilities.config." + PATTERN_GENERATOR_BASIC_CATEGORY + "." + ENABLED_PROPERTY);
        return property.getBoolean();
    }

    private static boolean readSuperWirelessKitEnabled(Configuration cfg) {
        Property property = cfg
            .get(SUPER_WIRELESS_KIT_BASIC_CATEGORY, ENABLED_PROPERTY, true, SUPER_WIRELESS_KIT_ENABLED_COMMENT);
        property.setRequiresMcRestart(true);
        property.setLanguageKey("nhaeutilities.config." + SUPER_WIRELESS_KIT_BASIC_CATEGORY + "." + ENABLED_PROPERTY);
        return property.getBoolean();
    }

    private static void applyCategoryMetadata(Configuration cfg) {
        cfg.getCategory(MODULES_CATEGORY)
            .setLanguageKey("nhaeutilities.config.modules");

        cfg.getCategory(PATTERN_GENERATOR_CATEGORY)
            .setLanguageKey("nhaeutilities.config.modules.patternGenerator")
            .setRequiresMcRestart(true);
        cfg.getCategory(PATTERN_GENERATOR_BASIC_CATEGORY)
            .setLanguageKey("nhaeutilities.config.modules.patternGenerator.basic")
            .setRequiresMcRestart(true);

        cfg.getCategory(SUPER_WIRELESS_KIT_CATEGORY)
            .setLanguageKey("nhaeutilities.config.modules.superWirelessKit")
            .setRequiresMcRestart(true);
        cfg.getCategory(SUPER_WIRELESS_KIT_BASIC_CATEGORY)
            .setLanguageKey("nhaeutilities.config.modules.superWirelessKit.basic")
            .setRequiresMcRestart(true);
    }
}
