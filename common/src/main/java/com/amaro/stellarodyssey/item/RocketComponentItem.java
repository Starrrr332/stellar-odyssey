package com.amaro.stellarodyssey.item;

import com.amaro.stellarodyssey.registry.tiers.RocketTier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/**
 * A craftable rocket component (nose cone, fins, tank, engine, ...) bound to a
 * specific rocket tier. The assembly table validates the exact set of components
 * required by each tier before producing the rocket.
 */
public class RocketComponentItem extends Item {
    private final RocketTier tier;
    private final String type;

    public RocketComponentItem(RocketTier tier, String type, Properties properties) {
        super(properties);
        this.tier = tier;
        this.type = type;
    }

    public RocketTier getTier() {
        return tier;
    }

    public String getType() {
        return type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);
        tooltip.accept(Component.translatable("tooltip.stellarodyssey.rocket_component.tier", tier.displayName())
                .withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("tooltip.stellarodyssey.rocket_component.type",
                        Component.translatable("rocket_component.stellarodyssey." + type))
                .withStyle(ChatFormatting.GRAY));
    }
}
