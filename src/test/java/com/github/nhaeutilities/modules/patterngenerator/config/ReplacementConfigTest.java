package com.github.nhaeutilities.modules.patterngenerator.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import org.junit.Test;

public class ReplacementConfigTest {

    @Test
    public void configFileUsesExpectedLocalFileName() {
        assertEquals("nhaeutilities_replacements.cfg", ReplacementConfig.getConfigFile().getName());
    }

    @Test
    public void resolveConfigFileUsesExpectedLocalFileNameUnderProvidedDirectory() {
        File configDir = new File("build/tmp/replacement-config-test");

        File configFile = ReplacementConfig.resolveConfigFile(configDir);

        assertEquals(configDir.getPath(), configFile.getParentFile().getPath());
        assertEquals("nhaeutilities_replacements.cfg", configFile.getName());
    }

    @Test
    public void loadCreatesTemplateAtRenamedLocalFile() throws IOException {
        Path tempDir = Files.createTempDirectory("replacement-config-test");
        try {
            File configDir = tempDir.toFile();

            assertEquals(0, ReplacementConfig.load(configDir));
            assertTrue(new File(configDir, "nhaeutilities_replacements.cfg").isFile());
            assertFalse(new File(configDir, "ae2patterngen_replacements.cfg").exists());
        } finally {
            Files.walk(tempDir)
                .sorted(Comparator.reverseOrder())
                .forEach(path -> path.toFile().delete());
        }
    }
}
