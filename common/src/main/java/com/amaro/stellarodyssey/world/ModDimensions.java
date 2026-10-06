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

    public static final ResourceKey<Level> NEXUS_MOON = ResourceKey.create(
            Registries.DIMENSION,
            StellarOdyssey.id("nexus_moon")
    );

    public static final ResourceKey<Level> EXOTIC_PRIME = ResourceKey.create(
            Registries.DIMENSION,
            StellarOdyssey.id("exotic_prime")
    );

    public static final ResourceKey<Level> GLIESE_DEEP = ResourceKey.create(
            Registries.DIMENSION,
            StellarOdyssey.id("gliese_deep")
    );

    private ModDimensions() {
    }
}
