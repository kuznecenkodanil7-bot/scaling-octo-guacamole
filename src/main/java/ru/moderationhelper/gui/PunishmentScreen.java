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

public final class PunishmentScreen extends Screen {
    private final String nick;
    private final PendingScreenshot pendingScreenshot;

    public PunishmentScreen(String nick, PendingScreenshot pendingScreenshot) {
        super(Text.literal("Moderation Helper GUI"));
        this.nick = nick;
        this.pendingScreenshot = pendingScreenshot;
    }

    @Override
    protected void init() {
        clearChildren();
        int panelW = Math.min(520, width - 40);
        int panelH = Math.min(300, height - 40);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        int buttonX = x + 24;
        int buttonY = y + 56;
        int buttonW = 138;
        int buttonH = 24;
        int gap = 8;

        addDrawableChild(ButtonWidget.builder(Text.literal("Warn"), b -> openDuration(SessionStats.PunishmentType.WARN))
                .dimensions(buttonX, buttonY, buttonW, buttonH).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Mute"), b -> openDuration(SessionStats.PunishmentType.MUTE))
                .dimensions(buttonX, buttonY + (buttonH + gap), buttonW, buttonH).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Ban"), b -> openDuration(SessionStats.PunishmentType.BAN))
                .dimensions(buttonX, buttonY + 2 * (buttonH + gap), buttonW, buttonH).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("IPBan"), b -> openDuration(SessionStats.PunishmentType.IPBAN))
                .dimensions(buttonX, buttonY + 3 * (buttonH + gap), buttonW, buttonH).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Вызвать на проверку"), b -> callCheck())
                .dimensions(buttonX, buttonY + 4 * (buttonH + gap) + 10, buttonW + 48, buttonH).build());

        addRecentButtons(x + 24, y + panelH - 76, panelW - 48);
    }

    private void openDuration(SessionStats.PunishmentType type) {
        MinecraftClient.getInstance().setScreen(new DurationScreen(nick, type, pendingScreenshot));
    }

    private void callCheck() {
        String command = ModerationHelperClient.CONFIG.checkCommandTemplate.replace("{nick}", nick);
        ModerationHelperClient.sendCommandOrMessage(command);
        ModerationHelperClient.RECENT_PLAYERS.add(nick);
        ModerationHelperClient.OBS.startRecording();
        ModerationHelperClient.sendClientMessage("Игрок вызван на проверку: " + nick);
    }

    private void addRecentButtons(int x, int y, int availableW) {
        List<String> recent = ModerationHelperClient.RECENT_PLAYERS.getPlayers();
        int currentX = x;
        int currentY = y + 18;
        for (String player : recent) {
            int w = Math.min(96, Math.max(52, textRenderer.getWidth(player) + 18));
            if (currentX + w > x + availableW) {
                currentX = x;
                currentY += 24;
            }
            if (currentY > y + 50) break;
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
        int panelW = Math.min(520, width - 40);
        int panelH = Math.min(300, height - 40);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        context.fill(0, 0, width, height, 0x99000000);
        GuiUtil.panel(context, x, y, panelW, panelH);
        GuiUtil.title(context, client, "Moderation Helper GUI", x + 24, y + 18);
        GuiUtil.label(context, client, "Игрок: §b" + nick, x + 24, y + 36);

        int statsX = x + panelW - 148;
        int statsY = y + 54;
        GuiUtil.panel(context, statsX - 10, statsY - 12, 124, 102);
        GuiUtil.title(context, client, "Сессия", statsX, statsY);
        GuiUtil.label(context, client, "warn: " + ModerationHelperClient.STATS.get(SessionStats.PunishmentType.WARN), statsX, statsY + 20);
        GuiUtil.label(context, client, "mute: " + ModerationHelperClient.STATS.get(SessionStats.PunishmentType.MUTE), statsX, statsY + 36);
        GuiUtil.label(context, client, "ban: " + ModerationHelperClient.STATS.get(SessionStats.PunishmentType.BAN), statsX, statsY + 52);
        GuiUtil.label(context, client, "ipban: " + ModerationHelperClient.STATS.get(SessionStats.PunishmentType.IPBAN), statsX, statsY + 68);

        GuiUtil.title(context, client, "Недавние игроки", x + 24, y + panelH - 76);
        if (ModerationHelperClient.RECENT_PLAYERS.getPlayers().isEmpty()) {
            GuiUtil.muted(context, client, "Пока пусто", x + 24, y + panelH - 38);
        }

        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
