package com.amaro.stellarodyssey.satellites.ecology.ai;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.lifesupport.AtmosphereHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

/**
 * AI goal for planetary fauna that detects atmospheric vacuum or decompression breaches
 * and flees rapidly toward pressurized, subterranean, or shielded sectors.
 */
public class VacuumFleeGoal extends Goal {
    public static final double DEFAULT_FLEE_SPEED = 1.35;
    public static final int DEFAULT_SEARCH_RADIUS = 20;
    public static final int DEFAULT_VERTICAL_RANGE = 7;

    private final PathfinderMob mob;
    private final double fleeSpeed;
    private final int searchRadius;
    private final int verticalRange;

    private Vec3 shelterTarget = null;
    private int searchCooldown = 0;
    private int fleeTicks = 0;

    /**
     * Constructs a {@link VacuumFleeGoal} with standard panic speed and search radius.
     *
     * @param mob The entity subject to vacuum evasion behavior.
     */
    public VacuumFleeGoal(PathfinderMob mob) {
        this(mob, DEFAULT_FLEE_SPEED, DEFAULT_SEARCH_RADIUS, DEFAULT_VERTICAL_RANGE);
    }

    /**
     * Constructs a {@link VacuumFleeGoal} with custom fleeing and spatial scan parameters.
     *
     * @param mob           The entity subject to vacuum evasion behavior.
     * @param fleeSpeed     Navigation speed modifier when fleeing (typically 1.25 - 1.5).
     * @param searchRadius  Horizontal search radius in blocks for shielded shelters.
     * @param verticalRange Vertical search range in blocks.
     */
    public VacuumFleeGoal(PathfinderMob mob, double fleeSpeed, int searchRadius, int verticalRange) {
        this.mob = mob;
        this.fleeSpeed = Math.max(1.0, fleeSpeed);
        this.searchRadius = Math.max(8, searchRadius);
        this.verticalRange = Math.max(3, verticalRange);
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    /**
     * Determines whether the entity is located within an unpressurized vacuum environment.
     *
     * @param entity The entity to check.
     * @return {@code true} if in vacuum.
     */
    public static boolean isVacuumEnvironment(PathfinderMob entity) {
        if (entity == null) {
            return false;
        }
        Level level = entity.level();
        if (entity.getY() >= AtmosphereHelper.VACUUM_ALTITUDE_THRESHOLD) {
            return true;
        }
        if (level.dimensionTypeRegistration().is(AtmosphereHelper.VACUUM_DIMENSIONS)) {
            return true;
        }
        return level.dimension().identifier().getNamespace().equals(StellarOdyssey.MOD_ID);
    }

    /**
     * Checks if the given position in the world is shielded from space vacuum and solar decompression.
     * <p>
     * A sector is considered shielded or pressurized if:
     * 1. It is not open to direct vacuum sky (has an overhead solid roof or subterranean rock layer).
     * 2. It has structural lateral enclosures (cavern walls or airtight habitat bulkheads).
     *
     * @param level The level.
     * @param pos   The block position to inspect.
     * @return {@code true} if the sector provides environmental shelter.
     */
    public static boolean isSectorShielded(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return false;
        }

        // Direct exposure to open sky in vacuum environment is unshielded
        if (level.canSeeSky(pos)) {
            return false;
        }

        // Verify solid ceiling within 1 to 7 blocks above
        boolean hasOverheadCeiling = false;
        for (int dy = 1; dy <= 7; dy++) {
            BlockPos abovePos = pos.above(dy);
            BlockState state = level.getBlockState(abovePos);
            if (!state.isAir() && state.isSolidRender()) {
                hasOverheadCeiling = true;
                break;
            }
        }

        if (!hasOverheadCeiling) {
            return false;
        }

        // Verify horizontal boundary confinement (caves, structures, or trenches)
        int enclosedSides = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int dist = 1; dist <= 4; dist++) {
                BlockPos wallPos = pos.relative(dir, dist);
                BlockState state = level.getBlockState(wallPos);
                if (!state.isAir() && state.isSolidRender()) {
                    enclosedSides++;
                    break;
                }
            }
        }

        // At least 2 lateral barriers plus solid ceiling provides atmospheric pooling / shelter
        return enclosedSides >= 2;
    }

    /**
     * Evaluates a candidate shelter position and assigns a fitness score.
     * Higher score indicates superior atmospheric retention and protection.
     *
     * @param level The level.
     * @param pos   Candidate position.
     * @return Shelter quality score.
     */
    public static double scoreShelterPosition(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return -100.0;
        }

        BlockState floorState = level.getBlockState(pos.below());
        if (floorState.isAir() || !floorState.isSolidRender()) {
            return -100.0; // Unwalkable or abyss
        }

        BlockState headState = level.getBlockState(pos);
        BlockState aboveHead = level.getBlockState(pos.above());
        if (!headState.isAir() || !aboveHead.isAir()) {
            return -100.0; // Blocked headroom
        }

        if (level.canSeeSky(pos)) {
            return -50.0; // Direct vacuum radiation exposure
        }

        double score = 10.0;

        // Reward lower elevation / deeper caverns where heavy gases pool
        for (int dy = 1; dy <= 5; dy++) {
            BlockPos roofPos = pos.above(dy);
            if (level.getBlockState(roofPos).isSolidRender()) {
                score += (12.0 - dy * 2.0); // Closer ceiling = tighter enclosure
                break;
            }
        }

        // Reward enclosed cardinal walls
        int walls = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            for (int dist = 1; dist <= 4; dist++) {
                BlockPos wall = pos.relative(dir, dist);
                if (level.getBlockState(wall).isSolidRender()) {
                    walls++;
                    score += (5.0 - dist);
                    break;
                }
            }
        }
        score += walls * 6.0;

        return score;
    }

    @Override
    public boolean canUse() {
        if (searchCooldown > 0) {
            searchCooldown--;
            return false;
        }

        if (!mob.isAlive()) {
            return false;
        }

        Level level = mob.level();

        // 1. Check if the environment presents vacuum / decompression conditions
        boolean inVacuum = isVacuumEnvironment(mob);
        if (!inVacuum) {
            return false;
        }

        // 2. If already safely situated inside a shielded / pressurized pocket, no need to flee
        if (isSectorShielded(level, mob.blockPosition())) {
            return false;
        }

        // 3. Search for nearest pressurized or shielded sector
        Vec3 shelter = findShieldedShelter();
        if (shelter != null) {
            this.shelterTarget = shelter;
            return true;
        }

        // Reset cooldown before searching again to conserve server cycles
        this.searchCooldown = 15;
        return false;
    }

    @Override
    public boolean canContinueToUse() {
        if (!mob.isAlive() || shelterTarget == null) {
            return false;
        }

        if (fleeTicks > 220) { // Safety timeout: 11 seconds of fleeing
            return false;
        }

        Level level = mob.level();
        BlockPos currentPos = mob.blockPosition();

        // If arrived at destination and position is shielded, panic subsides
        if (mob.distanceToSqr(shelterTarget) < 4.0 || isSectorShielded(level, currentPos)) {
            return false;
        }

        return !mob.getNavigation().isDone();
    }

    @Override
    public void start() {
        this.fleeTicks = 0;
        if (shelterTarget != null) {
            mob.getNavigation().moveTo(shelterTarget.x, shelterTarget.y, shelterTarget.z, fleeSpeed);
        }
    }

    @Override
    public void tick() {
        fleeTicks++;

        // Re-path periodically if path was blocked or lost
        if (fleeTicks % 25 == 0 && shelterTarget != null && mob.getNavigation().isDone()) {
            Level level = mob.level();
            if (!isSectorShielded(level, mob.blockPosition())) {
                mob.getNavigation().moveTo(shelterTarget.x, shelterTarget.y, shelterTarget.z, fleeSpeed);
            }
        }

        // Agitated movement look direction
        if (shelterTarget != null) {
            mob.getLookControl().setLookAt(shelterTarget.x, shelterTarget.y, shelterTarget.z);
        }
    }

    @Override
    public void stop() {
        this.shelterTarget = null;
        this.fleeTicks = 0;
        this.searchCooldown = 20; // 1 second calmdown before next scan
    }

    /**
     * Locates the optimal shielded shelter within the configured search parameters.
     */
    protected Vec3 findShieldedShelter() {
        Level level = mob.level();

        // 1. Pathfinding-guided heuristic search using LandRandomPos scoring
        Vec3 pathableSpot = LandRandomPos.getPos(
                mob,
                searchRadius,
                verticalRange,
                pos -> scoreShelterPosition(level, pos)
        );

        if (pathableSpot != null && isSectorShielded(level, BlockPos.containing(pathableSpot))) {
            return pathableSpot;
        }

        // 2. Deterministic spiral search for nearest shielded block if pathfinder heuristic didn't find one
        BlockPos origin = mob.blockPosition();
        BlockPos bestCandidate = null;
        double bestScore = 0.0;

        for (int r = 2; r <= searchRadius; r += 2) {
            for (int dy = -verticalRange; dy <= verticalRange; dy++) {
                for (int dx = -r; dx <= r; dx += 2) {
                    for (int dz = -r; dz <= r; dz += 2) {
                        BlockPos checkPos = origin.offset(dx, dy, dz);
                        if (isSectorShielded(level, checkPos)) {
                            double score = scoreShelterPosition(level, checkPos);
                            if (score > bestScore) {
                                bestScore = score;
                                bestCandidate = checkPos;
                            }
                        }
                    }
                }
            }
            if (bestCandidate != null) {
                break; // Found closest shielded shell
            }
        }

        if (bestCandidate != null) {
            return Vec3.atBottomCenterOf(bestCandidate);
        }

        return pathableSpot;
    }

    public PathfinderMob getMob() {
        return mob;
    }

    public Vec3 getShelterTarget() {
        return shelterTarget;
    }
}
