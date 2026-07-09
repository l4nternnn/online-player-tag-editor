package com.lantern.onlineplayertageditor.network;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record EditorSnapshot(
        String title,
        UUID selectedPlayerUuid,
        String selectedPlayerName,
        boolean selectedPlayerOnline,
        int selectedScore,
        String selectedStageName,
        boolean scoreboardObjectiveExists,
        List<PlayerEntry> players,
        List<TagEntry> presetTags,
        List<String> selectedTags,
        Map<String, String> tagDisplayNames,
        List<ScoreLevelEntry> scoreLevels,
        List<ScoreHistoryEntry> scoreHistory
) {
    public record PlayerEntry(UUID uuid, String name, int tagCount) {
    }

    public record TagEntry(String tag, String displayName, String categoryId, boolean selected) {
    }

    public record ScoreLevelEntry(int value, String displayName) {
    }

    public record ScoreHistoryEntry(String day, int value) {
    }
}
