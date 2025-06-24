package com.amberclient.utils.general;

import com.amberclient.events.MinecraftServerAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.EntityType;
import net.minecraft.world.chunk.Chunk;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MinecraftUtils {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public static double frameTime;

    private static final Set<EntityType<?>> HOSTILE_ENTITIES = new HashSet<>();
    private static final Set<EntityType<?>> NEUTRAL_ENTITIES = new HashSet<>();
    private static final Set<EntityType<?>> PASSIVE_ENTITIES = new HashSet<>();

    static {
        initializeEntitySets();
    }

    private static void initializeEntitySets() {
        HOSTILE_ENTITIES.addAll(Arrays.asList(
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
                EntityType.ENDERMAN, EntityType.WITCH, EntityType.BLAZE, EntityType.GHAST,
                EntityType.WITHER_SKELETON, EntityType.STRAY, EntityType.HUSK, EntityType.DROWNED,
                EntityType.PHANTOM, EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER,
                EntityType.RAVAGER, EntityType.VEX, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN,
                EntityType.SHULKER, EntityType.SILVERFISH, EntityType.ENDERMITE, EntityType.CAVE_SPIDER,
                EntityType.SLIME, EntityType.MAGMA_CUBE, EntityType.ZOMBIFIED_PIGLIN, EntityType.PIGLIN_BRUTE,
                EntityType.HOGLIN, EntityType.ZOGLIN, EntityType.WARDEN
        ));

        NEUTRAL_ENTITIES.addAll(Arrays.asList(
                EntityType.IRON_GOLEM, EntityType.SNOW_GOLEM, EntityType.WOLF, EntityType.LLAMA,
                EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.PANDA, EntityType.BEE,
                EntityType.DOLPHIN, EntityType.POLAR_BEAR
        ));

        PASSIVE_ENTITIES.addAll(Arrays.asList(
                EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN,
                EntityType.RABBIT, EntityType.VILLAGER, EntityType.HORSE, EntityType.DONKEY,
                EntityType.MULE, EntityType.CAT, EntityType.OCELOT, EntityType.PARROT,
                EntityType.BAT, EntityType.SQUID, EntityType.MOOSHROOM, EntityType.TURTLE,
                EntityType.COD, EntityType.SALMON, EntityType.PUFFERFISH, EntityType.TROPICAL_FISH,
                EntityType.AXOLOTL, EntityType.GLOW_SQUID, EntityType.GOAT, EntityType.ALLAY,
                EntityType.FROG, EntityType.TADPOLE, EntityType.CAMEL, EntityType.SNIFFER
        ));
    }

    public static Set<EntityType<?>> getHostileEntities() { return Set.copyOf(HOSTILE_ENTITIES); }
    public static Set<EntityType<?>> getNeutralEntitiesEntities() { return Set.copyOf(NEUTRAL_ENTITIES); }
    public static Set<EntityType<?>> getPassiveEntities() { return Set.copyOf(PASSIVE_ENTITIES); }

    public static boolean isHostile(EntityType<?> entityType) { return HOSTILE_ENTITIES.contains(entityType); }
    public static boolean isNeutral(EntityType<?> entityType) { return NEUTRAL_ENTITIES.contains(entityType); }
    public static boolean isPassive(EntityType<?> entityType) { return PASSIVE_ENTITIES.contains(entityType); }

    public static boolean isMob(EntityType<?> entityType) { return isHostile(entityType) ||isNeutral(entityType) || isPassive(entityType); }

    public static String getEntityCategory(EntityType<?> entityType) {
        if (entityType == EntityType.PLAYER)
            return "Player";
        else if (isHostile(entityType))
            return "Hostile";
        else if (isNeutral(entityType))
            return "Neutral";
        else if (isPassive(entityType))
            return "Passive";
        else
            return "Unknown";
    }

    public static String getWorldName() {
        // Singleplayer
        if (mc.isInSingleplayer()) {
            if (mc.world == null) return "";
            if (mc.getServer() == null) return "FAILED_BECAUSE_LEFT_WORLD";

            File folder = ((MinecraftServerAccessor) mc.getServer()).getSession().getWorldDirectory(mc.world.getRegistryKey()).toFile();
            if (folder.toPath().relativize(mc.runDirectory.toPath()).getNameCount() != 2) {
                folder = folder.getParentFile();
            }
            return folder.getName();
        }

        // Multiplayer
        if (mc.getCurrentServerEntry() != null) {
            return mc.getCurrentServerEntry().isRealm() ? "realms" : mc.getCurrentServerEntry().address;
        }

        return "";
    }
}
