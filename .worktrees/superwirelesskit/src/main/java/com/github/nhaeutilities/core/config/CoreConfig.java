package com.github.nhaeutilities.core.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class CoreConfig {

    private static final String PATTERN_GENERATOR_CATEGORY = "modules.patternGenerator";
    private static final String SUPER_WIRELESS_KIT_CATEGORY = "modules.superWirelessKit";
    private static final String ENABLED_PROPERTY = "enabled";

    private final boolean patternGeneratorEnabled;
    private final boolean superWirelessKitEnabled;

    public CoreConfig(boolean patternGeneratorEnabled, boolean superWirelessKitEnabled) {
        this.patternGeneratorEnabled = patternGeneratorEnabled;
        this.superWirelessKitEnabled = superWirelessKitEnabled;
    }

    public static CoreConfig load(File configFile) {
        Configuration configuration = new Configuration(configFile);
        configuration.load();

        boolean patternGeneratorEnabled = configuration
            .getBoolean(ENABLED_PROPERTY, PATTERN_GENERATOR_CATEGORY, true, "Enable the pattern generator module.");
        boolean superWirelessKitEnabled = configuration
            .getBoolean(ENABLED_PROPERTY, SUPER_WIRELESS_KIT_CATEGORY, true, "Enable the super wireless kit module.");

        if (configuration.hasChanged()) {
            configuration.save();
        }

        return new CoreConfig(patternGeneratorEnabled, superWirelessKitEnabled);
    }

    public boolean isPatternGeneratorEnabled() {
        return patternGeneratorEnabled;
    }

    public boolean isSuperWirelessKitEnabled() {
        return superWirelessKitEnabled;
    }
}
