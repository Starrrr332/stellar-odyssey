package com.amaro.stellarodyssey.world;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.api.celestial.ICelestialBody;
import com.amaro.stellarodyssey.api.celestial.ICelestialCatalog;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central thread-safe catalog and registry for charted celestial bodies in Stellar Odyssey.
 * Implements {@link ICelestialCatalog} to provide decoupled planetary physics,
 * environmental conditions, and star system coordinates to worldgen, navigation, and life support.
 */
public final class CelestialBodyRegistry implements ICelestialCatalog {

    private final Map<ResourceKey<Level>, ICelestialBody> bodies = new ConcurrentHashMap<>();

    /**
     * Built-in immutable record representing an astronomical planetary body.
     */
    public record PlanetaryBody(
            ResourceKey<Level> dimensionKey,
            double gravityMultiplier,
            float atmosphericPressure,
            boolean hasBreathableAtmosphere,
            float solarRadiation,
            String starSystemName,
            String description,
            double surfaceTemperatureKelvin
    ) implements ICelestialBody {
        public static final com.mojang.serialization.Codec<PlanetaryBody> CODEC = com.mojang.serialization.codecs.RecordCodecBuilder.create(instance ->
                instance.group(
                        ResourceKey.codec(Registries.DIMENSION).fieldOf("dimension").forGetter(PlanetaryBody::dimensionKey),
                        com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("gravity", 1.0).forGetter(PlanetaryBody::gravityMultiplier),
                        com.mojang.serialization.Codec.FLOAT.optionalFieldOf("pressure", 1.0f).forGetter(PlanetaryBody::atmosphericPressure),
                        com.mojang.serialization.Codec.BOOL.optionalFieldOf("breathable", false).forGetter(PlanetaryBody::hasBreathableAtmosphere),
                        com.mojang.serialization.Codec.FLOAT.optionalFieldOf("radiation", 1.0f).forGetter(PlanetaryBody::solarRadiation),
                        com.mojang.serialization.Codec.STRING.optionalFieldOf("star_system", "Sol").forGetter(PlanetaryBody::starSystemName),
                        com.mojang.serialization.Codec.STRING.optionalFieldOf("description", "").forGetter(PlanetaryBody::description),
                        com.mojang.serialization.Codec.DOUBLE.optionalFieldOf("temperature_kelvin", 288.0).forGetter(PlanetaryBody::surfaceTemperatureKelvin)
                ).apply(instance, PlanetaryBody::new)
        );

        public PlanetaryBody {
            Objects.requireNonNull(dimensionKey, "dimensionKey cannot be null");
            Objects.requireNonNull(starSystemName, "starSystemName cannot be null");
            if (description == null) {
                description = "";
            }
        }
    }

    // Built-in celestial bodies
    public static final ICelestialBody PROXIMA_B = new PlanetaryBody(
            ModDimensions.PROXIMA_B,
            0.35,
            0.15f,
            false,
            2.40f,
            "Alpha Centauri",
            "Tidally locked rocky planet with flare radiation storms and crystalline mineral formations.",
            234.0
    );

    public static final ICelestialBody EXOTIC_PRIME = new PlanetaryBody(
            ModDimensions.EXOTIC_PRIME,
            0.75,
            0.85f,
            false,
            1.10f,
            "Kepler-452",
            "Super-Earth with bioluminescent xenomorphic turf and dense resonant ore clusters.",
            288.0
    );

    public static final ICelestialBody NEXUS_MOON = new PlanetaryBody(
            ModDimensions.NEXUS_MOON,
            0.16,
            0.00f,
            false,
            3.20f,
            "Alpha Centauri",
            "Airless low-gravity orbital moon covered in impact craters and rich meteoric iron.",
            110.0
    );

    public static final ICelestialBody GLIESE_DEEP = new PlanetaryBody(
            ModDimensions.GLIESE_DEEP,
            1.35,
            2.50f,
            false,
            0.80f,
            "Gliese 667",
            "High-gravity terrestrial world featuring steep canyons and metamorphic alien stone strata.",
            315.0
    );

    public static final CelestialBodyRegistry INSTANCE = new CelestialBodyRegistry();

    public CelestialBodyRegistry() {
        registerDefaultBodies();
    }

    public static CelestialBodyRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Populates the catalog with the primary charted planetary bodies of Stellar Odyssey.
     */
    public void registerDefaultBodies() {
        registerBody(PROXIMA_B);
        registerBody(EXOTIC_PRIME);
        registerBody(NEXUS_MOON);
        registerBody(GLIESE_DEEP);
    }

    @Override
    public Optional<ICelestialBody> getBody(ResourceKey<Level> dimension) {
        if (dimension == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(this.bodies.get(dimension));
    }

    @Override
    public void registerBody(ICelestialBody body) {
        Objects.requireNonNull(body, "Celestial body cannot be null");
        Objects.requireNonNull(body.dimensionKey(), "Celestial body dimensionKey cannot be null");
        this.bodies.put(body.dimensionKey(), body);
        StellarOdyssey.LOGGER.debug("Registered celestial body: {} in system {}",
                body.dimensionKey().identifier(), body.starSystemName());
    }

    @Override
    public Collection<ICelestialBody> getAllBodies() {
        return Collections.unmodifiableCollection(this.bodies.values());
    }

    /**
     * Unregisters a celestial body from the catalog.
     *
     * @param dimension Dimension resource key to remove.
     * @return The removed celestial body, or null if not present.
     */
    public ICelestialBody unregisterBody(ResourceKey<Level> dimension) {
        if (dimension == null) {
            return null;
        }
        return this.bodies.remove(dimension);
    }

    /**
     * Clears all registered celestial bodies. Useful for reset or testing scenarios.
     */
    public void clear() {
        this.bodies.clear();
    }
}
