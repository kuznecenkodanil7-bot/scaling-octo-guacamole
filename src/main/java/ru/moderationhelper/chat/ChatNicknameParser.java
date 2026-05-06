package ru.moderationhelper.chat;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Extracts a Minecraft nickname from a raw formatted chat line.
 * The parser is deliberately strict: only 3-16 chars [A-Za-z0-9_] are accepted.
 */
public final class ChatNicknameParser {
    private static final Pattern MC_COLOR_CODES = Pattern.compile("(?i)§[0-9A-FK-OR]");
    private static final Pattern ANSI_COLOR_CODES = Pattern.compile("\\u001B\\[[;\\d]*m");
    private static final Pattern NICK_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,16}$");

    private static final Set<String> RANKS = Set.of(
            "HT5", "LT5", "HT4", "LT4", "HT3", "LT3", "HT2", "LT2", "HT1", "LT1",
            "RHT3", "RLT3", "RHT2", "RLT2", "RHT1", "RLT1",
            "XHT5", "XLT5", "XHT4", "XLT4", "XHT3", "XLT3", "XHT2", "XLT2", "XHT1", "XLT1",
            "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"
    );

    private static final Set<String> SERVER_WORDS = Set.of(
            "anarchy-alpha", "anarchy-beta", "anarchy-gamma", "anarchy-new", "duels"
    );

    private ChatNicknameParser() {}

    public static Optional<String> parse(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) return Optional.empty();

        String normalized = stripFormatting(rawLine);
        List<String> tokens = tokenize(normalized);
        if (tokens.isEmpty()) return Optional.empty();

        boolean nextAfterServerWord = false;
        for (String token : tokens) {
            String cleaned = cleanToken(token);
            if (cleaned.isBlank()) continue;

            String upper = cleaned.toUpperCase(Locale.ROOT);
            String lower = cleaned.toLowerCase(Locale.ROOT);

            if (RANKS.contains(upper)) {
                continue;
            }

            if (SERVER_WORDS.contains(lower)) {
                nextAfterServerWord = true;
                continue;
            }

            if (isValidNick(cleaned)) {
                // If a server word was encountered, the first valid nick after it wins.
                return Optional.of(cleaned);
            }

            if (nextAfterServerWord) {
                // After anarchy-alpha / duels we do not scan forever through arbitrary text.
                nextAfterServerWord = false;
            }
        }

        return Optional.empty();
    }

    public static boolean containsNoScreenshotPhrase(String rawLine) {
        if (rawLine == null) return false;
        String text = stripFormatting(rawLine).toLowerCase(Locale.ROOT);
        return text.contains("tick speed")
                || text.contains("reach")
                || text.contains("fighting suspiciously")
                || text.contains("block interaction");
    }

    public static String stripFormatting(String value) {
        String withoutMcColors = MC_COLOR_CODES.matcher(value).replaceAll("");
        return ANSI_COLOR_CODES.matcher(withoutMcColors).replaceAll("");
    }

    private static List<String> tokenize(String line) {
        String safe = line
                .replace('>', ' ')
                .replace('<', ' ')
                .replace(':', ' ')
                .replace('|', ' ')
                .replace('»', ' ')
                .replace('«', ' ')
                .replace('→', ' ')
                .replace('←', ' ')
                .replace(',', ' ')
                .replace(';', ' ');
        return List.of(safe.split("\\s+"));
    }

    private static String cleanToken(String token) {
        return token
                .replaceAll("^[\\[\\]{}()<>/\\\\|:+,.;'\"`~!@#$%^&*=\\-]+", "")
                .replaceAll("[\\[\\]{}()<>/\\\\|:+,.;'\"`~!@#$%^&*=\\-]+$", "");
    }

    public static boolean isValidNick(String value) {
        return NICK_PATTERN.matcher(value).matches();
    }
}
