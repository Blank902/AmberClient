package com.amberclient.utils.discord;

import com.amberclient.AmberClient;
import com.amberclient.utils.discord.callbacks.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class DiscordManager {
    private static DiscordManager instance;
    private boolean initialized = false;
    private static final String APPLICATION_ID;

    private DiscordManager() {}

    public static DiscordManager getInstance() {
        if (instance == null) {
            instance = new DiscordManager();
        }
        return instance;
    }

    static {
        String applicationId = null;
        Properties props = new Properties();
        try (InputStream input = DiscordManager.class.getResourceAsStream("/discord.properties")) {
            if (input != null) {
                props.load(input);
                applicationId = props.getProperty("application_id");
                if (applicationId == null) {
                    AmberClient.LOGGER.error("application_id not found in discord.properties");
                }
            } else {
                AmberClient.LOGGER.error("discord.properties not found");
            }
        } catch (IOException e) {
            AmberClient.LOGGER.error("Error loading discord.properties", e);
        }
        APPLICATION_ID = applicationId;
    }

    public void initialize() {
        if (initialized) return;

        if (APPLICATION_ID == null) {
            AmberClient.LOGGER.error("Cannot initialize Discord RPC: application ID not found");
            return;
        }

        try {
            DiscordEventHandlers handlers = new DiscordEventHandlers();

            handlers.ready = (user) -> {
                AmberClient.LOGGER.info("Discord RPC connected for the user: {}", user.username);
            };

            handlers.errored = (errorCode, message) -> {
                AmberClient.LOGGER.error("Discord RPC error: {} - {}", errorCode, message);
            };

            handlers.disconnected = (errorCode, message) -> {
                AmberClient.LOGGER.warn("Discord RPC disconnected: {} - {}", errorCode, message);
            };

            DiscordRPC.INSTANCE.Discord_Initialize(APPLICATION_ID, handlers, true, null);
            initialized = true;
            AmberClient.LOGGER.info("Discord RPC successfully initialized!");

            updatePresence();

        } catch (Exception e) {
            AmberClient.LOGGER.error("Discord RPC initialization failure: {}", e.getMessage());
        }
    }

    public void updatePresence() {
        if (!initialized) return;

        try {
            DiscordRichPresence presence = new DiscordRichPresence();
            MinecraftClient client = MinecraftClient.getInstance();

            presence.largeImageKey = "amber";
            presence.largeImageText = "AmberClient " + AmberClient.MOD_VERSION;
            presence.startTimestamp = System.currentTimeMillis() / 1000;

            if (client.world != null) {
                if (client.isInSingleplayer()) {
                    presence.state = "On a solo world";
                    presence.smallImageKey = "singleplayer";
                    presence.smallImageText = "On a solo world";
                } else {
                    ServerInfo serverInfo = client.getCurrentServerEntry();
                    if (serverInfo != null) {
                        presence.state = "In multiplayer - " + serverInfo.name;
                        presence.smallImageKey = "multiplayer";
                        presence.smallImageText = "On a server";
                    } else {
                        presence.state = "In multiplayer";
                        presence.smallImageKey = "multiplayer";
                        presence.smallImageText = "multiplayer";
                    }
                }
            } else {
                presence.state = "In the menus";
                presence.smallImageKey = "menu";
                presence.smallImageText = "Main Menu";
            }

            presence.button_label_1 = "Download AmberClient";
            presence.button_url_1 = "https://github.com/gqdThinky/AmberClient";

            DiscordRPC.INSTANCE.Discord_UpdatePresence(presence);

        } catch (Exception e) {
            AmberClient.LOGGER.error("Error updating Discord presence: {}", e.getMessage());
        }
    }

    public void runCallbacks() {
        if (initialized) {
            try {
                DiscordRPC.INSTANCE.Discord_RunCallbacks();
            } catch (Exception e) {
                AmberClient.LOGGER.error("Error executing Discord callbacks: {}", e.getMessage());
            }
        }
    }

    public void shutdown() {
        if (initialized) {
            try {
                DiscordRPC.INSTANCE.Discord_ClearPresence();
                DiscordRPC.INSTANCE.Discord_Shutdown();
                initialized = false;
                AmberClient.LOGGER.info("Discord RPC closed properly");
            } catch (Exception e) {
                AmberClient.LOGGER.error("Error closing Discord RPC: {}", e.getMessage());
            }
        }
    }

    public boolean isInitialized() {
        return initialized;
    }
}