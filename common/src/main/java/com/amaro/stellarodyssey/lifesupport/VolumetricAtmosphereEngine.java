package com.amaro.stellarodyssey.lifesupport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * High-performance 3D Volumetric Atmosphere & Hermetic Sealing Engine for Stellar Odyssey.
 * Implements bounded Flood-Fill evaluation, breach identification, and decompression force vectors.
 */
public final class VolumetricAtmosphereEngine {

    public static final int DEFAULT_MAX_VOLUME = 2048;
    public static final int DEFAULT_MAX_HORIZONTAL_RADIUS = 24;
    public static final int DEFAULT_MAX_VERTICAL_RADIUS = 16;

    public record RoomEvaluationResult(
            boolean isSealed,
            int volume,
            Set<BlockPos> interiorPositions,
            @Nullable AABB bounds,
            @Nullable BlockPos breachOrigin
    ) {
        public static final RoomEvaluationResult UNSEALED_NO_ORIGIN =
                new RoomEvaluationResult(false, 0, Collections.emptySet(), null, null);
    }

    private VolumetricAtmosphereEngine() {
    }

    /**
     * Evaluates whether a room around an origin is hermetically sealed within maximum bounds.
     * Identifies the exact breach coordinate if air escapes to the exterior.
     *
     * @param origin Starting position of the oxygen emitter/sealer.
     * @param maxVolume Maximum allowed internal volume in blocks.
     * @param maxHRadius Maximum horizontal radius from origin.
     * @param maxVRadius Maximum vertical radius from origin.
     * @param isSolidBarrier Predicate returning true if a block hermetically seals air.
     * @return RoomEvaluationResult with sealed state, bounds, and breach position if applicable.
     */
    public static RoomEvaluationResult evaluateRoom(
            BlockPos origin,
            int maxVolume,
            int maxHRadius,
            int maxVRadius,
            Predicate<BlockPos> isSolidBarrier
    ) {
        if (origin == null || isSolidBarrier == null) {
            return RoomEvaluationResult.UNSEALED_NO_ORIGIN;
        }

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> interior = new HashSet<>();

        // Seed with non-barrier neighbors
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = origin.relative(dir);
            if (!isSolidBarrier.test(neighbor)) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        if (queue.isEmpty()) {
            return RoomEvaluationResult.UNSEALED_NO_ORIGIN;
        }

        int minX = origin.getX();
        int maxX = origin.getX();
        int minY = origin.getY();
        int maxY = origin.getY();
        int minZ = origin.getZ();
        int maxZ = origin.getZ();

        BlockPos detectedBreach = null;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            interior.add(current);

            minX = Math.min(minX, current.getX());
            maxX = Math.max(maxX, current.getX());
            minY = Math.min(minY, current.getY());
            maxY = Math.max(maxY, current.getY());
            minZ = Math.min(minZ, current.getZ());
            maxZ = Math.max(maxZ, current.getZ());

            // Check distance constraints
            if (Math.abs(current.getX() - origin.getX()) > maxHRadius
                    || Math.abs(current.getZ() - origin.getZ()) > maxHRadius
                    || Math.abs(current.getY() - origin.getY()) > maxVRadius) {
                detectedBreach = current;
                break;
            }

            // Check volume limits
            if (interior.size() > maxVolume) {
                detectedBreach = current;
                break;
            }

            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (!isSolidBarrier.test(next)) {
                    if (visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
        }

        if (detectedBreach != null) {
            return new RoomEvaluationResult(false, interior.size(), Collections.unmodifiableSet(interior), null, detectedBreach);
        }

        AABB bounds = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        return new RoomEvaluationResult(true, interior.size(), Collections.unmodifiableSet(interior), bounds, null);
    }

    /**
     * Calculates the sudden physical blowout force vector exerted on an entity at a given position
     * during a hull breach or explosive decompression event.
     *
     * @param entityPos World coordinates of the entity.
     * @param breachPos Coordinates of the puncture or airlock opening.
     * @param pressureDelta Pressure differential (e.g. 1.0 atm inside vs 0.0 atm vacuum).
     * @return Physical 3D impulse vector pointing towards the breach.
     */
    public static Vec3 calculateDecompressionForce(Vec3 entityPos, BlockPos breachPos, double pressureDelta) {
        if (entityPos == null || breachPos == null || pressureDelta <= 0.0) {
            return Vec3.ZERO;
        }

        Vec3 breachCenter = new Vec3(breachPos.getX() + 0.5, breachPos.getY() + 0.5, breachPos.getZ() + 0.5);
        Vec3 direction = breachCenter.subtract(entityPos);
        double distanceSq = direction.lengthSqr();

        if (distanceSq < 0.01) {
            return Vec3.ZERO;
        }

        double distance = Math.sqrt(distanceSq);
        // Inverse square falloff with damping constant to avoid infinite velocity
        double forceMagnitude = Math.clamp((pressureDelta * 1.5) / (distanceSq + 1.0), 0.0, 1.2);

        return direction.normalize().scale(forceMagnitude);
    }
}
