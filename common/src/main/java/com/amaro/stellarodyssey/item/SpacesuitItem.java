package com.amaro.stellarodyssey.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.equipment.ArmorType;

import java.util.function.Consumer;

/**
 * Modular spacesuit equipment piece.
 */
public class SpacesuitItem extends Item {
    private final ArmorType armorType;

    public SpacesuitItem(ArmorType armorType, Properties properties) {
        super(properties.humanoidArmor(ModArmorMaterials.SPACESUIT, armorType));
        this.armorType = armorType;
    }

    public ArmorType getArmorType() {
        return armorType;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);

        if (entity instanceof Player player && slot == armorType.getSlot()) {
            if (armorType == ArmorType.BOOTS) {
                // Micro-thrusters: dampen fall damage when boots are equipped
                if (player.fallDistance > 2.0) {
                    player.fallDistance = Math.max(0.0, player.fallDistance * 0.75);
                }
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, tooltip, flag);

        switch (armorType) {
            case HELMET -> {
                tooltip.accept(Component.translatable("tooltip.stellarodyssey.spacesuit_helmet.desc")
                        .withStyle(ChatFormatting.AQUA));
                tooltip.accept(Component.translatable("tooltip.stellarodyssey.spacesuit_helmet.sub")
                        .withStyle(ChatFormatting.GRAY));
            }
            case CHESTPLATE -> {
                tooltip.accept(Component.translatable("tooltip.stellarodyssey.spacesuit_chestplate.desc")
                        .withStyle(ChatFormatting.AQUA));
                tooltip.accept(Component.translatable("tooltip.stellarodyssey.spacesuit_chestplate.sub")
                        .withStyle(ChatFormatting.GRAY));
            }
            case LEGGINGS -> {
                tooltip.accept(Component.translatable("tooltip.stellarodyssey.spacesuit_leggings.desc")
                        .withStyle(ChatFormatting.AQUA));
                tooltip.accept(Component.translatable("tooltip.stellarodyssey.spacesuit_leggings.sub")
                        .withStyle(ChatFormatting.GRAY));
            }
            case BOOTS -> {
                tooltip.accept(Component.translatable("tooltip.stellarodyssey.spacesuit_boots.desc")
                        .withStyle(ChatFormatting.AQUA));
                tooltip.accept(Component.translatable("tooltip.stellarodyssey.spacesuit_boots.sub")
                        .withStyle(ChatFormatting.GRAY));
            }
            default -> {}
        }

        tooltip.accept(Component.translatable("tooltip.stellarodyssey.spacesuit_set_bonus")
                .withStyle(ChatFormatting.GOLD));
    }
}
