package com.amberclient.utils.input.keybinds

import com.amberclient.utils.input.keybinds.KeybindConfigManager
import net.minecraft.client.MinecraftClient
import org.lwjgl.glfw.GLFW
import java.util.concurrent.ConcurrentHashMap

object CustomKeybindManager {
    private val keyBindings = ConcurrentHashMap<Int, MutableList<KeybindAction>>()
    private val keyStates = ConcurrentHashMap<Int, Boolean>()
    private val actionCallbacks = ConcurrentHashMap<String, Runnable>()
    private var isInitialized = false

    data class KeybindAction(
        val id: String,
        val description: String,
        val callback: Runnable,
        val requiresPlayer: Boolean = true
    )

    fun initialize() {
        if (isInitialized) return
        isInitialized = true

        KeybindConfigManager.loadConfig()
        loadKeybindsFromConfig()

        println("[CustomKeybindManager] Initialized custom keybind system")
    }

    private fun loadKeybindsFromConfig() {
        val savedKeybinds = KeybindConfigManager.getAllCustomKeybinds()

        savedKeybinds.forEach { (keyCodeStr, keybindData) ->
            try {
                val keyCode = keyCodeStr.toInt()

                val callback = actionCallbacks[keybindData.actionId]
                if (callback != null) {
                    val action = KeybindAction(
                        keybindData.actionId,
                        keybindData.description,
                        callback,
                        keybindData.requiresPlayer
                    )

                    keyBindings.computeIfAbsent(keyCode) { mutableListOf() }.add(action)
                    keyStates[keyCode] = false

                    println("[CustomKeybindManager] Restored keybind: ${getKeyName(keyCode)} -> ${keybindData.description}")
                } else {
                    println("[CustomKeybindManager] Warning: No callback found for action ${keybindData.actionId}")
                }
            } catch (e: Exception) {
                println("[CustomKeybindManager] Error loading keybind $keyCodeStr: ${e.message}")
            }
        }
    }

    fun registerActionCallback(actionId: String, callback: Runnable) {
        actionCallbacks[actionId] = callback
        println("[CustomKeybindManager] Registered callback for action: $actionId")
    }

    fun bindKey(
        keyCode: Int,
        actionId: String,
        description: String,
        requiresPlayer: Boolean = true,
        callback: Runnable
    ) {
        registerActionCallback(actionId, callback)

        val action = KeybindAction(actionId, description, callback, requiresPlayer)
        keyBindings.computeIfAbsent(keyCode) { mutableListOf() }.add(action)
        keyStates[keyCode] = false

        KeybindConfigManager.setCustomKeybind(keyCode.toString(), actionId, description, requiresPlayer)

        println("[CustomKeybindManager] Bound key ${getKeyName(keyCode)} to action: $description")
    }

    fun unbindAction(keyCode: Int, actionId: String) {
        keyBindings[keyCode]?.removeIf { it.id == actionId }
        if (keyBindings[keyCode]?.isEmpty() == true) {
            keyBindings.remove(keyCode)
            keyStates.remove(keyCode)
        }

        KeybindConfigManager.removeCustomKeybind(keyCode.toString())

        println("[CustomKeybindManager] Unbound action $actionId from key ${getKeyName(keyCode)}")
    }

    fun unbindKey(keyCode: Int) {
        keyBindings.remove(keyCode)
        keyStates.remove(keyCode)

        KeybindConfigManager.removeCustomKeybind(keyCode.toString())

        println("[CustomKeybindManager] Unbound all actions from key ${getKeyName(keyCode)}")
    }

    fun isKeyBound(keyCode: Int): Boolean {
        return keyBindings.containsKey(keyCode) && keyBindings[keyCode]?.isNotEmpty() == true
    }

    fun getKeyActions(keyCode: Int): List<KeybindAction> {
        return keyBindings[keyCode]?.toList() ?: emptyList()
    }

    fun tick() {
        if (!isInitialized) return

        val client = MinecraftClient.getInstance()
        val window = client.window?.handle ?: return

        keyBindings.keys.forEach { keyCode ->
            val isPressed = GLFW.glfwGetKey(window, keyCode) == GLFW.GLFW_PRESS
            val wasPressed = keyStates[keyCode] ?: false

            if (isPressed && !wasPressed) {
                executeKeyActions(keyCode)
            }

            keyStates[keyCode] = isPressed
        }
    }

