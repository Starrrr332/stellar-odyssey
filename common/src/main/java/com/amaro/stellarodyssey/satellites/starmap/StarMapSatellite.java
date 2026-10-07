package com.amaro.stellarodyssey.satellites.starmap;

import com.amaro.stellarodyssey.api.SatelliteModule;
import com.amaro.stellarodyssey.api.celestial.ICelestialCatalog;
import com.amaro.stellarodyssey.core.ModConstants;
import com.amaro.stellarodyssey.core.lifecycle.ModLifecycleManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;

import java.util.Objects;

/**
 * Intergalactic Star Map Navigation Satellite Module for Stellar Odyssey.
 * <p>
 * Decoupled expansion subsystem providing fullscreen celestial cartography,
 * interactive sector coordinate widgets, and orbital skybox projectors.
 * <p>
 * Strictly isolates and guards client-only GUI and rendering execution so dedicated
 * server runtimes never encounter class loading or missing renderer crashes.
 */
public final class StarMapSatellite implements SatelliteModule {
    public static final String MODULE_ID = "starmap";
    public static final int PRIORITY = 0;

    public static final StarMapSatellite INSTANCE = new StarMapSatellite();

    private static ICelestialCatalog activeCatalog;
    private static volatile ScreenOpener screenOpener;
    private static boolean clientInitialized = false;

    @FunctionalInterface
    public interface ScreenOpener {
        void open(ICelestialCatalog catalog, int rocketTier, int rocketEntityId);
    }

    public StarMapSatellite() {
    }

    /**
     * Initializes and registers the StarMap Satellite into the central lifecycle manager.
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
            ModConstants.LOGGER.debug("Deferred registration of StarMapSatellite until lifecycle bootstrap: {}", t.getMessage());
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
        ModConstants.LOGGER.info("StarMapSatellite: registering celestial navigation foundations (priority={}).", PRIORITY);
    }

    @Override
    public void onCommonSetup() {
        setActiveCatalog(com.amaro.stellarodyssey.world.CelestialBodyRegistry.INSTANCE);
        ModConstants.LOGGER.info("StarMapSatellite: common setup complete. Celestial navigation satellite active.");
    }

    /**
     * Dispatched strictly on the physical client. Guarded against dedicated server execution.
     */
    @Override
    public void onClientSetup() {
        if (Platform.getEnvironment() != Env.CLIENT) {
            ModConstants.LOGGER.info("StarMapSatellite: skipping client setup on dedicated server runtime.");
            return;
        }

        clientInitialized = screenOpener != null;
        if (clientInitialized) {
            ModConstants.LOGGER.info("StarMapSatellite: physical client setup completed successfully.");
        } else {
            ModConstants.LOGGER.warn("StarMapSatellite: client screen opener was not registered; star map will remain unavailable.");
        }
    }

    @Override
    public void onServerStarting() {
        ModConstants.LOGGER.debug("StarMapSatellite: server starting hook acknowledged.");
    }

    /**
     * Installs the screen factory from the client entrypoint without linking common code to Minecraft client classes.
     *
     * @param opener Client-side screen opener.
     */
    public static void registerScreenOpener(ScreenOpener opener) {
        if (Platform.getEnvironment() != Env.CLIENT) {
            throw new IllegalStateException("Star map screen opener can only be registered on the physical client");
        }
        screenOpener = Objects.requireNonNull(opener, "opener cannot be null");
    }

    /**
     * Sets the active celestial catalog queried by the star map navigation screen.
     *
     * @param catalog The catalog instance.
     */
    public static void setActiveCatalog(ICelestialCatalog catalog) {
        activeCatalog = catalog;
    }

    /**
     * Retrieves the currently active celestial catalog.
     *
     * @return The active catalog, or {@code null}.
     */
    public static ICelestialCatalog getActiveCatalog() {
        return activeCatalog;
    }

    /**
     * Returns whether the client-side setup for the star map has been completed.
     *
     * @return {@code true} if client setup is completed.
     */
    public static boolean isClientInitialized() {
        return clientInitialized;
    }

    /**
     * Opens the Star Map navigation screen on the physical client.
     * Guarded to be safely no-op on dedicated servers.
     *
     * @param catalog The catalog to query, or {@code null} to use the active catalog.
     */
    public static void openScreen(ICelestialCatalog catalog) {
        openScreen(catalog, 0, -1);
    }

    /**
     * Opens the Star Map navigation screen with active rocket context on the physical client.
     * Guarded to be safely no-op on dedicated servers.
     *
     * @param catalog The catalog to query, or {@code null} to use the active catalog.
     * @param rocketTier The tier level of the mounting rocket (1-3).
     * @param rocketEntityId The entity ID of the rocket.
     */
    public static void openScreen(ICelestialCatalog catalog, int rocketTier, int rocketEntityId) {
        if (Platform.getEnvironment() != Env.CLIENT) {
            ModConstants.LOGGER.warn("Attempted to open StarMapScreen on non-client environment.");
            return;
        }

        ICelestialCatalog cat = catalog != null ? catalog : activeCatalog;
        ScreenOpener opener = screenOpener;
        if (opener == null) {
            ModConstants.LOGGER.warn("Attempted to open StarMapScreen before the client screen opener was initialized.");
            return;
        }
        opener.open(cat, rocketTier, rocketEntityId);
    }
}
