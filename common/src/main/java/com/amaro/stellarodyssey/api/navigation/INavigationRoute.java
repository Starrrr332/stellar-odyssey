package com.amaro.stellarodyssey.api.navigation;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Represents an orbital or intergalactic navigation trajectory between celestial bodies.
 * Used by starship propulsion, navigation computers, and Star Map interfaces.
 */
public interface INavigationRoute {
    /**
     * Origin celestial body / dimension.
     *
     * @return Origin dimension key.
     */
    ResourceKey<Level> origin();

    /**
     * Destination celestial body / dimension.
     *
     * @return Destination dimension key.
     */
    ResourceKey<Level> destination();

    /**
     * Calculated distance in astronomical units (AU) or light years.
     *
     * @return Distance value.
     */
    double distance();

    /**
     * Estimated travel time or warp spool duration in game ticks.
     *
     * @return Travel time in ticks.
     */
    int warpTimeTicks();

    /**
     * Fuel / energy consumption units required for this hyperspace jump.
     *
     * @return Fuel cost.
     */
    int fuelCost();

    /**
     * Danger rating or hazard factor along the flight corridor (0.0 = safe, 1.0 = extreme hazard).
     *
     * @return Hazard factor.
     */
    float hazardFactor();

    /**
     * Name of the hyperspace corridor or transit sector.
     *
     * @return Route name.
     */
    String routeName();

    /**
     * Target spatial coordinate in the destination dimension.
     *
     * @return Destination 3D position vector.
     */
    Vec3 destinationCoordinates();

    /**
     * Validates whether the navigation route has valid origin, destination, and non-negative distance.
     *
     * @return {@code true} if route is valid.
     */
    default boolean isValid() {
        return origin() != null && destination() != null && !origin().equals(destination()) && distance() >= 0.0;
    }

    /**
     * Standard immutable record implementation of {@link INavigationRoute}.
     */
    record SimpleNavigationRoute(
            ResourceKey<Level> origin,
            ResourceKey<Level> destination,
            double distance,
            int warpTimeTicks,
            int fuelCost,
            float hazardFactor,
            String routeName,
            Vec3 destinationCoordinates
    ) implements INavigationRoute {
    }
}
