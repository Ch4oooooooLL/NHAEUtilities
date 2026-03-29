package com.github.nhaeutilities.modules.superwirelesskit.runtime;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.After;
import org.junit.Test;

import cpw.mods.fml.relauncher.FMLInjectionData;

public class SuperWirelessDebugLogTest {

    @After
    public void tearDown() {
        SuperWirelessDebugLog.resetForTests();
    }

    @Test
    public void disabledDebugLoggingDoesNotCreateLogFile() throws Exception {
        File minecraftHome = Files.createTempDirectory("swk-debug-disabled")
            .toFile();
        initializeMinecraftHome(minecraftHome);

        SuperWirelessDebugLog.configure(false);
        SuperWirelessDebugLog.log("TEST", "disabled log");

        assertFalse(
            SuperWirelessDebugLog.getResolvedLogFileForTests()
                .exists());
    }

    @Test
    public void enabledDebugLoggingWritesToDedicatedLogFile() throws Exception {
        File minecraftHome = Files.createTempDirectory("swk-debug-enabled")
            .toFile();
        initializeMinecraftHome(minecraftHome);

        SuperWirelessDebugLog.configure(true);
        SuperWirelessDebugLog.log("BIND", "binding=%s", "abc");
        SuperWirelessDebugLog.closeForTests();

        File logFile = SuperWirelessDebugLog.getResolvedLogFileForTests();
        assertTrue(logFile.exists());

        String contents = new String(Files.readAllBytes(logFile.toPath()), StandardCharsets.UTF_8);
        assertTrue(contents.contains("[BIND]"));
        assertTrue(contents.contains("binding=abc"));
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
