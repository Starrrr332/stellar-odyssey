package com.amaro.stellarodyssey.lifesupport;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.item.SpacesuitItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Determines atmospheric conditions and spacesuit hermetic seal integrity.
 */
public final class AtmosphereHelper {
    /** Dimension types tagged as having no breathable atmosphere (vacuum / alien planets). */
    public static final TagKey<DimensionType> VACUUM_DIMENSIONS = TagKey.create(
            Registries.DIMENSION_TYPE,
            StellarOdyssey.id("vacuum")
    );

    /** Altitude in blocks where atmospheric pressure drops and breathing becomes impossible. */
    public static final int VACUUM_ALTITUDE_THRESHOLD = 320;

    private AtmosphereHelper() {
    }

    /**
     * Checks if the player is in open space, orbit, or an unpressurized vacuum environment.
     */
    public static boolean isVacuumEnvironment(Player player) {
        Level level = player.level();

        // 1. High altitude / mesosphere & orbit
        if (player.getY() >= VACUUM_ALTITUDE_THRESHOLD) {
            return true;
        }

        // 2. Vacuum dimension type tag
        if (level.dimensionTypeRegistration().is(VACUUM_DIMENSIONS)) {
            return true;
        }

        // 3. Custom alien planet dimensions
        return level.dimension().identifier().getNamespace().equals(StellarOdyssey.MOD_ID);
    }

    /**
     * Checks if the current environment requires active life support (water immersion or vacuum).
     */
    public static boolean lacksOxygen(Player player) {
        return player.isEyeInFluid(FluidTags.WATER) || isVacuumEnvironment(player);
    }

    /**
     * Checks if the player has an intact, fully sealed spacesuit equipped across all 4 armor slots.
     */
    public static boolean isFullSuitEquipped(Player player) {
        return isSpacesuitPiece(player.getItemBySlot(EquipmentSlot.HEAD), ArmorType.HELMET)
                && isSpacesuitPiece(player.getItemBySlot(EquipmentSlot.CHEST), ArmorType.CHESTPLATE)
                && isSpacesuitPiece(player.getItemBySlot(EquipmentSlot.LEGS), ArmorType.LEGGINGS)
                && isSpacesuitPiece(player.getItemBySlot(EquipmentSlot.FEET), ArmorType.BOOTS);
    }

    /**
     * Verifies if an ItemStack is a valid spacesuit piece for a given armor slot.
     */
    public static boolean isSpacesuitPiece(ItemStack stack, ArmorType expectedType) {
        if (stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof SpacesuitItem spacesuitItem) {
            return spacesuitItem.getArmorType() == expectedType;
        }
        return false;
    }
}
