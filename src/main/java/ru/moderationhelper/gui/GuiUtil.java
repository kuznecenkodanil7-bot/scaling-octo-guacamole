package ru.moderationhelper.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

final class GuiUtil {
    private GuiUtil() {}

    static void panel(DrawContext context, int x, int y, int w, int h) {
        context.fill(x, y, x + w, y + h, 0xCC111318);
        context.fill(x, y, x + w, y + 1, 0x66FFFFFF);
        context.fill(x, y + h - 1, x + w, y + h, 0x66000000);
    }

    static void title(DrawContext context, MinecraftClient client, String text, int x, int y) {
        context.drawText(client.textRenderer, Text.literal(text), x, y, 0xFFFFFFFF, true);
    }

    static void label(DrawContext context, MinecraftClient client, String text, int x, int y) {
        context.drawText(client.textRenderer, Text.literal(text), x, y, 0xFFD7DDE8, false);
    }

    static void muted(DrawContext context, MinecraftClient client, String text, int x, int y) {
        context.drawText(client.textRenderer, Text.literal(text), x, y, 0xFF9099A8, false);
    }
}
