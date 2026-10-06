package com.amaro.stellarodyssey.api.celestial;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Representation of an astronomical celestial body (planet, moon, asteroid, or orbital station)
 * in the Stellar Odyssey galaxy.
 */
public interface ICelestialBody {
    /**
     * The Minecraft dimension key corresponding to this celestial body.
     *
     * @return The dimension ResourceKey.
     */
    ResourceKey<Level> dimensionKey();

    /**
     * Gravity multiplier relative to standard Earth/Overworld gravity (1.0 = standard gravity).
     *
     * @return Gravity multiplier.
     */
    double gravityMultiplier();

    /**
     * Atmospheric surface pressure in standard atmospheres (0.0 = hard vacuum, 1.0 = 1 atm).
     *
     * @return Atmospheric surface pressure.
     */
    float atmosphericPressure();

    /**
     * Whether the native atmosphere supports unassisted respiration without life support.
     *
     * @return {@code true} if breathable, {@code false} otherwise.
     */
    boolean hasBreathableAtmosphere();

    /**
     * Solar and cosmic radiation intensity (1.0 = standard solar flux, >1.5 = hazardous).
     *
     * @return Solar radiation level.
     */
    float solarRadiation();

    /**
     * The name of the star system this celestial body orbits (e.g. "Alpha Centauri", "Sol").
     *
     * @return Star system name.
     */
    String starSystemName();

    /**
     * Descriptive name or path identifier of the celestial body.
     *
     * @return Name string.
     */
    default String name() {
        return dimensionKey() != null ? dimensionKey().identifier().getPath() : "unknown";
    }

    /**
     * Whether this celestial body is a vacuum or near-vacuum environment.
     *
     * @return {@code true} if vacuum, {@code false} otherwise.
     */
    default boolean isVacuum() {
        return atmosphericPressure() < 0.05f;
    }

    /**
     * Whether environmental hazards exist on this body (vacuum, high radiation, or extreme pressure).
     *
     * @return {@code true} if hazardous, {@code false} otherwise.
     */
    default boolean isHazardous() {
        return !hasBreathableAtmosphere() || solarRadiation() > 1.5f || atmosphericPressure() > 3.0f || atmosphericPressure() < 0.2f;
    }

    /**
     * Standard immutable record implementation of {@link ICelestialBody}.
     */
    record SimpleCelestialBody(
            ResourceKey<Level> dimensionKey,
            double gravityMultiplier,
            float atmosphericPressure,
            boolean hasBreathableAtmosphere,
            float solarRadiation,
            String starSystemName
    ) implements ICelestialBody {
    }
}
