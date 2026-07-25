package com.lantern.onlineplayertageditor.scoreboard;

import com.lantern.onlineplayertageditor.config.ModConfig;
import com.lantern.onlineplayertageditor.network.TagCategory;

import java.util.ArrayList;
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

        assertEquals(5, history.size(), "valid history points are retained until the point limit");
        assertEquals("112", history.get(0).day(), "older points are kept before the limit is reached");
        assertEquals("118", history.get(2).day(), "same-game-day records keep their original point");
        assertEquals(45, history.get(2).value(), "original same-day value is retained");
        assertEquals("118", history.get(3).day(), "same-game-day records can append another point");
        assertEquals(60, history.get(3).value(), "appended same-day value is recorded");
        assertEquals("120", history.get(4).day(), "current game day is appended");
        assertEquals(80, history.get(4).value(), "current game day value is recorded");
        assertEquals(80, MonvhuaHistory.latestValue(history), "latest value comes from the newest retained point");

        List<MonvhuaHistory.Entry> manyPoints = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            manyPoints.add(new MonvhuaHistory.Entry(String.valueOf(i), i));
        }
        List<MonvhuaHistory.Entry> pointWindow = MonvhuaHistory.window(manyPoints, today);
        assertEquals(MonvhuaHistory.MAX_POINTS_TO_SHOW, pointWindow.size(), "latest point plus previous 20 are shown");
        assertEquals("4", pointWindow.get(0).day(), "oldest points beyond the 21-point limit are trimmed");
        assertEquals("24", pointWindow.get(pointWindow.size() - 1).day(), "latest retained point remains visible");

        pointWindow = MonvhuaHistory.record(pointWindow, 25L, 99);
        assertEquals(MonvhuaHistory.MAX_POINTS_TO_SHOW, pointWindow.size(), "recording one more point keeps the 21-point limit");
        assertEquals("5", pointWindow.get(0).day(), "recording trims only the oldest visible point");
        assertEquals(99, pointWindow.get(pointWindow.size() - 1).value(), "newest point is appended");
        assertEquals(false, MonvhuaHistory.shouldRecordChange(40, 42), "2-point changes are ignored");
        assertEquals(true, MonvhuaHistory.shouldRecordChange(40, 43), "changes above 2 points are recorded");
        assertEquals(true, MonvhuaHistory.shouldRecordChange(43, 40), "downward changes above 2 points are recorded");

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
