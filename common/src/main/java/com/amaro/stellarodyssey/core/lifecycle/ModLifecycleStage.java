package com.amaro.stellarodyssey.core.lifecycle;

/**
 * Deterministic lifecycle stages dispatched to registered {@link SatelliteModule} instances.
 */
public enum ModLifecycleStage {
    /**
     * Initial stage when DeferredRegisters are bound to the mod event bus.
     * Satellites register custom blocks, items, features, entities, and sounds.
     */
    REGISTRY,

    /**
     * Common setup phase occurring on both physical client and server.
     * Networking payloads, capabilities, data structures, and event listeners are initialized.
     */
    COMMON_SETUP,

    /**
     * Physical client-only setup phase.
     * Screen factories, entity renderers, particle providers, and emissive model layers are bound.
     */
    CLIENT_SETUP,

    /**
     * Server starting phase when dedicated server or integrated server boots.
     * Planetary catalogs, world data managers, and dimension states are prepared.
     */
    SERVER_STARTING
}
