package com.amaro.stellarodyssey.item;

import com.amaro.stellarodyssey.block.LaunchPadBlock;
import com.amaro.stellarodyssey.entity.RocketEntity;
import com.amaro.stellarodyssey.registry.tiers.RocketTier;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/**
 * A fully assembled rocket of a given tier, produced by the Rocket Assembly Table.
 * <p>
 * Deploying it on a {@code LaunchPadBlock} with a complete 3x3 base spawns the
 * corresponding {@code RocketEntity} and begins the launch countdown.
 * </p>
 */
public class RocketItem extends Item {
    private final RocketTier tier;

    public RocketItem(RocketTier tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public RocketTier getTier() {
        return tier;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }

        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof LaunchPadBlock)) {
            Player player = context.getPlayer();
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.stellarodyssey.need_launch_pad"));
            }
            return InteractionResult.FAIL;
        }
        if (!LaunchPadBlock.isCompletePad(level, pos)) {
            Player player = context.getPlayer();
            if (player != null) {
                player.sendSystemMessage(Component.translatable("message.stellarodyssey.incomplete_pad"));
            }
            return InteractionResult.FAIL;
        }

        // Spawn the rocket on top of the pad center
        BlockPos spawnPos = pos.above();
        RocketEntity rocket = new RocketEntity(level, spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5,
                this.tier.tierLevel());
        level.addFreshEntity(rocket);

        ItemStack stack = context.getItemInHand();
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.stellarodyssey.rocket.tier", tier.displayName())
                .withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.stellarodyssey.rocket.destination",
                        Component.translatable("dimension.stellarodyssey." + tier.destination().identifier().getPath()))
                .withStyle(ChatFormatting.GRAY));
    }
}
