package ru.moderationhelper.stats;

import java.util.EnumMap;
import java.util.Map;

public final class SessionStats {
    public enum PunishmentType {
        WARN("warn"),
        MUTE("mute"),
        BAN("ban"),
        IPBAN("ipban");

        private final String command;

        PunishmentType(String command) {
            this.command = command;
        }

        public String command() {
            return command;
        }

        public String displayName() {
            return switch (this) {
                case WARN -> "Warn";
                case MUTE -> "Mute";
                case BAN -> "Ban";
                case IPBAN -> "IPBan";
            };
        }
    }

    private final Map<PunishmentType, Integer> values = new EnumMap<>(PunishmentType.class);

    public SessionStats() {
        for (PunishmentType type : PunishmentType.values()) {
            values.put(type, 0);
        }
    }

    public void increment(PunishmentType type) {
        values.compute(type, (k, v) -> v == null ? 1 : v + 1);
    }

    public int get(PunishmentType type) {
        return values.getOrDefault(type, 0);
    }
}
