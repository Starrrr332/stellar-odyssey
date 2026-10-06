package com.amaro.stellarodyssey.block;

import com.amaro.stellarodyssey.item.OxygenTankItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Oxygen Refiller Station: recharges portable {@link OxygenTankItem} reserves to 100% capacity.
 */
public class OxygenRefillerBlock extends Block {
    public OxygenRefillerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                          Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof OxygenTankItem) {
            int currentOxygen = OxygenTankItem.getOxygen(stack);
            if (currentOxygen < OxygenTankItem.CAPACITY) {
                if (!level.isClientSide()) {
                    OxygenTankItem.fill(stack, OxygenTankItem.CAPACITY);
                    level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.2F);
                    player.sendSystemMessage(Component.translatable("message.stellarodyssey.oxygen_refilled"));
                }
                return InteractionResult.SUCCESS;
            } else {
                if (!level.isClientSide()) {
                    player.sendSystemMessage(Component.translatable("message.stellarodyssey.oxygen_already_full"));
                }
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hit) {
        Inventory inv = player.getInventory();
        int refilledCount = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.getItem() instanceof OxygenTankItem && OxygenTankItem.getOxygen(s) < OxygenTankItem.CAPACITY) {
                OxygenTankItem.fill(s, OxygenTankItem.CAPACITY);
                refilledCount++;
            }
        }

        if (refilledCount > 0) {
            if (!level.isClientSide()) {
                level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1.0F, 1.2F);
                player.sendSystemMessage(Component.translatable("message.stellarodyssey.oxygen_all_refilled"));
            }
            return InteractionResult.SUCCESS;
        } else {
            if (!level.isClientSide()) {
                player.sendSystemMessage(Component.translatable("message.stellarodyssey.oxygen_no_tanks"));
            }
            return InteractionResult.CONSUME;
        }
    }
}
