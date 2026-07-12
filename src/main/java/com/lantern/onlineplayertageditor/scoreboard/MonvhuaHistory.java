package com.lantern.onlineplayertageditor.scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class MonvhuaHistory {
    public static final int DAYS_TO_KEEP = 7;

    private MonvhuaHistory() {
    }

    public record Entry(String day, int value) {
    }

    public static List<Entry> record(List<Entry> history, long gameDay, int value) {
        Map<Long, Integer> byDay = new TreeMap<>();
        long firstKeptDay = firstKeptDay(gameDay);

        for (Entry entry : history) {
            Long entryDay = parseGameDay(entry.day());
            if (entryDay == null || entryDay < firstKeptDay || entryDay > gameDay) {
                continue;
            }
            byDay.put(entryDay, entry.value());
        }

        byDay.put(gameDay, value);
        return byDay.entrySet().stream()
                .map(entry -> new Entry(String.valueOf(entry.getKey()), entry.getValue()))
                .toList();
    }

    public static List<Entry> window(List<Entry> history, long gameDay) {
        long firstKeptDay = firstKeptDay(gameDay);
        List<Entry> kept = new ArrayList<>();

        for (Entry entry : history) {
            Long entryDay = parseGameDay(entry.day());
            if (entryDay == null || entryDay < firstKeptDay || entryDay > gameDay) {
                continue;
            }
            kept.add(entry);
        }

        kept.sort(Comparator.comparing(entry -> parseGameDay(entry.day())));
        return kept;
    }

    private static long firstKeptDay(long gameDay) {
        return Math.max(0L, gameDay - (DAYS_TO_KEEP - 1L));
    }

    private static Long parseGameDay(String day) {
        if (day == null || day.isBlank()) {
            return null;
        }
        try {
            long parsed = Long.parseLong(day);
            return parsed >= 0L ? parsed : null;
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
