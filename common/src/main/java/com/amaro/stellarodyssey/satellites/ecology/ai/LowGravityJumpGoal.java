package com.amaro.stellarodyssey.satellites.ecology.ai;

import com.amaro.stellarodyssey.lifesupport.AtmosphereHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * Pathfinder Goal for alien fauna navigating low-gravity exoplanet terrain.
 * <p>
 * Employs high-arc leaping and mid-air aerodynamic/biomechanical impulse control to negotiate
 * craters, canyons, steep ledges, and rough extraterrestrial topographies.
 */
public class LowGravityJumpGoal extends Goal {
    public static final double DEFAULT_LEAP_STRENGTH = 1.05;
    public static final double DEFAULT_MAX_HORIZONTAL_RANGE = 18.0;
    public static final int DEFAULT_LEAP_COOLDOWN = 35;

    private final PathfinderMob mob;
    private final double leapStrength;
    private final double maxHorizontalRange;
    private final int leapCooldown;

    private int cooldownTicks = 0;
    private int airTicks = 0;
    private Vec3 targetWaypoint = null;
    private boolean hasLaunched = false;

    /**
     * Constructs a {@link LowGravityJumpGoal} with default leaps and cooldown settings.
     *
     * @param mob The alien pathfinder mob.
     */
    public LowGravityJumpGoal(PathfinderMob mob) {
        this(mob, DEFAULT_LEAP_STRENGTH, DEFAULT_MAX_HORIZONTAL_RANGE, DEFAULT_LEAP_COOLDOWN);
    }

