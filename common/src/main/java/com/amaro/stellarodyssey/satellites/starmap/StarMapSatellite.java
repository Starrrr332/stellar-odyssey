package com.amaro.stellarodyssey.satellites.starmap;

import com.amaro.stellarodyssey.api.SatelliteModule;
import com.amaro.stellarodyssey.api.celestial.ICelestialCatalog;
import com.amaro.stellarodyssey.core.ModConstants;
import com.amaro.stellarodyssey.core.lifecycle.ModLifecycleManager;
import com.amaro.stellarodyssey.satellites.starmap.screen.StarMapScreen;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.architectury.utils.EnvExecutor;
import net.minecraft.client.Minecraft;

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
    private static boolean clientInitialized = false;

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

        EnvExecutor.runInEnv(Env.CLIENT, () -> StarMapClientHandler::setup);
        clientInitialized = true;
        ModConstants.LOGGER.info("StarMapSatellite: physical client setup completed successfully.");
    }

    @Override
    public void onServerStarting() {
        ModConstants.LOGGER.debug("StarMapSatellite: server starting hook acknowledged.");
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
        if (Platform.getEnvironment() != Env.CLIENT) {
            ModConstants.LOGGER.warn("Attempted to open StarMapScreen on non-client environment.");
            return;
        }

        ICelestialCatalog cat = catalog != null ? catalog : activeCatalog;
        EnvExecutor.runInEnv(Env.CLIENT, () -> () -> StarMapClientHandler.openScreen(cat));
    }

    /**
     * Isolated static inner client handler ensuring no client classes are loaded on dedicated servers.
     */
    private static final class StarMapClientHandler {
        static void setup() {
            ModConstants.LOGGER.debug("StarMapClientHandler: client rendering components ready.");
        }

        static void openScreen(ICelestialCatalog catalog) {
            Minecraft.getInstance().setScreenAndShow(new StarMapScreen(catalog));
        }
    }
}
