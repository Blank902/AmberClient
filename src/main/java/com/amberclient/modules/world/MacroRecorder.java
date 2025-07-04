package com.amberclient.modules.world;

import com.amberclient.utils.module.Module;
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
        //TODO: Should opens the Macro Recorder GUI when activated
    }

    @Override
    public void onDisable() {
        //TODO: Module should be disabled whenever the GUI opens
    }
}
