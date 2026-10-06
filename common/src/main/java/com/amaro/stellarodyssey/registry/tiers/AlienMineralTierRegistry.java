package com.amaro.stellarodyssey.registry.tiers;

import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Thread-safe registry catalog for alien mineral tiers.
 * <p>
 * Satisfies mod extensibility requirements by allowing runtime registration
 * of custom celestial minerals (Tier 6+, add-ons, or expansion modules)
 * while failing fast on duplicate tier registrations.
 * </p>
 */
public final class AlienMineralTierRegistry {

    private static final Map<Integer, IAlienMineralTier> BY_LEVEL = new ConcurrentSkipListMap<>();
    private static final Map<String, IAlienMineralTier> BY_NAME = new ConcurrentHashMap<>();

    static {
        AlienMineralTier.registerBuiltinTiers();
    }

    private AlienMineralTierRegistry() {
    }

    /**
     * Registers a new alien mineral tier in the catalog.
     *
     * @param tier the tier implementation to register
     * @throws NullPointerException if tier is null
     * @throws IllegalArgumentException if a tier with the same level or name is already registered
     */
    public static void registerTier(IAlienMineralTier tier) {
        Objects.requireNonNull(tier, "Tier cannot be null");
        if (BY_LEVEL.containsKey(tier.getTierLevel())) {
            throw new IllegalArgumentException(
                    "Duplicate mineral tier level: " + tier.getTierLevel() + " (attempted to register '" + tier.getName() + "')"
            );
        }
        String normalizedName = tier.getName().toLowerCase(Locale.ROOT);
        if (BY_NAME.containsKey(normalizedName)) {
            throw new IllegalArgumentException(
                    "Duplicate mineral tier name: '" + tier.getName() + "' (level " + tier.getTierLevel() + ")"
            );
        }

        BY_LEVEL.put(tier.getTierLevel(), tier);
        BY_NAME.put(normalizedName, tier);
    }

    /**
     * Retrieves an alien mineral tier by its integer rank.
     *
     * @param level tier rank level (e.g. 1 to 5)
     * @return Optional containing the tier, or empty if unregistered
     */
    public static Optional<IAlienMineralTier> getTier(int level) {
        return Optional.ofNullable(BY_LEVEL.get(level));
    }

    /**
     * Retrieves an alien mineral tier by its path name (case-insensitive).
     *
     * @param name tier path name (e.g. "celidium", "Celidium")
     * @return Optional containing the tier, or empty if unregistered
     */
    public static Optional<IAlienMineralTier> getTier(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_NAME.get(name.toLowerCase(Locale.ROOT)));
    }

    /**
     * Returns an unmodifiable collection of all currently registered tiers,
     * ordered by tier level.
     */
    public static Collection<IAlienMineralTier> getAllTiers() {
        return Collections.unmodifiableCollection(BY_LEVEL.values());
    }

    /**
     * Returns the total count of registered tiers.
     */
    public static int getTierCount() {
        return BY_LEVEL.size();
    }

    /**
     * Checks if a tier with the given level exists.
     */
    public static boolean hasTier(int level) {
        return BY_LEVEL.containsKey(level);
    }

    /**
     * Checks if a tier with the given name exists.
     */
    public static boolean hasTier(String name) {
        if (name == null) {
            return false;
        }
        return BY_NAME.containsKey(name.toLowerCase(Locale.ROOT));
    }
}
