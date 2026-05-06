package ru.moderationhelper;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.moderationhelper.config.ModConfig;
import ru.moderationhelper.gui.PunishmentScreen;
import ru.moderationhelper.hud.RecordingHudOverlay;
import ru.moderationhelper.keybind.KeybindManager;
import ru.moderationhelper.obs.ObsController;
import ru.moderationhelper.recent.RecentPlayersManager;
import ru.moderationhelper.screenshot.PendingScreenshot;
import ru.moderationhelper.screenshot.ScreenshotManager;
import ru.moderationhelper.stats.SessionStats;

/**
 * Client entrypoint. The mod is intentionally client-only: it never registers server content.
 */
public final class ModerationHelperClient implements ClientModInitializer {
    public static final String MOD_ID = "moderation_helper_gui";
    public static final Logger LOGGER = LoggerFactory.getLogger("Moderation Helper GUI");

    public static ModConfig CONFIG;
    public static SessionStats STATS;
    public static RecentPlayersManager RECENT_PLAYERS;
    public static ScreenshotManager SCREENSHOTS;
    public static ObsController OBS;

    @Override
    public void onInitializeClient() {
        CONFIG = ModConfig.load();
        STATS = new SessionStats();
        RECENT_PLAYERS = new RecentPlayersManager(CONFIG.maxRecentPlayers);
        SCREENSHOTS = new ScreenshotManager(CONFIG);
        OBS = new ObsController(CONFIG);

        SCREENSHOTS.createFolders();
        SCREENSHOTS.cleanupOldScreenshots();

        KeybindManager.register();
        registerRecordingHud();

        LOGGER.info("Moderation Helper GUI initialized");
    }

    private void registerRecordingHud() {
        HudElementRegistry.attachElementAfter(
                VanillaHudElements.HOTBAR,
                Identifier.of(MOD_ID, "recording_timer"),
                new RecordingHudOverlay()
        );
    }

    /**
     * Used by the chat mixin after a middle click was detected.
     */
    public static void openPunishmentPanel(String nick, PendingScreenshot pendingScreenshot) {
        MinecraftClient client = MinecraftClient.getInstance();
        client.execute(() -> {
            RECENT_PLAYERS.add(nick);
            client.setScreen(new PunishmentScreen(nick, pendingScreenshot));
        });
    }

    public static void sendClientMessage(String message) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud != null) {
            client.inGameHud.getChatHud().addMessage(Text.literal("§8[§bModeration Helper§8] §f" + message));
        }
    }

    public static void sendCommandOrMessage(String commandOrMessage) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.getNetworkHandler() == null) {
            sendClientMessage("Нет подключения к серверу, команда не отправлена.");
            return;
        }

        String value = commandOrMessage.trim();
        try {
            if (value.startsWith("/")) {
                client.getNetworkHandler().sendChatCommand(value.substring(1));
            } else {
                client.getNetworkHandler().sendChatMessage(value);
            }
        } catch (Throwable t) {
            LOGGER.error("Failed to send chat command/message: {}", value, t);
            sendClientMessage("Не удалось отправить команду: " + t.getMessage());
        }
    }
}
