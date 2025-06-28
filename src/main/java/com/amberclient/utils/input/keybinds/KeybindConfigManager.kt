package com.amberclient.utils.input.keybinds

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.lang.reflect.Type

object KeybindConfigManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val configDir = File(FabricLoader.getInstance().configDir.toFile(), "amberclient")
    private val keybindConfigFile = File(configDir, "keybinds.json")

    data class KeybindConfig(
        val moduleKeybinds: MutableMap<String, String> = mutableMapOf(), // moduleName -> keyName
        val customKeybinds: MutableMap<String, CustomKeybindData> = mutableMapOf() // keyCode -> action data
    )

    data class CustomKeybindData(
        val actionId: String,
        val description: String,
        val requiresPlayer: Boolean = true
    )

    private var config = KeybindConfig()

    init {
        ensureConfigDirectory()
    }

    private fun ensureConfigDirectory() {
        if (!configDir.exists()) {
            configDir.mkdirs()
            println("[KeybindConfigManager] Created config directory: ${configDir.absolutePath}")
        }
    }

    fun loadConfig(): KeybindConfig {
        return try {
            if (keybindConfigFile.exists()) {
                FileReader(keybindConfigFile).use { reader ->
                    val type: Type = object : TypeToken<KeybindConfig>() {}.type
                    config = gson.fromJson(reader, type) ?: KeybindConfig()
                }
                println("[KeybindConfigManager] Loaded ${config.moduleKeybinds.size} module keybinds and ${config.customKeybinds.size} custom keybinds")
            } else {
                config = KeybindConfig()
                println("[KeybindConfigManager] No existing config found, using defaults")
            }
            config
        } catch (e: Exception) {
            println("[KeybindConfigManager] Error loading keybind config: ${e.message}")
            e.printStackTrace()
            KeybindConfig().also { config = it }
        }
    }

    fun saveConfig() {
        try {
            ensureConfigDirectory()
            FileWriter(keybindConfigFile).use { writer ->
                gson.toJson(config, writer)
            }
            println("[KeybindConfigManager] Saved ${config.moduleKeybinds.size} module keybinds and ${config.customKeybinds.size} custom keybinds")
        } catch (e: Exception) {
            println("[KeybindConfigManager] Error saving keybind config: ${e.message}")
            e.printStackTrace()
        }
    }

    fun setModuleKeybind(moduleName: String, keyName: String) {
        config.moduleKeybinds[moduleName] = keyName
        saveConfig()
    }

    fun getModuleKeybind(moduleName: String): String? {
        return config.moduleKeybinds[moduleName]
    }

    fun removeModuleKeybind(moduleName: String) {
        config.moduleKeybinds.remove(moduleName)
        saveConfig()
    }

    fun setCustomKeybind(keyCode: String, actionId: String, description: String, requiresPlayer: Boolean = true) {
        config.customKeybinds[keyCode] = CustomKeybindData(actionId, description, requiresPlayer)
        saveConfig()
    }

    fun getCustomKeybind(keyCode: String): CustomKeybindData? {
        return config.customKeybinds[keyCode]
    }

    fun removeCustomKeybind(keyCode: String) {
        config.customKeybinds.remove(keyCode)
        saveConfig()
    }

    fun getAllModuleKeybinds(): Map<String, String> {
        return config.moduleKeybinds.toMap()
    }

    fun getAllCustomKeybinds(): Map<String, CustomKeybindData> {
        return config.customKeybinds.toMap()
    }

    fun clearAllKeybinds() {
        config.moduleKeybinds.clear()
        config.customKeybinds.clear()
        saveConfig()
    }
}