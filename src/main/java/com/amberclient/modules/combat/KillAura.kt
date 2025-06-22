package com.amberclient.modules.combat

import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleSettings
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.PlayerListEntry
import net.minecraft.entity.Entity
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.Tameable
import net.minecraft.entity.attribute.EntityAttributes
import net.minecraft.entity.mob.MobEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.world.GameMode
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.PI // used to make a "toRadians" function since Kotlin does not support this directly

import com.amberclient.utils.general.TickRate.getTickRate
import com.amberclient.utils.general.TickRate.getTimeSinceLastTick

/*
    Thanks to https://github.com/enzzzh for the KillAura module base!
    Implemented by https://github.com/gqdThinky.
 */

class KillAura : Module("KillAura", "Automatically attacks nearby entities", "Combat"), ConfigurableModule {

    companion object {
        const val MOD_ID = "amberclient-killaura"
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)
    }

    private var lastClickTime = 0L
    private var scanTick = 0
    private var attacking = false

    // Settings
    private val range = ModuleSettings("Range", "Maximum attack range", 3.5, 0.5, 6.0, 0.25)
    private val clickDelay = ModuleSettings("Click Delay", "Delay between clicks in ms", 100.0, 25.0, 500.0, 10.0)
    private val useWeaponSpeed = ModuleSettings("Use Weapon Speed", "Use the weapon's attack speed", true)
    private val onlyOnClick = ModuleSettings("Only on Click", "Attacks only when left-clicking", false)
    private val onlyOnLook = ModuleSettings("Only on Look", "Attacks only when looking at an entity", false)
    private val wallsRange = ModuleSettings("Walls Range", "Range through walls", 3.5, 0.0, 6.0, 0.1)
    private val ignoreTamed = ModuleSettings("Ignore Tamed", "Ignore tamed mobs", false)
    private val pauseOnLag = ModuleSettings("Pause on Lag", "Pause when server lags", true)
    private val tpsSync = ModuleSettings("TPS Sync", "Sync attack speed with server TPS", true)

    private val settings = listOf(
        range, clickDelay, useWeaponSpeed, onlyOnClick, onlyOnLook,
        wallsRange, ignoreTamed, pauseOnLag, tpsSync
    )

    override fun onDisable() {
        stopAttacking()
    }

    private fun getGameMode(player: PlayerEntity?): GameMode? {
        if (player == null || MinecraftClient.getInstance().networkHandler == null) return null
        val entry: PlayerListEntry? = MinecraftClient.getInstance().networkHandler!!.getPlayerListEntry(player.uuid)
        return entry?.gameMode
    }

    override fun onTick() {
        if (!isEnabled()) return

        val client = MinecraftClient.getInstance()
        val player = client.player
        if (player == null || client.world == null || !player.isAlive || getGameMode(player) == GameMode.SPECTATOR) {
            stopAttacking()
            return
        }

        if (onlyOnClick.booleanValue && !client.options.attackKey.isPressed) {
            stopAttacking()
            return
        }
        if (pauseOnLag.booleanValue && getTimeSinceLastTick() >= 1f) {
            stopAttacking()
            return
        }

        if (scanTick++ % 5 != 0) return

        val target: LivingEntity? = if (onlyOnLook.booleanValue) {
            val targeted = client.targetedEntity
            if (targeted is LivingEntity && entityCheck(targeted)) {
                targeted
            } else null
        } else {
            var closestDist = Double.MAX_VALUE
            var closestTarget: LivingEntity? = null
            for (entity in client.world!!.entities) {
                if (entity is LivingEntity && entityCheck(entity)) {
                    val dist = player.distanceTo(entity).toDouble()
                    if (dist < closestDist) {
                        closestDist = dist
                        closestTarget = entity
                    }
                }
            }
            closestTarget
        }

        if (target == null) {
            stopAttacking()
            return
        }

        if (Random.nextDouble() < 0.05) return

        val delayToUse = if (useWeaponSpeed.booleanValue) {
            getWeaponAttackDelay()
        } else {
            clickDelay.doubleValue
        }.let { delay ->
            if (tpsSync.booleanValue) {
                val tps = getTickRate()
                if (tps > 0) delay * (20.0 / tps) else delay
            } else delay
        }

        val randomDelay = delayToUse * (0.8 + Random.nextDouble() * 0.4)

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= randomDelay) {
            client.interactionManager?.attackEntity(player, target)
            player.swingHand(Hand.MAIN_HAND)
            lastClickTime = currentTime
            attacking = true
        } else {
            attacking = false
        }
    }

    private fun getWeaponAttackDelay(): Double {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return 100.0

        val heldItem: ItemStack = player.mainHandStack
        val cooldown = player.getAttackCooldownProgress(0.0f)
        if (cooldown < 1.0f) {
            return Double.MAX_VALUE // Not ready to attack yet
        }
        val attackSpeed = player.getAttributeValue(EntityAttributes.ATTACK_SPEED)
        return 1000.0 / attackSpeed
    }

    private fun entityCheck(entity: LivingEntity): Boolean {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return false

        if (entity == player) return false
        if (!entity.isAlive || entity.isDead) return false
        if (entity !is PlayerEntity && entity !is MobEntity) return false

        if (!isInFOV(entity)) return false

        val distance = player.distanceTo(entity).toDouble()
        val effectiveRange = range.doubleValue * (0.95 + Random.nextDouble() * 0.1)
        if (distance > effectiveRange) return false
        if (!canSeeEntity(entity) && distance > wallsRange.doubleValue) return false

        if (ignoreTamed.booleanValue && entity is Tameable && entity.owner == player) return false

        return true
    }

    // Check if entity is within player's field of view
    private fun isInFOV(entity: LivingEntity): Boolean {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return false

        val lookVec = player.rotationVector
        val entityVec = entity.pos.subtract(player.pos).normalize()
        val dot = lookVec.dotProduct(entityVec)
        return dot > cos(toRadians(90.0))
    }

    private fun canSeeEntity(entity: Entity): Boolean {
        return MinecraftClient.getInstance().player?.canSee(entity) ?: false
    }

    private fun stopAttacking() {
        attacking = false
    }

    override fun getSettings(): List<ModuleSettings> {
        return settings
    }

    override fun onSettingChanged(setting: ModuleSettings) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return

        when (setting) {
            range -> {
                player.sendMessage(Text.literal("§6KillAura settings updated: Range=${range.doubleValue}"), true)
                LOGGER.info("KillAura settings updated: Range={}", range.doubleValue)
            }
            clickDelay -> {
                player.sendMessage(Text.literal("§6KillAura settings updated: Click Delay=${clickDelay.doubleValue} ms"), true)
                LOGGER.info("KillAura settings updated: Click Delay={} ms", clickDelay.doubleValue)
            }
            useWeaponSpeed -> {
                val status = if (useWeaponSpeed.booleanValue) "enabled" else "disabled"
                player.sendMessage(Text.literal("§6KillAura settings updated: Use Weapon Speed=$status"), true)
                LOGGER.info("KillAura settings updated: Use Weapon Speed={}", status)
            }
            onlyOnClick -> {
                val status = if (onlyOnClick.booleanValue) "enabled" else "disabled"
                player.sendMessage(Text.literal("§6KillAura settings updated: Only on Click=$status"), true)
                LOGGER.info("KillAura settings updated: Only on Click={}", status)
            }
            onlyOnLook -> {
                val status = if (onlyOnLook.booleanValue) "enabled" else "disabled"
                player.sendMessage(Text.literal("§6KillAura settings updated: Only on Look=$status"), true)
                LOGGER.info("KillAura settings updated: Only on Look={}", status)
            }
            wallsRange -> {
                player.sendMessage(Text.literal("§6KillAura settings updated: Walls Range=${wallsRange.doubleValue}"), true)
                LOGGER.info("KillAura settings updated: Walls Range={}", wallsRange.doubleValue)
            }
            ignoreTamed -> {
                val status = if (ignoreTamed.booleanValue) "enabled" else "disabled"
                player.sendMessage(Text.literal("§6KillAura settings updated: Ignore Tamed=$status"), true)
                LOGGER.info("KillAura settings updated: Ignore Tamed={}", status)
            }
            pauseOnLag -> {
                val status = if (pauseOnLag.booleanValue) "enabled" else "disabled"
                player.sendMessage(Text.literal("§6KillAura settings updated: Pause on Lag=$status"), true)
                LOGGER.info("KillAura settings updated: Pause on Lag={}", status)
            }
            tpsSync -> {
                val status = if (tpsSync.booleanValue) "enabled" else "disabled"
                player.sendMessage(Text.literal("§6KillAura settings updated: TPS Sync=$status"), true)
                LOGGER.info("KillAura settings updated: TPS Sync={}", status)
            }
        }
    }




    fun toRadians(deg: Double): Double = deg / 180.0 * PI
}