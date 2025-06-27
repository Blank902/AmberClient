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
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.MathHelper
import net.minecraft.world.GameMode
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

import kotlin.random.Random
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.atan2
import kotlin.math.asin
import kotlin.math.PI

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

    enum class TargetingMode {
        ALL_ENTITIES,
        PLAYERS,
        MOBS
    }

    enum class RotationMode {
        OFF,
        INSTANT,
        SMOOTH
    }

    private var lastClickTime = 0L
    private var scanTick = 0
    private var attacking = false

    private var targetYaw: Float = 0f
    private var targetPitch: Float = 0f
    private var currentYaw: Float = 0f
    private var currentPitch: Float = 0f
    private var isRotating = false

    // Settings
    private val range = ModuleSettings("Range", "Maximum attack range", 3.5, 0.5, 6.0, 0.25)
    private val attackSpeed = ModuleSettings("Attack Speed", "Attack delay in ms", 100.0, 25.0, 500.0, 25.0)
    private val useWeaponSpeed = ModuleSettings("Use Weapon Speed", "Use the weapon's attack speed", true)
    private val targeting = ModuleSettings("Targeting", "Target selection mode", TargetingMode.ALL_ENTITIES)
    private val rotation = ModuleSettings("Rotation", "Rotation mode", RotationMode.INSTANT)
    private val rotationSpeed = ModuleSettings("Rotation Speed", "Rotation speed (degrees/tick)", 25.0, 5.0, 50.0, 5.0)

    private val settings = listOf(range, attackSpeed, useWeaponSpeed, targeting, rotation, rotationSpeed)

    override fun onDisable() {
        stopAttacking()
        isRotating = false
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

        if (!isRotating) {
            currentYaw = player.yaw
            currentPitch = player.pitch
        }

        val rotationMode = rotation.getEnumValue<RotationMode>()
        if (rotationMode != RotationMode.OFF && isRotating) {
            applySmoothRotation()
        }

        if (scanTick++ % 5 != 0) return

        val targetingMode = targeting.getEnumValue<TargetingMode>()
        var closestDist = Double.MAX_VALUE
        var closestTarget: LivingEntity? = null

        for (entity in client.world!!.entities) {
            if (entity is LivingEntity && entityCheck(entity, targetingMode)) {
                val dist = player.distanceTo(entity).toDouble()
                if (dist < closestDist) {
                    closestDist = dist
                    closestTarget = entity
                }
            }
        }

        if (closestTarget == null) {
            stopAttacking()
            return
        }

        if (Random.nextDouble() < 0.05) return

        val delayToUse = if (useWeaponSpeed.booleanValue) {
            getWeaponAttackDelay()
        } else {
            attackSpeed.doubleValue
        }

        val randomDelay = delayToUse * (0.8 + Random.nextDouble() * 0.4)

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastClickTime >= randomDelay) {
            if (rotationMode != RotationMode.OFF) {
                setRotationTarget(closestTarget)
            }

            client.interactionManager?.attackEntity(player, closestTarget)
            player.swingHand(Hand.MAIN_HAND)
            lastClickTime = currentTime
            attacking = true
        } else {
            attacking = false
        }
    }

    private fun setRotationTarget(target: LivingEntity) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return

        val playerPos = Vec3d(player.x, player.y + player.standingEyeHeight, player.z)

        val targetBox = target.boundingBox

        val closestPoint = Vec3d(
            MathHelper.clamp(playerPos.x, targetBox.minX, targetBox.maxX),
            MathHelper.clamp(playerPos.y, targetBox.minY, targetBox.maxY),
            MathHelper.clamp(playerPos.z, targetBox.minZ, targetBox.maxZ)
        )

        val direction = closestPoint.subtract(playerPos)
        val distance = direction.length()

        if (distance == 0.0) return

        targetYaw = Math.toDegrees(atan2(-direction.x, direction.z)).toFloat()
        targetPitch = Math.toDegrees(-asin(direction.y / distance)).toFloat()

        targetYaw = normalizeYaw(targetYaw)

        targetPitch = MathHelper.clamp(targetPitch, -90f, 90f)

        if (!isRotating) {
            currentYaw = player.yaw
            currentPitch = player.pitch
        }

        isRotating = true
    }

    private fun applySmoothRotation() {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return

        val rotationMode = rotation.getEnumValue<RotationMode>()
        val maxSpeed = rotationSpeed.doubleValue.toFloat()

        var yawDiff = targetYaw - currentYaw
        yawDiff = normalizeYawDiff(yawDiff)

        val pitchDiff = targetPitch - currentPitch

        when (rotationMode) {
            RotationMode.INSTANT -> {
                val yawStep = MathHelper.clamp(yawDiff, -maxSpeed, maxSpeed)
                val pitchStep = MathHelper.clamp(pitchDiff, -maxSpeed, maxSpeed)

                currentYaw += yawStep
                currentPitch += pitchStep
            }
            RotationMode.SMOOTH -> {
                val smoothnessFactor = 0.3f

                val yawStep = if (kotlin.math.abs(yawDiff) <= maxSpeed) {
                    yawDiff * smoothnessFactor
                } else {
                    kotlin.math.sign(yawDiff) * maxSpeed * smoothnessFactor
                }

                val pitchStep = if (kotlin.math.abs(pitchDiff) <= maxSpeed) {
                    pitchDiff * smoothnessFactor
                } else {
                    kotlin.math.sign(pitchDiff) * maxSpeed * smoothnessFactor
                }

                currentYaw += yawStep
                currentPitch += pitchStep
            }
            RotationMode.OFF -> { return }
        }

        currentYaw = normalizeYaw(currentYaw)
        currentPitch = MathHelper.clamp(currentPitch, -90f, 90f)

        player.yaw = currentYaw
        player.pitch = currentPitch
        player.headYaw = currentYaw
        player.prevYaw = currentYaw
        player.prevPitch = currentPitch

        var finalYawDiff = targetYaw - currentYaw
        finalYawDiff = normalizeYawDiff(finalYawDiff)
        val finalPitchDiff = targetPitch - currentPitch

        if (kotlin.math.abs(finalYawDiff) < 1f && kotlin.math.abs(finalPitchDiff) < 1f) {
            isRotating = false
        }
    }

    private fun normalizeYaw(yaw: Float): Float {
        var normalizedYaw = yaw % 360f
        if (normalizedYaw > 180f) {
            normalizedYaw -= 360f
        } else if (normalizedYaw < -180f) {
            normalizedYaw += 360f
        }
        return normalizedYaw
    }

    private fun normalizeYawDiff(diff: Float): Float {
        var normalizedDiff = diff
        while (normalizedDiff > 180f) normalizedDiff -= 360f
        while (normalizedDiff < -180f) normalizedDiff += 360f
        return normalizedDiff
    }

    private fun getWeaponAttackDelay(): Double {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return 100.0

        val heldItem: ItemStack = player.mainHandStack
        val cooldown = player.getAttackCooldownProgress(0.0f)
        if (cooldown < 1.0f) {
            return Double.MAX_VALUE
        }
        val attackSpeed = player.getAttributeValue(EntityAttributes.ATTACK_SPEED)
        return 1000.0 / attackSpeed
    }

    private fun entityCheck(entity: LivingEntity, targetingMode: TargetingMode): Boolean {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return false

        if (entity == player) return false
        if (!entity.isAlive || entity.isDead) return false

        when (targetingMode) {
            TargetingMode.PLAYERS -> {
                if (entity !is PlayerEntity) return false
            }
            TargetingMode.MOBS -> {
                if (entity !is MobEntity) return false
            }
            TargetingMode.ALL_ENTITIES -> {
                if (entity !is PlayerEntity && entity !is MobEntity) return false
            }
        }

        val rotationMode = rotation.getEnumValue<RotationMode>()
        if (rotationMode == RotationMode.OFF && !isInFOV(entity)) return false

        val distance = player.distanceTo(entity).toDouble()
        val effectiveRange = range.doubleValue * (0.95 + Random.nextDouble() * 0.1)
        if (distance > effectiveRange) return false

        if (entity is Tameable && entity.owner == player) return false

        return true
    }

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
        isRotating = false
    }

    override fun getSettings(): List<ModuleSettings> {
        return settings
    }

    override fun onSettingChanged(setting: ModuleSettings) {
        val client = MinecraftClient.getInstance()
        val player = client.player ?: return

        when (setting) {
            range -> {
                player.sendMessage(Text.literal("§6KillAura: Range=${range.doubleValue}"), true)
                LOGGER.info("KillAura: Range={}", range.doubleValue)
            }
            attackSpeed -> {
                player.sendMessage(Text.literal("§6KillAura: Attack Speed=${attackSpeed.doubleValue}ms"), true)
                LOGGER.info("KillAura: Attack Speed={}ms", attackSpeed.doubleValue)
            }
            useWeaponSpeed -> {
                val status = if (useWeaponSpeed.booleanValue) "enabled" else "disabled"
                player.sendMessage(Text.literal("§6KillAura: Use Weapon Speed=$status"), true)
                LOGGER.info("KillAura: Use Weapon Speed={}", status)
            }
            targeting -> {
                val targetingMode = targeting.getEnumValue<TargetingMode>()
                val targetingText = when (targetingMode) {
                    TargetingMode.ALL_ENTITIES -> "All Entities"
                    TargetingMode.PLAYERS -> "Players"
                    TargetingMode.MOBS -> "Mobs"
                }
                player.sendMessage(Text.literal("§6KillAura: Targeting=$targetingText"), true)
                LOGGER.info("KillAura: Targeting={}", targetingText)
            }
            rotation -> {
                val rotationMode = rotation.getEnumValue<RotationMode>()
                val rotationText = when (rotationMode) {
                    RotationMode.OFF -> "Off"
                    RotationMode.INSTANT -> "Fast"
                    RotationMode.SMOOTH -> "Smooth"
                }
                player.sendMessage(Text.literal("§6KillAura: Rotation=$rotationText"), true)
                LOGGER.info("KillAura: Rotation={}", rotationText)
            }
            rotationSpeed -> {
                player.sendMessage(Text.literal("§6KillAura: Rotation Speed=${rotationSpeed.doubleValue}°/tick"), true)
                LOGGER.info("KillAura: Rotation Speed={}°/tick", rotationSpeed.doubleValue)
            }
        }
    }

    fun toRadians(deg: Double): Double = deg / 180.0 * PI
}