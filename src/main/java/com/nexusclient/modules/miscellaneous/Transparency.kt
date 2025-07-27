package com.amberclient.modules.miscellaneous;

import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleCategory
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class Transparency : Module("Transparency", "Make Click GUI Transparent", ModuleCategory.MISC) {

    companion object {
        const val MOD_ID = "amberclient-transparency"
        val LOGGER: Logger? = LogManager.getLogger(MOD_ID)
    }

    init { enabled = true }

    fun getTransparencyLevel(): Float {
        return if (isEnabled()) 0.0f else 0.9f
    }
}