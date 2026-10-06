package com.amaro.stellarodyssey.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Portable oxygen reserve for the (upcoming) life-support system.
 * <p>
 * Oxygen is stored as remaining durability, which gives a free, loader-agnostic
 * "fill bar" in every inventory. The life-support tick (phase 3) will drain tanks
 * via {@link #drain(ItemStack, int)} and the O2 HUD will read {@link #getOxygen(ItemStack)}.
 */
public class OxygenTankItem extends Item {
    /** Oxygen units in a full tank. 1 unit = 1 second of breathing in vacuum. */
    public static final int CAPACITY = 600;

    public OxygenTankItem(Properties properties) {
        super(properties);
    }

    public static int getOxygen(ItemStack stack) {
        return stack.getMaxDamage() - stack.getDamageValue();
    }

    public static boolean isEmpty(ItemStack stack) {
        return getOxygen(stack) <= 0;
    }

    /**
     * Removes up to {@code amount} units without ever breaking the tank.
     *
     * @return the amount actually drained
     */
    public static int drain(ItemStack stack, int amount) {
        int drained = Math.min(amount, getOxygen(stack));
        if (drained > 0) {
            stack.setDamageValue(stack.getDamageValue() + drained);
        }
        return drained;
    }

    /** @return the amount actually added */
    public static int fill(ItemStack stack, int amount) {
        int added = Math.min(amount, stack.getDamageValue());
        if (added > 0) {
            stack.setDamageValue(stack.getDamageValue() - added);
        }
        return added;
    }
}
