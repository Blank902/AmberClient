package com.amberclient.modules.movement

import com.amberclient.utils.module.ConfigurableModule
import com.amberclient.utils.module.Module
import com.amberclient.utils.module.ModuleSettings
import net.minecraft.block.Blocks
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

class NoFall : Module("NoFall", "Prevents fall damage", "Movement"), ConfigurableModule {
    companion object {
        const val MOD_ID = "amberclient-nofall"
        val LOGGER: Logger = LogManager.getLogger(MOD_ID)
    }

    enum class NoFallMode {
        PACKET,
        MLG,
        HYBRID
    }

    // Settings
    private val nofallMode = ModuleSettings("Mode", "NoFall mode selection", NoFallMode.PACKET)
    private val minFallDistanceSetting = ModuleSettings("Min Fall Distance", "Minimum fall distance to trigger MLG", 3.0, 1.0, 10.0, 0.5)
    private val pickupWaterSetting = ModuleSettings("Pickup Water", "Automatically pickup placed water", true)

    private var waterPlacements = mutableListOf<WaterPlacement>()
    private var lastPlacementTime = 0L

    private val netherItems = arrayOf(
        Items.COBWEB,
        Items.POWDER_SNOW_BUCKET,
        Items.HAY_BLOCK,
        Items.SLIME_BLOCK,
        Items.HONEY_BLOCK,
        Items.TWISTING_VINES
    )

    private val overworldItems = arrayOf(
        Items.WATER_BUCKET,
        Items.COBWEB,
        Items.POWDER_SNOW_BUCKET,
        Items.HAY_BLOCK,
        Items.SLIME_BLOCK,
        Items.HONEY_BLOCK
    )

    private val fallDamageBlockingBlocks = setOf(
        Blocks.WATER,
        Blocks.COBWEB,
        Blocks.SLIME_BLOCK,
        Blocks.HONEY_BLOCK,
        Blocks.HAY_BLOCK,
        Blocks.POWDER_SNOW
    )

    data class WaterPlacement(
        val pos: BlockPos,
        val placementTime: Long
    )

    override fun getSettings(): List<ModuleSettings> {
        return listOf(
            nofallMode,
            minFallDistanceSetting,
            pickupWaterSetting,
        )
    }

    override fun onTick() {
        val player: ClientPlayerEntity = client.player ?: return
        if (player.isSpectator || player.isCreative) return

        val currentMode = nofallMode.getEnumValue<NoFallMode>()
        val minFallDistance = minFallDistanceSetting.getDoubleValue().toFloat()
        val pickupWater = pickupWaterSetting.getBooleanValue()

        if (pickupWater) {
            handleWaterPickup(player)
        }

        when (currentMode) {
            NoFallMode.PACKET -> handlePacketMode(player)
            NoFallMode.MLG -> handleMLGMode(player, minFallDistance)
            NoFallMode.HYBRID -> {
                if (!handleMLGMode(player, minFallDistance)) {
                    handlePacketMode(player)
                }
            }
        }
    }

    private fun handlePacketMode(player: ClientPlayerEntity) {
        if (!player.isOnGround && player.velocity.y < 0.0) {
            player.networkHandler.sendPacket(PlayerMoveC2SPacket.OnGroundOnly(true, false))
        }
    }

    private fun handleMLGMode(player: ClientPlayerEntity, minFallDistance: Float): Boolean {
        if (player.fallDistance < minFallDistance || player.velocity.y >= -0.5) {
            return false
        }

        LOGGER.info("MLG Mode triggered - Fall distance: ${player.fallDistance}, Velocity Y: ${player.velocity.y}")

        val landingPos = calculateLandingPosition(player)
        if (landingPos == null) {
            LOGGER.info("Could not calculate landing position")
            return false
        }

        LOGGER.info("Calculated landing position: $landingPos")

        if (willSurviveFall(landingPos)) {
            LOGGER.info("Will survive fall naturally")
            return false
        }

        val mlgItem = findMLGItem(player)
        if (mlgItem == null) {
            LOGGER.info("No MLG item found")
            return false
        }

        LOGGER.info("Found MLG item: ${mlgItem.second.item} in slot ${mlgItem.first}")

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastPlacementTime < 300) {
            return false
        }

