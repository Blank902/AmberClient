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
    private final ModuleSettings aimSpeed = new ModuleSettings("Aim Speed", "Base aim speed multiplier", 3.5, 0.1, 15.0, 0.1);

    // Settings - Advanced
    private final ModuleSettings smoothingIntensity = new ModuleSettings("Smoothing Intensity", "How smooth the aim movement is", 0.75, 0.1, 2.0, 0.05);
    private final ModuleSettings maxRotationPerTick = new ModuleSettings("Max Rotation", "Maximum rotation per tick (degrees)", 25.0, 1.0, 90.0, 1.0);
    private final ModuleSettings accelerationFactor = new ModuleSettings("Acceleration", "How quickly aim accelerates", 1.2, 0.5, 3.0, 0.1);

    private final MinecraftClient mc = getClient();
    private final Vector3d targetPos = new Vector3d();
    private Entity currentTarget;
    private final Set<EntityType<?>> hostileEntities = new HashSet<>();
    private final Set<EntityType<?>> neutralEntities = new HashSet<>();
    private final Set<EntityType<?>> passiveEntities = new HashSet<>();

    private float lastYawDelta = 0.0f;
    private float lastPitchDelta = 0.0f;
    private double currentYawVelocity = 0.0;
    private double currentPitchVelocity = 0.0;
    private int ticksSinceTargetChange = 0;

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
        super("Aim Assist", "Automatically aims at nearby entities with smooth movement", "Combat");

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
                range, fov, ignoreWalls, priority, bodyTarget,

                targetsPlayers, targetsMobs,

                instantLook, aimSpeed,

                smoothingIntensity, maxRotationPerTick, accelerationFactor
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
        resetSmoothingVariables();
        LOGGER.info("AimAssist enabled");
    }

    @Override
    public void onDisable() {
        currentTarget = null;
        resetSmoothingVariables();
        LOGGER.info("AimAssist disabled");
    }

    private void resetSmoothingVariables() {
        lastYawDelta = 0.0f;
        lastPitchDelta = 0.0f;
        currentYawVelocity = 0.0;
        currentPitchVelocity = 0.0;
        ticksSinceTargetChange = 0;
    }

    @Override
    public void onTick() {
        if (mc.player == null || mc.world == null) return;

        Entity previousTarget = currentTarget;
        currentTarget = findTarget();

        if (currentTarget != previousTarget) {
            resetSmoothingVariables();
        }

        if (currentTarget != null) {
            aimAtTarget(currentTarget);
            ticksSinceTargetChange++;
        } else {
            applyDeceleration();
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
            resetSmoothingVariables();
        } else {
            applyAdvancedSmoothRotation(targetYaw, targetPitch);
        }
    }

    private void applyAdvancedSmoothRotation(double targetYaw, double targetPitch) {
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        double deltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
        double deltaPitch = MathHelper.wrapDegrees(targetPitch - currentPitch);

        double baseSpeed = aimSpeed.getDoubleValue();
        double smoothing = smoothingIntensity.getDoubleValue();
        double acceleration = accelerationFactor.getDoubleValue();
        double maxRotation = maxRotationPerTick.getDoubleValue();

        double accelerationMultiplier = Math.min(1.0, (ticksSinceTargetChange * 0.1) * acceleration);

        double yawDistance = Math.abs(deltaYaw);
        double pitchDistance = Math.abs(deltaPitch);

        double yawSpeedFactor = calculateSpeedFactor(yawDistance);
        double pitchSpeedFactor = calculateSpeedFactor(pitchDistance);

        double targetYawVelocity = Math.min(yawDistance * yawSpeedFactor * baseSpeed * accelerationMultiplier, maxRotation);
        double targetPitchVelocity = Math.min(pitchDistance * pitchSpeedFactor * baseSpeed * accelerationMultiplier, maxRotation);

        currentYawVelocity = lerp(currentYawVelocity, targetYawVelocity, smoothing);
        currentPitchVelocity = lerp(currentPitchVelocity, targetPitchVelocity, smoothing);

        double yawStep = Math.min(Math.abs(deltaYaw), currentYawVelocity) * Math.signum(deltaYaw);
        double pitchStep = Math.min(Math.abs(deltaPitch), currentPitchVelocity) * Math.signum(deltaPitch);

        yawStep += (Math.random() - 0.5) * 0.1 * smoothing;
        pitchStep += (Math.random() - 0.5) * 0.1 * smoothing;

        mc.player.setYaw(currentYaw + (float) yawStep);
        mc.player.setPitch(MathHelper.clamp(currentPitch + (float) pitchStep, -90f, 90f));

        lastYawDelta = (float) yawStep;
        lastPitchDelta = (float) pitchStep;
    }

    private double calculateSpeedFactor(double distance) {
        if (distance > 45) {
            return 1.0;
        } else if (distance > 15) {
            return 0.8;
        } else if (distance > 5) {
            return 0.4;
        } else {
            return 0.2;
        }
    }

    private void applyDeceleration() {
        currentYawVelocity *= 0.85;
        currentPitchVelocity *= 0.85;

        if (Math.abs(currentYawVelocity) > 0.1 || Math.abs(currentPitchVelocity) > 0.1) {
            float currentYaw = mc.player.getYaw();
            float currentPitch = mc.player.getPitch();

            mc.player.setYaw(currentYaw + (float) (currentYawVelocity * Math.signum(lastYawDelta)));
            mc.player.setPitch(MathHelper.clamp(currentPitch + (float) (currentPitchVelocity * Math.signum(lastPitchDelta)), -90f, 90f));
        }
    }

    private double lerp(double start, double end, double factor) {
        return start + (end - start) * factor;
    }

    private void calculateTargetPosition(Entity target) {
        Vec3d velocity = target.getVelocity();

        double distance = mc.player.distanceTo(target);
        double predictionTime = distance / 20.0;
        Vec3d predictedPos = target.getPos().add(velocity.multiply(predictionTime));

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