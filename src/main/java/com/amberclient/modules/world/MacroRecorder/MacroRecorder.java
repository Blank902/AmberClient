package com.amberclient.modules.world.MacroRecorder;

import com.amberclient.screens.MacroRecorderGUI;
import com.amberclient.utils.module.Module;
import com.amberclient.utils.module.ModuleCategory;
import net.minecraft.client.MinecraftClient;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MacroRecorder extends Module {
    public static final String MOD_ID = "amberclient-macrorecorder";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private final MacroRecordingSystem recordingSystem;

    public MacroRecorder() {
        super("MacroRecorder", "Record and play macros with a easy-to-use GUI!", ModuleCategory.WORLD);
        this.recordingSystem = MacroRecordingSystem.getInstance();
        MacrosManager persistenceManager = new MacrosManager();

        try {
            persistenceManager.cleanupOrphanedFiles();
        } catch (Exception e) {
            LOGGER.warn("Failed to cleanup orphaned files at startup", e);
        }
    }

    @Override
    public void onEnable() {
        MinecraftClient client = getClient();
        client.setScreen(new MacroRecorderGUI());
        this.enabled = false;
    }

    @Override
    public void onDisable() {
        if (recordingSystem.isRecording()) {
            recordingSystem.stopRecording();
            LOGGER.info("Stopped macro recording due to module disable");
        }
        LOGGER.info("MacroRecorder module disabled");
    }

    public boolean isRecording() {
        return recordingSystem.isRecording();
    }

    public int getRecordedActionCount() {
        return recordingSystem.getActionCount();
    }
}