    /**
     * Constructs a {@link LowGravityJumpGoal} with customizable parameters.
     *
     * @param mob                The alien pathfinder mob.
     * @param leapStrength       Multiplier for vertical and horizontal jump impulse.
     * @param maxHorizontalRange Maximum distance to target waypoint for leaping.
     * @param leapCooldown       Ticks between consecutive jumps.
     */
    public LowGravityJumpGoal(PathfinderMob mob, double leapStrength, double maxHorizontalRange, int leapCooldown) {
        this.mob = mob;
        this.leapStrength = Math.max(0.4, leapStrength);
        this.maxHorizontalRange = Math.max(4.0, maxHorizontalRange);
        this.leapCooldown = Math.max(10, leapCooldown);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP));
    }

    /**
     * Determines whether the entity is in a low-gravity environment or exoplanet terrain.
     *
     * @param entity The entity to check.
     * @return {@code true} if low gravity physics apply.
     */
    public static boolean isLowGravityEnvironment(LivingEntity entity) {
        if (entity == null) {
            return false;
        }

        AttributeInstance gravity = entity.getAttribute(Attributes.GRAVITY);
        if (gravity != null && gravity.getValue() < 0.075) {
            return true;
        }

        Level level = entity.level();
        return entity.getY() >= AtmosphereHelper.VACUUM_ALTITUDE_THRESHOLD
                || level.dimensionTypeRegistration().is(AtmosphereHelper.VACUUM_DIMENSIONS);
    }

    @Override
    public boolean canUse() {
        if (cooldownTicks > 0) {
            cooldownTicks--;
            return false;
        }

        if (!mob.isAlive() || !mob.onGround() || mob.isInWater() || mob.isInLava()) {
            return false;
        }

        // Alien fauna are especially adapted to leap in low-gravity or rugged exoplanet terrain
        boolean lowGrav = isLowGravityEnvironment(mob);

        // 1. Check for active combat target
        LivingEntity target = mob.getTarget();
        if (target != null && target.isAlive()) {
            double distSqr = mob.distanceToSqr(target);
            double minDist = 3.0 * 3.0;
            double maxDist = maxHorizontalRange * maxHorizontalRange;

            if (distSqr >= minDist && distSqr <= maxDist) {
                double dy = target.getY() - mob.getY();
                // Prefer leaping when there is an elevation difference, low gravity, or clear distance
                if (lowGrav || dy > 1.2 || mob.getRandom().nextFloat() < 0.65f) {
                    this.targetWaypoint = target.position();
                    return true;
                }
            }
        }

        // 2. Check for active navigation path with elevation climb or gap
        Path path = mob.getNavigation().getPath();
        if (path != null && !path.isDone()) {
            int nextIdx = path.getNextNodeIndex();
            int nodeCount = path.getNodeCount();

            // Look 2 to 4 nodes ahead for elevation leaps or chasm traversals
            for (int i = nextIdx; i < Math.min(nodeCount, nextIdx + 4); i++) {
                Node node = path.getNode(i);
                double dy = node.y - mob.getY();
                double dx = node.x + 0.5 - mob.getX();
                double dz = node.z + 0.5 - mob.getZ();
                double horizDistSqr = dx * dx + dz * dz;

                if (dy >= 1.25 || (lowGrav && horizDistSqr >= 9.0)) {
                    this.targetWaypoint = new Vec3(node.x + 0.5, node.y, node.z + 0.5);
                    return true;
                }
            }
        }

        // 3. Spontaneous terrain exploration leaps in low gravity
        if (lowGrav && mob.getRandom().nextFloat() < 0.05f) {
            Vec3 forward = mob.getForward().scale(mob.getRandom().nextDouble() * 6.0 + 4.0);
            BlockPos targetBlock = mob.blockPosition().offset(
                    (int) Math.round(forward.x),
                    mob.getRandom().nextInt(3),
                    (int) Math.round(forward.z)
            );

            if (!mob.level().getBlockState(targetBlock).isAir()
                    && mob.level().getBlockState(targetBlock.above()).isAir()) {
                this.targetWaypoint = Vec3.atBottomCenterOf(targetBlock.above());
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (!mob.isAlive()) {
            return false;
        }

        if (airTicks > 70) { // Safety timeout in case of getting stuck in air/cobwebs
            return false;
        }

        // Once launched, continue until landed on solid ground
        if (hasLaunched && mob.onGround() && airTicks > 4) {
            return false;
        }

        return true;
    }

    @Override
    public void start() {
        this.hasLaunched = false;
        this.airTicks = 0;

        if (targetWaypoint == null) {
            return;
        }

        // Orient mob facing the jump target
        mob.getLookControl().setLookAt(targetWaypoint.x, targetWaypoint.y, targetWaypoint.z);

        Vec3 mobPos = mob.position();
        Vec3 diff = targetWaypoint.subtract(mobPos);
        double horizDist = Math.sqrt(diff.x * diff.x + diff.z * diff.z);
        if (horizDist < 0.1) {
            horizDist = 0.1;
        }

        AttributeInstance gravAttr = mob.getAttribute(Attributes.GRAVITY);
        double effectiveGravity = gravAttr != null ? gravAttr.getValue() : 0.08;

        // Controlled horizontal velocity: scales with distance, bounded by leapStrength
        double maxSpeed = Math.min(1.4, leapStrength * 0.95);
        double hSpeed = Math.min(maxSpeed, Math.max(0.35, horizDist * 0.24));
        double vx = (diff.x / horizDist) * hSpeed;
        double vz = (diff.z / horizDist) * hSpeed;

        // Controlled vertical impulse: calculates parabolic arc required to clear height
        double dy = diff.y;
        double flightTicksEst = Math.max(8.0, Math.min(32.0, horizDist / Math.max(0.2, hSpeed)));
        double requiredVy = (dy + 0.5 * effectiveGravity * flightTicksEst * flightTicksEst * 0.25) / (flightTicksEst * 0.5);

        // Clamp to prevent flinging into the stratosphere while enabling high leaps
        double vy = Math.max(0.48, Math.min(1.45, requiredVy * leapStrength));

        mob.setDeltaMovement(new Vec3(vx, vy, vz));
        mob.getJumpControl().jump();
    }

    @Override
    public void tick() {
        airTicks++;

        if (!mob.onGround()) {
            hasLaunched = true;
        }

        // Mid-air impulse control: micro-corrections and descent dampening
        if (hasLaunched && !mob.onGround() && targetWaypoint != null) {
            Vec3 currentVel = mob.getDeltaMovement();
            Vec3 remaining = targetWaypoint.subtract(mob.position());
            double remHoriz = Math.sqrt(remaining.x * remaining.x + remaining.z * remaining.z);

            if (remHoriz > 0.4) {
                // Subtle horizontal impulse steering towards landing target
                double steerWeight = 0.028;
                double desiredVx = (remaining.x / remHoriz) * Math.min(0.65, remHoriz * 0.2);
                double desiredVz = (remaining.z / remHoriz) * Math.min(0.65, remHoriz * 0.2);

                double newVx = currentVel.x + (desiredVx - currentVel.x) * steerWeight;
                double newVz = currentVel.z + (desiredVz - currentVel.z) * steerWeight;
                double newVy = currentVel.y;

                // Retro-impulse / descent stabilization when nearing target
                if (currentVel.y < -0.35 && remHoriz < 2.5) {
                    newVy = currentVel.y * 0.88; // Aerodynamic soft touchdown deceleration
                }

                mob.setDeltaMovement(new Vec3(newVx, newVy, newVz));
            }

            // Dampen fall distance calculation to reflect low-gravity skeletal adaptations
            mob.fallDistance = Math.min(mob.fallDistance, 1.2F);
        }
    }

    @Override
    public void stop() {
        this.cooldownTicks = leapCooldown;
        this.hasLaunched = false;
        this.airTicks = 0;
        this.targetWaypoint = null;
        this.mob.fallDistance = 0.0F; // Soft touchdown complete
    }

    public PathfinderMob getMob() {
        return mob;
    }

    public double getLeapStrength() {
        return leapStrength;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }
}
