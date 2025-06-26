package com.amberclient.modules.render.xray

import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.ModuleSettings
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.text.Text
import net.minecraft.util.math.ChunkPos
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.LogManager

class Xray : Module("XRay", "Shows the outlines of the selected ores in the chunk", "Render"), ConfigurableModule {

    companion object {
        const val MOD_ID = "amberclient-xray"
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)
    }

    private var lastPlayerChunk: ChunkPos? = null
    private var wasActive = false

    private val chunkRadius: ModuleSettings
    private val exposedOnly: ModuleSettings
    private val showLava: ModuleSettings
    private val settings: List<ModuleSettings>

    init {
        // Settings
        chunkRadius = ModuleSettings("Chunk Radius", "Number of chunks to scan around the player", 1.0, 1.0, 8.0, 1.0)
        exposedOnly = ModuleSettings("Exposed Only", "Show only ores exposed to air", false)
        showLava = ModuleSettings("Show Lava", "Show lava with the ores", false)

        settings = listOf(chunkRadius, exposedOnly, showLava)

        WorldRenderEvents.AFTER_TRANSLUCENT.register(RenderOutlines::render)
    }

    override fun onEnable() {
        val settingsStore = SettingsStore.getInstance().get()
        settingsStore.isActive = true
        settingsStore.isExposedOnly = exposedOnly.booleanValue
        settingsStore.isShowLava = showLava.booleanValue
        settingsStore.halfRange = chunkRadius.doubleValue.toInt()

        ScanTask.resetLocationTracking()

        val client = MinecraftClient.getInstance()
        client.player?.let { player ->
            ScanTask.runTask(player.chunkPos, settingsStore.halfRange, true)
            lastPlayerChunk = null
        }
        wasActive = true
    }

    override fun onDisable() {
        SettingsStore.getInstance().get().isActive = false
        ScanTask.renderQueue.clear()
        lastPlayerChunk = null
        wasActive = false
    }

    override fun onTick() {
        val settingsStore = SettingsStore.getInstance().get()
        if (settingsStore.isActive) {
            val client = MinecraftClient.getInstance()
            val player = client.player ?: return

            val currentChunk = player.chunkPos
            if (currentChunk != lastPlayerChunk || !wasActive) {
                ScanTask.runTask(currentChunk, settingsStore.halfRange)
                lastPlayerChunk = currentChunk
                wasActive = true
            }
        }
    }

    override fun getSettings(): List<ModuleSettings> = settings

    override fun onSettingChanged(setting: ModuleSettings) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return
        val settingsStore = SettingsStore.getInstance().get()

        when (setting) {
            exposedOnly -> {
                settingsStore.isExposedOnly = exposedOnly.booleanValue
                player.sendMessage(
                    Text.literal("§6Exposed Only: §l${if (exposedOnly.booleanValue) "ON" else "OFF"}"),
                    true
                )
                if (settingsStore.isActive) {
                    ScanTask.runTask(player.chunkPos, settingsStore.halfRange, true)
                }
            }
            chunkRadius -> {
                settingsStore.halfRange = chunkRadius.doubleValue.toInt()
                player.sendMessage(
                    Text.literal("§6Chunk Range: §l${chunkRadius.doubleValue.toInt()} chunks"),
                    true
                )
                if (settingsStore.isActive) {
                    ScanTask.runTask(player.chunkPos, settingsStore.halfRange, true)
                }
            }
            showLava -> {
                settingsStore.isShowLava = showLava.booleanValue
                player.sendMessage(
                    Text.literal("§6Show Lava: §l${if (showLava.booleanValue) "ON" else "OFF"}"),
                    true
                )
                if (settingsStore.isActive) {
                    ScanTask.runTask(player.chunkPos, settingsStore.halfRange, true)
                }
            }
        }
    }
}