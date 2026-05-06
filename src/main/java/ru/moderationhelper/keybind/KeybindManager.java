package ru.moderationhelper.keybind;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import ru.moderationhelper.ModerationHelperClient;
import ru.moderationhelper.gui.StatsScreen;

public final class KeybindManager {
    private static final KeyBinding.Category CATEGORY = KeyBinding.Category.create(
            Identifier.of(ModerationHelperClient.MOD_ID, "main")
    );

    private static KeyBinding openStatsKey;
    private static KeyBinding stopObsKey;

    private KeybindManager() {}

    public static void register() {
        openStatsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.moderation_helper_gui.open_stats",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                CATEGORY
        ));

        stopObsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.moderation_helper_gui.stop_obs",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(KeybindManager::onEndTick);
    }

    private static void onEndTick(MinecraftClient client) {
        while (openStatsKey.wasPressed()) {
            // H opens only the mod panel. It never tries to read the last chat message and never screenshots.
            client.setScreen(new StatsScreen());
        }

        while (stopObsKey.wasPressed()) {
            if (ModerationHelperClient.OBS != null && ModerationHelperClient.OBS.isRecording()) {
                ModerationHelperClient.OBS.stopRecording();
                ModerationHelperClient.sendClientMessage("OBS-запись остановлена.");
            } else {
                ModerationHelperClient.sendClientMessage("OBS-запись сейчас не идёт.");
            }
        }
    }
}
