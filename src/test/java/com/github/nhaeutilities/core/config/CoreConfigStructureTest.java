package com.github.nhaeutilities.core.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import net.minecraftforge.common.config.Configuration;

import org.junit.Test;
import com.github.nhaeutilities.modules.patterngenerator.config.ForgeConfig;
import cpw.mods.fml.relauncher.FMLInjectionData;

public class CoreConfigStructureTest {

    @Test
    public void moduleEnabledFlagsBelongToBasicSubcategories() throws IOException {
        Configuration cfg = loadConfiguration();
        Set<String> categoryNames = cfg.getCategoryNames();

        assertTrue(categoryNames.contains("modules.patternGenerator.basic"));
        assertTrue(categoryNames.contains("modules.superWirelessKit.basic"));

        assertEquals(
            "nhaeutilities.config.modules.patternGenerator.basic.enabled",
            cfg.getCategory("modules.patternGenerator.basic")
                .get("enabled")
                .getLanguageKey());
        assertEquals(
            "nhaeutilities.config.modules.superWirelessKit.basic.enabled",
            cfg.getCategory("modules.superWirelessKit.basic")
                .get("enabled")
                .getLanguageKey());
    }

    @Test
    public void patternGeneratorUsesModuleScopedPlayerFacingGroups() throws IOException {
        Configuration cfg = loadConfiguration();
        Set<String> categoryNames = cfg.getCategoryNames();

        assertTrue(categoryNames.contains("modules.patternGenerator.conflict"));
        assertTrue(categoryNames.contains("modules.patternGenerator.requestProtection"));
        assertTrue(categoryNames.contains("modules.patternGenerator.ui"));
        assertTrue(categoryNames.contains("modules.patternGenerator.storage"));
        assertTrue(categoryNames.contains("modules.patternGenerator.advanced"));

        assertEquals(
            "nhaeutilities.config.modules.patternGenerator.requestProtection",
            cfg.getCategory("modules.patternGenerator.requestProtection")
                .getLanguagekey());
        assertEquals(
            "nhaeutilities.config.modules.patternGenerator.advanced",
            cfg.getCategory("modules.patternGenerator.advanced")
                .getLanguagekey());
    }

    @Test
    public void configGuiShowsModuleEntriesAsTopLevelChoices() throws Exception {
        Configuration cfg = loadConfiguration();
        List<String> names = new ArrayList<String>(ConfigGuiLayout.getTopLevelModuleCategoryNames(cfg));
        Collections.sort(names);

        assertEquals(2, names.size());
        assertEquals("modules.patternGenerator", names.get(0));
        assertEquals("modules.superWirelessKit", names.get(1));
    }

    private static Configuration loadConfiguration() throws IOException {
        Path tempDir = Files.createTempDirectory("nhaeutilities-config-structure");
        initializeMinecraftHome(tempDir.toFile());

        File configFile = tempDir.resolve("nhaeutilities.cfg")
            .toFile();
        CoreConfig.load(configFile);
        Configuration cfg = CoreConfig.getConfiguration();
        assertNotNull(cfg);
        ForgeConfig.load(cfg);
        return cfg;
    }

    private static void initializeMinecraftHome(File minecraftHome) {
        try {
            Field field = FMLInjectionData.class.getDeclaredField("minecraftHome");
            field.setAccessible(true);
            if (field.get(null) == null) {
                field.set(null, minecraftHome);
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to initialize FMLInjectionData.minecraftHome", e);
        }
    }
}
