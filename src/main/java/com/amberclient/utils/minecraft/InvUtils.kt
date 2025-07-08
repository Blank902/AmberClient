package com.amberclient.utils.minecraft

import net.minecraft.client.MinecraftClient
import net.minecraft.item.ItemStack
import net.minecraft.item.SplashPotionItem
import net.minecraft.component.DataComponentTypes
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.potion.Potions
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.Hand

object InvUtils {

    private val mc = MinecraftClient.getInstance()

    fun swapSlots(player: PlayerEntity, slot1: Int, slot2: Int) {
        val inventory = player.inventory
        val stack1 = inventory.getStack(slot1)
        val stack2 = inventory.getStack(slot2)
        inventory.setStack(slot1, stack2)
        inventory.setStack(slot2, stack1)
    }

    fun findPotionSlot(checkType: Boolean = true): Int {
        val player = mc.player ?: return -1

        for (i in 0 until 9) {
            val stack = player.inventory.getStack(i)
            if (isPotionOfHealing(stack, checkType)) {
                return i
            }
        }

        return -1
    }

    fun findPotionSlotInInventory(checkType: Boolean = true): Int {
        val player = mc.player ?: return -1

        for (i in 0 until player.inventory.size()) {
            val stack = player.inventory.getStack(i)
            if (isPotionOfHealing(stack, checkType)) {
                return i
            }
        }

        return -1
    }

    fun isPotionOfHealing(stack: ItemStack, checkType: Boolean = true): Boolean {
        if (stack.isEmpty) return false

        if (stack.item !is SplashPotionItem) return false

        if (!checkType) return true

        val potionComponent = stack.get(DataComponentTypes.POTION_CONTENTS)
        if (potionComponent != null) {
            val potion = potionComponent.potion().orElse(null)
            return potion == Potions.HEALING || potion == Potions.STRONG_HEALING
        }

        return false
    }

    /**
     * Utilise une potion de soin depuis l'inventaire
     * @param moveToHotbar Si true, déplace la potion vers la hotbar avant de l'utiliser
     * @return true si la potion a été utilisée avec succès
     */
    fun useHealingPotion(moveToHotbar: Boolean = true): Boolean {
        val player = mc.player ?: return false
        val interactionManager = mc.interactionManager ?: return false

        // Chercher d'abord dans la hotbar
        val hotbarSlot = findPotionSlot(true)
        if (hotbarSlot != -1) {
            // Sélectionner le slot et utiliser la potion
            val previousSlot = player.inventory.selectedSlot
            player.inventory.selectedSlot = hotbarSlot

            // Utiliser la potion (clic droit)
            val result = interactionManager.interactItem(player, Hand.MAIN_HAND)

            // Restaurer le slot précédent après un délai
            if (result.isAccepted) {
                return true
            }
        }

        // Si pas dans la hotbar, chercher dans l'inventaire
        if (moveToHotbar) {
            val inventorySlot = findPotionSlotInInventory(true)
            if (inventorySlot != -1 && inventorySlot > 8) { // Pas dans la hotbar
                val emptyHotbarSlot = findEmptyHotbarSlot()
                if (emptyHotbarSlot != -1) {
                    // Déplacer vers la hotbar
                    swapWithHotbar(inventorySlot, emptyHotbarSlot)

                    // Utiliser la potion
                    val previousSlot = player.inventory.selectedSlot
                    player.inventory.selectedSlot = emptyHotbarSlot
                    val result = interactionManager.interactItem(player, Hand.MAIN_HAND)

                    return result.isAccepted
                }
            }
        }

        return false
    }

