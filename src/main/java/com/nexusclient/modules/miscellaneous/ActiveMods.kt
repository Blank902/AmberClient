package com.amberclient.modules.miscellaneous

import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.ModuleCategory
import com.amberclient.utils.module.ModuleSettings
import net.minecraft.text.Text

class ActiveMods : Module("ActiveMods", "Toggles display of active modules", ModuleCategory.MISC), ConfigurableModule {

    private val enableRGB: ModuleSettings
    private val settings: MutableList<ModuleSettings>

    init {
        enabled = true

        enableRGB = ModuleSettings("Enable RGB", "Use animated RGB text color", true)

        settings = mutableListOf<ModuleSettings>().apply {
            add(enableRGB)
        }
    }

    fun isRGBEnabled(): Boolean = enableRGB.getBooleanValue()

    private fun updateRGBSetting() {
        client.player?.sendMessage(
            Text.literal("§6RGB enabled: §l${isRGBEnabled()}"),
            true
        )
    }

    override fun getSettings(): List<ModuleSettings> = settings

    override fun onSettingChanged(setting: ModuleSettings) {
        if (setting == enableRGB) {
            updateRGBSetting()
        }
    }
}