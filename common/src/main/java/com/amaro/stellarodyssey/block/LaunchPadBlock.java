package com.amaro.stellarodyssey.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Central block of the 3x3 launch pad. A rocket item deployed on this block
 * (with a complete 3x3 base of {@code LaunchPadBaseBlock} beneath) starts the
 * launch sequence.
 */
public class LaunchPadBlock extends Block {
    private static final VoxelShape SHAPE = Shapes.box(0.0, 0.0, 0.0, 1.0, 0.125, 1.0);

    public LaunchPadBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /**
     * Checks whether a complete 3x3 launch pad base exists centered on {@code pos}.
     * The center block itself is this block; the 8 neighbours must be
     * {@code LaunchPadBaseBlock}.
     */
    public static boolean isCompletePad(Level level, BlockPos pos) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                BlockState neighbour = level.getBlockState(pos.offset(dx, 0, dz));
                if (!(neighbour.getBlock() instanceof LaunchPadBaseBlock)) {
                    return false;
                }
            }
        }
        return true;
    }
}
