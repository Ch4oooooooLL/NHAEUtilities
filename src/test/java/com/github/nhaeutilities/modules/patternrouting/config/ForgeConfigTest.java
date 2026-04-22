package com.github.nhaeutilities.modules.patternrouting.config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;

import net.minecraftforge.common.config.Configuration;

import org.junit.Test;

import com.github.nhaeutilities.modules.patternrouting.PatternRoutingRuntime;

import cpw.mods.fml.relauncher.FMLInjectionData;

public class ForgeConfigTest {

    @Test
    public void loadDefaultsPatternRoutingDebugLoggingToFalse() throws Exception {
        Path tempDir = Files.createTempDirectory("patternrouting-config-default");
        File previousMinecraftHome = getMinecraftHome();
        try {
            initializeMinecraftHome(tempDir.toFile());
            Configuration cfg = new Configuration(
                tempDir.resolve("nhaeutilities.cfg")
                    .toFile(),
                true);
            cfg.load();

            ForgeConfig.load(cfg);

            assertFalse(ForgeConfig.isDebugModeEnabled());
            assertFalse(PatternRoutingRuntime.isDebugLogEnabled());
        } finally {
            initializeMinecraftHome(previousMinecraftHome);
        }
    }

    @Test
    public void loadEnablesPatternRoutingDebugLoggingWhenConfigured() throws Exception {
        Path tempDir = Files.createTempDirectory("patternrouting-config-enabled");
        File previousMinecraftHome = getMinecraftHome();
        try {
            initializeMinecraftHome(tempDir.toFile());
            Configuration cfg = new Configuration(
                tempDir.resolve("nhaeutilities.cfg")
                    .toFile(),
                true);
            cfg.load();
            cfg.get("modules.patternRouting.debug", "enabled", false)
                .set(true);

            ForgeConfig.load(cfg);

            assertTrue(ForgeConfig.isDebugModeEnabled());
            assertTrue(PatternRoutingRuntime.isDebugLogEnabled());
        } finally {
            initializeMinecraftHome(previousMinecraftHome);
        }
    }

    private static File getMinecraftHome() {
        try {
            Field field = FMLInjectionData.class.getDeclaredField("minecraftHome");
            field.setAccessible(true);
            return (File) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Failed to read FMLInjectionData.minecraftHome", e);
        }
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
