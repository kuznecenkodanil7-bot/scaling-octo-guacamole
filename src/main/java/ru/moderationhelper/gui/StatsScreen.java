package ru.moderationhelper.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import ru.moderationhelper.ModerationHelperClient;
import ru.moderationhelper.screenshot.PendingScreenshot;
import ru.moderationhelper.stats.SessionStats;

import java.util.List;

public final class StatsScreen extends Screen {
    public StatsScreen() {
        super(Text.literal("Moderation Helper Stats"));
    }

    @Override
    protected void init() {
        clearChildren();
        int panelW = Math.min(460, width - 40);
        int panelH = Math.min(260, height - 40);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        addRecentButtons(x + 24, y + 120, panelW - 48);

        addDrawableChild(ButtonWidget.builder(Text.literal("Закрыть"), b -> close())
                .dimensions(x + panelW - 112, y + panelH - 38, 88, 24).build());
    }

    private void addRecentButtons(int x, int y, int availableW) {
        List<String> recent = ModerationHelperClient.RECENT_PLAYERS.getPlayers();
        int currentX = x;
        int currentY = y + 18;
        for (String player : recent) {
            int w = Math.min(100, Math.max(52, textRenderer.getWidth(player) + 18));
            if (currentX + w > x + availableW) {
                currentX = x;
                currentY += 24;
            }
            if (currentY > y + 72) break;
            addDrawableChild(ButtonWidget.builder(Text.literal(player), b -> {
                MinecraftClient client = MinecraftClient.getInstance();
                client.keyboard.setClipboard(player);
                ModerationHelperClient.RECENT_PLAYERS.add(player);
                client.setScreen(new PunishmentScreen(player, PendingScreenshot.empty(player)));
            }).dimensions(currentX, currentY, w, 20).build());
            currentX += w + 6;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        int panelW = Math.min(460, width - 40);
        int panelH = Math.min(260, height - 40);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        context.fill(0, 0, width, height, 0x99000000);
        GuiUtil.panel(context, x, y, panelW, panelH);
        GuiUtil.title(context, client, "Moderation Helper GUI", x + 24, y + 20);
        GuiUtil.muted(context, client, "H открывает только эту панель: без поиска ника и без скриншота.", x + 24, y + 40);

        int statsY = y + 70;
        GuiUtil.label(context, client, "warn: " + ModerationHelperClient.STATS.get(SessionStats.PunishmentType.WARN), x + 24, statsY);
        GuiUtil.label(context, client, "mute: " + ModerationHelperClient.STATS.get(SessionStats.PunishmentType.MUTE), x + 126, statsY);
        GuiUtil.label(context, client, "ban: " + ModerationHelperClient.STATS.get(SessionStats.PunishmentType.BAN), x + 228, statsY);
        GuiUtil.label(context, client, "ipban: " + ModerationHelperClient.STATS.get(SessionStats.PunishmentType.IPBAN), x + 330, statsY);

        GuiUtil.title(context, client, "Недавние игроки", x + 24, y + 120);
        if (ModerationHelperClient.RECENT_PLAYERS.getPlayers().isEmpty()) {
            GuiUtil.muted(context, client, "Пока пусто", x + 24, y + 152);
        }

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
