package com.github.nhaeutilities.core.config;

import java.io.File;

import net.minecraftforge.common.config.Configuration;

public final class CoreConfig {

    private static final String PATTERN_GENERATOR_CATEGORY = "modules.patternGenerator";
    private static final String ENABLED_PROPERTY = "enabled";

    private final boolean patternGeneratorEnabled;

    public CoreConfig(boolean patternGeneratorEnabled) {
        this.patternGeneratorEnabled = patternGeneratorEnabled;
    }

    public static CoreConfig load(File configFile) {
        Configuration configuration = new Configuration(configFile);
        configuration.load();

        boolean patternGeneratorEnabled = configuration
            .getBoolean(ENABLED_PROPERTY, PATTERN_GENERATOR_CATEGORY, true, "Enable the pattern generator module.");

        if (configuration.hasChanged()) {
            configuration.save();
        }

        return new CoreConfig(patternGeneratorEnabled);
    }

    public boolean isPatternGeneratorEnabled() {
        return patternGeneratorEnabled;
    }
}
