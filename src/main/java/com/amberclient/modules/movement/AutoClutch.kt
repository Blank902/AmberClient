package com.amberclient.modules.movement

import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleSettings
import net.minecraft.block.Block
import net.minecraft.block.SnowBlock
import net.minecraft.client.MinecraftClient
import net.minecraft.item.BlockItem
import net.minecraft.text.Text
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.MathHelper
import net.minecraft.util.math.Vec3d
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.lang.Math.toDegrees
import java.lang.Math.toRadians
import kotlin.math.*
import kotlin.random.Random

class AutoClutch : Module("AutoClutch", "Automatically clutches (blocks disappear sometimes)", "Movement"), ConfigurableModule {

    companion object {
        const val MOD_ID = "amberclient-autoclutch"
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)

        fun getBlock(pos: BlockPos): Block? {
            return MinecraftClient.getInstance().world?.getBlockState(pos)?.block
        }
    }

    // Settings
    private val range = ModuleSettings("Range", "Distance to search for block placement", 4.0, 1.0, 8.0, 1.0)
    private val rotationSpeed = ModuleSettings("Rotation Speed", "Speed of rotation smoothing", 15.0, 1.0, 50.0, 1.0)
    private val cpsLimit = ModuleSettings("CPS Limit", "Maximum clicks per second (if Uncap CPS is disabled)", 20.0, 1.0, 50.0, 1.0)
    private val uncapCps = ModuleSettings("Uncap CPS", "Disable CPS limitation (for longer clutches)", true)
    private val holdMode = ModuleSettings("Hold Mode", "Deactivate module when key is released", false)
    private val smartRotation = ModuleSettings("Smart Rotation", "Intelligently rotate to closest placement", true)
    private val humanizeRotations = ModuleSettings("Humanize Rotations", "Add human-like rotation variations", false)

    private val settings = listOf(
        range, rotationSpeed, cpsLimit, uncapCps, holdMode, smartRotation, humanizeRotations
    )

    private val mc = client
    private var lastPlaceTime = 0L
    private var placeDelay = 50L
    private var hasTarget = false

    init {
        // initial delay
        updatePlaceDelay()
    }

    override fun onEnable() {
        if (mc.player != null) {
            hasTarget = false
        }
        LOGGER.info("{} module enabled", name)
    }

    override fun onDisable() {
        if (mc.player != null) {
            hasTarget = false
        }
        LOGGER.info("{} module disabled", name)
    }

    override fun onTick() {
        handleKeyInput()

        if (isEnabled() && mc.player != null && mc.world != null) {
            if (shouldPlaceBlock()) {
                val bestPlacement = findBestPlacement()

                if (bestPlacement != null) {
                    if (smartRotation.booleanValue) {
                        updateRotation(bestPlacement.yaw, bestPlacement.pitch)
                    }

                    // Place block if CPS allows or uncap CPS is enabled
                    if (canPlaceBlock()) {
                        if (placeBlockAt(bestPlacement)) {
                            lastPlaceTime = System.currentTimeMillis()
                        }
                    }
                }
            }
        }
    }

    private fun shouldPlaceBlock(): Boolean {
        val player = mc.player ?: return false
        val world = mc.world ?: return false

        val playerPos = player.pos
        val playerBlockPos = BlockPos(playerPos.x.toInt(), playerPos.y.toInt(), playerPos.z.toInt())

        return player.velocity.y < 0 && isAirBlock(getBlock(playerBlockPos.down()) ?: return false)
    }

    private fun findBestPlacement(): PlacementInfo? {
        val player = mc.player ?: return null
        val world = mc.world ?: return null

        val playerPos = player.pos
        val playerBlockPos = BlockPos(playerPos.x.toInt(), playerPos.y.toInt(), playerPos.z.toInt())

        var bestPlacement: PlacementInfo? = null
        var closestDistance = Double.MAX_VALUE

        val searchRange = range.doubleValue.toInt()

        for (y in -1 downTo -searchRange) {
            val directBelow = playerBlockPos.add(0, y, 0)
            val placement = canPlaceAt(directBelow)
            if (placement != null) {
                return placement
            }

            for (radius in 1..searchRange) {
                for (angle in 0 until 360 step 15) {
                    val radians = toRadians(angle.toDouble())
                    val x = (radius * cos(radians)).roundToInt()
                    val z = (radius * sin(radians)).roundToInt()

                    val targetPos = playerBlockPos.add(x, y, z)

                    val distance = playerPos.distanceTo(Vec3d.ofCenter(targetPos))
                    if (distance > searchRange) continue

                    val currentPlacement = canPlaceAt(targetPos)
                    if (currentPlacement != null && distance < closestDistance) {
                        closestDistance = distance
                        bestPlacement = currentPlacement
                    }
                }
            }

            if (bestPlacement != null) {
                return bestPlacement
            }
        }

        return bestPlacement
    }

    private fun canPlaceBlock(): Boolean {
        if (uncapCps.booleanValue) {
            return true
        }

        val currentTime = System.currentTimeMillis()
        return (currentTime - lastPlaceTime) >= placeDelay
    }

    private fun updatePlaceDelay() {
        if (!uncapCps.booleanValue) {
            val cps = cpsLimit.doubleValue
            placeDelay = (1000.0 / cps).toLong()
        } else {
            placeDelay = 0
        }
    }

    private fun canPlaceAt(pos: BlockPos): PlacementInfo? {
        val player = mc.player ?: return null
        val world = mc.world ?: return null

        val playerPos = player.pos
        if (pos.y >= playerPos.y.toInt()) return null

        if (!isAirBlock(getBlock(pos) ?: return null)) return null

        if (hasBlocks()) return null

        val eyesPos = Vec3d(
            player.x,
            player.y + player.getEyeHeight(player.pose),
            player.z
        )

        // Find the best face to place against
        for (side in arrayOf(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST, Direction.DOWN)) {
            val neighbor = pos.offset(side)
            val placementSide = side.opposite

            val neighborState = world.getBlockState(neighbor)
            if (!neighborState.isSolidBlock(world, neighbor)) continue

            // Calculate hit vector
            val hitVec = Vec3d.ofCenter(neighbor)
                .add(Vec3d.of(placementSide.vector).multiply(0.5))

            if (eyesPos.squaredDistanceTo(hitVec) > 36.0) continue

            val angles = calculateRotation(hitVec)

            return PlacementInfo(pos, neighbor, placementSide, hitVec, angles[0], angles[1])
        }

        return null
    }

    private fun placeBlockAt(placement: PlacementInfo): Boolean {
        val player = mc.player ?: return false
        if (hasBlocks()) return false

        selectBlockInHotbar()

        player.yaw = placement.yaw
        player.pitch = placement.pitch

        val hitResult = BlockHitResult(placement.hitVec, placement.side, placement.neighbor, false)
        mc.interactionManager?.interactBlock(player, Hand.MAIN_HAND, hitResult)
        player.swingHand(Hand.MAIN_HAND)

        return true
    }

    private fun updateRotation(newYaw: Float, newPitch: Float) {
        val player = mc.player ?: return

        val (targetYaw, targetPitch) = if (!hasTarget) {
            hasTarget = true
            newYaw to newPitch
        } else {
            var speed = rotationSpeed.doubleValue.toFloat()
            var adjustedYaw = newYaw
            var adjustedPitch = newPitch

            if (humanizeRotations.booleanValue) {
                val randomFactor = 0.7f + Random.nextFloat() * 0.6f
                speed *= randomFactor

                val jitterYaw = (Random.nextDouble() - 0.5).toFloat()
                val jitterPitch = ((Random.nextDouble() - 0.5) * 0.6).toFloat()

                adjustedYaw += jitterYaw
                adjustedPitch += jitterPitch
            }

            val yawDiff = MathHelper.wrapDegrees(adjustedYaw - player.yaw)
            val pitchDiff = adjustedPitch - player.pitch

            val newTargetYaw = player.yaw + sign(yawDiff) * min(abs(yawDiff), speed)
            val newTargetPitch = player.pitch + sign(pitchDiff) * min(abs(pitchDiff), speed)

            newTargetYaw to newTargetPitch
        }

        player.yaw = targetYaw
        player.pitch = MathHelper.clamp(targetPitch, -90.0f, 90.0f)
    }

    private fun calculateRotation(target: Vec3d): FloatArray {
        val player = mc.player ?: return floatArrayOf(0f, 0f)

        val eyesPos = Vec3d(
            player.x,
            player.y + player.getEyeHeight(player.pose),
            player.z
        )

        val diff = target.subtract(eyesPos)
        val horizontalDistance = sqrt(diff.x * diff.x + diff.z * diff.z)

        val yaw = toDegrees(atan2(diff.z, diff.x)).toFloat() - 90.0f
        val pitch = -toDegrees(atan2(diff.y, horizontalDistance)).toFloat()

        return floatArrayOf(MathHelper.wrapDegrees(yaw), MathHelper.clamp(pitch, -90.0f, 90.0f))
    }

    private fun hasBlocks(): Boolean {
        return getFirstHotBarSlotWithBlocks() == -1
    }

    private fun selectBlockInHotbar() {
        val player = mc.player ?: return

        if (!doesSlotHaveBlocks(player.inventory.selectedSlot)) {
            val blockSlot = getFirstHotBarSlotWithBlocks()
            if (blockSlot != -1) {
                player.inventory.selectedSlot = blockSlot
            }
        }
    }

    private data class PlacementInfo(
        val pos: BlockPos,
        val neighbor: BlockPos,
        val side: Direction,
        val hitVec: Vec3d,
        val yaw: Float,
        val pitch: Float
    )

    private fun isAirBlock(block: Block): Boolean {
        if (block.defaultState.isAir) {
            return block !is SnowBlock || block.defaultState.get(SnowBlock.LAYERS) <= 1
        }
        return false
    }

    private fun getFirstHotBarSlotWithBlocks(): Int {
        val player = mc.player ?: return -1
        for (i in 0 until 9) {
            val stack = player.inventory.getStack(i)
            if (!stack.isEmpty && stack.item is BlockItem) {
                return i
            }
        }
        return -1
    }

    private fun doesSlotHaveBlocks(slotToCheck: Int): Boolean {
        val player = mc.player ?: return false
        val stack = player.inventory.getStack(slotToCheck)
        return !stack.isEmpty && stack.item is BlockItem && stack.count > 0
    }

    override fun getSettings(): List<ModuleSettings> {
        return settings
    }

    override fun onSettingChanged(setting: ModuleSettings) {
        val player = mc.player ?: return

        when (setting) {
            range -> {
                player.sendMessage(
                    Text.literal("§6AutoClutch settings updated: Range=${range.doubleValue} blocks"),
                    true
                )
                LOGGER.info("AutoClutch settings updated: Range={} blocks", range.doubleValue)
            }
            uncapCps -> {
                val status = if (uncapCps.booleanValue) "enabled" else "disabled"
                updatePlaceDelay()
                player.sendMessage(
                    Text.literal("§6AutoClutch settings updated: CPS Cap=$status"),
                    true
                )
                LOGGER.info("AutoClutch settings updated: CPS Cap={}", status)
            }
            cpsLimit -> {
                updatePlaceDelay()
                player.sendMessage(
                    Text.literal("§6AutoClutch settings updated: CPS Limit=${cpsLimit.doubleValue}"),
                    true
                )
                LOGGER.info("AutoClutch settings updated: CPS Limit={}", cpsLimit.doubleValue)
            }
            holdMode -> {
                val status = if (holdMode.booleanValue) "enabled" else "disabled"
                player.sendMessage(
                    Text.literal("§6AutoClutch settings updated: Hold Mode=$status"),
                    true
                )
                LOGGER.info("AutoClutch settings updated: Hold Mode={}", status)
            }
            rotationSpeed -> {
                player.sendMessage(
                    Text.literal("§6AutoClutch settings updated: Rotation Speed=${rotationSpeed.doubleValue}"),
                    true
                )
                LOGGER.info("AutoClutch settings updated: Rotation Speed={}", rotationSpeed.doubleValue)
            }
            smartRotation -> {
                val status = if (smartRotation.booleanValue) "enabled" else "disabled"
                player.sendMessage(
                    Text.literal("§6AutoClutch settings updated: Smart Rotation=$status"),
                    true
                )
                LOGGER.info("AutoClutch settings updated: Smart Rotation={}", status)
            }
            humanizeRotations -> {
                val status = if (humanizeRotations.booleanValue) "enabled" else "disabled"
                player.sendMessage(
                    Text.literal("§6AutoClutch settings updated: Humanize Rotations=$status"),
                    true
                )
                LOGGER.info("AutoClutch settings updated: Humanize Rotations={}", status)
            }
        }
    }
}