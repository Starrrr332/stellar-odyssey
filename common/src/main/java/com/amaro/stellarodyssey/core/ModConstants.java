package com.amaro.stellarodyssey.core;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global constants and identifier factory for Stellar Odyssey.
 * Serves as the single source of truth for mod identification across all loaders and modules.
 */
public final class ModConstants {
    /** Mod ID used for registration, networking, datapacks, and asset namespaces. */
    public static final String MOD_ID = "stellarodyssey";

    /** Human-readable display name for the mod. */
    public static final String MOD_NAME = "Stellar Odyssey";

    /** Mod-wide SLF4J logger instance. */
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private ModConstants() {
    }

    /**
     * Constructs a namespaced {@link Identifier} under {@code stellarodyssey:<path>}.
     *
     * @param path The path of the resource or registry entry.
     * @return A valid {@link Identifier} under the mod's namespace.
     */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
