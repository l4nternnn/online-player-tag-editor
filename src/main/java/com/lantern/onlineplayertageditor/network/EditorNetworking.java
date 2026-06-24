package com.lantern.onlineplayertageditor.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lantern.onlineplayertageditor.config.ConfigManager;
import com.lantern.onlineplayertageditor.scoreboard.ScoreboardService;
import com.lantern.onlineplayertageditor.tag.TagService;
import com.lantern.onlineplayertageditor.util.PermissionUtil;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;

public final class EditorNetworking {
    public static final Gson GSON = new GsonBuilder().create();

    private EditorNetworking() {
    }

    public static void registerPayloadTypes() {
        PayloadTypeRegistry.playS2C().register(EditorPayloads.OpenEditorS2C.ID, EditorPayloads.OpenEditorS2C.CODEC);
        PayloadTypeRegistry.playS2C().register(EditorPayloads.EditorNoticeS2C.ID, EditorPayloads.EditorNoticeS2C.CODEC);
        PayloadTypeRegistry.playC2S().register(EditorPayloads.RefreshEditorC2S.ID, EditorPayloads.RefreshEditorC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(EditorPayloads.ToggleTagC2S.ID, EditorPayloads.ToggleTagC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(EditorPayloads.AddPresetTagC2S.ID, EditorPayloads.AddPresetTagC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(EditorPayloads.RemovePresetTagC2S.ID, EditorPayloads.RemovePresetTagC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(EditorPayloads.ClearPresetTagsC2S.ID, EditorPayloads.ClearPresetTagsC2S.CODEC);
        PayloadTypeRegistry.playC2S().register(EditorPayloads.SetMonvhuaScoreC2S.ID, EditorPayloads.SetMonvhuaScoreC2S.CODEC);
    }

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(EditorPayloads.RefreshEditorC2S.ID,
                (payload, context) -> sendSnapshot(context.player(), payload.selectedPlayerUuid()));

        ServerPlayNetworking.registerGlobalReceiver(EditorPayloads.ToggleTagC2S.ID,
                (payload, context) -> toggleTag(context.player(), payload.targetPlayerUuid(), payload.tag()));

        ServerPlayNetworking.registerGlobalReceiver(EditorPayloads.AddPresetTagC2S.ID,
                (payload, context) -> addPresetTag(context.player(), payload.selectedPlayerUuid(), payload.categoryId(), payload.displayName(), payload.tag()));

        ServerPlayNetworking.registerGlobalReceiver(EditorPayloads.RemovePresetTagC2S.ID,
                (payload, context) -> removePresetTag(context.player(), payload.selectedPlayerUuid(), payload.categoryId(), payload.tag()));

        ServerPlayNetworking.registerGlobalReceiver(EditorPayloads.ClearPresetTagsC2S.ID,
                (payload, context) -> clearPresetTags(context.player(), payload.targetPlayerUuid(), payload.categoryId()));

        ServerPlayNetworking.registerGlobalReceiver(EditorPayloads.SetMonvhuaScoreC2S.ID,
                (payload, context) -> setMonvhuaScore(context.player(), payload.targetPlayerUuid(), payload.value()));
    }

    public static void openEditor(ServerPlayerEntity viewer, UUID selectedPlayerUuid) {
        if (!PermissionUtil.canOpenGui(viewer)) {
            viewer.sendMessage(Text.literal("你没有权限使用 Tag 编辑器").formatted(Formatting.RED));
            return;
        }
        sendSnapshot(viewer, selectedPlayerUuid);
    }

    private static void sendSnapshot(ServerPlayerEntity viewer, UUID selectedPlayerUuid) {
        if (!ServerPlayNetworking.canSend(viewer, EditorPayloads.OpenEditorS2C.ID)) {
            viewer.sendMessage(Text.literal("你的客户端未安装新版 Online Player Tag Editor，无法打开 Modern UI 管理台。")
                    .formatted(Formatting.RED));
            return;
        }

        EditorSnapshot snapshot = EditorSnapshotService.create(viewer, selectedPlayerUuid);
        ServerPlayNetworking.send(viewer, new EditorPayloads.OpenEditorS2C(GSON.toJson(snapshot)));
    }

    private static void toggleTag(ServerPlayerEntity viewer, UUID targetUuid, String tag) {
        if (!PermissionUtil.canEditTags(viewer)) {
            sendNotice(viewer, "你没有权限编辑 Tags", true);
            return;
        }

        ServerPlayerEntity target = getOnlineTarget(viewer, targetUuid);
        if (target == null) {
            sendNotice(viewer, "目标玩家已离线", true);
            sendSnapshot(viewer, targetUuid);
            return;
        }

        boolean added = TagService.toggleTag(target, tag);
        String message = added ? "已添加 tag: " + tag : "已移除 tag: " + tag;
        sendNotice(viewer, message, false);
        sendSnapshot(viewer, targetUuid);
    }

    private static void addPresetTag(ServerPlayerEntity viewer, UUID selectedUuid, String categoryId, String displayName, String tag) {
        if (!PermissionUtil.canEditTags(viewer)) {
            sendNotice(viewer, "你没有权限编辑 Tags", true);
            return;
        }

        String error = TagService.validate(tag);
        if (error != null) {
            sendNotice(viewer, error, true);
            sendSnapshot(viewer, selectedUuid);
            return;
        }
        if (displayName == null || displayName.isBlank()) {
            sendNotice(viewer, "名称不能为空", true);
            sendSnapshot(viewer, selectedUuid);
            return;
        }

        boolean added = ConfigManager.addPresetTag(TagCategory.fromId(categoryId).id(), tag, displayName.trim());
        sendNotice(viewer, added ? "已添加可赋予 tag: " + displayName + " (" + tag + ")" : "已更新可赋予 tag: " + displayName + " (" + tag + ")", false);
        sendSnapshot(viewer, selectedUuid);
    }

    private static void removePresetTag(ServerPlayerEntity viewer, UUID selectedUuid, String categoryId, String tag) {
        if (!PermissionUtil.canEditTags(viewer)) {
            sendNotice(viewer, "你没有权限编辑 Tags", true);
            return;
        }

        String error = TagService.validate(tag);
        if (error != null) {
            sendNotice(viewer, error, true);
            sendSnapshot(viewer, selectedUuid);
            return;
        }

        boolean removed = ConfigManager.removePresetTag(tag);
        sendNotice(viewer, removed ? "已从当前面板移除可赋予 tag: " + tag : "当前面板没有可移除 tag: " + tag, !removed);
        sendSnapshot(viewer, selectedUuid);
    }

    private static void clearPresetTags(ServerPlayerEntity viewer, UUID targetUuid, String categoryId) {
        if (!PermissionUtil.canEditTags(viewer)) {
            sendNotice(viewer, "你没有权限编辑 Tags", true);
            return;
        }

        ServerPlayerEntity target = getOnlineTarget(viewer, targetUuid);
        if (target == null) {
            sendNotice(viewer, "目标玩家已离线", true);
            sendSnapshot(viewer, targetUuid);
            return;
        }

        List<String> tagsToClear = EditorSnapshotService.getPresetTagsForCategory(categoryId);
        TagService.clearPresetTags(target, tagsToClear);
        sendNotice(viewer, "已清除当前分类预设 tags", false);
        sendSnapshot(viewer, targetUuid);
    }

    private static void setMonvhuaScore(ServerPlayerEntity viewer, UUID targetUuid, int value) {
        if (!PermissionUtil.canEditTags(viewer)) {
            sendNotice(viewer, "你没有权限编辑计分板", true);
            return;
        }

        ServerPlayerEntity target = getOnlineTarget(viewer, targetUuid);
        if (target == null) {
            sendNotice(viewer, "目标玩家已离线", true);
            sendSnapshot(viewer, targetUuid);
            return;
        }

        if (!ScoreboardService.objectiveExists(viewer.getServer())) {
            sendNotice(viewer, "计分板 objective monvhua 不存在", true);
            sendSnapshot(viewer, targetUuid);
            return;
        }

        ScoreboardService.setScore(viewer.getServer(), target.getGameProfile().getName(), value);
        sendNotice(viewer, "已设置 monvhua: " + value + " (" + ScoreboardService.getStageName(value) + ")", false);
        sendSnapshot(viewer, targetUuid);
    }

    private static ServerPlayerEntity getOnlineTarget(ServerPlayerEntity viewer, UUID targetUuid) {
        return viewer.getServer().getPlayerManager().getPlayer(targetUuid);
    }

    private static void sendNotice(ServerPlayerEntity viewer, String message, boolean error) {
        if (ServerPlayNetworking.canSend(viewer, EditorPayloads.EditorNoticeS2C.ID)) {
            ServerPlayNetworking.send(viewer, new EditorPayloads.EditorNoticeS2C(message, error));
        } else {
            viewer.sendMessage(Text.literal(message).formatted(error ? Formatting.RED : Formatting.GREEN));
        }
    }
}
