package com.amberclient.modules.combat

import com.amberclient.events.core.EventManager
import com.amberclient.events.player.PreMotionListener
import com.amberclient.events.player.PostMotionListener
import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleSettings
import com.amberclient.utils.minecraft.rotation.RotationFaker
import net.minecraft.text.Text
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager

class Hitbox : Module("Hitbox", "(DETECTABLE) Increases hitboxes' size", "Combat"), ConfigurableModule {

    companion object {
        const val MOD_ID = "amberclient-hitbox"
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)

        @JvmStatic
        var isHitboxModuleEnabled = false

        private var calculatingTarget = false
        private var instance: Hitbox? = null

        @JvmStatic
        fun securityReset() {
            isHitboxModuleEnabled = false
            calculatingTarget = false
        }

        @JvmStatic
        fun isCalculatingTarget(): Boolean = calculatingTarget

        @JvmStatic
        fun setCalculatingTarget(state: Boolean) {
            calculatingTarget = state
        }

        @JvmStatic
        fun getInstance(): Hitbox? = instance
    }

    private val expandX = ModuleSettings("Expand X", "Horizontal hitbox expansion", 0.25, 0.0, 2.0, 0.05)
    private val expandYUp = ModuleSettings("Expand Y Up", "Upward hitbox expansion", 0.6, 0.0, 2.0, 0.05)
    private val expandZ = ModuleSettings("Expand Z", "Depth hitbox expansion", 0.25, 0.0, 2.0, 0.05)

    private val rotationFaker = RotationFaker()

    private val settings = mutableListOf<ModuleSettings>().apply {
        add(expandX)
        add(expandYUp)
        add(expandZ)
    }

    init {
        instance = this
    }

    override fun onEnable() {
        isHitboxModuleEnabled = true
        with(EventManager.getInstance()) {
            add(PreMotionListener::class.java, rotationFaker)
            add(PostMotionListener::class.java, rotationFaker)
        }
    }

    override fun onDisable() {
        isHitboxModuleEnabled = false
        with(EventManager.getInstance()) {
            remove(PreMotionListener::class.java, rotationFaker)
            remove(PostMotionListener::class.java, rotationFaker)
        }
    }

    override fun onTick() {
        if (!isHitboxModuleEnabled) return
    }

    override fun getSettings(): List<ModuleSettings> = settings

    override fun onSettingChanged(setting: ModuleSettings) {
        if (setting in listOf(expandX, expandYUp, expandZ)) {
            client.player?.let { player ->
                player.sendMessage(
                    Text.literal(
                        "§6Hitbox expansion updated: X=${expandX.doubleValue}" +
                                ", YUp=${expandYUp.doubleValue}" +
                                ", Z=${expandZ.doubleValue}"
                    ),
                    true
                )
            }
        }
    }

    fun getExpandX(): Double = expandX.doubleValue
    fun getExpandYUp(): Double = expandYUp.doubleValue
    fun getExpandZ(): Double = expandZ.doubleValue

    fun getRotationFaker(): RotationFaker = rotationFaker
}