package com.lantern.onlineplayertageditor.scoreboard;

import com.lantern.onlineplayertageditor.config.ModConfig;
import com.lantern.onlineplayertageditor.network.TagCategory;

import java.util.List;

public final class MonvhuaHistoryTest {
    private MonvhuaHistoryTest() {
    }

    public static void main(String[] args) {
        long today = 120L;
        List<MonvhuaHistory.Entry> history = List.of(
                new MonvhuaHistory.Entry("112", 10),
                new MonvhuaHistory.Entry("114", 25),
                new MonvhuaHistory.Entry("118", 45),
                new MonvhuaHistory.Entry("2026-07-09", 50)
        );

        history = MonvhuaHistory.record(history, today - 2L, 60);
        history = MonvhuaHistory.record(history, today, 80);

        assertEquals(3, history.size(), "only the 7-day window is kept");
        assertEquals("114", history.get(0).day(), "history is sorted oldest first by game day");
        assertEquals("118", history.get(1).day(), "same-game-day records are replaced");
        assertEquals(60, history.get(1).value(), "replacement uses the latest value");
        assertEquals("120", history.get(2).day(), "current game day is appended");
        assertEquals(80, history.get(2).value(), "current game day value is recorded");

        assertEquals("轻度魔女化", ScoreboardService.getStageName(30), "scores between 25 and 45 use the lower stage");
        assertEquals("轻度魔女化", ScoreboardService.getStageName(44), "stage changes only after reaching the next threshold");
        assertEquals("中度魔女化", ScoreboardService.getStageName(45), "exact thresholds use that stage");

        assertEquals(TagCategory.OTHER, TagCategory.fromId("other"), "other category is addressable by id");
        assertEquals(TagCategory.OTHER, TagCategory.forTag("custom_manual_tag"), "unknown tags fall back to other");
        ModConfig defaults = ModConfig.createDefault();
        long defaultOtherTags = defaults.presetTags.stream()
                .filter(tag -> TagCategory.OTHER.id().equals(defaults.getCategoryId(tag)))
                .count();
        assertEquals(0L, defaultOtherTags, "default config does not seed other tags");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected " + expected + " but got " + actual);
        }
    }
}
