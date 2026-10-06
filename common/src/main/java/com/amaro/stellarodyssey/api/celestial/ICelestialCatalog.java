package com.amaro.stellarodyssey.api.celestial;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Collection;
import java.util.Optional;

/**
 * Service catalog providing lookup and registration of charted {@link ICelestialBody} objects.
 * Decouples celestial navigation, starmap rendering, and atmospheric physics from concrete
 * world generation or dimension internals.
 */
public interface ICelestialCatalog {
    /**
     * Retrieves the celestial body associated with the given dimension key.
     *
     * @param dimension The dimension ResourceKey.
     * @return An {@link Optional} containing the celestial body if charted.
     */
    Optional<ICelestialBody> getBody(ResourceKey<Level> dimension);

    /**
     * Registers a charted celestial body into this catalog.
     *
     * @param body The celestial body to register.
     */
    void registerBody(ICelestialBody body);

    /**
     * Returns an unmodifiable collection of all charted celestial bodies in this catalog.
     *
     * @return Collection of all celestial bodies.
     */
    Collection<ICelestialBody> getAllBodies();

    /**
     * Checks if the given dimension is charted in this catalog.
     *
     * @param dimension The dimension ResourceKey.
     * @return {@code true} if present, {@code false} otherwise.
     */
    default boolean hasBody(ResourceKey<Level> dimension) {
        return getBody(dimension).isPresent();
    }

    /**
     * Retrieves all celestial bodies residing within a specific star system.
     *
     * @param starSystemName The name of the star system.
     * @return Collection of bodies orbiting within that system.
     */
    default Collection<ICelestialBody> getBodiesInSystem(String starSystemName) {
        if (starSystemName == null) {
            return java.util.List.of();
        }
        return getAllBodies().stream()
                .filter(b -> starSystemName.equalsIgnoreCase(b.starSystemName()))
                .toList();
    }
}
