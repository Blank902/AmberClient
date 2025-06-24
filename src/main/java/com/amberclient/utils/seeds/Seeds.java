package com.amberclient.utils.seeds;

import java.util.HashMap;

import com.amberclient.events.EventBus;
import com.amberclient.events.SeedChangedEvent;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.nbt.NbtCompound;

import com.seedfinding.mccore.version.MCVersion;
import com.amberclient.utils.general.MinecraftUtils;

public class Seeds {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Seeds INSTANCE = new Seeds();

    public HashMap<String, Seed> seeds = new HashMap<>();

    public static Seeds get() {
        return INSTANCE;
    }

    public Seed getSeed() {
        if (mc.isIntegratedServerRunning() && mc.getServer() != null) {
            MCVersion version = MCVersion.fromString(mc.getServer().getVersion());
            if (version == null)
                version = MCVersion.latest();
            return new Seed(mc.getServer().getOverworld().getSeed(), version);
        }

        return seeds.get(MinecraftUtils.getWorldName());
    }

    public void setSeed(String seed, MCVersion version) {
        if (mc.isIntegratedServerRunning()) return;

        long numSeed = toSeed(seed);
        seeds.put(MinecraftUtils.getWorldName(), new Seed(numSeed, version));
        EventBus.getInstance().post(SeedChangedEvent.get(numSeed));
    }

    public void setSeed(String seed) {
        if (mc.isIntegratedServerRunning()) return;

        ServerInfo server = mc.getCurrentServerEntry();
        MCVersion ver = null;
        if (server != null)
            ver = MCVersion.fromString(server.version.getString());
        if (ver == null) {
            String targetVer = "unknown";
            if (server != null) targetVer = server.version.getString();
            ver = MCVersion.latest();
        }
        setSeed(seed, ver);
    }

    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        seeds.forEach((key, seed) -> {
            if (seed == null) return;
            tag.put(key, seed.toTag());
        });
        return tag;
    }

    public Seeds fromTag(NbtCompound tag) {
        tag.getKeys().forEach(key -> {
            seeds.put(key, Seed.fromTag(tag.getCompound(key)));
        });
        return this;
    }

    private static long toSeed(String inSeed) {
        try {
            return Long.parseLong(inSeed);
        } catch (NumberFormatException e) {
            return inSeed.strip().hashCode();
        }
    }
}
