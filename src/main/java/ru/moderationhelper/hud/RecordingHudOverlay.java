package ru.moderationhelper.hud;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import ru.moderationhelper.ModerationHelperClient;

public final class RecordingHudOverlay implements HudElement {
    @Override
    public void render(DrawContext context, RenderTickCounter tickCounter) {
        if (ModerationHelperClient.OBS == null || !ModerationHelperClient.OBS.isRecording()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.textRenderer == null) return;

        long seconds = ModerationHelperClient.OBS.getRecordingSeconds();
        String time = String.format("%02d:%02d", seconds / 60, seconds % 60);
        String text = "Идёт запись: " + time;

        int width = client.getWindow().getScaledWidth();
        int y = client.getWindow().getScaledHeight() - 72;
        int textWidth = client.textRenderer.getWidth(text);
        int x = (width - textWidth) / 2;

        context.fill(x - 6, y - 4, x + textWidth + 6, y + 12, 0xAA000000);
        context.drawText(client.textRenderer, Text.literal(text), x, y, 0xFFFF5555, true);
    }
}
