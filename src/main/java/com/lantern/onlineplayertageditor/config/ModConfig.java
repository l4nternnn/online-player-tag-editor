package com.lantern.onlineplayertageditor.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    public int permissionLevel = 2;
    public String guiTitle = "在线玩家 Tag 管理";
    public boolean enableEscButton = true;
    public String escButtonText = "Tag 管理";
    public List<String> presetTags = new ArrayList<>();
    public Map<String, String> tagDisplayNames = new HashMap<>();
    public boolean dangerousConfirm = true;

    public static ModConfig createDefault() {
        ModConfig config = new ModConfig();

        config.presetTags.addAll(List.of(
                "lobby", "script_1", "script_2",
                "host", "spectator", "dead", "alive"
        ));

        config.tagDisplayNames.put("lobby", "大厅");
        config.tagDisplayNames.put("script_1", "剧本杀一服");
        config.tagDisplayNames.put("script_2", "剧本杀二服");
        config.tagDisplayNames.put("host", "主持人");
        config.tagDisplayNames.put("spectator", "旁观者");
        config.tagDisplayNames.put("dead", "死亡");
        config.tagDisplayNames.put("alive", "存活");

        return config;
    }

    public String getDisplayName(String tag) {
        return tagDisplayNames.getOrDefault(tag, tag);
    }
}
