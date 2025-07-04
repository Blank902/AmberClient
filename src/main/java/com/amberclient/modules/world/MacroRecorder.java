package com.amberclient.modules.world;

import com.amberclient.screens.MacroRecorderGUI;
import com.amberclient.utils.module.Module;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MacroRecorder extends Module {
    public static final String MOD_ID = "amberclient-macrorecorder";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public MacroRecorder() {
        super("MacroRecorder", "X", "World");
    }

    @Override
    public void onEnable() {
        MinecraftClient client = getClient();
        if (client != null) {
            client.setScreen(new MacroRecorderGUI());
            this.enabled = false;
        } else {
            LOGGER.warn("Unable to open MacroRecorderGUI: Minecraft client not available");
        }
    }

    @Override
    public void onDisable() {
        LOGGER.info("MacroRecorder module disabled");
    }
}