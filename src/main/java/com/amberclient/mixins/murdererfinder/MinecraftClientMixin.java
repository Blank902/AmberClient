package com.amberclient.mixins.murdererfinder;

import com.amberclient.utils.general.MinecraftUtils;
import com.amberclient.utils.murdererfinder.MurdererFinder;
import com.amberclient.mixins.accessors.ArmorStandEntityMixinAccessor;
import com.amberclient.mixins.accessors.PlayerEntityMixinAccessor;
import com.amberclient.mixins.accessors.EntityMixinAccessor;
import com.amberclient.utils.murdererfinder.config.Config;
import com.amberclient.utils.murdererfinder.config.ConfigManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.DownloadingTerrainScreen;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(at = @At("HEAD"), method = "joinWorld")
    private void onJoinWorld(ClientWorld world, DownloadingTerrainScreen.WorldEntryReason worldEntryReason, CallbackInfo ci) {
        if (!MurdererFinder.onHypixel) {
            ServerInfo entry = ((MinecraftClient)(Object)this).getCurrentServerEntry();
            if (entry != null) {
                MurdererFinder.onHypixel = entry.address.contains("hypixel");
                if (MurdererFinder.onHypixel)
                    MinecraftUtils.sendChatMessage("§4[AmberClient] " + Formatting.RED + Text.translatable("message.mm.is_mm_lobby").getString());
            }
        }
    }

    @Inject(at = @At("HEAD"), method = "setWorld")
    private void onSetWorld(ClientWorld world, CallbackInfo info) {
        MurdererFinder.setCurrentLobby(MurdererFinder.HypixelLobbies.None);
    }

    @Inject(at = @At("HEAD"), method = "hasOutline", cancellable = true)
    private void onHasOutline(Entity entity, CallbackInfoReturnable<Boolean> info) {
        if (MurdererFinder.isActive()) {
            Config config = ConfigManager.getConfig();

            if (entity instanceof PlayerEntity) {
                if (((PlayerEntityMixinAccessor) entity).isRealPlayer())
                    if (!ConfigManager.getConfig().getMm().shouldHighlightSpectators() && ((PlayerEntityMixinAccessor) entity).isDeadSpectator())
                        ((EntityMixinAccessor) entity).setGlowColor(-1);
                    else if (config.getMm().shouldHighlightMurders() && ((PlayerEntityMixinAccessor) entity).isMurder()) {
                        ((EntityMixinAccessor) entity).setGlowColor(Config.MurderMystery.murderTeamColorValue);
                        info.setReturnValue(true);
                    } else if (config.getMm().shouldHighlightDetectives(MurdererFinder.clientIsMurder) && ((PlayerEntityMixinAccessor) entity).hasBow()) {
                        ((EntityMixinAccessor) entity).setGlowColor(Config.MurderMystery.detectiveTeamColorValue);
                        info.setReturnValue(true);
                    } else if (config.getMm().shouldHighlightInnocents(MurdererFinder.clientIsMurder))
                        info.setReturnValue(true);
            } else if (entity instanceof ItemEntity) {
                if (config.getMm().shouldHighlightGold())
                    if (((ItemEntity)entity).getStack().getItem() == Items.GOLD_INGOT) {
                        ((EntityMixinAccessor)entity).setGlowColor(Config.MurderMystery.goldTeamColorValue);
                        info.setReturnValue(true);
                    }
            } else if (entity instanceof ArmorStandEntity) {
                if (config.getMm().shouldHighlightBows())
                    if (((ArmorStandEntityMixinAccessor)entity).isHoldingDetectiveBow()) {
                        ((EntityMixinAccessor)entity).setGlowColor(Config.MurderMystery.bowTeamColorValue);
                        info.setReturnValue(true);
                    }
            }
        }
    }
}
