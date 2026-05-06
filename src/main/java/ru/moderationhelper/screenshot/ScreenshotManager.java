package ru.moderationhelper.screenshot;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.ScreenshotRecorder;
import ru.moderationhelper.ModerationHelperClient;
import ru.moderationhelper.config.ModConfig;
import ru.moderationhelper.stats.SessionStats;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.Locale;
import java.util.stream.Stream;

/** Handles temp screenshots, final sorting, and cleanup. */
public final class ScreenshotManager {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final ModConfig config;
    private final Path root;

    public ScreenshotManager(ModConfig config) {
        this.config = config;
        Path gameDir = FabricLoader.getInstance().getGameDir();
        this.root = gameDir.resolve(config.screenshotsFolder);
    }

    public void createFolders() {
        try {
            Files.createDirectories(root);
            Files.createDirectories(root.resolve("warn"));
            Files.createDirectories(root.resolve("mute"));
            Files.createDirectories(root.resolve("ban"));
            Files.createDirectories(root.resolve("ipban"));
            Files.createDirectories(root.resolve("temp"));
            Files.createDirectories(root.resolve("archive"));
        } catch (IOException e) {
            ModerationHelperClient.LOGGER.error("Cannot create screenshot folders", e);
        }
    }

    /**
     * Captures the current framebuffer before the punishment GUI is opened.
     * If the screenshot cannot be written, the mod continues without crashing.
     */
    public PendingScreenshot captureTemp(String nick) {
        createFolders();
        LocalDateTime now = LocalDateTime.now();
        Path file = root.resolve("temp").resolve(safe(nick) + "_" + FILE_TIME.format(now) + ".png");

        try {
            MinecraftClient client = MinecraftClient.getInstance();
            ScreenshotRecorder.takeScreenshot(client.getFramebuffer(), image -> {
                try {
                    image.writeTo(file);
                } catch (IOException e) {
                    ModerationHelperClient.LOGGER.error("Cannot write temp screenshot {}", file, e);
                    ModerationHelperClient.sendClientMessage("Скриншот не сохранён: " + e.getMessage());
                } finally {
                    image.close();
                }
            });
            return new PendingScreenshot(nick, file, now, true);
        } catch (Throwable t) {
            ModerationHelperClient.LOGGER.error("Cannot capture temp screenshot", t);
            ModerationHelperClient.sendClientMessage("Скриншот не сделан: " + t.getMessage());
            return PendingScreenshot.empty(nick);
        }
    }

    public void finishScreenshot(PendingScreenshot pending, SessionStats.PunishmentType type, String duration, String reason) {
        if (pending == null || !pending.exists() || pending.path() == null) return;
        try {
            if (!Files.exists(pending.path())) {
                ModerationHelperClient.LOGGER.warn("Temp screenshot does not exist yet: {}", pending.path());
                return;
            }
            Path dir = root.resolve(type.command().toLowerCase(Locale.ROOT));
            Files.createDirectories(dir);
            String name = safe(pending.nick()) + "_" + safe(type.command()) + "_" + safe(duration) + "_" + safe(reason) + "_" + FILE_TIME.format(pending.createdAt()) + ".png";
            Files.move(pending.path(), uniquePath(dir.resolve(name)), StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            ModerationHelperClient.LOGGER.error("Cannot finalize screenshot", e);
            ModerationHelperClient.sendClientMessage("Скриншот не перенесён: " + e.getMessage());
        }
    }

    public void cleanupOldScreenshots() {
        createFolders();
        String mode = config.screenshotCleanupMode == null ? "DELETE" : config.screenshotCleanupMode.toUpperCase(Locale.ROOT);
        if (mode.equals("OFF")) return;

        Instant threshold = Instant.now().minusSeconds(Math.max(1, config.screenshotRetentionDays) * 24L * 60L * 60L);
        try (Stream<Path> stream = Files.walk(root)) {
            stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().toLowerCase(Locale.ROOT).endsWith(".png"))
                    .filter(path -> !path.startsWith(root.resolve("archive")))
                    .filter(path -> isOlderThan(path, threshold))
                    .forEach(path -> cleanupOne(path, mode));
        } catch (IOException e) {
            ModerationHelperClient.LOGGER.error("Screenshot cleanup failed", e);
        }
    }

    private boolean isOlderThan(Path path, Instant threshold) {
        try {
            return Files.getLastModifiedTime(path).toInstant().isBefore(threshold);
        } catch (IOException e) {
            return false;
        }
    }

    private void cleanupOne(Path path, String mode) {
        try {
            if (mode.equals("ARCHIVE")) {
                Path target = uniquePath(root.resolve("archive").resolve(path.getFileName().toString()));
                Files.createDirectories(target.getParent());
                Files.move(path, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.deleteIfExists(path);
            }
        } catch (IOException e) {
            ModerationHelperClient.LOGGER.warn("Cannot cleanup screenshot {}", path, e);
        }
    }

    private Path uniquePath(Path base) throws IOException {
        if (!Files.exists(base)) return base;
        String fileName = base.getFileName().toString();
        String name = fileName;
        String ext = "";
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0) {
            name = fileName.substring(0, dot);
            ext = fileName.substring(dot);
        }
        for (int i = 1; i < 1000; i++) {
            Path candidate = base.getParent().resolve(name + "_" + i + ext);
            if (!Files.exists(candidate)) return candidate;
        }
        return base.getParent().resolve(name + "_" + System.currentTimeMillis() + ext);
    }

    public static String safe(String value) {
        if (value == null || value.isBlank()) return "empty";
        return value.trim().replaceAll("[^A-Za-z0-9_\\-.а-яА-ЯёЁ]+", "_");
    }

    public Path root() {
        return root;
    }
}
