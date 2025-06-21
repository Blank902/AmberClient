package com.amberclient.modules.combat;

import com.amberclient.utils.module.ConfigurableModule;
import com.amberclient.utils.module.Module;
import com.amberclient.utils.module.ModuleSettings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Vector3d;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


public class AimAssist extends Module implements ConfigurableModule {

    public static final String MOD_ID = "amberclient-aimassist";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    // Settings - General
    private final ModuleSettings range = new ModuleSettings("Range", "The range at which an entity can be targeted", 5.0, 0.0, 20.0, 0.1);
    private final ModuleSettings fov = new ModuleSettings("FOV", "Will only aim entities in the fov", 360.0, 0.0, 360.0, 1.0);
    private final ModuleSettings ignoreWalls = new ModuleSettings("Ignore Walls", "Whether or not to ignore aiming through walls", false);
    private final ModuleSettings priority = new ModuleSettings("Priority", "How to filter targets within range", SortPriority.CLOSEST);
    private final ModuleSettings bodyTarget = new ModuleSettings("Aim Target", "Which part of the entities body to aim at", TargetPart.BODY);

    // Settings - Entities
    private final ModuleSettings targetsPlayers = new ModuleSettings("Target Players", "Target player entities", true);
    private final ModuleSettings targetsMobs = new ModuleSettings("Target Mobs", "Target all mobs (hostile, neutral, passive)", true);

    // Settings - Aim Speed
    private final ModuleSettings instantLook = new ModuleSettings("Instant Look", "Instantly looks at the entity", false);
    private final ModuleSettings aimSpeed = new ModuleSettings("Aim Speed", "How fast to aim at the entity", 5.0, 0.1, 20.0, 0.1);

    // Settings - Advanced
    private final ModuleSettings smoothing = new ModuleSettings("Smoothing", "Enable aim smoothing for more natural movement", true);
    private final ModuleSettings maxRotationPerTick = new ModuleSettings("Max Rotation", "Maximum rotation per tick (degrees)", 30.0, 1.0, 180.0, 1.0);

    private final MinecraftClient mc = getClient();
    private final Vector3d targetPos = new Vector3d();
    private Entity currentTarget;
    private final Set<EntityType<?>> hostileEntities = new HashSet<>();
    private final Set<EntityType<?>> neutralEntities = new HashSet<>();
    private final Set<EntityType<?>> passiveEntities = new HashSet<>();

    public enum SortPriority {
        CLOSEST,
        LOWEST_HEALTH,
        HIGHEST_HEALTH,
        ANGLE
    }

    public enum TargetPart {
        HEAD,
        BODY,
        FEET
    }

