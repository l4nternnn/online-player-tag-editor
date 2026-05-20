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
                "ema", "cero", "nnk", "mago", "milya",
                "sherry", "yalisa", "noa", "anan", "yuki",
                "mll", "coco", "hanna"
        ));

        config.tagDisplayNames.put("ema", "樱羽艾玛");
        config.tagDisplayNames.put("cero", "二阶堂希罗");
        config.tagDisplayNames.put("nnk", "黑部奈叶香");
        config.tagDisplayNames.put("mago", "宝生玛格");
        config.tagDisplayNames.put("milya", "佐伯米莉亚");
        config.tagDisplayNames.put("sherry", "橘雪莉");
        config.tagDisplayNames.put("yalisa", "紫藤亚里沙");
        config.tagDisplayNames.put("noa", "城崎诺亚");
        config.tagDisplayNames.put("anan", "夏目安安");
        config.tagDisplayNames.put("yuki", "月代雪");
        config.tagDisplayNames.put("mll", "冰上梅露露");
        config.tagDisplayNames.put("coco", "泽渡可可");
        config.tagDisplayNames.put("hanna", "远野汉娜");

        return config;
    }

    public String getDisplayName(String tag) {
        return tagDisplayNames.getOrDefault(tag, tag);
    }
}
