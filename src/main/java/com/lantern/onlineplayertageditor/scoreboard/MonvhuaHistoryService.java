package com.lantern.onlineplayertageditor.scoreboard;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lantern.onlineplayertageditor.OnlinePlayerTagEditor;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class MonvhuaHistoryService {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path HISTORY_PATH = FabricLoader.getInstance().getConfigDir()
            .resolve("online-player-tag-editor-monvhua-history.json");

    private static HistoryData data;

    private MonvhuaHistoryService() {
    }

    public static synchronized void recordScore(UUID playerUuid, int score, long gameDay) {
        if (playerUuid == null) {
            return;
        }

        HistoryData historyData = data();
        String key = playerUuid.toString();
        List<MonvhuaHistory.Entry> updated = MonvhuaHistory.record(
                historyData.players.getOrDefault(key, List.of()),
                gameDay,
                score
        );
        historyData.players.put(key, updated);
        save(historyData);
    }

    public static synchronized List<MonvhuaHistory.Entry> historyFor(UUID playerUuid, long gameDay) {
        if (playerUuid == null) {
            return List.of();
        }

        HistoryData historyData = data();
        String key = playerUuid.toString();
        List<MonvhuaHistory.Entry> window = MonvhuaHistory.window(
                historyData.players.getOrDefault(key, List.of()),
                gameDay
        );
        historyData.players.put(key, window);
        save(historyData);
        return List.copyOf(window);
    }

    private static HistoryData data() {
        if (data != null) {
            return data;
        }

        if (!Files.exists(HISTORY_PATH)) {
            data = new HistoryData();
            return data;
        }

        try (Reader reader = Files.newBufferedReader(HISTORY_PATH, StandardCharsets.UTF_8)) {
            data = GSON.fromJson(reader, HistoryData.class);
        } catch (IOException | RuntimeException e) {
            OnlinePlayerTagEditor.LOGGER.warn("Failed to load monvhua history, starting with an empty history", e);
            data = new HistoryData();
        }

        if (data == null) {
            data = new HistoryData();
        }
        if (data.players == null) {
            data.players = new HashMap<>();
        }
        return data;
    }

    private static void save(HistoryData historyData) {
        try {
            Files.createDirectories(HISTORY_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(HISTORY_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(historyData, writer);
            }
        } catch (IOException e) {
            OnlinePlayerTagEditor.LOGGER.warn("Failed to save monvhua history", e);
        }
    }

    private static final class HistoryData {
        private Map<String, List<MonvhuaHistory.Entry>> players = new HashMap<>();
    }
}
