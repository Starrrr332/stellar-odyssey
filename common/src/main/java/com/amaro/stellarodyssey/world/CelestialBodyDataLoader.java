package com.amaro.stellarodyssey.world;

import com.amaro.stellarodyssey.StellarOdyssey;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import java.util.Map;

/**
 * Server-side Data-driven resource reload listener for celestial bodies.
 * Reads JSON definitions from `data/<namespace>/stellar_odyssey/celestial_bodies/*.json`
 * and populates {@link CelestialBodyRegistry} dynamically on world load and `/reload`.
 */
public class CelestialBodyDataLoader extends SimpleJsonResourceReloadListener<CelestialBodyRegistry.PlanetaryBody> {
    public static final String DIRECTORY = "stellar_odyssey/celestial_bodies";

    public CelestialBodyDataLoader() {
        super(CelestialBodyRegistry.PlanetaryBody.CODEC, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, CelestialBodyRegistry.PlanetaryBody> resources, ResourceManager resourceManager, ProfilerFiller profiler) {
        profiler.push("stellar_odyssey_celestial_bodies_load");

        CelestialBodyRegistry registry = CelestialBodyRegistry.getInstance();
        registry.clear();
        registry.registerDefaultBodies(); // Base fallback bodies

        for (Map.Entry<Identifier, CelestialBodyRegistry.PlanetaryBody> entry : resources.entrySet()) {
            Identifier id = entry.getKey();
            CelestialBodyRegistry.PlanetaryBody body = entry.getValue();

            if (body != null) {
                registry.registerBody(body);
                StellarOdyssey.LOGGER.info("Loaded data-driven celestial body: {} [{}]", id, body.dimensionKey().identifier());
            }
        }

        profiler.pop();
        StellarOdyssey.LOGGER.info("Stellar Odyssey: Registered {} total celestial bodies.", registry.getAllBodies().size());
    }
}
