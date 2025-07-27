package com.amberclient.modules.render.xray

class SettingsStore private constructor() {
    private val settings = StateSettings()

    companion object {
        @Volatile
        private var instance: SettingsStore? = null

        fun getInstance(): SettingsStore {
            return instance ?: synchronized(this) {
                instance ?: SettingsStore().also { instance = it }
            }
        }
    }

    fun getInstance(): SettingsStore = SettingsStore.getInstance()

    fun get(): StateSettings = settings

    class StateSettings {
        var isActive: Boolean = false
        var isExposedOnly: Boolean = false
        var isShowLava: Boolean = false
        var halfRange: Int = 4
        var isOreSim: Boolean = false
    }
}