package ru.moderationhelper.recent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/** Session-only recent player list. Duplicates are moved to the top. */
public final class RecentPlayersManager {
    private final LinkedList<String> players = new LinkedList<>();
    private int limit;

    public RecentPlayersManager(int limit) {
        this.limit = Math.max(1, limit);
    }

    public synchronized void add(String nick) {
        if (nick == null || nick.isBlank()) return;
        players.removeIf(n -> n.equalsIgnoreCase(nick));
        players.addFirst(nick);
        while (players.size() > limit) {
            players.removeLast();
        }
    }

    public synchronized List<String> getPlayers() {
        return Collections.unmodifiableList(new ArrayList<>(players));
    }

    public synchronized void setLimit(int limit) {
        this.limit = Math.max(1, limit);
        while (players.size() > this.limit) {
            players.removeLast();
        }
    }
}
