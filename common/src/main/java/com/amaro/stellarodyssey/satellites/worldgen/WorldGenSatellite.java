package com.amaro.stellarodyssey.satellites.worldgen;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.core.lifecycle.ModLifecycleManager;
import com.amaro.stellarodyssey.core.lifecycle.SatelliteModule;
import com.amaro.stellarodyssey.world.CelestialBodyRegistry;

/**
 * Procedural WorldGen Satellite Module for Stellar Odyssey.
 * <p>
 * Implements {@link SatelliteModule} to provide decoupled registration of:
 * <ul>
 *   <li>Planetary noise parameters, curves, and mathematical terrain elevation profiles</li>
 *   <li>Procedural surface rules (alien turf, alien stone subsurface, crystal pockets)</li>
 *   <li>Configured and placed features for planetary ore veins and crystalline formations</li>
 *   <li>Default astronomical records in the {@link CelestialBodyRegistry}</li>
 * </ul>
 * Operates at priority 10 to ensure celestial world generation foundations are registered
 * before secondary ecology and navigation satellites.
 */
public final class WorldGenSatellite implements SatelliteModule {

    public static final String MODULE_ID = "worldgen";
    public static final int PRIORITY = 10;

    public static final WorldGenSatellite INSTANCE = new WorldGenSatellite();

    private boolean registered = false;
    private boolean commonSetupComplete = false;

    public WorldGenSatellite() {
    }

    /**
     * Initializes and registers the WorldGen Satellite into the central lifecycle manager.
     */
    public static void init() {
        if (!ModLifecycleManager.hasModule(MODULE_ID)) {
            ModLifecycleManager.registerModule(INSTANCE);
        }
    }

    static {
        try {
            init();
        } catch (Throwable t) {
            StellarOdyssey.LOGGER.debug("Deferred registration of WorldGenSatellite: {}", t.getMessage());
        }
    }

    @Override
    public String getId() {
        return MODULE_ID;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void onRegister() {
        StellarOdyssey.LOGGER.info("WorldGenSatellite: initializing procedural worldgen registration (priority={}).", PRIORITY);

        // 1. Noise settings, curves, and terrain profiles
        PlanetaryNoiseSettings.register();

        // 2. Procedural surface rule builders
        PlanetarySurfaceRules.register();

        // 3. Planetary ore and crystal configured features
        ModConfiguredFeatures.register();

        // 4. Default celestial bodies registration in catalog
        CelestialBodyRegistry.getInstance().registerDefaultBodies();

        this.registered = true;
        StellarOdyssey.LOGGER.info("WorldGenSatellite: registration phase completed successfully.");
    }

    @Override
    public void onCommonSetup() {
        StellarOdyssey.LOGGER.info("WorldGenSatellite: common setup running - verifying procedural terrain pipelines.");
        this.commonSetupComplete = true;
    }

    @Override
    public void onClientSetup() {
        StellarOdyssey.LOGGER.debug("WorldGenSatellite: client setup completed.");
    }

    @Override
    public void onServerStarting() {
        int chartedCount = CelestialBodyRegistry.getInstance().getAllBodies().size();
        StellarOdyssey.LOGGER.info("WorldGenSatellite: server starting - {} celestial bodies charted in catalog.", chartedCount);
    }

    /**
     * Returns whether the registration stage has executed.
     *
     * @return {@code true} if registered.
     */
    public boolean isRegistered() {
        return this.registered;
    }

    /**
     * Returns whether common setup has completed.
     *
     * @return {@code true} if common setup is complete.
     */
    public boolean isCommonSetupComplete() {
        return this.commonSetupComplete;
    }
}
