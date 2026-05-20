package com.lantern.onlineplayertageditor.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnlinePlayerTagEditorClient implements ClientModInitializer {
    public static final String MOD_ID = "online_player_tag_editor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID + "-client");

    @Override
    public void onInitializeClient() {
        LOGGER.info("Online Player Tag Editor client initialized");
    }
}
