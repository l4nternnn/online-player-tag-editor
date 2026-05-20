package com.lantern.onlineplayertageditor.client;

import com.lantern.onlineplayertageditor.config.ConfigManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class OnlinePlayerTagEditorClient implements ClientModInitializer {
    public static final String MOD_ID = "online_player_tag_editor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID + "-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Online Player Tag Editor client initialized");

        ScreenEvents.AFTER_INIT.register(this::onScreenInit);
    }

    private void onScreenInit(MinecraftClient client, Screen screen, int scaledWidth, int scaledHeight) {
        if (!(screen instanceof GameMenuScreen)) return;

        String buttonText = "Tag 管理";
        try {
            buttonText = ConfigManager.getConfig().escButtonText;
        } catch (Exception ignored) {
        }

        String finalButtonText = buttonText;
        ButtonWidget button = ButtonWidget.builder(
                Text.literal(finalButtonText),
                btn -> {
                    if (client.player == null || client.player.networkHandler == null) {
                        if (client.player != null) {
                            client.player.sendMessage(
                                    Text.literal("当前服务器可能未安装 Online Player Tag Editor")
                                            .formatted(Formatting.RED),
                                    false
                            );
                        }
                        return;
                    }
                    client.player.networkHandler.sendChatCommand("playertags");
                }
        ).dimensions(screen.width / 2 - 100, screen.height - 30, 200, 20).build();

        addDrawableChild(screen, button);
    }

    private void addDrawableChild(Screen screen, Object child) {
        for (Method m : Screen.class.getDeclaredMethods()) {
            if (m.getParameterCount() != 1) continue;
            Class<?> paramType = m.getParameterTypes()[0];
            if (Element.class.isAssignableFrom(paramType)
                    && Drawable.class.isAssignableFrom(paramType)
                    && Selectable.class.isAssignableFrom(paramType)) {
                try {
                    m.setAccessible(true);
                    m.invoke(screen, child);
                    return;
                } catch (Exception e) {
                    LOGGER.warn("Failed to add ESC button: {}", e.toString());
                }
            }
        }
    }
}
