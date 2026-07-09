package com.lantern.onlineplayertageditor.network;

import java.util.Set;

public enum TagCategory {
    CHARACTER("character", "角色 Tags", Set.of(
            "ema", "cero", "nnk", "mago", "milya",
            "sherry", "yalisa", "noa", "anan", "yuki", "leiya",
            "mll", "coco", "hanna"
    )),
    MAGIC("magic", "魔法 Tags", Set.of(
            "WitchSlayer", "Reversal", "Floating", "Power", "BrainWash",
            "Imitation", "Heal", "VisionControl", "Clairvoyance", "FireControl",
            "LiquidControl", "Swap", "Vision", "Sandevistan", "Perception",
            "Intervention", "Through", "MindReading"
    )),
    IDENTITY("identity", "身份 Tags", Set.of(
            "GrandWitch", "muhou", "player", "master", "guard", "MonvhuaFull"
    )),
    OTHER("other", "其他 Tags", Set.of());

    private final String id;
    private final String displayName;
    private final Set<String> fixedTags;

    TagCategory(String id, String displayName, Set<String> fixedTags) {
        this.id = id;
        this.displayName = displayName;
        this.fixedTags = fixedTags;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public static TagCategory fromId(String id) {
        for (TagCategory category : values()) {
            if (category.id.equals(id)) {
                return category;
            }
        }
        return OTHER;
    }

    public static TagCategory forTag(String tag) {
        if (CHARACTER.fixedTags.contains(tag)) {
            return CHARACTER;
        }
        if (MAGIC.fixedTags.contains(tag)) {
            return MAGIC;
        }
        if (IDENTITY.fixedTags.contains(tag)) {
            return IDENTITY;
        }
        return OTHER;
    }
}
