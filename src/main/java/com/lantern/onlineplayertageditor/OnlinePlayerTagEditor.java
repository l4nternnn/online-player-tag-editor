package com.lantern.onlineplayertageditor;

import com.lantern.onlineplayertageditor.command.PlayerTagsCommand;
import com.lantern.onlineplayertageditor.config.ConfigManager;
import com.lantern.onlineplayertageditor.network.EditorNetworking;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OnlinePlayerTagEditor implements ModInitializer {
    public static final String MOD_ID = "online_player_tag_editor";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Online Player Tag Editor initializing...");

        ConfigManager.load();
        EditorNetworking.registerPayloadTypes();
        EditorNetworking.registerServerReceivers();
        PlayerTagsCommand.register();

        LOGGER.info("Online Player Tag Editor initialized successfully");
    }
}
