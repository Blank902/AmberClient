package com.amberclient.utils.module

enum class ModuleCategory(val displayName: String) {
    COMBAT("Combat"),
    MINIGAMES("Mini-games"),
    MOVEMENT("Movement"),
    RENDER("Render"),
    PLAYER("Player"),
    WORLD("World"),
    MISC("Miscellaneous");

    override fun toString(): String = displayName
}