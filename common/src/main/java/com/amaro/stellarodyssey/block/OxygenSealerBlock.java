package com.amaro.stellarodyssey.block;

import com.amaro.stellarodyssey.block.entity.OxygenSealerBlockEntity;
import com.amaro.stellarodyssey.registry.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Oxygen Sealer: pressurizes and oxygenates an enclosed habitat on airless celestial bodies.
 * Employs server-side 3D BFS room flood-fill to verify hermetic containment.
 */
public class OxygenSealerBlock extends BaseEntityBlock {
    public static final BooleanProperty SEALED = BooleanProperty.create("sealed");

    public OxygenSealerBlock(Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any().setValue(SEALED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SEALED);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OxygenSealerBlockEntity(pos, state);
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntityTypes.OXYGEN_SEALER.get(), OxygenSealerBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof OxygenSealerBlockEntity sealer) {
                sealer.checkSeal(level, pos, state);
                if (sealer.isSealed()) {
                    serverPlayer.sendSystemMessage(
                            Component.translatable("message.stellarodyssey.sealer_sealed", sealer.getVolume())
                    );
                } else {
                    serverPlayer.sendSystemMessage(
                            Component.translatable("message.stellarodyssey.sealer_unsealed")
                    );
                }
            }
        }
        return InteractionResult.SUCCESS;
    }
}
