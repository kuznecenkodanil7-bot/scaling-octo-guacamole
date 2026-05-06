package ru.moderationhelper.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import ru.moderationhelper.ModerationHelperClient;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON config stored in .minecraft/config/moderation_helper_gui.json
 */
public class ModConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("moderation_helper_gui.json");

    public boolean obsEnabled = false;
    public String obsHost = "localhost";
    public int obsPort = 4455;
    public String obsPassword = "";

    public int maxRecentPlayers = 12;

    /** DELETE / ARCHIVE / OFF */
    public String screenshotCleanupMode = "DELETE";
    public int screenshotRetentionDays = 30;
    public String screenshotsFolder = "moderation_screenshots";

    public String checkCommandTemplate = "/check {nick}";

    public List<String> quickReasons = new ArrayList<>(List.of(
            "3.1", "3.2", "3.3", "3.4", "3.5", "3.6", "3.7", "3.8",
            "spam", "flood", "cheats", "toxicity", "bug abuse"
    ));

    public static ModConfig load() {
        try {
            if (!Files.exists(CONFIG_PATH)) {
                ModConfig defaults = new ModConfig();
                defaults.save();
                return defaults;
            }
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                ModConfig config = GSON.fromJson(reader, ModConfig.class);
                if (config == null) {
                    config = new ModConfig();
                }
                config.normalize();
                config.save();
                return config;
            }
        } catch (Exception e) {
            ModerationHelperClient.LOGGER.error("Failed to load config, using defaults", e);
            return new ModConfig();
        }
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            ModerationHelperClient.LOGGER.error("Failed to save config", e);
        }
    }

    private void normalize() {
        if (maxRecentPlayers < 1) maxRecentPlayers = 1;
        if (maxRecentPlayers > 30) maxRecentPlayers = 30;
        if (screenshotRetentionDays < 1) screenshotRetentionDays = 30;
        if (screenshotsFolder == null || screenshotsFolder.isBlank()) screenshotsFolder = "moderation_screenshots";
        if (checkCommandTemplate == null || checkCommandTemplate.isBlank()) checkCommandTemplate = "/check {nick}";
        if (quickReasons == null || quickReasons.isEmpty()) quickReasons = new ModConfig().quickReasons;
        if (screenshotCleanupMode == null) screenshotCleanupMode = "DELETE";
        screenshotCleanupMode = screenshotCleanupMode.trim().toUpperCase();
        if (!List.of("DELETE", "ARCHIVE", "OFF").contains(screenshotCleanupMode)) screenshotCleanupMode = "DELETE";
    }
}
