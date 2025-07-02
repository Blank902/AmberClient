package com.amberclient.modules.miscellaneous

import com.amberclient.utils.module.Module
import com.amberclient.utils.discord.DiscordManager
import net.minecraft.text.Text

class DiscordRPC : Module("DiscordRPC", "Enable/disable Discord Rich Presence", "Miscellaneous") {
    init {
        enabled = true
    }

    override fun onDisable() {
        val discordManager = DiscordManager.getInstance()

        if (discordManager.isInitialized) {
            discordManager.shutdown()
            client.player?.sendMessage(
                Text.literal("§cDiscord RPC §7has been §cdisabled"),
                true
            )
        }
    }
}
