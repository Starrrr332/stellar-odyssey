package com.amaro.stellarodyssey.api.ecology;

/**
 * Environmental contract representing atmospheric and ecological conditions.
 * Used by fauna/flora AI, life support systems, and planetary sensors without coupling
 * to concrete dimension or world generation classes.
 */
public interface IAtmosphereCondition {
    /**
     * Atmospheric pressure in standard atmospheres (1.0 = Earth normal, 0.0 = vacuum).
     *
     * @return Pressure in atmospheres.
     */
    float pressure();

    /**
     * Fraction of breathable oxygen (0.0 to 1.0, 0.21 = Earth normal).
     *
     * @return Oxygen fraction.
     */
    float oxygenFraction();

    /**
     * Toxicity or hazard level (0.0 = pristine/safe, 1.0 = lethal).
     *
     * @return Toxicity rating.
     */
    float toxicity();

    /**
     * Ambient surface temperature in Kelvin (e.g. 288.15 K = 15°C).
     *
     * @return Temperature in Kelvin.
     */
    float temperatureKelvin();

    /**
     * Radiation index (0.0 = safe, higher = hostile solar or cosmic radiation).
     *
     * @return Radiation level.
     */
    float radiationLevel();

    /**
     * Checks if the atmosphere can sustain unassisted human/terrestrial respiration.
     *
     * @return {@code true} if breathable without life support.
     */
    default boolean isBreathable() {
        return pressure() >= 0.5f && pressure() <= 2.5f && oxygenFraction() >= 0.16f && toxicity() < 0.1f;
    }

    /**
     * Checks if the atmosphere represents a hard vacuum or near-vacuum.
     *
     * @return {@code true} if vacuum.
     */
    default boolean isVacuum() {
        return pressure() < 0.05f;
    }

    /**
     * Checks if toxic or corrosive gases require hazardous environment protection.
     *
     * @return {@code true} if atmosphere is toxic.
     */
    default boolean isToxic() {
        return toxicity() >= 0.25f;
    }

    /**
     * Checks if ambient temperature is outside the tolerable range for unprotected organics (240K - 330K).
     *
     * @return {@code true} if temperature is dangerously extreme.
     */
    default boolean isExtremeTemperature() {
        return temperatureKelvin() < 240.0f || temperatureKelvin() > 330.0f;
    }

    /**
     * Immutable standard record implementation of {@link IAtmosphereCondition}.
     */
    record SimpleAtmosphereCondition(
            float pressure,
            float oxygenFraction,
            float toxicity,
            float temperatureKelvin,
            float radiationLevel
    ) implements IAtmosphereCondition {
        public static final IAtmosphereCondition EARTH_LIKE =
                new SimpleAtmosphereCondition(1.0f, 0.21f, 0.0f, 288.15f, 0.0f);
        public static final IAtmosphereCondition VACUUM =
                new SimpleAtmosphereCondition(0.0f, 0.0f, 0.0f, 2.7f, 2.0f);
        public static final IAtmosphereCondition TOXIC_EXOPLANET =
                new SimpleAtmosphereCondition(1.8f, 0.02f, 0.85f, 340.0f, 1.2f);
    }
}