    public AimAssist() {
        super("Aim Assist", "Automatically aims at nearby entities", "Combat");

        initializeEntitySets();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (this.isEnabled()) {
                onTick();
            }
        });
    }

    @Override
    public List<ModuleSettings> getSettings() {
        return Arrays.asList(
                // General
                range, fov, ignoreWalls, priority, bodyTarget,
                // Entities
                targetsPlayers, targetsMobs,
                // Aim Speed
                instantLook, aimSpeed,
                // Advanced
                smoothing, maxRotationPerTick
        );
    }

    private void initializeEntitySets() {
        hostileEntities.addAll(Arrays.asList(
                EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.SPIDER,
                EntityType.ENDERMAN, EntityType.WITCH, EntityType.BLAZE, EntityType.GHAST,
                EntityType.WITHER_SKELETON, EntityType.STRAY, EntityType.HUSK, EntityType.DROWNED,
                EntityType.PHANTOM, EntityType.PILLAGER, EntityType.VINDICATOR, EntityType.EVOKER,
                EntityType.RAVAGER, EntityType.VEX, EntityType.GUARDIAN, EntityType.ELDER_GUARDIAN,
                EntityType.SHULKER, EntityType.SILVERFISH, EntityType.ENDERMITE, EntityType.CAVE_SPIDER,
                EntityType.SLIME, EntityType.MAGMA_CUBE, EntityType.ZOMBIFIED_PIGLIN, EntityType.PIGLIN_BRUTE,
                EntityType.HOGLIN, EntityType.ZOGLIN, EntityType.WARDEN
        ));

        neutralEntities.addAll(Arrays.asList(
                EntityType.IRON_GOLEM, EntityType.SNOW_GOLEM, EntityType.WOLF, EntityType.LLAMA,
                EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN, EntityType.PANDA, EntityType.BEE,
                EntityType.DOLPHIN, EntityType.POLAR_BEAR
        ));

        passiveEntities.addAll(Arrays.asList(
                EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.CHICKEN,
                EntityType.RABBIT, EntityType.VILLAGER, EntityType.HORSE, EntityType.DONKEY,
                EntityType.MULE, EntityType.CAT, EntityType.OCELOT, EntityType.PARROT,
                EntityType.BAT, EntityType.SQUID, EntityType.MOOSHROOM, EntityType.TURTLE,
                EntityType.COD, EntityType.SALMON, EntityType.PUFFERFISH, EntityType.TROPICAL_FISH,
                EntityType.AXOLOTL, EntityType.GLOW_SQUID, EntityType.GOAT, EntityType.ALLAY,
                EntityType.FROG, EntityType.TADPOLE, EntityType.CAMEL, EntityType.SNIFFER
        ));
    }

    @Override
    public void onEnable() {
        currentTarget = null;
        LOGGER.info("AimAssist enabled");
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        LOGGER.info("AimAssist disabled");
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        currentTarget = findTarget();

        if (currentTarget != null) {
            aimAtTarget(currentTarget);
        }
    }

    private Entity findTarget() {
        if (mc.world == null || mc.player == null) return null;

        List<Entity> entities = mc.world.getOtherEntities(mc.player,
                mc.player.getBoundingBox().expand(range.getDoubleValue()));

        Entity bestTarget = null;
        double bestScore = Double.MAX_VALUE;

        for (Entity entity : entities) {
            if (!isValidTarget(entity)) continue;

            double score = calculateTargetScore(entity);
            if (score < bestScore) {
                bestScore = score;
                bestTarget = entity;
            }
        }

        return bestTarget;
    }

    private boolean isValidTarget(Entity entity) {
        if (!(entity instanceof LivingEntity livingEntity) || !livingEntity.isAlive()) {
            return false;
        }

        if (!isTargetableEntityType(entity)) {
            return false;
        }

        double distance = mc.player.distanceTo(entity);
        if (distance > range.getDoubleValue()) {
            return false;
        }

        if (!isInFov(entity)) {
            return false;
        }

        if (!ignoreWalls.getBooleanValue() && !canSeeEntity(entity)) {
            return false;
        }

        return true;
    }

    private boolean isTargetableEntityType(Entity entity) {
        EntityType<?> type = entity.getType();

        if (type == EntityType.PLAYER) {
            return targetsPlayers.getBooleanValue();
        }

        if (hostileEntities.contains(type) || neutralEntities.contains(type) || passiveEntities.contains(type)) {
            return targetsMobs.getBooleanValue();
        }

        return false;
    }

    private boolean isInFov(Entity entity) {
        double fovValue = fov.getDoubleValue();
        if (fovValue >= 360) return true;

        Vec3d playerPos = mc.player.getPos().add(0, mc.player.getEyeHeight(mc.player.getPose()), 0);
        Vec3d entityPos = entity.getPos().add(0, entity.getHeight() / 2, 0);

        Vec3d toEntity = entityPos.subtract(playerPos).normalize();
        Vec3d playerLook = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw()).normalize();

        double angle = Math.toDegrees(Math.acos(MathHelper.clamp(playerLook.dotProduct(toEntity), -1.0, 1.0)));
        return angle <= fovValue / 2;
    }

    private boolean canSeeEntity(Entity entity) {
        if (mc.world == null || mc.player == null) return false;

        Vec3d start = mc.player.getEyePos();
        Vec3d end = entity.getPos().add(0, entity.getHeight() / 2, 0);

        return mc.world.raycast(new net.minecraft.world.RaycastContext(
                start, end,
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE,
                mc.player
        )).getType() == net.minecraft.util.hit.HitResult.Type.MISS;
    }

    private double calculateTargetScore(Entity entity) {
        double distance = mc.player.distanceTo(entity);
        SortPriority priorityValue = priority.getEnumValue();

        return switch (priorityValue) {
            case CLOSEST -> distance;
            case LOWEST_HEALTH -> entity instanceof LivingEntity living ? living.getHealth() : Double.MAX_VALUE;
            case HIGHEST_HEALTH -> entity instanceof LivingEntity living ? -living.getHealth() : Double.MAX_VALUE;
            case ANGLE -> {
                Vec3d playerPos = mc.player.getEyePos();
                Vec3d entityPos = entity.getPos().add(0, entity.getHeight() / 2, 0);
                Vec3d toEntity = entityPos.subtract(playerPos).normalize();
                Vec3d playerLook = Vec3d.fromPolar(mc.player.getPitch(), mc.player.getYaw()).normalize();
                yield Math.abs(Math.toDegrees(Math.acos(MathHelper.clamp(playerLook.dotProduct(toEntity), -1.0, 1.0))));
            }
        };
    }

    private void aimAtTarget(Entity target) {
        if (mc.player == null) return;

        calculateTargetPosition(target);

        double deltaX = targetPos.x - mc.player.getX();
        double deltaZ = targetPos.z - mc.player.getZ();
        double deltaY = targetPos.y - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));

        double targetYaw = Math.toDegrees(Math.atan2(deltaZ, deltaX)) - 90;
        double horizontalDistance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);
        double targetPitch = -Math.toDegrees(Math.atan2(deltaY, horizontalDistance));

        if (instantLook.getBooleanValue()) {
            mc.player.setYaw((float) targetYaw);
            mc.player.setPitch((float) targetPitch);
        } else {
            applyProgressiveRotation(targetYaw, targetPitch);
        }
    }

    private void applyProgressiveRotation(double targetYaw, double targetPitch) {
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        double deltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
        double deltaPitch = MathHelper.wrapDegrees(targetPitch - currentPitch);

        double speed = aimSpeed.getDoubleValue();
        double maxRotation = maxRotationPerTick.getDoubleValue();

        double yawStep = Math.min(Math.abs(deltaYaw), Math.min(speed, maxRotation)) * Math.signum(deltaYaw);
        double pitchStep = Math.min(Math.abs(deltaPitch), Math.min(speed, maxRotation)) * Math.signum(deltaPitch);

        if (smoothing.getBooleanValue()) {
            double yawDistance = Math.abs(deltaYaw);
            double pitchDistance = Math.abs(deltaPitch);

            if (yawDistance < 10) {
                yawStep *= (yawDistance / 10.0) * 0.5 + 0.5;
            }
            if (pitchDistance < 10) {
                pitchStep *= (pitchDistance / 10.0) * 0.5 + 0.5;
            }
        }

        mc.player.setYaw(currentYaw + (float) yawStep);
        mc.player.setPitch(MathHelper.clamp(currentPitch + (float) pitchStep, -90f, 90f));
    }

    private void calculateTargetPosition(Entity target) {
        Vec3d velocity = target.getVelocity();
        Vec3d predictedPos = target.getPos().add(velocity.multiply(2));

        targetPos.set(predictedPos.x, predictedPos.y, predictedPos.z);

        TargetPart targetPart = bodyTarget.getEnumValue();
        switch (targetPart) {
            case HEAD -> targetPos.add(0, target.getHeight() * 0.9, 0);
            case BODY -> targetPos.add(0, target.getHeight() * 0.5, 0);
            case FEET -> targetPos.add(0, target.getHeight() * 0.1, 0);
        }
    }

    public Entity getCurrentTarget() {
        return currentTarget;
    }
}