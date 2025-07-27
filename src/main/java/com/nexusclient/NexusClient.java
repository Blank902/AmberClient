package com.nexusclient;

import com.nexusclient.modules.world.MacroRecorder.MacroRecordingSystem;
import com.nexusclient.screens.HudRenderer;
import com.nexusclient.screens.NexusClickGUI;
import com.nexusclient.commands.NexusCommand;
import com.nexusclient.utils.input.keybinds.KeybindsManager;
import com.nexusclient.utils.module.ModuleManager;
import com.nexusclient.utils.module.EnhancedModuleManager;
import com.nexusclient.utils.features.murdererfinder.config.ConfigManager;
import com.nexusclient.utils.discord.DiscordManager;
import com.nexusclient.ui.theme.ThemeManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.IdentifiedLayer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NexusClient implements ModInitializer {
    public static final String MOD_ID = "nexusclient";
    public static final String MOD_VERSION = "v1.0.0";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static boolean hudLayerRegistered = false;
    private int discordUpdateTicker = 0;

    @Override
    public void onInitialize() {
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        if (!hudLayerRegistered) {
            HudLayerRegistrationCallback.EVENT.register(layeredDrawer -> {
                layeredDrawer.attachLayerAfter(
                        IdentifiedLayer.EXPERIENCE_LEVEL,
                        Identifier.of(MOD_ID, "hud_overlay"),
                        (context, tickCounter) -> {
                            HudRenderer hudRenderer = new HudRenderer();
                            hudRenderer.onHudRender(context, tickCounter);
                        }
                );
            });
            hudLayerRegistered = true;
        }

        NexusCommand.register();

        KeybindsManager.INSTANCE.initialize();
        ConfigManager.init();
        
        // Initialize theme system
        ThemeManager.getInstance();

        // Initialize enhanced module manager
        EnhancedModuleManager.getInstance().initializeKeybinds();

        DiscordManager.getInstance().initialize();

        LOGGER.info("Nexus Client started! Version: " + MOD_VERSION);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DiscordManager.getInstance().shutdown();
            EnhancedModuleManager.getInstance().shutdown();
        }));
    }


    private void onClientTick(MinecraftClient client) {
        if (KeybindsManager.INSTANCE.getOpenClickGui().wasPressed() && client.currentScreen == null)
            client.setScreen(new NexusClickGUI());

        // Use enhanced module manager for better performance and error handling
        EnhancedModuleManager.getInstance().onTick();
        EnhancedModuleManager.getInstance().handleKeyInputs();

        MacroRecordingSystem.getInstance().tick();

        DiscordManager discordManager = DiscordManager.getInstance();
        if (discordManager.isInitialized()) {
            discordManager.runCallbacks();

            discordUpdateTicker++;
            if (discordUpdateTicker >= 100) {
                discordManager.updatePresence();
                discordUpdateTicker = 0;
            }
        }
    }
}