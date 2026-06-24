package com.lantern.onlineplayertageditor.client;

import com.lantern.onlineplayertageditor.network.EditorPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class EditorClientNetworking {
    private EditorClientNetworking() {
    }

    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(EditorPayloads.OpenEditorS2C.ID, (payload, context) ->
                context.client().execute(() -> ModernTagEditorLauncher.open(payload.json())));

        ClientPlayNetworking.registerGlobalReceiver(EditorPayloads.EditorNoticeS2C.ID, (payload, context) ->
                context.client().execute(() -> {
                    ModernTagEditorLauncher.showNotice(payload.message(), payload.error());
                    if (context.player() != null) {
                        context.player().sendMessage(
                                Text.literal(payload.message()).formatted(payload.error() ? Formatting.RED : Formatting.GREEN),
                                false
                        );
                    }
                }));
    }
}
