package com.lantern.onlineplayertageditor.tag;

import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;
import java.util.regex.Pattern;

public class TagService {

    private static final Pattern VALID_TAG_PATTERN = Pattern.compile("^[a-zA-Z0-9_:\\-.]+$");
    private static final int MAX_TAG_LENGTH = 64;

    /**
     * Validate a tag string. Returns error message if invalid, null if valid.
     */
    public static String validate(String tag) {
        if (tag == null || tag.isEmpty()) {
            return "Tag 不能为空";
        }
        if (tag.contains(" ")) {
            return "Tag 不能包含空格";
        }
        if (tag.length() > MAX_TAG_LENGTH) {
            return "Tag 长度不能超过 " + MAX_TAG_LENGTH + " 字符";
        }
        if (!VALID_TAG_PATTERN.matcher(tag).matches()) {
            return "Tag 包含非法字符，只允许 [a-zA-Z0-9_:\\-.]";
        }
        return null;
    }

    public static boolean isValid(String tag) {
        return validate(tag) == null;
    }

    public static boolean hasTag(ServerPlayerEntity player, String tag) {
        return player.getCommandTags().contains(tag);
    }

    public static boolean addTag(ServerPlayerEntity player, String tag) {
        String error = validate(tag);
        if (error != null) {
            return false;
        }
        return player.addCommandTag(tag);
    }

    public static boolean removeTag(ServerPlayerEntity player, String tag) {
        String error = validate(tag);
        if (error != null) {
            return false;
        }
        return player.removeCommandTag(tag);
    }

    public static boolean toggleTag(ServerPlayerEntity player, String tag) {
        String error = validate(tag);
        if (error != null) {
            return false;
        }
        if (hasTag(player, tag)) {
            player.removeCommandTag(tag);
            return false; // false = tag was removed
        } else {
            player.addCommandTag(tag);
            return true; // true = tag was added
        }
    }

    public static List<String> getTags(ServerPlayerEntity player) {
        return new ArrayList<>(player.getCommandTags());
    }

    public static int getTagCount(ServerPlayerEntity player) {
        return player.getCommandTags().size();
    }

    /**
     * Remove all preset tags from a player. Does NOT remove other unknown tags.
     */
    public static void clearPresetTags(ServerPlayerEntity player, Collection<String> presetTags) {
        for (String tag : presetTags) {
            if (isValid(tag)) {
                player.removeCommandTag(tag);
            }
        }
    }
}
