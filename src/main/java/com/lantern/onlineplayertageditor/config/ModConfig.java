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
    public Map<String, String> tagCategories = new HashMap<>();
    public boolean dangerousConfirm = true;

    public static ModConfig createDefault() {
        ModConfig config = new ModConfig();

        // 角色 Tags
        config.presetTags.addAll(List.of(
                "ema", "cero", "nnk", "mago", "milya",
                "sherry", "yalisa", "noa", "anan", "yuki", "leiya",
                "mll", "coco", "hanna"
        ));

        // 魔法 Tags
        config.presetTags.addAll(List.of(
                "WitchSlayer", "Reversal", "Floating", "Power", "BrainWash",
                "Imitation", "Heal", "VisionControl", "Clairvoyance", "FireControl",
                "LiquidControl", "Swap", "Vision", "Sandevistan", "Perception",
                "Intervention", "Through", "MindReading"
        ));

        // 身份 Tags
        config.presetTags.addAll(List.of(
                "GrandWitch", "muhou", "player", "master", "guard",
                "MonvhuaFull"
        ));

        // 角色显示名
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
        config.tagDisplayNames.put("leiya", "莲见蕾雅");

        // 魔法显示名
        config.tagDisplayNames.put("WitchSlayer", "魔杀");
        config.tagDisplayNames.put("Reversal", "回溯");
        config.tagDisplayNames.put("Floating", "漂浮");
        config.tagDisplayNames.put("Power", "怪力");
        config.tagDisplayNames.put("BrainWash", "洗脑");
        config.tagDisplayNames.put("Imitation", "模仿");
        config.tagDisplayNames.put("Heal", "治愈");
        config.tagDisplayNames.put("VisionControl", "视线诱导");
        config.tagDisplayNames.put("Clairvoyance", "千里眼");
        config.tagDisplayNames.put("FireControl", "点火");
        config.tagDisplayNames.put("LiquidControl", "液体操纵");
        config.tagDisplayNames.put("Swap", "互换");
        config.tagDisplayNames.put("Vision", "幻视");
        config.tagDisplayNames.put("Sandevistan", "过载");
        config.tagDisplayNames.put("Perception", "感知");
        config.tagDisplayNames.put("Intervention", "介入过去");
        config.tagDisplayNames.put("Through", "穿墙");
        config.tagDisplayNames.put("MindReading", "窃密");

        // 身份显示名
        config.tagDisplayNames.put("GrandWitch", "大魔女");
        config.tagDisplayNames.put("muhou", "幕后");
        config.tagDisplayNames.put("player", "玩家");
        config.tagDisplayNames.put("master", "典狱长");
        config.tagDisplayNames.put("guard", "看守");
        config.tagDisplayNames.put("MonvhuaFull", "完全魔女化");

        return config;
    }

    public String getDisplayName(String tag) {
        return tagDisplayNames.getOrDefault(tag, tag);
    }

    public String getCategoryId(String tag) {
        return tagCategories.getOrDefault(tag, com.lantern.onlineplayertageditor.network.TagCategory.forTag(tag).id());
    }
}
