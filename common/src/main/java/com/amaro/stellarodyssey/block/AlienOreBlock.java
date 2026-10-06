package com.amaro.stellarodyssey.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Crystalline alien ore. Drops 3-7 XP like vanilla ores (when not silk-touched) and
 * occasionally vents glowing spores from any exposed face.
 */
public class AlienOreBlock extends DropExperienceBlock {
    public AlienOreBlock(Properties properties) {
        super(UniformInt.of(3, 7), properties);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0) {
            return;
        }
        Direction face = Direction.getRandom(random);
        BlockPos neighbour = pos.relative(face);
        if (level.getBlockState(neighbour).isSolidRender()) {
            return; // buried face: nothing to see
        }
        double x = pos.getX() + 0.5 + face.getStepX() * 0.55 + (face.getStepX() == 0 ? random.nextDouble() - 0.5 : 0);
        double y = pos.getY() + 0.5 + face.getStepY() * 0.55 + (face.getStepY() == 0 ? random.nextDouble() - 0.5 : 0);
        double z = pos.getZ() + 0.5 + face.getStepZ() * 0.55 + (face.getStepZ() == 0 ? random.nextDouble() - 0.5 : 0);
        level.addParticle(ParticleTypes.GLOW, x, y, z, 0.0, 0.02, 0.0);
    }
}
