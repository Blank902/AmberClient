package com.amberclient.utils.module

interface ConfigurableModule {

    fun getSettings(): List<ModuleSettings>

    fun onSettingChanged(setting: ModuleSettings) { }
}