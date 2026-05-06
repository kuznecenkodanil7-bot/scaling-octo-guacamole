package ru.moderationhelper.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import ru.moderationhelper.screenshot.PendingScreenshot;
import ru.moderationhelper.stats.SessionStats;

import java.util.regex.Pattern;

public final class DurationScreen extends Screen {
    private static final Pattern DURATION_PATTERN = Pattern.compile("^[1-9][0-9]*(d|h)$", Pattern.CASE_INSENSITIVE);

    private final String nick;
    private final SessionStats.PunishmentType type;
    private final PendingScreenshot pendingScreenshot;
    private TextFieldWidget durationField;
    private String error = "";

    public DurationScreen(String nick, SessionStats.PunishmentType type, PendingScreenshot pendingScreenshot) {
        super(Text.literal("Выбор времени"));
        this.nick = nick;
        this.type = type;
        this.pendingScreenshot = pendingScreenshot;
    }

    @Override
    protected void init() {
        int panelW = Math.min(380, width - 40);
        int panelH = 170;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        durationField = new TextFieldWidget(textRenderer, x + 26, y + 72, panelW - 52, 22, Text.literal("duration"));
        durationField.setMaxLength(8);
        durationField.setPlaceholder(Text.literal("Например: 7d или 12h"));
        durationField.setText("");
        addDrawableChild(durationField);
        setInitialFocus(durationField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Дальше"), b -> next())
                .dimensions(x + 26, y + 112, 110, 24).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Назад"), b -> MinecraftClient.getInstance().setScreen(new PunishmentScreen(nick, pendingScreenshot)))
                .dimensions(x + 146, y + 112, 90, 24).build());
    }

    private void next() {
        String duration = durationField.getText().trim().toLowerCase();
        if (!DURATION_PATTERN.matcher(duration).matches()) {
            error = "Формат: число + d/h. Пример: 7d, 12h";
            return;
        }
        MinecraftClient.getInstance().setScreen(new ReasonScreen(nick, type, duration, pendingScreenshot));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        renderBackground(context, mouseX, mouseY, deltaTicks);
        int panelW = Math.min(380, width - 40);
        int panelH = 170;
        int x = (width - panelW) / 2;
        int y = (height - panelH) / 2;

        context.fill(0, 0, width, height, 0x99000000);
        GuiUtil.panel(context, x, y, panelW, panelH);
        GuiUtil.title(context, client, type.displayName() + " для " + nick, x + 26, y + 20);
        GuiUtil.label(context, client, "Время наказания: d — дни, h — часы", x + 26, y + 48);
        if (!error.isBlank()) {
            context.drawText(textRenderer, Text.literal(error), x + 26, y + 98, 0xFFFF6666, false);
        }
        super.render(context, mouseX, mouseY, deltaTicks);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
