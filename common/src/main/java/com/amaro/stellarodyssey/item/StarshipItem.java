package com.amaro.stellarodyssey.item;

import com.amaro.stellarodyssey.entity.StarshipEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * Item used to deploy the Starship vehicle in the world.
 */
public class StarshipItem extends Item {
    public StarshipItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos spawnPos = pos.relative(face);

        StarshipEntity ship = new StarshipEntity(level, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
        Player player = context.getPlayer();
        if (player != null) {
            ship.setYRot(player.getYRot());
        }

        level.addFreshEntity(ship);

        ItemStack stack = context.getItemInHand();
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }
}
