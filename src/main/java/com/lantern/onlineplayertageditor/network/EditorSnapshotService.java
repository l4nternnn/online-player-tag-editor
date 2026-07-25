package com.lantern.onlineplayertageditor.network;

import com.lantern.onlineplayertageditor.config.ConfigManager;
import com.lantern.onlineplayertageditor.config.ModConfig;
import com.lantern.onlineplayertageditor.scoreboard.MonvhuaHistoryService;
import com.lantern.onlineplayertageditor.scoreboard.ScoreboardService;
import com.lantern.onlineplayertageditor.tag.TagService;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class EditorSnapshotService {
    private EditorSnapshotService() {
    }

    public static EditorSnapshot create(ServerPlayerEntity viewer, UUID requestedTargetUuid) {
        List<ServerPlayerEntity> onlinePlayers = new ArrayList<>(viewer.getServer().getPlayerManager().getPlayerList());
        onlinePlayers.sort(Comparator.comparing(player -> player.getGameProfile().getName().toLowerCase()));

        ServerPlayerEntity selected = null;
        if (requestedTargetUuid != null) {
            selected = viewer.getServer().getPlayerManager().getPlayer(requestedTargetUuid);
        }
        if (selected == null && !onlinePlayers.isEmpty()) {
            selected = onlinePlayers.get(0);
        }

        UUID selectedUuid = selected != null ? selected.getUuid() : requestedTargetUuid;
        String selectedName = selected != null ? selected.getGameProfile().getName() : "目标玩家已离线";
        boolean objectiveExists = ScoreboardService.objectiveExists(viewer.getServer());
        int score = selected != null && objectiveExists
                ? ScoreboardService.getScore(viewer.getServer(), selected.getGameProfile().getName())
                : 0;
        long gameDay = selected != null ? gameDay(selected) : 0L;

        List<String> selectedTags = selected != null ? TagService.getTags(selected) : List.of();
        Set<String> selectedTagSet = Set.copyOf(selectedTags);
        ModConfig config = ConfigManager.getConfig();

        List<EditorSnapshot.PlayerEntry> players = onlinePlayers.stream()
                .map(player -> new EditorSnapshot.PlayerEntry(
                        player.getUuid(),
                        player.getGameProfile().getName(),
                        TagService.getTagCount(player)
                ))
                .toList();

        List<EditorSnapshot.TagEntry> presetTags = config.presetTags.stream()
                .map(tag -> new EditorSnapshot.TagEntry(
                        tag,
                        config.getDisplayName(tag),
                        config.getCategoryId(tag),
                        selectedTagSet.contains(tag)
                ))
                .toList();

        List<EditorSnapshot.ScoreLevelEntry> scoreLevels = ScoreboardService.LEVELS.stream()
                .map(level -> new EditorSnapshot.ScoreLevelEntry(level.value(), level.displayName()))
                .toList();
        if (selected != null && objectiveExists) {
            MonvhuaHistoryService.recordObservedScore(selected.getUuid(), score, gameDay);
        }
        List<EditorSnapshot.ScoreHistoryEntry> scoreHistory = selected != null && objectiveExists
                ? MonvhuaHistoryService.historyFor(selected.getUuid(), gameDay).stream()
                .map(entry -> new EditorSnapshot.ScoreHistoryEntry(entry.day(), entry.value()))
                .toList()
                : List.of();

        return new EditorSnapshot(
                config.guiTitle,
                selectedUuid,
                selectedName,
                selected != null,
                score,
                ScoreboardService.getStageName(score),
                objectiveExists,
                players,
                presetTags,
                selectedTags,
                new HashMap<>(config.tagDisplayNames),
                scoreLevels,
                scoreHistory
        );
    }

    public static List<String> getPresetTagsForCategory(String categoryId) {
        return ConfigManager.getConfig().presetTags.stream()
                .filter(tag -> categoryId.equals(ConfigManager.getConfig().getCategoryId(tag)))
                .toList();
    }

    private static long gameDay(ServerPlayerEntity player) {
        return Math.max(0L, player.getWorld().getTimeOfDay() / 24000L);
    }
}
