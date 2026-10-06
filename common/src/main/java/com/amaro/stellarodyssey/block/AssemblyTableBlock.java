package com.amaro.stellarodyssey.block;

import com.amaro.stellarodyssey.block.entity.AssemblyTableBlockEntity;
import com.amaro.stellarodyssey.block.entity.AssemblyTableMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Rocket Assembly Table: the crafting station where tiered rocket components are
 * combined into a fully assembled rocket. Opens a container menu with 8 component
 * slots and a result slot.
 */
public class AssemblyTableBlock extends Block implements EntityBlock {
    public AssemblyTableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AssemblyTableBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide()) {
            MenuProvider provider = this.getMenuProvider(state, level, pos);
            if (provider != null) {
                player.openMenu(provider);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof AssemblyTableBlockEntity table) {
            return new SimpleMenuProvider((id, inv, p) ->
                    new AssemblyTableMenu(id, inv, table, ContainerLevelAccess.create(level, pos)),
                    Component.translatable("container.stellarodyssey.assembly_table"));
        }
        return null;
    }
}
