package ru.moderationhelper.screenshot;

import java.nio.file.Path;
import java.time.LocalDateTime;

public record PendingScreenshot(String nick, Path path, LocalDateTime createdAt, boolean exists) {
    public static PendingScreenshot empty(String nick) {
        return new PendingScreenshot(nick, null, LocalDateTime.now(), false);
    }
}
