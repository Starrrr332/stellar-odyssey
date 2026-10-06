package com.amaro.stellarodyssey.core.lifecycle;

/**
 * Pluggable SPI contract for decoupled satellite subsystems (WorldGen, Ecology AI, Star Map GUI).
 * Allows expansion modules to bind into mod lifecycle stages deterministically without circular
 * dependencies or monolithic coupling to the main mod class.
 */
public interface SatelliteModule {
    /**
     * Unique identifier for this satellite module (e.g. "worldgen", "ecology", "starmap").
     *
     * @return The unique module identifier string.
     */
    String getId();

    /**
     * Priority for ordered execution during each lifecycle stage.
     * Modules with higher priority values execute before modules with lower priority values.
     * Default is 0.
     *
     * @return The priority integer.
     */
    default int getPriority() {
        return 0;
    }

    /**
     * Whether this satellite module is currently enabled.
     * Disabled modules will skip execution during lifecycle stages.
     * Default is {@code true}.
     *
     * @return {@code true} if enabled, {@code false} otherwise.
     */
    default boolean isEnabled() {
        return true;
    }

    /**
     * Dispatched during {@link ModLifecycleStage#REGISTRY}.
     * Use this stage to register blocks, items, features, entities, or custom registries.
     */
    default void onRegister() {
    }

    /**
     * Dispatched during {@link ModLifecycleStage#COMMON_SETUP}.
     * Use this stage to configure networking, cross-module handlers, or shared game logic.
     */
    default void onCommonSetup() {
    }

    /**
     * Dispatched during {@link ModLifecycleStage#CLIENT_SETUP} strictly on the physical client.
     * Use this stage to register renderers, model layers, client screens, and HUD overlays.
     */
    default void onClientSetup() {
    }

    /**
     * Dispatched during {@link ModLifecycleStage#SERVER_STARTING} when the Minecraft server boots.
     * Use this stage to populate planetary catalogs or initialize world save data.
     */
    default void onServerStarting() {
    }
}
