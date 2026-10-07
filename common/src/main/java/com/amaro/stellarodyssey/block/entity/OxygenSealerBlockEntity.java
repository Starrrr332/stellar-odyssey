package com.amaro.stellarodyssey.block.entity;

import com.amaro.stellarodyssey.block.OxygenSealerBlock;
import com.amaro.stellarodyssey.lifesupport.AtmosphereHelper;
import com.amaro.stellarodyssey.registry.ModBlockEntityTypes;
import com.amaro.stellarodyssey.registry.ModSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Block entity for the Oxygen Sealer.
 * <p>
 * Uses a server-side 3D Breadth-First Search (BFS) flood-fill to determine whether the surrounding
 * room is hermetically sealed within maximum volume and radius bounds.
 * Active sealed bounds are registered with {@link AtmosphereHelper} so players inside
 * breathe safe pressurized air without depleting oxygen tanks.
 * </p>
 */
public class OxygenSealerBlockEntity extends BlockEntity {
    public static final int MAX_VOLUME = 1024;
    public static final int MAX_HORIZONTAL_RADIUS = 16;
    public static final int MAX_VERTICAL_RADIUS = 10;
    public static final int CHECK_INTERVAL_TICKS = 40;

    private boolean sealed = false;
    private int volume = 0;
    private Set<BlockPos> interiorPositions = Collections.emptySet();
    private @Nullable AABB bounds = null;
    private int tickCounter = 0;

    public record SealedRoomResult(boolean sealed, int volume, Set<BlockPos> interiorPositions, @Nullable AABB bounds) {
        public static final SealedRoomResult UNSEALED = new SealedRoomResult(false, 0, Collections.emptySet(), null);
    }

