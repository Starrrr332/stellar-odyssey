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
 * Thread-safe registry catalog for rocket tiers.
 * <p>
 * Follows the same extensibility contract as {@link AlienMineralTierRegistry}:
 * add-on modules can register custom rocket tiers at runtime while duplicate
 * registrations fail fast.
 * </p>
 */
public final class RocketTierRegistry {

    private static final Map<Integer, RocketTier> BY_LEVEL = new ConcurrentSkipListMap<>();
    private static final Map<String, RocketTier> BY_NAME = new ConcurrentHashMap<>();

    static {
        RocketTiers.registerBuiltinTiers();
    }

    private RocketTierRegistry() {
    }

    public static void registerTier(RocketTier tier) {
        Objects.requireNonNull(tier, "Rocket tier cannot be null");
        if (BY_LEVEL.containsKey(tier.tierLevel())) {
            throw new IllegalArgumentException(
                    "Duplicate rocket tier level: " + tier.tierLevel() + " (attempted to register '" + tier.name() + "')"
            );
        }
        String normalizedName = tier.name().toLowerCase(Locale.ROOT);
        if (BY_NAME.containsKey(normalizedName)) {
            throw new IllegalArgumentException(
                    "Duplicate rocket tier name: '" + tier.name() + "' (level " + tier.tierLevel() + ")"
            );
        }
        BY_LEVEL.put(tier.tierLevel(), tier);
        BY_NAME.put(normalizedName, tier);
    }

    public static Optional<RocketTier> getTier(int level) {
        return Optional.ofNullable(BY_LEVEL.get(level));
    }

    public static Optional<RocketTier> getTier(String name) {
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_NAME.get(name.toLowerCase(Locale.ROOT)));
    }

    public static Collection<RocketTier> getAllTiers() {
        return Collections.unmodifiableCollection(BY_LEVEL.values());
    }

    public static int getTierCount() {
        return BY_LEVEL.size();
    }

    public static boolean hasTier(int level) {
        return BY_LEVEL.containsKey(level);
    }
}
