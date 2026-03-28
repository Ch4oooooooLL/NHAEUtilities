package com.github.nhaeutilities.modules.patterngenerator.util;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.Test;

public class LangKeyCompletenessTest {

    private static final Path EN_US = Paths
        .get("src", "main", "resources", "assets", "nhaeutilities", "lang", "en_US.lang");
    private static final Path ZH_CN = Paths
        .get("src", "main", "resources", "assets", "nhaeutilities", "lang", "zh_CN.lang");

    private static final List<String> REQUIRED_KEYS = Arrays.asList(
        "item.nhaeutilities.pattern_generator.name",
        "nhaeutilities.gui.pattern_gen.title",
        "nhaeutilities.gui.pattern_storage.title",
        "nhaeutilities.gui.pattern_detail.title",
        "nhaeutilities.gui.recipe_picker.title",
        "nhaeutilities.msg.storage.empty_extract",
        "nhaeutilities.msg.generate.no_matching_map",
        "nhaeutilities.msg.cache.missing_or_invalid",
        "nhaeutilities.msg.pattern.generated_and_consumed",
        "nhaeutilities.msg.conflict.cancelled",
        "nhaeutilities.command.help.title",
        "nhaeutilities.command.list.available_maps",
        "nhaeutilities.tooltip.feature.title",
        "nhaeutilities.tooltip.hint.hold_shift");

    @Test
    public void enAndZhMustContainSameKeys() throws IOException {
        Set<String> enKeys = readKeys(EN_US);
        Set<String> zhKeys = readKeys(ZH_CN);
        assertTrue("Language key sets differ between en_US and zh_CN", enKeys.equals(zhKeys));
    }

    @Test
    public void requiredKeysMustExistInBothFiles() throws IOException {
        Set<String> enKeys = readKeys(EN_US);
        Set<String> zhKeys = readKeys(ZH_CN);

        for (String key : REQUIRED_KEYS) {
            assertTrue("Missing key in en_US.lang: " + key, enKeys.contains(key));
            assertTrue("Missing key in zh_CN.lang: " + key, zhKeys.contains(key));
        }
    }

    private static Set<String> readKeys(Path path) throws IOException {
        Set<String> keys = new LinkedHashSet<String>();
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        for (String raw : lines) {
            if (raw == null) {
                continue;
            }
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            int idx = line.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            keys.add(line.substring(0, idx).trim());
        }
        return keys;
    }
}
