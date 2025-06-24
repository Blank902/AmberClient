package com.amberclient.modules.render.xray;

import com.amberclient.utils.module.Module;
import com.amberclient.utils.module.ConfigurableModule;
import com.amberclient.utils.module.ModuleSettings;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.ChunkPos;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import java.util.List;
import java.util.ArrayList;

public class Xray extends Module implements ConfigurableModule {
    public static final String MOD_ID = "amberclient-xray";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    private ChunkPos lastPlayerChunk;
    private boolean wasActive = false;

    private final ModuleSettings chunkRadius;
    private final ModuleSettings exposedOnly;
    private final ModuleSettings showLava;
    private final ModuleSettings oreSim;
    private final List<ModuleSettings> settings;

    public Xray() {
        super("XRay", "Shows the outlines of the selected ores in the chunk", "Render");

        // Settings
        chunkRadius = new ModuleSettings("Chunk Radius", "Number of chunks to scan around the player", 1.0, 1.0, 8.0, 1.0);
        exposedOnly = new ModuleSettings("Exposed Only", "Show only ores exposed to air", false);
        showLava = new ModuleSettings("Show Lava", "Show lava with the ores", false);
        oreSim = new ModuleSettings("OreSim", "Use world seed to predict ore generation instead of scanning existing blocks", false);

        settings = new ArrayList<>();
        settings.add(chunkRadius);
        settings.add(exposedOnly);
        settings.add(showLava);
        settings.add(oreSim);

        WorldRenderEvents.AFTER_TRANSLUCENT.register(RenderOutlines::render);
    }

    @Override
    public void onEnable() {
        SettingsStore.getInstance().get().setActive(true);
        SettingsStore.getInstance().get().setExposedOnly(exposedOnly.getBooleanValue());
        SettingsStore.getInstance().get().setShowLava(showLava.getBooleanValue());
        SettingsStore.getInstance().get().setHalfRange((int) chunkRadius.getDoubleValue());
        SettingsStore.getInstance().get().setOreSim(oreSim.getBooleanValue());

        ScanTask.resetLocationTracking();

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player != null) {
            ScanTask.runTask(client.player.getChunkPos(), SettingsStore.getInstance().get().getHalfRange(), true);
            lastPlayerChunk = null;
        }
        wasActive = true;
    }

    @Override
    public void onDisable() {
        SettingsStore.getInstance().get().setActive(false);
        ScanTask.renderQueue.clear();
        lastPlayerChunk = null;
        wasActive = false;

        OreSim.chunkRenderers.clear();
        OreSim.oreConfig = null;
    }

    @Override
    public void onTick() {
        if (SettingsStore.getInstance().get().isActive()) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player == null) return;

            ChunkPos currentChunk = client.player.getChunkPos();
            if (!currentChunk.equals(lastPlayerChunk) || !wasActive) {
                ScanTask.runTask(currentChunk, SettingsStore.getInstance().get().getHalfRange());
                lastPlayerChunk = currentChunk;
                wasActive = true;
            }
        }
    }

    @Override
    public List<ModuleSettings> getSettings() {
        return settings;
    }

    @Override
    public void onSettingChanged(ModuleSettings setting) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        if (setting == exposedOnly) {
            SettingsStore.getInstance().get().setExposedOnly(exposedOnly.getBooleanValue());
            client.player.sendMessage(
                    Text.literal("§6Exposed Only: §l" + (exposedOnly.getBooleanValue() ? "ON" : "OFF")),
                    true
            );
            if (SettingsStore.getInstance().get().isActive()) {
                ScanTask.runTask(client.player.getChunkPos(), SettingsStore.getInstance().get().getHalfRange(), true);
            }
        } else if (setting == chunkRadius) {
            SettingsStore.getInstance().get().setHalfRange((int) chunkRadius.getDoubleValue());
            client.player.sendMessage(
                    Text.literal("§6Chunk Range: §l" + (int) chunkRadius.getDoubleValue() + " chunks"),
                    true
            );
            if (SettingsStore.getInstance().get().isActive()) {
                ScanTask.runTask(client.player.getChunkPos(), SettingsStore.getInstance().get().getHalfRange(), true);
            }
        } else if (setting == showLava) {
            SettingsStore.getInstance().get().setShowLava(showLava.getBooleanValue());
            client.player.sendMessage(
                    Text.literal("§6Show Lava: §l" + (showLava.getBooleanValue() ? "ON" : "OFF")),
                    true
            );
            if (SettingsStore.getInstance().get().isActive()) {
                ScanTask.runTask(client.player.getChunkPos(), SettingsStore.getInstance().get().getHalfRange(), true);
            }
        } else if (setting == oreSim) { // Nouveau paramètre
            SettingsStore.getInstance().get().setOreSim(oreSim.getBooleanValue());
            client.player.sendMessage(
                    Text.literal("§6OreSim: §l" + (oreSim.getBooleanValue() ? "ON" : "OFF")),
                    true
            );
            if (SettingsStore.getInstance().get().isActive()) {
                ScanTask.runTask(client.player.getChunkPos(), SettingsStore.getInstance().get().getHalfRange(), true);
            }
        }
    }
}