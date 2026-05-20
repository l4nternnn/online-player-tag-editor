package com.lantern.onlineplayertageditor.util;

import com.lantern.onlineplayertageditor.config.ConfigManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class PermissionUtil {

    /**
     * Check if source can open the tag management GUI.
     * Requires OP level >= configured permissionLevel.
     * Reserved permission node: online_player_tag_editor.open
     */
    public static boolean canOpen(ServerCommandSource source) {
        return source.hasPermissionLevel(ConfigManager.getConfig().permissionLevel);
    }

    /**
     * Check if source can edit tags (add/remove/toggle).
     * Requires OP level >= configured permissionLevel.
     * Reserved permission node: online_player_tag_editor.edit
     */
    public static boolean canEdit(ServerCommandSource source) {
        return source.hasPermissionLevel(ConfigManager.getConfig().permissionLevel);
    }

    /**
     * Check if source can reload config.
     * Requires OP level >= configured permissionLevel.
     * Reserved permission node: online_player_tag_editor.reload
     */
    public static boolean canReload(ServerCommandSource source) {
        return source.hasPermissionLevel(ConfigManager.getConfig().permissionLevel);
    }

    /**
     * Check if a server player entity can open the GUI.
     * Re-checks permission on every GUI interaction.
     */
    public static boolean canOpenGui(ServerPlayerEntity player) {
        return player.hasPermissionLevel(ConfigManager.getConfig().permissionLevel);
    }

    /**
     * Check if a server player entity can edit tags.
     */
    public static boolean canEditTags(ServerPlayerEntity player) {
        return player.hasPermissionLevel(ConfigManager.getConfig().permissionLevel);
    }
}
