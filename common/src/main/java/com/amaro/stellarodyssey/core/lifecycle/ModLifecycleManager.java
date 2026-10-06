package com.amaro.stellarodyssey.core.lifecycle;

import com.amaro.stellarodyssey.core.ModConstants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Thread-safe lifecycle orchestrator that manages and dispatches deterministic lifecycle
 * events to registered {@link SatelliteModule} instances.
 */
public final class ModLifecycleManager {
    private static final List<SatelliteModule> MODULES = new ArrayList<>();

    private static final Comparator<SatelliteModule> MODULE_COMPARATOR = Comparator
            .comparingInt(SatelliteModule::getPriority)
            .reversed()
            .thenComparing(SatelliteModule::getId);

    private static boolean discovered = false;

    private ModLifecycleManager() {
    }

    /**
     * Initializes the lifecycle manager and triggers dynamic SPI satellite discovery.
     */
    public static synchronized void init() {
        if (!discovered) {
            discoverModules();
        }
    }

    /**
     * Discovers and registers {@link SatelliteModule} implementations via Java SPI
     * ({@link ServiceLoader}). Handles duplicate modules idempotently.
     */
    public static synchronized void discoverModules() {
        discovered = true;
        try {
            ServiceLoader<SatelliteModule> loader = ServiceLoader.load(
                    SatelliteModule.class,
                    ModLifecycleManager.class.getClassLoader()
            );
            for (SatelliteModule module : loader) {
                if (module != null && !hasModule(module.getId())) {
                    registerModule(module);
                }
            }
        } catch (Throwable t) {
            ModConstants.LOGGER.error("Failed to discover satellite modules via SPI", t);
        }
    }

    /**
     * Registers a satellite module into the lifecycle manager.
     * Modules are automatically sorted by priority (descending), then by ID (ascending).
     *
     * @param module The satellite module to register.
     */
    public static synchronized void registerModule(SatelliteModule module) {
        Objects.requireNonNull(module, "SatelliteModule cannot be null");
        if (hasModule(module.getId())) {
            Optional<SatelliteModule> existing = getModule(module.getId());
            if (existing.isPresent() && existing.get() == module) {
                return; // Idempotent: exact same instance already registered
            }
            ModConstants.LOGGER.warn("Overwriting or replacing previously registered satellite module: {}", module.getId());
            MODULES.removeIf(m -> m.getId().equals(module.getId()));
        }
        MODULES.add(module);
        MODULES.sort(MODULE_COMPARATOR);
        ModConstants.LOGGER.info("Registered satellite module: {} (priority={})", module.getId(), module.getPriority());
    }

    /**
     * Dispatches a specific lifecycle stage to all enabled satellite modules in priority order.
     * Exceptions thrown by individual modules are caught and logged to prevent cascade failures.
     *
     * @param stage The lifecycle stage to execute.
     */
    public static void fireStage(ModLifecycleStage stage) {
        Objects.requireNonNull(stage, "Lifecycle stage cannot be null");
        ModConstants.LOGGER.info("Firing lifecycle stage: {}", stage);

        List<SatelliteModule> modulesSnapshot;
        synchronized (ModLifecycleManager.class) {
            if (!discovered && MODULES.isEmpty()) {
                discoverModules();
            }
            modulesSnapshot = new ArrayList<>(MODULES);
        }

        for (SatelliteModule module : modulesSnapshot) {
            if (!module.isEnabled()) {
                ModConstants.LOGGER.debug("Skipping disabled satellite module: {}", module.getId());
                continue;
            }

            try {
                switch (stage) {
                    case REGISTRY -> module.onRegister();
                    case COMMON_SETUP -> module.onCommonSetup();
                    case CLIENT_SETUP -> module.onClientSetup();
                    case SERVER_STARTING -> module.onServerStarting();
                }
            } catch (Throwable t) {
                ModConstants.LOGGER.error("Error executing lifecycle stage {} on module '{}'", stage, module.getId(), t);
            }
        }
    }

    /**
     * Returns an unmodifiable snapshot of currently registered satellite modules.
     *
     * @return List of registered modules.
     */
    public static synchronized List<SatelliteModule> getRegisteredModules() {
        return Collections.unmodifiableList(new ArrayList<>(MODULES));
    }

    /**
     * Looks up a registered module by its unique identifier.
     *
     * @param id The module identifier.
     * @return An {@link Optional} containing the module if registered, or empty.
     */
    public static synchronized Optional<SatelliteModule> getModule(String id) {
        if (id == null) {
            return Optional.empty();
        }
        return MODULES.stream().filter(m -> id.equals(m.getId())).findFirst();
    }

    /**
     * Checks if a module with the given identifier is registered.
     *
     * @param id The module identifier.
     * @return {@code true} if present, {@code false} otherwise.
     */
    public static synchronized boolean hasModule(String id) {
        if (id == null) {
            return false;
        }
        return MODULES.stream().anyMatch(m -> id.equals(m.getId()));
    }

    /**
     * Clears all registered modules. Intended primarily for testing environments.
     */
    public static synchronized void clearModules() {
        MODULES.clear();
        discovered = false;
        ModConstants.LOGGER.debug("Cleared all registered satellite modules.");
    }

    /**
     * Returns whether SPI discovery has already been executed.
     *
     * @return {@code true} if modules were discovered.
     */
    public static synchronized boolean isDiscovered() {
        return discovered;
    }
}
