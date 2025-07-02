package com.amberclient.modules.misc

import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.ModuleSettings
import com.amberclient.utils.discord.DiscordManager
import net.minecraft.text.Text

class DiscordRPC : Module("DiscordRPC", "Enable/disable Discord Rich Presence", "Miscellaneous"), ConfigurableModule {

    private val autoStart: ModuleSettings
    private val settings: MutableList<ModuleSettings>

    init {
        enabled = true

        autoStart = ModuleSettings("Auto Start", "Automatically start Discord RPC on game launch", true)

        settings = mutableListOf<ModuleSettings>().apply {
            add(autoStart)
        }
    }

    fun isAutoStartEnabled(): Boolean = autoStart.getBooleanValue()

    private fun updateAutoStartSetting() {
        client.player?.sendMessage(
            Text.literal("§6Auto Start enabled: §l${isAutoStartEnabled()}"),
            true
        )
    }

    override fun getSettings(): List<ModuleSettings> = settings

    override fun onSettingChanged(setting: ModuleSettings) {
        if (setting == autoStart) {
            updateAutoStartSetting()
        }
    }
}