package ru.moderationhelper.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import ru.moderationhelper.ModerationHelperClient;
import ru.moderationhelper.mixin.ChatHudAccessor;

import java.util.List;
import java.util.Optional;

/**
 * Maps mouse coordinates in the opened chat screen to a ChatHudLine.
 * Minecraft does not expose a public "get clicked chat line" method in 1.21.11,
 * so this class reads ChatHud internals through a tiny accessor mixin and uses
 * vanilla-like geometry. It works for normal non-wrapped chat rows; wrapped
 * messages may still resolve to the parent recent message.
 */
public final class ChatClickLocator {
    private static final int CHAT_BOTTOM_OFFSET = 40;
    private static final int LINE_HEIGHT = 9;

    private ChatClickLocator() {}

    public static Optional<String> findMessageAt(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.inGameHud == null) return Optional.empty();

        try {
            ChatHud chatHud = client.inGameHud.getChatHud();
            ChatHudAccessor accessor = (ChatHudAccessor) chatHud;
            List<ChatHudLine> lines = accessor.moderationhelper$getMessages();
            if (lines == null || lines.isEmpty()) return Optional.empty();

            double chatScale = client.options.getChatScale().getValue();
            if (chatScale <= 0.0D) return Optional.empty();

            int screenHeight = client.getWindow().getScaledHeight();
            double yFromBottom = (screenHeight - CHAT_BOTTOM_OFFSET - mouseY) / chatScale;
            int lineIndex = (int) Math.floor(yFromBottom / LINE_HEIGHT) + accessor.moderationhelper$getScrolledLines();

            if (lineIndex < 0 || lineIndex >= lines.size()) return Optional.empty();
            String message = lines.get(lineIndex).content().getString();
            return message == null || message.isBlank() ? Optional.empty() : Optional.of(message);
        } catch (Throwable t) {
            ModerationHelperClient.LOGGER.warn("Cannot locate clicked chat line", t);
            return Optional.empty();
        }
    }
}
