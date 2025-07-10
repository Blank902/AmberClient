package com.amberclient.modules.player;

import com.amberclient.utils.module.ConfigurableModule;
import com.amberclient.utils.module.Module;
import com.amberclient.utils.module.ModuleSettings;
import com.amberclient.events.player.SendMovementPacketsEvent;
import com.amberclient.events.network.PacketEvent;
import com.amberclient.mixins.PlayerMoveC2SPacketAccessor;
import com.amberclient.events.core.EventHandler;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

public class AntiHunger extends Module implements ConfigurableModule {
    private final ModuleSettings sprintSetting = new ModuleSettings("Sprint", "Spoofs sprinting packets.", true);
    private final ModuleSettings onGroundSetting = new ModuleSettings("On Ground", "Spoofs the onGround flag.", true);

    private boolean lastOnGround, ignorePacket;

    public AntiHunger() {
        super("AntiHunger", "Reduces (does NOT remove) hunger consumption.", "Player");
    }

    @Override
    public List<ModuleSettings> getSettings() {
        List<ModuleSettings> settings = new ArrayList<>();
        settings.add(sprintSetting);
        settings.add(onGroundSetting);
        return settings;
    }

    @Override
    protected void onEnable() {
        super.onEnable();
        lastOnGround = client.player.isOnGround();
    }

    @EventHandler
    private void onSendPacket(PacketEvent.Send event) {
        if (ignorePacket && event.packet instanceof PlayerMoveC2SPacket) {
            ignorePacket = false;
            return;
        }
        if (client.player.hasVehicle() || client.player.isTouchingWater() || client.player.isSubmergedInWater()) return;
        if (event.packet instanceof ClientCommandC2SPacket packet && sprintSetting.getBooleanValue()) {
            if (packet.getMode() == ClientCommandC2SPacket.Mode.START_SPRINTING) event.cancel();
        }
        if (event.packet instanceof PlayerMoveC2SPacket packet && onGroundSetting.getBooleanValue() && client.player.isOnGround() && client.player.fallDistance <= 0.0 && !client.interactionManager.isBreakingBlock()) {
            ((PlayerMoveC2SPacketAccessor) packet).setOnGround(false);
        }
    }

    @EventHandler
    private void onTick(SendMovementPacketsEvent.Pre event) {
        if (client.player.isOnGround() && !lastOnGround && onGroundSetting.getBooleanValue()) {
            ignorePacket = true;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.player.sendMessage(Text.literal("§4test"), true);
        lastOnGround = client.player.isOnGround();
    }
}