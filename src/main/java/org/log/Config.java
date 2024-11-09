package org.log;

import app.ccls.yml.YamlHandler;
import app.ccls.yml.YamlHandlerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Config {
    private static final YamlHandler yamlHandler = YamlHandlerFactory.getHandler("nested");
    public static final String CONFIG_FILE = "config/backinv.yml";

    // Configuration fields
    public static boolean enabled;
    public static String prefix;
    public static String suffix;
    public static String reJoinMessage;
    public static String loadPlayerMessage;
    public static String errorLoadPlayerMessage;
    public static String itemPrefix;
    public static String itemSuffix;
    public static String noSavesFound;
    public static String invalidIndex;
    public static String saveNotFound;
    public static String errorOccurred;

    // Initialize the configuration by loading or creating it
    public static void init() {
        try {
            Path configPath = Path.of(CONFIG_FILE);
            if (!Files.exists(configPath)) {
                // If the config file does not exist, create it with default values
                createDefaultConfig(configPath);
            }

            // Read the YAML configuration
            Map<String, Object> configData = yamlHandler.readYaml(CONFIG_FILE);

            // Extract values from the config, using defaults if not found
            enabled = Boolean.parseBoolean((String) configData.getOrDefault("enabled", "true"));

            prefix = (String) configData.getOrDefault("prefix", "§6[§aBackInv§6] ");
            suffix = (String) configData.getOrDefault("suffix", "§7");
            reJoinMessage = (String) configData.getOrDefault("reJoin", "Sorry come back again");
            loadPlayerMessage = (String) configData.getOrDefault("loadPlayer", "Player {player} inventory is loaded from file {file}.");
            errorLoadPlayerMessage = (String) configData.getOrDefault("errorLoadPlayer", "Failed to save inventory for player {player} file {file} - Error- {e}");
            itemPrefix = (String) configData.getOrDefault("itemPrefix", "§7");
            itemSuffix = (String) configData.getOrDefault("itemSuffix", "§6");
            noSavesFound = (String) configData.getOrDefault("no_saves_found", "§cNo saved inventories found for player {player}");
            invalidIndex = (String) configData.getOrDefault("invalid_index", "§cInvalid save index- {index}");
            saveNotFound = (String) configData.getOrDefault("save_not_found", "§cSave not found- {save}");
            errorOccurred = (String) configData.getOrDefault("error_occurred", "§cAn error occurred while {action}");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Create the default config file if it does not exist
    private static void createDefaultConfig(Path configPath) {
        try {
            // Create necessary directories if they don't exist
            Files.createDirectories(configPath.getParent());

            // Define the default configuration values using HashMap
            Map<String, Object> defaultConfig = new HashMap<>();
            defaultConfig.put("enabled", true);
            defaultConfig.put("prefix", "§6[§aBackInv§6] ");
            defaultConfig.put("suffix", "§7");
            defaultConfig.put("reJoin", "Sorry come back again");
            defaultConfig.put("loadPlayer", "Player {player} inventory is loaded from file {file}.");
            defaultConfig.put("errorLoadPlayer", "Failed to save inventory for player {player} file {file} - Error- {e}");
            defaultConfig.put("itemPrefix", "§7");
            defaultConfig.put("itemSuffix", "§6");
            defaultConfig.put("no_saves_found", "§cNo saved inventories found for player {player}");
            defaultConfig.put("invalid_index", "§cInvalid save index- {index}");
            defaultConfig.put("save_not_found", "§cSave not found- {save}");
            defaultConfig.put("error_occurred", "§cAn error occurred while {action}");

            // Write the default config to the file
            yamlHandler.writeYaml(CONFIG_FILE, defaultConfig);
            System.out.println("Created default config file at " + configPath);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
