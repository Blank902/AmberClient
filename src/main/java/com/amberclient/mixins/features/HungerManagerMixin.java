package com.amberclient.mixins.features;

import com.amberclient.modules.player.AntiHunger;
import net.minecraft.entity.player.HungerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HungerManager.class)
public class HungerManagerMixin {

    @Shadow
    private int foodLevel;

    @Shadow
    private float saturationLevel;

    @Shadow
    private float exhaustion;

    private int lastFoodLevel = 20;
    private float lastSaturationLevel = 5.0f;

    @Inject(method = "update", at = @At("HEAD"))
    private void onUpdateStart(ServerPlayerEntity player, CallbackInfo ci) {
        if (!AntiHunger.isModuleEnabled()) {  // Changed method name
            return;
        }

        // Save values before update
        this.lastFoodLevel = this.foodLevel;
        this.lastSaturationLevel = this.saturationLevel;
    }

    @Inject(method = "update", at = @At("TAIL"))
    private void onUpdateEnd(ServerPlayerEntity player, CallbackInfo ci) {
        if (!AntiHunger.isModuleEnabled()) {  // Changed method name
            return;
        }

        // Check for hunger loss and apply reduction
        if (this.foodLevel < this.lastFoodLevel) {
            int hungerLoss = this.lastFoodLevel - this.foodLevel;
            float reducedLoss = hungerLoss * (float) AntiHunger.getHungerReductionFactor();

            int newFoodLevel = (int) (this.foodLevel + reducedLoss);
            this.foodLevel = Math.min(Math.max(newFoodLevel, 0), 20);

            AntiHunger.showHungerReductionNotification(hungerLoss, reducedLoss);
        }

        // Check for saturation loss and apply reduction
        if (this.saturationLevel < this.lastSaturationLevel) {
            float saturationLoss = this.lastSaturationLevel - this.saturationLevel;
            float reducedLoss = saturationLoss * (float) AntiHunger.getSaturationReductionFactor();

            float newSaturationLevel = this.saturationLevel + reducedLoss;
            this.saturationLevel = Math.min(Math.max(newSaturationLevel, 0.0f), 20.0f);
        }
    }

    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    private void onAddExhaustion(float exhaustion, CallbackInfo ci) {
        if (!AntiHunger.isModuleEnabled()) {  // Changed method name
            return;
        }

        // Reduce the added exhaustion
        float reducedExhaustion = exhaustion * (1.0f - (float) AntiHunger.getHungerReductionFactor());

        // Manually add reduced exhaustion
        this.exhaustion += reducedExhaustion;

        // Cancel the original call
        ci.cancel();
    }
}