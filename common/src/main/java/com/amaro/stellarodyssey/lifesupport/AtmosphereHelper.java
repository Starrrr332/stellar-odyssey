package com.amaro.stellarodyssey.lifesupport;

import com.amaro.stellarodyssey.StellarOdyssey;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Determines whether a player is in an environment that requires life support (lack of oxygen).
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
     * Checks if the player is currently in an oxygen-deprived environment:
     * <ul>
     *     <li>Submerged underwater without a conduit / water breathing</li>
     *     <li>In high orbit / space (altitude Y >= 320)</li>
     *     <li>In a vacuum or hostile alien dimension</li>
     * </ul>
     */
    public static boolean lacksOxygen(Player player) {
        Level level = player.level();

        // 1. Submerged underwater (oxygen tanks act as emergency rebreathers)
        if (player.isEyeInFluid(FluidTags.WATER)) {
            return true;
        }

        // 2. High altitude / mesosphere & orbit
        if (player.getY() >= VACUUM_ALTITUDE_THRESHOLD) {
            return true;
        }

        // 3. Vacuum dimension type
        if (level.dimensionTypeRegistration().is(VACUUM_DIMENSIONS)) {
            return true;
        }

        // 4. Default alien planet dimensions (custom dimensions will match here)
        return level.dimension().identifier().getNamespace().equals(StellarOdyssey.MOD_ID);
    }
}