    private fun executeKeyActions(keyCode: Int) {
        val actions = keyBindings[keyCode] ?: return
        val client = MinecraftClient.getInstance()

        actions.forEach { action ->
            try {
                if (action.requiresPlayer && client.player == null) {
                    return@forEach
                }

                action.callback.run()

            } catch (e: Exception) {
                println("[CustomKeybindManager] Error executing action ${action.id}: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun getKeyName(keyCode: Int): String {
        return when (keyCode) {
            GLFW.GLFW_KEY_UNKNOWN -> "Unknown"
            GLFW.GLFW_KEY_SPACE -> "Space"
            GLFW.GLFW_KEY_APOSTROPHE -> "'"
            GLFW.GLFW_KEY_COMMA -> ","
            GLFW.GLFW_KEY_MINUS -> "-"
            GLFW.GLFW_KEY_PERIOD -> "."
            GLFW.GLFW_KEY_SLASH -> "/"
            GLFW.GLFW_KEY_SEMICOLON -> ";"
            GLFW.GLFW_KEY_EQUAL -> "="
            GLFW.GLFW_KEY_LEFT_BRACKET -> "["
            GLFW.GLFW_KEY_BACKSLASH -> "\\"
            GLFW.GLFW_KEY_RIGHT_BRACKET -> "]"
            GLFW.GLFW_KEY_GRAVE_ACCENT -> "`"
            GLFW.GLFW_KEY_ESCAPE -> "Escape"
            GLFW.GLFW_KEY_ENTER -> "Enter"
            GLFW.GLFW_KEY_TAB -> "Tab"
            GLFW.GLFW_KEY_BACKSPACE -> "Backspace"
            GLFW.GLFW_KEY_INSERT -> "Insert"
            GLFW.GLFW_KEY_DELETE -> "Delete"
            GLFW.GLFW_KEY_RIGHT -> "Right Arrow"
            GLFW.GLFW_KEY_LEFT -> "Left Arrow"
            GLFW.GLFW_KEY_DOWN -> "Down Arrow"
            GLFW.GLFW_KEY_UP -> "Up Arrow"
            GLFW.GLFW_KEY_PAGE_UP -> "Page Up"
            GLFW.GLFW_KEY_PAGE_DOWN -> "Page Down"
            GLFW.GLFW_KEY_HOME -> "Home"
            GLFW.GLFW_KEY_END -> "End"
            GLFW.GLFW_KEY_CAPS_LOCK -> "Caps Lock"
            GLFW.GLFW_KEY_SCROLL_LOCK -> "Scroll Lock"
            GLFW.GLFW_KEY_NUM_LOCK -> "Num Lock"
            GLFW.GLFW_KEY_PRINT_SCREEN -> "Print Screen"
            GLFW.GLFW_KEY_PAUSE -> "Pause"
            GLFW.GLFW_KEY_LEFT_SHIFT -> "Left Shift"
            GLFW.GLFW_KEY_LEFT_CONTROL -> "Left Ctrl"
            GLFW.GLFW_KEY_LEFT_ALT -> "Left Alt"
            GLFW.GLFW_KEY_LEFT_SUPER -> "Left Super"
            GLFW.GLFW_KEY_RIGHT_SHIFT -> "Right Shift"
            GLFW.GLFW_KEY_RIGHT_CONTROL -> "Right Ctrl"
            GLFW.GLFW_KEY_RIGHT_ALT -> "Right Alt"
            GLFW.GLFW_KEY_RIGHT_SUPER -> "Right Super"
            in GLFW.GLFW_KEY_0..GLFW.GLFW_KEY_9 -> (keyCode - GLFW.GLFW_KEY_0 + '0'.code).toChar().toString()
            in GLFW.GLFW_KEY_A..GLFW.GLFW_KEY_Z -> (keyCode - GLFW.GLFW_KEY_A + 'A'.code).toChar().toString()
            in GLFW.GLFW_KEY_F1..GLFW.GLFW_KEY_F25 -> "F${keyCode - GLFW.GLFW_KEY_F1 + 1}"
            in GLFW.GLFW_KEY_KP_0..GLFW.GLFW_KEY_KP_9 -> "Numpad ${keyCode - GLFW.GLFW_KEY_KP_0}"
            GLFW.GLFW_KEY_KP_DECIMAL -> "Numpad ."
            GLFW.GLFW_KEY_KP_DIVIDE -> "Numpad /"
            GLFW.GLFW_KEY_KP_MULTIPLY -> "Numpad *"
            GLFW.GLFW_KEY_KP_SUBTRACT -> "Numpad -"
            GLFW.GLFW_KEY_KP_ADD -> "Numpad +"
            GLFW.GLFW_KEY_KP_ENTER -> "Numpad Enter"
            GLFW.GLFW_KEY_KP_EQUAL -> "Numpad ="
            else -> "Key $keyCode"
        }
    }

    fun getKeyCodeFromName(keyName: String): Int {
        val upperName = keyName.uppercase()

        return when (upperName) {
            "SPACE" -> GLFW.GLFW_KEY_SPACE
            "ENTER" -> GLFW.GLFW_KEY_ENTER
            "TAB" -> GLFW.GLFW_KEY_TAB
            "BACKSPACE" -> GLFW.GLFW_KEY_BACKSPACE
            "DELETE" -> GLFW.GLFW_KEY_DELETE
            "ESCAPE", "ESC" -> GLFW.GLFW_KEY_ESCAPE
            "INSERT" -> GLFW.GLFW_KEY_INSERT
            "HOME" -> GLFW.GLFW_KEY_HOME
            "END" -> GLFW.GLFW_KEY_END
            "PAGE_UP", "PAGEUP" -> GLFW.GLFW_KEY_PAGE_UP
            "PAGE_DOWN", "PAGEDOWN" -> GLFW.GLFW_KEY_PAGE_DOWN
            "CAPS_LOCK", "CAPSLOCK" -> GLFW.GLFW_KEY_CAPS_LOCK
            "SCROLL_LOCK", "SCROLLLOCK" -> GLFW.GLFW_KEY_SCROLL_LOCK
            "NUM_LOCK", "NUMLOCK" -> GLFW.GLFW_KEY_NUM_LOCK
            "PRINT_SCREEN", "PRINTSCREEN" -> GLFW.GLFW_KEY_PRINT_SCREEN
            "PAUSE" -> GLFW.GLFW_KEY_PAUSE

            // Touches de modificateur
            "SHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT
            "CTRL", "CONTROL" -> GLFW.GLFW_KEY_LEFT_CONTROL
            "ALT" -> GLFW.GLFW_KEY_LEFT_ALT
            "SUPER", "CMD", "WINDOWS" -> GLFW.GLFW_KEY_LEFT_SUPER
            "LSHIFT", "LEFT_SHIFT" -> GLFW.GLFW_KEY_LEFT_SHIFT
            "RSHIFT", "RIGHT_SHIFT" -> GLFW.GLFW_KEY_RIGHT_SHIFT
            "LCTRL", "LEFT_CTRL", "LEFT_CONTROL" -> GLFW.GLFW_KEY_LEFT_CONTROL
            "RCTRL", "RIGHT_CTRL", "RIGHT_CONTROL" -> GLFW.GLFW_KEY_RIGHT_CONTROL
            "LALT", "LEFT_ALT" -> GLFW.GLFW_KEY_LEFT_ALT
            "RALT", "RIGHT_ALT" -> GLFW.GLFW_KEY_RIGHT_ALT
            "LSUPER", "LEFT_SUPER" -> GLFW.GLFW_KEY_LEFT_SUPER
            "RSUPER", "RIGHT_SUPER" -> GLFW.GLFW_KEY_RIGHT_SUPER

            "UP", "ARROW_UP" -> GLFW.GLFW_KEY_UP
            "DOWN", "ARROW_DOWN" -> GLFW.GLFW_KEY_DOWN
            "LEFT", "ARROW_LEFT" -> GLFW.GLFW_KEY_LEFT
            "RIGHT", "ARROW_RIGHT" -> GLFW.GLFW_KEY_RIGHT

            "APOSTROPHE", "'" -> GLFW.GLFW_KEY_APOSTROPHE
            "COMMA", "," -> GLFW.GLFW_KEY_COMMA
            "MINUS", "-" -> GLFW.GLFW_KEY_MINUS
            "PERIOD", "." -> GLFW.GLFW_KEY_PERIOD
            "SLASH", "/" -> GLFW.GLFW_KEY_SLASH
            "SEMICOLON", ";" -> GLFW.GLFW_KEY_SEMICOLON
            "EQUAL", "=" -> GLFW.GLFW_KEY_EQUAL
            "LEFT_BRACKET", "[" -> GLFW.GLFW_KEY_LEFT_BRACKET
            "BACKSLASH", "\\" -> GLFW.GLFW_KEY_BACKSLASH
            "RIGHT_BRACKET", "]" -> GLFW.GLFW_KEY_RIGHT_BRACKET
            "GRAVE_ACCENT", "`" -> GLFW.GLFW_KEY_GRAVE_ACCENT

            "KP_0", "NUMPAD_0" -> GLFW.GLFW_KEY_KP_0
            "KP_1", "NUMPAD_1" -> GLFW.GLFW_KEY_KP_1
            "KP_2", "NUMPAD_2" -> GLFW.GLFW_KEY_KP_2
            "KP_3", "NUMPAD_3" -> GLFW.GLFW_KEY_KP_3
            "KP_4", "NUMPAD_4" -> GLFW.GLFW_KEY_KP_4
            "KP_5", "NUMPAD_5" -> GLFW.GLFW_KEY_KP_5
            "KP_6", "NUMPAD_6" -> GLFW.GLFW_KEY_KP_6
            "KP_7", "NUMPAD_7" -> GLFW.GLFW_KEY_KP_7
            "KP_8", "NUMPAD_8" -> GLFW.GLFW_KEY_KP_8
            "KP_9", "NUMPAD_9" -> GLFW.GLFW_KEY_KP_9
            "KP_DECIMAL", "NUMPAD_DECIMAL" -> GLFW.GLFW_KEY_KP_DECIMAL
            "KP_DIVIDE", "NUMPAD_DIVIDE" -> GLFW.GLFW_KEY_KP_DIVIDE
            "KP_MULTIPLY", "NUMPAD_MULTIPLY" -> GLFW.GLFW_KEY_KP_MULTIPLY
            "KP_SUBTRACT", "NUMPAD_SUBTRACT" -> GLFW.GLFW_KEY_KP_SUBTRACT
            "KP_ADD", "NUMPAD_ADD" -> GLFW.GLFW_KEY_KP_ADD
            "KP_ENTER", "NUMPAD_ENTER" -> GLFW.GLFW_KEY_KP_ENTER
            "KP_EQUAL", "NUMPAD_EQUAL" -> GLFW.GLFW_KEY_KP_EQUAL

            else -> {
                if (upperName.length == 1) {
                    val char = upperName[0]
                    when (char) {
                        in '0'..'9' -> GLFW.GLFW_KEY_0 + (char - '0')
                        in 'A'..'Z' -> GLFW.GLFW_KEY_A + (char - 'A')
                        else -> GLFW.GLFW_KEY_UNKNOWN
                    }
                }
                else if (upperName.startsWith("F") && upperName.length <= 3) {
                    try {
                        val num = upperName.substring(1).toInt()
                        if (num in 1..25) GLFW.GLFW_KEY_F1 + (num - 1)
                        else GLFW.GLFW_KEY_UNKNOWN
                    } catch (e: NumberFormatException) {
                        GLFW.GLFW_KEY_UNKNOWN
                    }
                }
                else if (upperName.startsWith("NUMPAD ")) {
                    val numpadKey = upperName.substring(7)
                    when (numpadKey) {
                        "0" -> GLFW.GLFW_KEY_KP_0
                        "1" -> GLFW.GLFW_KEY_KP_1
                        "2" -> GLFW.GLFW_KEY_KP_2
                        "3" -> GLFW.GLFW_KEY_KP_3
                        "4" -> GLFW.GLFW_KEY_KP_4
                        "5" -> GLFW.GLFW_KEY_KP_5
                        "6" -> GLFW.GLFW_KEY_KP_6
                        "7" -> GLFW.GLFW_KEY_KP_7
                        "8" -> GLFW.GLFW_KEY_KP_8
                        "9" -> GLFW.GLFW_KEY_KP_9
                        "." -> GLFW.GLFW_KEY_KP_DECIMAL
                        "/" -> GLFW.GLFW_KEY_KP_DIVIDE
                        "*" -> GLFW.GLFW_KEY_KP_MULTIPLY
                        "-" -> GLFW.GLFW_KEY_KP_SUBTRACT
                        "+" -> GLFW.GLFW_KEY_KP_ADD
                        "ENTER" -> GLFW.GLFW_KEY_KP_ENTER
                        "=" -> GLFW.GLFW_KEY_KP_EQUAL
                        else -> GLFW.GLFW_KEY_UNKNOWN
                    }
                } else {
                    GLFW.GLFW_KEY_UNKNOWN
                }
            }
        }
    }

    fun getAllBindings(): Map<Int, List<KeybindAction>> {
        return keyBindings.toMap()
    }

    fun clearAllBindings() {
        keyBindings.clear()
        keyStates.clear()
        actionCallbacks.clear()
        KeybindConfigManager.clearAllKeybinds()
        println("[CustomKeybindManager] Cleared all bindings")
    }

    fun hasAction(actionId: String): Boolean {
        return actionCallbacks.containsKey(actionId)
    }

    fun getActionDescription(actionId: String): String? {
        keyBindings.values.forEach { actions ->
            actions.forEach { action ->
                if (action.id == actionId) {
                    return action.description
                }
            }
        }
        return null
    }

    fun getKeyForAction(actionId: String): Int? {
        keyBindings.entries.forEach { (keyCode, actions) ->
            if (actions.any { it.id == actionId }) {
                return keyCode
            }
        }
        return null
    }

    fun rebindAction(oldKeyCode: Int, newKeyCode: Int, actionId: String) {
        val action = keyBindings[oldKeyCode]?.find { it.id == actionId }
        if (action != null) {
            unbindAction(oldKeyCode, actionId)
            bindKey(newKeyCode, action.id, action.description, action.requiresPlayer, action.callback)
            println("[CustomKeybindManager] Rebound action $actionId from ${getKeyName(oldKeyCode)} to ${getKeyName(newKeyCode)}")
        }
    }
}