    fun throwHealingPotion(): Boolean {
        val player = mc.player ?: return false
        val interactionManager = mc.interactionManager ?: return false

        // Chercher une potion dans la hotbar
        val hotbarSlot = findPotionSlot(true)
        if (hotbarSlot != -1) {
            val previousSlot = player.inventory.selectedSlot
            player.inventory.selectedSlot = hotbarSlot

            // Sauvegarder la rotation actuelle
            val originalPitch = player.pitch
            val originalYaw = player.yaw

            // Faire regarder le joueur vers le bas
            player.pitch = 90.0f

            // Lancer la potion avec interactItem
            val result = interactionManager.interactItem(player, Hand.MAIN_HAND)

            // Restaurer la rotation
            player.pitch = originalPitch
            player.yaw = originalYaw

            // Restaurer le slot précédent
            player.inventory.selectedSlot = previousSlot

            return result.isAccepted
        }

        // Si pas dans la hotbar, déplacer depuis l'inventaire
        val inventorySlot = findPotionSlotInInventory(true)
        if (inventorySlot != -1 && inventorySlot > 8) {
            val emptyHotbarSlot = findEmptyHotbarSlot()
            if (emptyHotbarSlot != -1) {
                swapWithHotbar(inventorySlot, emptyHotbarSlot)

                // Sélectionner le slot
                val previousSlot = player.inventory.selectedSlot
                player.inventory.selectedSlot = emptyHotbarSlot

                // Sauvegarder la rotation actuelle
                val originalPitch = player.pitch
                val originalYaw = player.yaw

                // Faire regarder le joueur vers le bas
                player.pitch = 90.0f

                // Lancer la potion avec interactItem
                val result = interactionManager.interactItem(player, Hand.MAIN_HAND)

                // Restaurer la rotation
                player.pitch = originalPitch
                player.yaw = originalYaw

                // Restaurer le slot précédent
                player.inventory.selectedSlot = previousSlot

                return result.isAccepted
            }
        }

        return false
    }

    /**
     * Compte le nombre de potions de soin dans l'inventaire
     */
    fun countHealingPotions(): Int {
        val player = mc.player ?: return 0
        var count = 0

        for (i in 0 until player.inventory.size()) {
            val stack = player.inventory.getStack(i)
            if (isPotionOfHealing(stack, true)) {
                count += stack.count
            }
        }

        return count
    }

    fun moveItemToSlot(fromSlot: Int, toSlot: Int) {
        val player = mc.player ?: return
        val handler = player.currentScreenHandler

        mc.interactionManager?.clickSlot(
            handler.syncId,
            fromSlot,
            toSlot,
            SlotActionType.SWAP,
            player
        )
    }

    fun swapWithHotbar(inventorySlot: Int, hotbarSlot: Int) {
        val player = mc.player ?: return
        val handler = player.currentScreenHandler

        mc.interactionManager?.clickSlot(
            handler.syncId,
            inventorySlot,
            hotbarSlot,
            SlotActionType.SWAP,
            player
        )
    }

    fun findEmptyHotbarSlot(): Int {
        val player = mc.player ?: return -1

        for (i in 0 until 9) {
            if (player.inventory.getStack(i).isEmpty) {
                return i
            }
        }

        return -1
    }

    fun countItem(stack: ItemStack): Int {
        val player = mc.player ?: return 0
        var count = 0

        for (i in 0 until player.inventory.size()) {
            val invStack = player.inventory.getStack(i)
            if (ItemStack.areItemsEqual(stack, invStack)) {
                count += invStack.count
            }
        }

        return count
    }

    fun hasItem(stack: ItemStack): Boolean {
        val player = mc.player ?: return false

        for (i in 0 until player.inventory.size()) {
            val invStack = player.inventory.getStack(i)
            if (ItemStack.areItemsEqual(stack, invStack)) {
                return true
            }
        }

        return false
    }

    fun getHeldItem(): ItemStack {
        val player = mc.player ?: return ItemStack.EMPTY
        return player.getStackInHand(Hand.MAIN_HAND)
    }

    fun getOffhandItem(): ItemStack {
        val player = mc.player ?: return ItemStack.EMPTY
        return player.getStackInHand(Hand.OFF_HAND)
    }

    /**
     * Sélectionne un slot dans la hotbar
     */
    fun selectHotbarSlot(slot: Int) {
        val player = mc.player ?: return
        if (slot in 0..8) {
            player.inventory.selectedSlot = slot
        }
    }

    /**
     * Obtient le slot actuellement sélectionné
     */
    fun getSelectedSlot(): Int {
        val player = mc.player ?: return -1
        return player.inventory.selectedSlot
    }
}