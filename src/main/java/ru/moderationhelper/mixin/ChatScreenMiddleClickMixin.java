package ru.moderationhelper.mixin;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ChatScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.moderationhelper.ModerationHelperClient;
import ru.moderationhelper.chat.ChatClickLocator;
import ru.moderationhelper.chat.ChatNicknameParser;
import ru.moderationhelper.screenshot.PendingScreenshot;

import java.util.Optional;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMiddleClickMixin {
    /**
     * Minecraft 1.21.11 uses Click + doubled instead of the old mouseClicked(double,double,int).
     * Middle click opens the moderation GUI and cancels normal chat handling.
     */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void moderationhelper$onMouseClicked(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE) {
            return;
        }

        Optional<String> rawMessage = ChatClickLocator.findMessageAt(click.x(), click.y());
        if (rawMessage.isEmpty()) {
            ModerationHelperClient.sendClientMessage("Ник не найден: нажми СКМ прямо по строке чата.");
            cir.setReturnValue(true);
            return;
        }

        Optional<String> nick = ChatNicknameParser.parse(rawMessage.get());
        if (nick.isEmpty()) {
            ModerationHelperClient.sendClientMessage("Ник не найден в сообщении: " + rawMessage.get());
            cir.setReturnValue(true);
            return;
        }

        PendingScreenshot pendingScreenshot = ChatNicknameParser.containsNoScreenshotPhrase(rawMessage.get())
                ? PendingScreenshot.empty(nick.get())
                : ModerationHelperClient.SCREENSHOTS.captureTemp(nick.get());

        ModerationHelperClient.openPunishmentPanel(nick.get(), pendingScreenshot);
        cir.setReturnValue(true);
    }
}
