package ru.moderationhelper.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import ru.moderationhelper.ModerationHelperClient;
import ru.moderationhelper.screenshot.PendingScreenshot;
import ru.moderationhelper.stats.SessionStats;

import java.util.List;

public final class ReasonScreen extends Screen {
    private final String nick;
    private final SessionStats.PunishmentType type;
    private final String duration;
    private final PendingScreenshot pendingScreenshot;
    private TextFieldWidget reasonField;
    private String error = "";

    public ReasonScreen(String nick, SessionStats.PunishmentType type, String duration, PendingScreenshot pendingScreenshot) {
        super(Text.literal("Выбор причины"));
        this.nick = nick;
        this.type = type;
        this.duration = duration;
        this.pendingScreenshot = pendingScreenshot;
    }

    @Override
    protected void init() {
        int panelW = Math.min(460, width - 40);
        int panelH = Math.min(260, height - 40);
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        reasonField = new TextFieldWidget(textRenderer, x + 26, y + 72, panelW - 52, 22, Text.literal("reason"));
        reasonField.setMaxLength(64);
        reasonField.setPlaceholder(Text.literal("Причина наказания"));
        addDrawableChild(reasonField);
        setInitialFocus(reasonField);

        addQuickReasonButtons(x + 26, y + 108, panelW - 52);

        addDrawableChild(ButtonWidget.builder(Text.literal("Выдать наказание"), b -> punish())
                .dimensions(x + 26, y + panelH - 42, 150, 24).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> MinecraftClient.getInstance().setScreen(new DurationScreen(nick, type, pendingScreenshot)))
                .dimensions(x + 186, y + panelH - 42, 90, 24).build());
    }

    private void addQuickReasonButtons(int x, int y, int availableW) {
        List<String> reasons = ModerationHelperClient.CONFIG.quickReasons;
        int currentX = x;
        int currentY = y;
        int maxY = y + 72;
        for (String reason : reasons) {
            int w = Math.min(90, Math.max(42, textRenderer.getWidth(reason) + 18));
            if (currentX + w > x + availableW) {
                currentX = x;
                currentY += 24;
            }
            if (currentY > maxY) break;
            addDrawableChild(ButtonWidget.builder(Text.literal(reason), b -> reasonField.setText(reason))
                    .dimensions(currentX, currentY, w, 20).build());
            currentX += w + 6;
        }
    }

    private void punish() {
        String reason = reasonField.getText().trim();
        if (reason.isBlank()) {
            error = "Причина не может быть пустой.";
            return;
        }

        String command = "/" + type.command() + " " + nick + " " + duration + " " + reason;
        ModerationHelperClient.sendCommandOrMessage(command);
        ModerationHelperClient.STATS.increment(type);
        ModerationHelperClient.RECENT_PLAYERS.add(nick);
        ModerationHelperClient.SCREENSHOTS.finishScreenshot(pendingScreenshot, type, duration, reason);

        if (type == SessionStats.PunishmentType.IPBAN && !reason.equalsIgnoreCase("3.8")) {
            ModerationHelperClient.OBS.stopRecording();
        }

        MinecraftClient.getInstance().setScreen(null);
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
        GuiUtil.title(context, client, type.displayName() + " для " + nick, x + 26, y + 20);
        GuiUtil.label(context, client, "Время: §b" + duration + "§r  Причина:", x + 26, y + 48);
        GuiUtil.muted(context, client, "Быстрые причины:", x + 26, y + 98);
        if (!error.isBlank()) {
            context.drawText(textRenderer, Text.literal(error), x + 26, y + panelH - 58, 0xFFFF6666, false);
        }
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
