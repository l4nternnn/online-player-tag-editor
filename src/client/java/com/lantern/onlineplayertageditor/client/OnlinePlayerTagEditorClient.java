package com.lantern.onlineplayertageditor.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnlinePlayerTagEditorClient implements ClientModInitializer {
    public static final String MOD_ID = "online_player_tag_editor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID + "-client");

    private static KeyBinding openTagEditorKey;

    @Override
    public void onInitializeClient() {
        LOGGER.info("Online Player Tag Editor client initialized");

        openTagEditorKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.online_player_tag_editor.open",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_EQUAL,
                "category.online_player_tag_editor"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openTagEditorKey.wasPressed()) {
                if (client.player != null && client.player.networkHandler != null) {
                    client.player.networkHandler.sendChatCommand("playertags");
                } else if (client.player != null) {
                    client.player.sendMessage(
                            Text.literal("当前服务器可能未安装 Online Player Tag Editor")
                                    .formatted(Formatting.RED),
                            false
                    );
                }
            }
        });
    }
}
