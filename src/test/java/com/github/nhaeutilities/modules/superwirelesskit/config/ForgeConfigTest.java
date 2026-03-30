package com.github.nhaeutilities.modules.superwirelesskit.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraftforge.common.config.Configuration;

import org.junit.Test;

import cpw.mods.fml.relauncher.FMLInjectionData;

public class ForgeConfigTest {

    @Test
    public void debugModeDefaultsToDisabled() {
        assertFalse(ForgeConfig.isDebugModeEnabled());
    }

    @Test
    public void loadDeclaresDebugCategoryAndLanguageKeys() throws IOException {
        Path tempDir = Files.createTempDirectory("swk-config");
        initializeMinecraftHome(tempDir.toFile());
        Configuration cfg = new Configuration(
            tempDir.resolve("nhaeutilities.cfg")
                .toFile(),
            true);
        cfg.load();

        ForgeConfig.load(cfg);

        assertTrue(
            cfg.getCategoryNames()
                .contains("modules.superWirelessKit.debug"));
        assertEquals(
            "nhaeutilities.config.modules.superWirelessKit.debug",
            cfg.getCategory("modules.superWirelessKit.debug")
                .getLanguagekey());
        assertEquals(
            "nhaeutilities.config.modules.superWirelessKit.debug.enabled",
            cfg.getCategory("modules.superWirelessKit.debug")
                .get("enabled")
                .getLanguageKey());
        assertFalse(ForgeConfig.isDebugModeEnabled());
    }

    private static void initializeMinecraftHome(File minecraftHome) {
        try {
            Field field = FMLInjectionData.class.getDeclaredField("minecraftHome");
            field.setAccessible(true);
            field.set(null, minecraftHome);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to initialize FMLInjectionData.minecraftHome", e);
        }
    }
}
