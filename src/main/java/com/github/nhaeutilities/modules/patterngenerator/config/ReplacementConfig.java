package com.github.nhaeutilities.modules.patterngenerator.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import cpw.mods.fml.common.Loader;

public class ReplacementConfig {

    private static final String FILE_NAME = "nhaeutilities_replacements.cfg";

    private static List<String> loadedRules = new ArrayList<String>();
    private static int ruleCount = 0;

    public static int load() {
        return load(
            Loader.instance()
                .getConfigDir());
    }

    static int load(File configDir) {
        loadedRules.clear();
        ruleCount = 0;

        File file = resolveConfigFile(configDir);
        if (!file.exists()) {
            generateTemplate(file);
            return 0;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                int eq = line.indexOf('=');
                if (eq > 0 && eq < line.length() - 1) {
                    loadedRules.add(line);
                    ruleCount++;
                }
            }
        } catch (Exception e) {
            System.err.println("[NHAEUtilities] Failed to load replacement config: " + e.getMessage());
        }

        return ruleCount;
    }

    public static String getRulesString() {
        if (loadedRules.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < loadedRules.size(); i++) {
            if (i > 0) {
                sb.append(";");
            }
            sb.append(loadedRules.get(i));
        }
        return sb.toString();
    }

    public static int getRuleCount() {
        return ruleCount;
    }

    public static File getConfigFile() {
        return resolveConfigFile(
            Loader.instance()
                .getConfigDir());
    }

    static File resolveConfigFile(File configDir) {
        return new File(configDir, FILE_NAME);
    }

    private static void generateTemplate(File file) {
        try {
            file.getParentFile()
                .mkdirs();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
                writer.write("# NHAEUtilities Pattern Generator - Ore Dictionary replacement rules");
                writer.newLine();
                writer.write("# Format: sourceOreDict=targetOreDict (one per line)");
                writer.newLine();
                writer.write("#");
                writer.newLine();
                writer.write("# Examples:");
                writer.newLine();
                writer.write("# ingotCopper=dustCopper");
                writer.newLine();
                writer.write("# ingotTin=dustTin");
                writer.newLine();
                writer.write("# plateIron=plateSteel");
                writer.newLine();
                writer.newLine();
            }
        } catch (Exception e) {
            System.err.println("[NHAEUtilities] Failed to generate replacement config template: " + e.getMessage());
        }
    }
}
