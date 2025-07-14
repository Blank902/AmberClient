package com.amberclient.modules.combat

import com.amberclient.utils.minecraft.InvUtils
import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleCategory
import com.amberclient.utils.module.ModuleSettings

class AutoPotion : Module("AutoPotion", "Automatically throws healing potions at your feet", ModuleCategory.COMBAT), ConfigurableModule {

    companion object {
        private var instance: AutoPotion? = null
        fun getInstance(): AutoPotion? = instance
    }

    private val healthThreshold = ModuleSettings("Health Threshold", "Health in half-hearts to trigger healing", 6.0, 1.0, 20.0, 1.0)
    private val delay = ModuleSettings("Delay", "Delay between heals in ms", 1000.0, 100.0, 5000.0, 100.0)
    private val switchBack = ModuleSettings("Switch Back", "Switch back to original slot after healing", true)
    private val checkPotionType = ModuleSettings("Check Potion Type", "Only use instant health potions", true)

    private val settings: MutableList<ModuleSettings> = mutableListOf<ModuleSettings>().apply {
        add(healthThreshold)
        add(delay)
        add(switchBack)
        add(checkPotionType)
    }

    private val mc = getClient()
    private var lastHealTime = 0L

    init {
        instance = this
    }

    override fun onTick() {
        val player = mc.player ?: return

        if (System.currentTimeMillis() - lastHealTime < delay.doubleValue) return

        val currentHealth = player.health
        if (currentHealth > healthThreshold.doubleValue) return

        if (player.isCreative) return

        val success = InvUtils.throwHealingPotion()

        if (success) {
            lastHealTime = System.currentTimeMillis()
        } else {
            val inventorySlot = InvUtils.findPotionSlotInInventory(checkPotionType.booleanValue)
            if (inventorySlot != -1 && inventorySlot > 8) {
                val emptyHotbarSlot = (0..8).find { player.inventory.getStack(it).isEmpty } ?: -1
                if (emptyHotbarSlot != -1) {
                    InvUtils.swapSlots(player, inventorySlot, emptyHotbarSlot)
                    val successAfterSwap = InvUtils.throwHealingPotion()
                    if (successAfterSwap) {
                        lastHealTime = System.currentTimeMillis()
                    }
                }
            }
        }
    }

    override fun getSettings(): List<ModuleSettings> = settings
}