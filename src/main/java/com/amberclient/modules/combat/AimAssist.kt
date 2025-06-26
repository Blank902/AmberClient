package com.amberclient.modules.combat

import com.amberclient.utils.general.MinecraftUtils
import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleSettings
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.LivingEntity
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import net.minecraft.world.RaycastContext
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import org.joml.Vector3d
import kotlin.math.*
import kotlin.random.Random

class AimAssist : Module("AimAssist", "Automatically aims at nearby entities", "Combat"), ConfigurableModule {

    companion object {
        const val MOD_ID = "amberclient-aimassist"
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)
    }

    // Settings - General
    private val range = ModuleSettings("Range", "The range at which an entity can be targeted", 5.0, 0.0, 20.0, 0.1)
    private val fov = ModuleSettings("FOV", "Will only aim entities in the fov", 360.0, 0.0, 360.0, 1.0)
    private val ignoreWalls = ModuleSettings("Ignore Walls", "Whether or not to ignore aiming through walls", false)
    private val priority = ModuleSettings("Priority", "How to filter targets within range", SortPriority.CLOSEST)
    private val bodyTarget = ModuleSettings("Aim Target", "Which part of the entities body to aim at", TargetPart.BODY)

    // Settings - Entities
    private val targetsPlayers = ModuleSettings("Target Players", "Target player entities", true)
    private val targetsMobs = ModuleSettings("Target Mobs", "Target all mobs (hostile, neutral, passive)", true)

    // Settings - Aim Speed
    private val instantLook = ModuleSettings("Instant Look", "Instantly looks at the entity", false)
    private val aimSpeed = ModuleSettings("Aim Speed", "Base aim speed multiplier", 3.5, 0.1, 15.0, 0.1)

    // Settings - Advanced
    private val smoothingIntensity = ModuleSettings("Smoothing Intensity", "How smooth the aim movement is", 0.75, 0.1, 2.0, 0.05)
    private val maxRotationPerTick = ModuleSettings("Max Rotation", "Maximum rotation per tick (degrees)", 25.0, 1.0, 90.0, 1.0)
    private val accelerationFactor = ModuleSettings("Acceleration", "How quickly aim accelerates", 1.2, 0.5, 3.0, 0.1)

    private val mc = client
    private val targetPos = Vector3d()
    private var currentTarget: Entity? = null

    private var lastYawDelta = 0.0f
    private var lastPitchDelta = 0.0f
    private var currentYawVelocity = 0.0
    private var currentPitchVelocity = 0.0
    private var ticksSinceTargetChange = 0

    enum class SortPriority {
        NONE,
        CLOSEST,
        LOWEST_HEALTH,
        HIGHEST_HEALTH,
        ANGLE
    }

    enum class TargetPart {
        NONE,
        HEAD,
        BODY,
        FEET
    }

    init {
        ClientTickEvents.END_CLIENT_TICK.register { client ->
            if (isEnabled) {
                onTick()
            }
        }
    }

    override fun getSettings(): List<ModuleSettings> {
        return listOf(
            range, fov, ignoreWalls, priority, bodyTarget,
            targetsPlayers, targetsMobs,
            instantLook, aimSpeed,
            smoothingIntensity, maxRotationPerTick, accelerationFactor
        )
    }

    override fun onEnable() {
        currentTarget = null
        resetSmoothingVars()
        LOGGER.info("AimAssist enabled")
    }

    override fun onDisable() {
        currentTarget = null
        resetSmoothingVars()
        LOGGER.info("AimAssist disabled")
    }

    private fun resetSmoothingVars() {
        lastYawDelta = 0.0f
        lastPitchDelta = 0.0f
        currentYawVelocity = 0.0
        currentPitchVelocity = 0.0
        ticksSinceTargetChange = 0
    }

    override fun onTick() {
        if (mc.player == null || mc.world == null) return

        val previousTarget = currentTarget
        currentTarget = findTarget()

        if (currentTarget != previousTarget) {
            resetSmoothingVars()
        }

        currentTarget?.let {
            aimAtTarget(it)
            ticksSinceTargetChange++
        } ?: applyDeceleration()
    }

    private fun findTarget(): Entity? {
        val world = mc.world ?: return null
        val player = mc.player ?: return null

        val entities = world.getOtherEntities(
            player,
            player.boundingBox.expand(range.doubleValue)
        )

        var bestTarget: Entity? = null
        var bestScore = Double.MAX_VALUE

        for (entity in entities) {
            if (!isValidTarget(entity)) continue

            val score = calculateTargetScore(entity)
            if (score < bestScore) {
                bestScore = score
                bestTarget = entity
            }
        }

        return bestTarget
    }

    private fun isValidTarget(entity: Entity): Boolean {
        if (entity !is LivingEntity || !entity.isAlive) {
            return false
        }

        if (!isTargetableEntityType(entity)) {
            return false
        }

        val distance = mc.player!!.distanceTo(entity)
        if (distance > range.doubleValue) {
            return false
        }

        if (!isInFov(entity)) {
            return false
        }

        if (!ignoreWalls.booleanValue && !canSeeEntity(entity)) {
            return false
        }

        return true
    }

    private fun isTargetableEntityType(entity: Entity): Boolean {
        val type = entity.type

        return when (type) {
            EntityType.PLAYER -> targetsPlayers.booleanValue
            else -> if (MinecraftUtils.isMob(type)) targetsMobs.booleanValue else false
        }
    }

    private fun isInFov(entity: Entity): Boolean {
        val fovValue = fov.doubleValue
        if (fovValue >= 360) return true

        val player = mc.player!!
        val playerPos = player.pos.add(0.0, player.getEyeHeight(player.pose).toDouble(), 0.0)
        val entityPos = entity.pos.add(0.0, entity.height / 2.0, 0.0)

        val toEntity = entityPos.subtract(playerPos).normalize()
        val playerLook = Vec3d.fromPolar(player.pitch, player.yaw).normalize()

        val angle = Math.toDegrees(acos(MathHelper.clamp(playerLook.dotProduct(toEntity), -1.0, 1.0)))
        return angle <= fovValue / 2
    }

    private fun canSeeEntity(entity: Entity): Boolean {
        val world = mc.world ?: return false
        val player = mc.player ?: return false

        val start = player.eyePos
        val end = entity.pos.add(0.0, entity.height / 2.0, 0.0)

        return world.raycast(
            RaycastContext(
                start, end,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                player
            )
        ).type == HitResult.Type.MISS
    }

    private fun calculateTargetScore(entity: Entity): Double {
        val distance = mc.player!!.distanceTo(entity)
        val priorityValue = priority.getEnumValue<SortPriority>()

        return when (priorityValue) {
            SortPriority.NONE -> 0.0
            SortPriority.CLOSEST -> distance.toDouble()
            SortPriority.LOWEST_HEALTH -> if (entity is LivingEntity) entity.health.toDouble() else Double.MAX_VALUE
            SortPriority.HIGHEST_HEALTH -> if (entity is LivingEntity) -entity.health.toDouble() else Double.MAX_VALUE
            SortPriority.ANGLE -> {
                val playerPos = mc.player!!.eyePos
                val entityPos = entity.pos.add(0.0, entity.height / 2.0, 0.0)
                val toEntity = entityPos.subtract(playerPos).normalize()
                val playerLook = Vec3d.fromPolar(mc.player!!.pitch, mc.player!!.yaw).normalize()
                abs(Math.toDegrees(acos(MathHelper.clamp(playerLook.dotProduct(toEntity), -1.0, 1.0))))
            }
        }
    }

    private fun aimAtTarget(target: Entity) {
        val player = mc.player ?: return

        calculateTargetPosition(target)

        val deltaX = targetPos.x - player.x
        val deltaZ = targetPos.z - player.z
        val deltaY = targetPos.y - (player.y + player.getEyeHeight(player.pose))

        val targetYaw = Math.toDegrees(atan2(deltaZ, deltaX)) - 90
        val horizontalDistance = sqrt(deltaX * deltaX + deltaZ * deltaZ)
        val targetPitch = -Math.toDegrees(atan2(deltaY, horizontalDistance))

        if (instantLook.booleanValue) {
            player.yaw = targetYaw.toFloat()
            player.pitch = targetPitch.toFloat()
            resetSmoothingVars()
        } else {
            applyAdvancedSmoothRotation(targetYaw, targetPitch)
        }
    }

    private fun applyAdvancedSmoothRotation(targetYaw: Double, targetPitch: Double) {
        val player = mc.player!!
        val currentYaw = player.yaw
        val currentPitch = player.pitch

        val deltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw)
        val deltaPitch = MathHelper.wrapDegrees(targetPitch - currentPitch)

        val baseSpeed = aimSpeed.doubleValue
        val smoothing = smoothingIntensity.doubleValue
        val acceleration = accelerationFactor.doubleValue
        val maxRotation = maxRotationPerTick.doubleValue

        val accelerationMultiplier = min(1.0, (ticksSinceTargetChange * 0.1) * acceleration)

        val yawDistance = abs(deltaYaw)
        val pitchDistance = abs(deltaPitch)

        val yawSpeedFactor = calculateSpeedFactor(yawDistance)
        val pitchSpeedFactor = calculateSpeedFactor(pitchDistance)

        val targetYawVelocity = min(yawDistance * yawSpeedFactor * baseSpeed * accelerationMultiplier, maxRotation)
        val targetPitchVelocity = min(pitchDistance * pitchSpeedFactor * baseSpeed * accelerationMultiplier, maxRotation)

        currentYawVelocity = lerp(currentYawVelocity, targetYawVelocity, smoothing)
        currentPitchVelocity = lerp(currentPitchVelocity, targetPitchVelocity, smoothing)

        var yawStep = min(abs(deltaYaw), currentYawVelocity) * sign(deltaYaw)
        var pitchStep = min(abs(deltaPitch), currentPitchVelocity) * sign(deltaPitch)

        yawStep += (Random.nextDouble() - 0.5) * 0.1 * smoothing
        pitchStep += (Random.nextDouble() - 0.5) * 0.1 * smoothing

        player.yaw = currentYaw + yawStep.toFloat()
        player.pitch = MathHelper.clamp(currentPitch + pitchStep.toFloat(), -90f, 90f)

        lastYawDelta = yawStep.toFloat()
        lastPitchDelta = pitchStep.toFloat()
    }

    private fun calculateSpeedFactor(distance: Double): Double {
        return when {
            distance > 45 -> 1.0
            distance > 15 -> 0.8
            distance > 5 -> 0.4
            else -> 0.2
        }
    }

    private fun applyDeceleration() {
        currentYawVelocity *= 0.85
        currentPitchVelocity *= 0.85

        if (abs(currentYawVelocity) > 0.1 || abs(currentPitchVelocity) > 0.1) {
            val player = mc.player!!
            val currentYaw = player.yaw
            val currentPitch = player.pitch

            player.yaw = currentYaw + (currentYawVelocity * sign(lastYawDelta.toDouble())).toFloat()
            player.pitch = MathHelper.clamp(
                currentPitch + (currentPitchVelocity * sign(lastPitchDelta.toDouble())).toFloat(),
                -90f, 90f
            )
        }
    }

    private fun lerp(start: Double, end: Double, factor: Double): Double {
        return start + (end - start) * factor
    }

    private fun calculateTargetPosition(target: Entity) {
        val velocity = target.velocity

        val distance = mc.player!!.distanceTo(target)
        val predictionTime = distance / 20.0
        val predictedPos = target.pos.add(velocity.multiply(predictionTime))

        targetPos.set(predictedPos.x, predictedPos.y, predictedPos.z)

        val targetPart = bodyTarget.getEnumValue<TargetPart>()
        when (targetPart) {
            TargetPart.NONE -> { /* nothing lol */ }
            TargetPart.HEAD -> targetPos.add(0.0, target.height * 0.9, 0.0)
            TargetPart.BODY -> targetPos.add(0.0, target.height * 0.5, 0.0)
            TargetPart.FEET -> targetPos.add(0.0, target.height * 0.1, 0.0)
        }
    }

    fun getCurrentTarget(): Entity? = currentTarget
}