        return attemptMLGPlacement(player, landingPos, mlgItem)
    }

    private fun calculateLandingPosition(player: ClientPlayerEntity): BlockPos? {
        val world = client.world ?: return null

        var pos = player.pos
        var velocity = player.velocity

        val horizontalVel = kotlin.math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z)
        val timeToFall = if (velocity.y < 0) kotlin.math.abs(velocity.y) else 1.0

        val futureX = pos.x + velocity.x * timeToFall
        val futureZ = pos.z + velocity.z * timeToFall

        var testY = pos.y.toInt()
        val minY = world.bottomY

        while (testY > minY) {
            val testPos = BlockPos(futureX.toInt(), testY, futureZ.toInt())
            val blockState = world.getBlockState(testPos)

            if (!blockState.isAir && !blockState.isReplaceable) {
                val placementPos = testPos.up()
                // Vérifier que la position de placement est libre
                if (world.getBlockState(placementPos).isReplaceable) {
                    LOGGER.info("Found solid ground at $testPos, placing at $placementPos")
                    return placementPos
                }
            }
            testY--
        }

        val playerBlockPos = BlockPos(pos.x.toInt(), (pos.y - 1).toInt(), pos.z.toInt())
        if (world.getBlockState(playerBlockPos).isReplaceable) {
            LOGGER.info("Fallback: placing near player at $playerBlockPos")
            return playerBlockPos
        }

        return null
    }

    private fun willSurviveFall(pos: BlockPos): Boolean {
        val world = client.world ?: return false
        val blockBelow = world.getBlockState(pos.down())
        val isBlockingFall = fallDamageBlockingBlocks.contains(blockBelow.block)
        LOGGER.info("Checking survival at $pos: block below = ${blockBelow.block}, blocking = $isBlockingFall")
        return isBlockingFall
    }

    private fun findMLGItem(player: ClientPlayerEntity): Pair<Int, ItemStack>? {
        val inventory = player.inventory
        val isNether = client.world?.dimension?.ultrawarm == true
        val items = if (isNether) netherItems else overworldItems

        val mainHand = inventory.getStack(inventory.selectedSlot)
        if (items.contains(mainHand.item) && !mainHand.isEmpty) {
            LOGGER.info("MLG item found in main hand: ${mainHand.item}")
            return inventory.selectedSlot to mainHand
        }

        val offHand = inventory.getStack(40)
        if (items.contains(offHand.item) && !offHand.isEmpty) {
            LOGGER.info("MLG item found in offhand: ${offHand.item}")
            return 40 to offHand
        }

        for (i in 0..8) {
            val stack = inventory.getStack(i)
            if (items.contains(stack.item) && !stack.isEmpty) {
                LOGGER.info("MLG item found in hotbar slot $i: ${stack.item}")
                return i to stack
            }
        }
        return null
    }

    private fun attemptMLGPlacement(
        player: ClientPlayerEntity,
        targetPos: BlockPos,
        mlgItem: Pair<Int, ItemStack>
    ): Boolean {
        val world = client.world ?: return false
        val (slot, itemStack) = mlgItem

        LOGGER.info("Attempting MLG placement at $targetPos with ${itemStack.item} from slot $slot")

        val targetState = world.getBlockState(targetPos)
        if (!targetState.isReplaceable) {
            LOGGER.info("Target position is not replaceable: ${targetState.block}")
            return false
        }

        val playerPos = player.pos
        val targetCenter = Vec3d.ofCenter(targetPos)
        val distance = playerPos.distanceTo(targetCenter)
        if (distance > 6.0) {
            LOGGER.info("Target too far: distance = $distance")
            return false
        }

        val originalSlot = player.inventory.selectedSlot
        var slotChanged = false

        try {
            if (slot != originalSlot && slot < 9) {
                player.inventory.selectedSlot = slot
                slotChanged = true
                LOGGER.info("Changed slot from $originalSlot to $slot")
            }

            val hand = if (slot == 40) Hand.OFF_HAND else Hand.MAIN_HAND

            val targetBelow = targetPos.down()
            val hitResult = BlockHitResult(
                Vec3d.ofCenter(targetBelow).add(0.0, 0.5, 0.0),
                Direction.UP,
                targetBelow,
                false
            )

            LOGGER.info("Placing with hand $hand at $targetPos (hitting $targetBelow)")

            // Tenter le placement
            val result = client.interactionManager?.interactBlock(player, hand, hitResult)

            if (result?.isAccepted == true) {
                // Enregistrer le placement d'eau pour pickup
                if (itemStack.item == Items.WATER_BUCKET) {
                    waterPlacements.add(WaterPlacement(targetPos, System.currentTimeMillis()))
                    LOGGER.info("Water placement recorded for pickup")
                }

                lastPlacementTime = System.currentTimeMillis()
                LOGGER.info("MLG placement SUCCESS at $targetPos with ${itemStack.item}")
                return true
            } else {
                LOGGER.info("MLG placement FAILED: interaction result = $result")
            }
        } catch (e: Exception) {
            LOGGER.error("Error during MLG placement: ${e.message}", e)
        } finally {
            if (slotChanged) {
                player.inventory.selectedSlot = originalSlot
                LOGGER.info("Restored original slot $originalSlot")
            }
        }

        return false
    }

    private fun handleWaterPickup(player: ClientPlayerEntity) {
        if (waterPlacements.isEmpty()) return

        val world = client.world ?: return

        val bucketSlot = findEmptyBucket(player) ?: return

        val iterator = waterPlacements.iterator()
        while (iterator.hasNext()) {
            val placement = iterator.next()

            val blockState = world.getBlockState(placement.pos)
            if (blockState.block != Blocks.WATER) {
                iterator.remove()
                continue
            }

            if (attemptWaterPickup(player, placement.pos, bucketSlot)) {
                iterator.remove()
                LOGGER.info("Water picked up at ${placement.pos}")
                break
            }
        }
    }

    private fun findEmptyBucket(player: ClientPlayerEntity): Int? {
        val inventory = player.inventory
        for (i in 0..8) {
            val stack = inventory.getStack(i)
            if (stack.item == Items.BUCKET && !stack.isEmpty) {
                return i
            }
        }
        return null
    }

    private fun attemptWaterPickup(player: ClientPlayerEntity, pos: BlockPos, bucketSlot: Int): Boolean {
        val originalSlot = player.inventory.selectedSlot
        try {
            player.inventory.selectedSlot = bucketSlot
            val waterCenter = Vec3d.ofCenter(pos)
            val hitResult = BlockHitResult(waterCenter, Direction.UP, pos, false)
            val result = client.interactionManager?.interactBlock(player, Hand.MAIN_HAND, hitResult)
            return result?.isAccepted == true
        } catch (e: Exception) {
            LOGGER.error("Error during water pickup: ${e.message}", e)
            return false
        } finally {
            player.inventory.selectedSlot = originalSlot
        }
    }

    private fun cleanupOldPlacements(maxTime: Long) {
        val currentTime = System.currentTimeMillis()
        waterPlacements.removeIf { placement ->
            val age = currentTime - placement.placementTime
            if (age > maxTime) {
                LOGGER.debug("Cleaning up old water placement at ${placement.pos}")
                true
            } else {
                false
            }
        }
    }

    override fun onEnable() {
        super.onEnable()
        LOGGER.info("NoFall enabled with mode: ${nofallMode.getEnumValue<NoFallMode>()}")
    }

    override fun onDisable() {
        super.onDisable()
        waterPlacements.clear()
        LOGGER.info("NoFall disabled, cleared ${waterPlacements.size} water placements")
    }
}