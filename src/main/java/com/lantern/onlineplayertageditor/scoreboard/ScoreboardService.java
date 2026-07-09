package com.lantern.onlineplayertageditor.scoreboard;

import net.minecraft.scoreboard.ScoreAccess;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.server.MinecraftServer;

import java.util.List;

public class ScoreboardService {

    public static final String MONVHUA_OBJECTIVE = "monvhua";

    public static final List<MonvhuaLevel> LEVELS = List.of(
        new MonvhuaLevel(0, "神志清醒"),
        new MonvhuaLevel(10, "略染污浊"),
        new MonvhuaLevel(25, "轻度魔女化"),
        new MonvhuaLevel(45, "中度魔女化"),
        new MonvhuaLevel(60, "高度魔女化"),
        new MonvhuaLevel(70, "重度魔女化"),
        new MonvhuaLevel(80, "准魔女"),
        new MonvhuaLevel(90, "魔女")
    );

    public record MonvhuaLevel(int value, String displayName) {}

    public static ScoreboardObjective getObjective(MinecraftServer server) {
        return server.getScoreboard().getNullableObjective(MONVHUA_OBJECTIVE);
    }

    public static boolean objectiveExists(MinecraftServer server) {
        return getObjective(server) != null;
    }

    public static int getScore(MinecraftServer server, String playerName) {
        ScoreboardObjective objective = getObjective(server);
        if (objective == null) return 0;
        ScoreHolder holder = ScoreHolder.fromName(playerName);
        ScoreAccess score = server.getScoreboard().getOrCreateScore(holder, objective);
        return score.getScore();
    }

    public static void setScore(MinecraftServer server, String playerName, int value) {
        ScoreboardObjective objective = getObjective(server);
        if (objective == null) return;
        ScoreHolder holder = ScoreHolder.fromName(playerName);
        server.getScoreboard().getOrCreateScore(holder, objective).setScore(value);
    }

    public static String getStageName(int score) {
        MonvhuaLevel level = getLevel(score);
        return level == null ? "非预设值" : level.displayName();
    }

    public static MonvhuaLevel getLevel(int score) {
        MonvhuaLevel current = null;
        for (MonvhuaLevel level : LEVELS) {
            if (level.value() <= score) {
                current = level;
            } else {
                break;
            }
        }
        return current;
    }
}
