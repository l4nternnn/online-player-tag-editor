package com.lantern.onlineplayertageditor.scoreboard;

import java.util.ArrayList;
import java.util.List;

public final class MonvhuaHistory {
    public static final int PREVIOUS_POINTS_TO_SHOW = 20;
    public static final int MAX_POINTS_TO_SHOW = PREVIOUS_POINTS_TO_SHOW + 1;
    public static final int SIGNIFICANT_CHANGE_THRESHOLD = 2;

    private MonvhuaHistory() {
    }

    public record Entry(String day, int value) {
    }

    public static boolean shouldRecordChange(int previousValue, int nextValue) {
        return Math.abs(nextValue - previousValue) > SIGNIFICANT_CHANGE_THRESHOLD;
    }

    public static List<Entry> record(List<Entry> history, long gameDay, int value) {
        List<Entry> kept = compact(history);
        kept.add(new Entry(String.valueOf(Math.max(0L, gameDay)), value));
        return List.copyOf(trimToMaxPoints(kept));
    }

    public static List<Entry> window(List<Entry> history, long gameDay) {
        return List.copyOf(compact(history));
    }

    public static Integer latestValue(List<Entry> history) {
        List<Entry> compacted = compact(history);
        if (compacted.isEmpty()) {
            return null;
        }
        return compacted.get(compacted.size() - 1).value();
    }

    private static List<Entry> compact(List<Entry> history) {
        List<Entry> kept = new ArrayList<>();
        for (Entry entry : history) {
            Long entryDay = parseGameDay(entry.day());
            if (entryDay == null) {
                continue;
            }
            kept.add(entry);
        }
        return trimToMaxPoints(kept);
    }

    private static List<Entry> trimToMaxPoints(List<Entry> entries) {
        if (entries.size() <= MAX_POINTS_TO_SHOW) {
            return entries;
        }
        return new ArrayList<>(entries.subList(entries.size() - MAX_POINTS_TO_SHOW, entries.size()));
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
