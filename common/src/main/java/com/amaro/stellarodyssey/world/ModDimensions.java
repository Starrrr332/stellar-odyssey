package com.amaro.stellarodyssey.world;

import com.amaro.stellarodyssey.StellarOdyssey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * Registry keys for planetary dimensions.
 */
public final class ModDimensions {
    public static final ResourceKey<DimensionType> ALIEN_PLANET_TYPE = ResourceKey.create(
            Registries.DIMENSION_TYPE,
            StellarOdyssey.id("alien_planet")
    );

    public static final ResourceKey<Level> PROXIMA_B = ResourceKey.create(
            Registries.DIMENSION,
            StellarOdyssey.id("proxima_b")
    );

    private ModDimensions() {
    }
}
