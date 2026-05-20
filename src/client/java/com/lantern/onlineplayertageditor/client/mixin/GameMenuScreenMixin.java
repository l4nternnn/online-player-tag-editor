package com.lantern.onlineplayertageditor.client.mixin;

import com.lantern.onlineplayertageditor.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Drawable;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin {

    @Shadow
    protected abstract <T extends Element & Drawable & Selectable> T addDrawableChild(T drawableElement);

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        GameMenuScreen self = (GameMenuScreen) (Object) this;

        String buttonText = "Tag 管理";
        try {
            buttonText = ConfigManager.getConfig().escButtonText;
        } catch (Exception ignored) {
        }

        String finalButtonText = buttonText;
        this.addDrawableChild(ButtonWidget.builder(
                Text.literal(finalButtonText),
                button -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.player == null) {
                        if (client.world == null) {
                            return;
                        }
                        return;
                    }
                    if (client.player.networkHandler == null) {
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
        ).dimensions(self.width / 2 - 100, self.height - 30, 200, 20).build());
    }
}
