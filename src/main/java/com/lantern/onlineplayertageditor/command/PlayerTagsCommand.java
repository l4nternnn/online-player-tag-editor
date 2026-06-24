package com.lantern.onlineplayertageditor.command;

import com.lantern.onlineplayertageditor.config.ConfigManager;
import com.lantern.onlineplayertageditor.network.EditorNetworking;
import com.lantern.onlineplayertageditor.tag.TagService;
import com.lantern.onlineplayertageditor.util.PermissionUtil;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PlayerTagsCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(literal("tageditor")
                    .requires(PermissionUtil::canOpen)
                    .executes(PlayerTagsCommand::openGui)
                    .then(literal("reload")
                            .requires(PermissionUtil::canReload)
                            .executes(PlayerTagsCommand::reloadConfig)
                    )
                    .then(argument("player", EntityArgumentType.player())
                            .executes(PlayerTagsCommand::openEditor)
                            .then(literal("list")
                                    .executes(PlayerTagsCommand::listTags)
                            )
                            .then(literal("add")
                                    .then(argument("tag", StringArgumentType.word())
                                            .suggests(PlayerTagsCommand::suggestTags)
                                            .executes(PlayerTagsCommand::addTag)
                                    )
                            )
                            .then(literal("remove")
                                    .then(argument("tag", StringArgumentType.word())
                                            .suggests(PlayerTagsCommand::suggestPlayerTags)
                                            .executes(PlayerTagsCommand::removeTag)
                                    )
                            )
                            .then(literal("toggle")
                                    .then(argument("tag", StringArgumentType.word())
                                            .suggests(PlayerTagsCommand::suggestTags)
                                            .executes(PlayerTagsCommand::toggleTag)
                                    )
                            )
                    )
            );
        });
    }

    private static int openGui(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("该命令只能由玩家打开 GUI"));
            return 0;
        }
        ServerPlayerEntity player = source.getPlayerOrThrow();
        EditorNetworking.openEditor(player, null);
        return 1;
    }

    private static int openEditor(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        if (!source.isExecutedByPlayer()) {
            source.sendError(Text.literal("该命令只能由玩家打开 GUI"));
            return 0;
        }
        ServerPlayerEntity viewer = source.getPlayerOrThrow();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

        if (target == null) {
            source.sendError(Text.literal("玩家不在线"));
            return 0;
        }

        EditorNetworking.openEditor(viewer, target.getUuid());
        return 1;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> ctx) {
        ConfigManager.reload();
        ctx.getSource().sendFeedback(
                () -> Text.literal("配置文件已重载").formatted(Formatting.GREEN),
                true
        );
        return 1;
    }

    private static int listTags(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");

        if (target == null) {
            source.sendError(Text.literal("玩家不在线"));
            return 0;
        }

        List<String> tags = TagService.getTags(target);
        String playerName = target.getGameProfile().getName();

        source.sendFeedback(
                () -> Text.literal("===== " + playerName + " 的 Tags (" + tags.size() + ") =====")
                        .formatted(Formatting.GOLD),
                false
        );

        if (tags.isEmpty()) {
            source.sendFeedback(() -> Text.literal("  (无)").formatted(Formatting.GRAY), false);
        } else {
            for (String tag : tags) {
                String display = ConfigManager.getConfig().getDisplayName(tag);
                if (!display.equals(tag)) {
                    source.sendFeedback(
                            () -> Text.literal("  " + display + " (" + tag + ")").formatted(Formatting.WHITE),
                            false
                    );
                } else {
                    source.sendFeedback(
                            () -> Text.literal("  " + tag).formatted(Formatting.WHITE),
                            false
                    );
                }
            }
        }
        return 1;
    }

    private static int addTag(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        if (!PermissionUtil.canEdit(source)) {
            source.sendError(Text.literal("你没有权限编辑 Tags"));
            return 0;
        }

        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        if (target == null) {
            source.sendError(Text.literal("玩家不在线"));
            return 0;
        }

        String tag = StringArgumentType.getString(ctx, "tag");
        String error = TagService.validate(tag);
        if (error != null) {
            source.sendError(Text.literal(error));
            return 0;
        }

        boolean added = TagService.addTag(target, tag);
        if (added) {
            source.sendFeedback(
                    () -> Text.literal("已为 " + target.getGameProfile().getName() + " 添加 tag: " + tag)
                            .formatted(Formatting.GREEN),
                    true
            );
        } else {
            source.sendFeedback(
                    () -> Text.literal(target.getGameProfile().getName() + " 已拥有 tag: " + tag)
                            .formatted(Formatting.YELLOW),
                    false
            );
        }
        return 1;
    }

    private static int removeTag(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        if (!PermissionUtil.canEdit(source)) {
            source.sendError(Text.literal("你没有权限编辑 Tags"));
            return 0;
        }

        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        if (target == null) {
            source.sendError(Text.literal("玩家不在线"));
            return 0;
        }

        String tag = StringArgumentType.getString(ctx, "tag");
        String error = TagService.validate(tag);
        if (error != null) {
            source.sendError(Text.literal(error));
            return 0;
        }

        boolean removed = TagService.removeTag(target, tag);
        if (removed) {
            source.sendFeedback(
                    () -> Text.literal("已从 " + target.getGameProfile().getName() + " 移除 tag: " + tag)
                            .formatted(Formatting.GREEN),
                    true
            );
        } else {
            source.sendFeedback(
                    () -> Text.literal(target.getGameProfile().getName() + " 没有 tag: " + tag)
                            .formatted(Formatting.YELLOW),
                    false
            );
        }
        return 1;
    }

    private static int toggleTag(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException {
        ServerCommandSource source = ctx.getSource();
        if (!PermissionUtil.canEdit(source)) {
            source.sendError(Text.literal("你没有权限编辑 Tags"));
            return 0;
        }

        ServerPlayerEntity target = EntityArgumentType.getPlayer(ctx, "player");
        if (target == null) {
            source.sendError(Text.literal("玩家不在线"));
            return 0;
        }

        String tag = StringArgumentType.getString(ctx, "tag");
        String error = TagService.validate(tag);
        if (error != null) {
            source.sendError(Text.literal(error));
            return 0;
        }

        boolean added = TagService.toggleTag(target, tag);
        if (added) {
            source.sendFeedback(
                    () -> Text.literal("已为 " + target.getGameProfile().getName() + " 添加 tag: " + tag)
                            .formatted(Formatting.GREEN),
                    true
            );
        } else {
            source.sendFeedback(
                    () -> Text.literal("已移除 " + target.getGameProfile().getName() + " 的 tag: " + tag)
                            .formatted(Formatting.YELLOW),
                    true
            );
        }
        return 1;
    }

    /**
     * Suggest preset tags for the tag argument.
     */
    private static CompletableFuture<Suggestions> suggestTags(
            CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();

        // Suggest from preset tags
        for (String tag : ConfigManager.getConfig().presetTags) {
            if (tag.toLowerCase().startsWith(remaining)) {
                builder.suggest(tag);
            }
        }

        return builder.buildFuture();
    }

    /**
     * Suggest tags that the target player already has (for remove command).
     */
    private static CompletableFuture<Suggestions> suggestPlayerTags(
            CommandContext<ServerCommandSource> ctx, SuggestionsBuilder builder) {
        String remaining = builder.getRemaining().toLowerCase();

        try {
            ServerPlayerEntity target = ctx.getArgument("player", ServerPlayerEntity.class);
            if (target != null) {
                for (String tag : target.getCommandTags()) {
                    if (tag.toLowerCase().startsWith(remaining)) {
                        builder.suggest(tag);
                    }
                }
            }
        } catch (Exception ignored) {
        }

        // Also suggest presets
        for (String tag : ConfigManager.getConfig().presetTags) {
            if (tag.toLowerCase().startsWith(remaining) && !builder.getRemaining().isEmpty()) {
                // Only add presets as fallback suggestions
            }
        }

        return builder.buildFuture();
    }
}