    public OxygenSealerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.OXYGEN_SEALER.get(), pos, state);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        AtmosphereHelper.unregisterSealer(this);
    }

    @Override
    public void clearRemoved() {
        super.clearRemoved();
        AtmosphereHelper.registerSealer(this);
    }

    public boolean isSealed() {
        return this.sealed;
    }

    public int getVolume() {
        return this.volume;
    }

    public Set<BlockPos> getInteriorPositions() {
        return this.interiorPositions;
    }

    public @Nullable AABB getBounds() {
        return this.bounds;
    }

    /**
     * Checks if a world position lies within this sealer's active sealed room.
     */
    public boolean containsPos(BlockPos target) {
        if (!this.sealed || this.bounds == null) {
            return false;
        }
        if (!this.bounds.contains(target.getX() + 0.5, target.getY() + 0.5, target.getZ() + 0.5)) {
            return false;
        }
        return this.interiorPositions.contains(target);
    }

    /**
     * Periodic server-side ticker. Re-evaluates room seal every {@link #CHECK_INTERVAL_TICKS} ticks.
     */
    public static void serverTick(Level level, BlockPos pos, BlockState state, OxygenSealerBlockEntity sealer) {
        sealer.tickCounter++;
        if (sealer.tickCounter >= CHECK_INTERVAL_TICKS) {
            sealer.tickCounter = 0;
            sealer.checkSeal(level, pos, state);
        }
    }

    /**
     * Evaluates room seal against the live Minecraft level.
     */
    public void checkSeal(Level level, BlockPos pos, BlockState state) {
        AtmosphereHelper.registerSealer(this);

        Predicate<BlockPos> barrierPredicate = p -> {
            if (p.equals(pos)) {
                return true;
            }
            return isBarrierBlock(level, p, level.getBlockState(p));
        };

        SealedRoomResult result = calculateSealedRoom(pos, MAX_VOLUME, MAX_HORIZONTAL_RADIUS, MAX_VERTICAL_RADIUS, barrierPredicate);

        boolean wasSealed = this.sealed;
        boolean stateChanged = (this.sealed != result.sealed()) || (this.volume != result.volume());
        this.sealed = result.sealed();
        this.volume = result.volume();
        this.interiorPositions = result.interiorPositions();
        this.bounds = result.bounds();

        if (state.hasProperty(OxygenSealerBlock.SEALED) && state.getValue(OxygenSealerBlock.SEALED) != this.sealed) {
            level.setBlock(pos, state.setValue(OxygenSealerBlock.SEALED, this.sealed), Block.UPDATE_CLIENTS);
        }

        // Habitat acoustics: a pressurization hiss when a room is successfully sealed, and the
        // airlock decompression cycle when an existing seal is breached. Mirrors the pressure
        // cues of Galacticraft's Oxygen Sealer so sealing/unsealing is audible to the player.
        if (wasSealed != this.sealed && level instanceof ServerLevel serverLevel) {
            if (this.sealed) {
                serverLevel.playSound(null, pos, ModSoundEvents.OXYGEN_SEALER_PRESSURIZE.get(),
                        SoundSource.BLOCKS, 0.9F, 1.0F);
            } else {
                serverLevel.playSound(null, pos, ModSoundEvents.AIRLOCK_CYCLE.get(),
                        SoundSource.BLOCKS, 0.7F, 1.0F);
            }
        }

        if (stateChanged) {
            setChanged();
        }
    }

    /**
     * Pure 3D BFS flood-fill algorithm for hermetic room seal detection.
     */
    public static SealedRoomResult calculateSealedRoom(
            BlockPos origin,
            int maxVolume,
            int maxHorizontalRadius,
            int maxVerticalRadius,
            Predicate<BlockPos> isBarrierPredicate
    ) {
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        Set<BlockPos> interior = new HashSet<>();

        // Seed with adjacent non-barrier blocks around the sealer
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = origin.relative(dir);
            if (!isBarrierPredicate.test(neighbor)) {
                if (visited.add(neighbor)) {
                    queue.add(neighbor);
                }
            }
        }

        if (queue.isEmpty()) {
            return SealedRoomResult.UNSEALED;
        }

        int minX = origin.getX();
        int maxX = origin.getX();
        int minY = origin.getY();
        int maxY = origin.getY();
        int minZ = origin.getZ();
        int maxZ = origin.getZ();

        boolean leaked = false;

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            interior.add(current);

            minX = Math.min(minX, current.getX());
            maxX = Math.max(maxX, current.getX());
            minY = Math.min(minY, current.getY());
            maxY = Math.max(maxY, current.getY());
            minZ = Math.min(minZ, current.getZ());
            maxZ = Math.max(maxZ, current.getZ());

            // Check distance constraints from origin
            if (Math.abs(current.getX() - origin.getX()) > maxHorizontalRadius
                    || Math.abs(current.getZ() - origin.getZ()) > maxHorizontalRadius
                    || Math.abs(current.getY() - origin.getY()) > maxVerticalRadius) {
                leaked = true;
                break;
            }

            if (interior.size() > maxVolume) {
                leaked = true;
                break;
            }

            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);
                if (!isBarrierPredicate.test(next)) {
                    if (visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
        }

        if (leaked) {
            return SealedRoomResult.UNSEALED;
        }

        AABB roomBounds = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);
        return new SealedRoomResult(true, interior.size(), Collections.unmodifiableSet(interior), roomBounds);
    }

    /**
     * Determines whether a given block state functions as an airtight barrier against vacuum/gas.
     */
    public static boolean isBarrierBlock(Level level, BlockPos pos, BlockState state) {
        if (state.isAir()) {
            return false;
        }
        if (state.getBlock() instanceof DoorBlock) {
            return !state.getValue(DoorBlock.OPEN);
        }
        if (state.getBlock() instanceof TrapDoorBlock) {
            return !state.getValue(TrapDoorBlock.OPEN);
        }
        if (state.isSolidRender()) {
            return true;
        }
        if (state.isCollisionShapeFullBlock(level, pos)) {
            return true;
        }
        return state.isSolid();
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.sealed = input.getBooleanOr("sealed", false);
        this.volume = input.getIntOr("volume", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("sealed", this.sealed);
        output.putInt("volume", this.volume);
    }
}
