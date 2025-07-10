package com.amberclient.modules.player

import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleSettings
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class AntiHunger : Module("AntiHunger", "Reduces hunger loss", "Player"), ConfigurableModule {

    companion object {
        const val MOD_ID = "amberclient-antihunger"
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)

        // Variables statiques pour les mixins - now properly accessible from Java
        @JvmStatic
        var hungerReductionFactor: Double = 0.5
        @JvmStatic
        var saturationReductionFactor: Double = 0.5
        @JvmStatic
        var moduleEnabled: Boolean = false  // Renamed to avoid conflict
        @JvmStatic
        var showNotifications: Boolean = true
        @JvmStatic
        var lastNotificationTime: Long = 0

        // Renamed method to avoid conflict with inherited isEnabled()
        @JvmStatic
        fun isModuleEnabled(): Boolean = moduleEnabled

        // Méthode statique pour les notifications depuis le mixin
        @JvmStatic
        fun showHungerReductionNotification(originalLoss: Float, reducedAmount: Float) {
            val currentTime = System.currentTimeMillis()

            // Limiter les notifications à une par seconde
            if (currentTime - lastNotificationTime < 1000) return

            val client = MinecraftClient.getInstance()
            val player = client.player ?: return

            if (showNotifications && reducedAmount > 0.01f) {
                player.sendMessage(
                    Text.literal("§6AntiHunger: Reduced hunger loss by ${String.format("%.1f", reducedAmount)} (was ${String.format("%.1f", originalLoss)})"),
                    true
                )
            }

            lastNotificationTime = currentTime
        }
    }

    private val hungerReduction: ModuleSettings
    private val saturationReduction: ModuleSettings
    private val showNotificationsSettings: ModuleSettings
    private val settings: MutableList<ModuleSettings>

    init {
        hungerReduction = ModuleSettings(
            "Hunger Reduction",
            "Percentage of hunger loss to reduce (0-100%)",
            50.0, 0.0, 100.0, 5.0
        )
        saturationReduction = ModuleSettings(
            "Saturation Reduction",
            "Percentage of saturation loss to reduce (0-100%)",
            50.0, 0.0, 100.0, 5.0
        )
        showNotificationsSettings = ModuleSettings(
            "Show Notifications",
            "Show hunger reduction notifications in chat",
            true
        )

        settings = mutableListOf<ModuleSettings>().apply {
            add(hungerReduction)
            add(saturationReduction)
            add(showNotificationsSettings)
        }
    }

    override fun onEnable() {
        super.onEnable()
        moduleEnabled = true  // Update static variable
    }

    override fun onDisable() {
        super.onDisable()
        moduleEnabled = false  // Update static variable
    }

    override fun onTick() {
        hungerReductionFactor = hungerReduction.doubleValue / 100.0
        saturationReductionFactor = saturationReduction.doubleValue / 100.0
        showNotifications = showNotificationsSettings.booleanValue
    }

    override fun getSettings(): List<ModuleSettings> = settings

    override fun onSettingChanged(setting: ModuleSettings) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return

        // Mettre à jour les variables statiques immédiatement
        hungerReductionFactor = hungerReduction.doubleValue / 100.0
        saturationReductionFactor = saturationReduction.doubleValue / 100.0
        showNotifications = showNotificationsSettings.booleanValue

        when (setting) {
            hungerReduction -> {
                if (showNotifications) {
                    player.sendMessage(
                        Text.literal("§6AntiHunger: Hunger reduction set to ${hungerReduction.doubleValue.toInt()}%"),
                        true
                    )
                }
                LOGGER.info("AntiHunger: Hunger reduction updated to {}%", hungerReduction.doubleValue)
            }
            saturationReduction -> {
                if (showNotifications) {
                    player.sendMessage(
                        Text.literal("§6AntiHunger: Saturation reduction set to ${saturationReduction.doubleValue.toInt()}%"),
                        true
                    )
                }
                LOGGER.info("AntiHunger: Saturation reduction updated to {}%", saturationReduction.doubleValue)
            }
            showNotificationsSettings -> {
                val status = if (showNotifications) "enabled" else "disabled"
                player.sendMessage(
                    Text.literal("§6AntiHunger: Notifications $status"),
                    true
                )
                LOGGER.info("AntiHunger: Notifications {}", status)
            }
        }
    }

    // Méthodes utilitaires pour obtenir des informations sur l'état actuel
    fun getHungerReductionPercentage(): Double = hungerReduction.doubleValue
    fun getSaturationReductionPercentage(): Double = saturationReduction.doubleValue
    fun areNotificationsEnabled(): Boolean = showNotificationsSettings.booleanValue

    // Méthode pour obtenir des statistiques
    fun getHungerStats(): Pair<Int, Float> {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return Pair(0, 0.0f)
        val hungerManager = player.hungerManager
        return Pair(hungerManager.foodLevel, hungerManager.saturationLevel)
    }
}