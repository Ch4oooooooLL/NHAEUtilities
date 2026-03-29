package com.github.nhaeutilities.modules.patterngenerator.network;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class Task5LanguageResourcesTest {

    @Test
    public void englishLangContainsTask5CommandAndNetworkKeys() throws IOException {
        Set<String> keys = loadKeys("assets/nhaeutilities/lang/en_US.lang");

        assertContains(keys, "nhaeutilities.command.help.title");
        assertContains(keys, "nhaeutilities.command.generate.usage");
        assertContains(keys, "nhaeutilities.command.count.result");
        assertContains(keys, "nhaeutilities.msg.cache.missing_or_invalid");
        assertContains(keys, "nhaeutilities.msg.generate.conflicts_detected");
        assertContains(keys, "nhaeutilities.msg.conflict.invalid_batch_selection");
        assertContains(keys, "nhaeutilities.msg.pattern.generated_and_consumed");
        assertContains(keys, "nhaeutilities.msg.storage.deleted");
        assertContains(keys, "nhaeutilities.gui.pattern_gen.status.filter_result");
    }

    private static Set<String> loadKeys(String path) throws IOException {
        InputStream stream = Task5LanguageResourcesTest.class.getClassLoader()
            .getResourceAsStream(path);
        assertNotNull(path + " should exist", stream);

        Set<String> keys = new HashSet<String>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                int equals = trimmed.indexOf('=');
                if (equals > 0) {
                    keys.add(trimmed.substring(0, equals));
                }
            }
        }
        return keys;
    }

    private static void assertContains(Set<String> keys, String key) {
        assertTrue("Missing lang key: " + key, keys.contains(key));
    }
}
