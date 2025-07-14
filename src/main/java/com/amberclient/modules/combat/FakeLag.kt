package com.amberclient.modules.combat

import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleCategory
import com.amberclient.utils.module.ModuleSettings
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSignC2SPacket

class FakeLag : Module("FakeLag", "Holds back packets to prevent you from being hit by an enemy", ModuleCategory.COMBAT), ConfigurableModule {

    // Settings
    private val range = ModuleSettings("Range", "Range settings", 3.5, 0.0, 10.0, 0.1)
    private val delay = ModuleSettings("Delay", "Delay in milliseconds", 450, 0, 1000)
    private val recoilTime = ModuleSettings("RecoilTime", "Recoil time in milliseconds", 250, 0, 1000)
    private val mode = ModuleSettings("Mode", "FakeLag mode", Mode.DYNAMIC)
    private val flushOn = ModuleSettings("FlushOn", "Flush on packet types", setOf(FlushOn.ENTITY_INTERACT))

    enum class Mode {
        DYNAMIC,
        STATIC
    }

    enum class FlushOn(
        val choiceName: String,
        val testPacket: (packet: Packet<*>?) -> Boolean
    ) {
        ENTITY_INTERACT("EntityInteract", {
            it is PlayerInteractEntityC2SPacket
                    || it is HandSwingC2SPacket
        }),
        BLOCK_INTERACT("BlockInteract", {
            it is PlayerInteractBlockC2SPacket
                    || it is UpdateSignC2SPacket
        }),
        ACTION("Action", {
            it is PlayerActionC2SPacket
        })
    }

    override fun getSettings(): List<ModuleSettings> {
        return listOf(
            range, delay, recoilTime, mode, flushOn
        )
    }
}