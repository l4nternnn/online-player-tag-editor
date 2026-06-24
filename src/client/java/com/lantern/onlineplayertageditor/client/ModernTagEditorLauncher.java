package com.lantern.onlineplayertageditor.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ModernTagEditorLauncher {
    private ModernTagEditorLauncher() {
    }

    public static void open(String json) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!FabricLoader.getInstance().isModLoaded("modernui")) {
            if (client.player != null) {
                client.player.sendMessage(Text.literal("请在客户端安装 Modern UI 3.12.0.4 后再打开 Tag 管理台。")
                        .formatted(Formatting.RED), false);
            }
            return;
        }
        ModernTagEditorScreen.open(json);
    }

    public static void showNotice(String message, boolean error) {
        ModernTagEditorScreen.showNotice(message, error);
    }
}
