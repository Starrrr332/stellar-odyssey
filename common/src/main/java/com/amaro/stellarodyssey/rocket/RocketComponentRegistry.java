package com.amaro.stellarodyssey.rocket;

import com.amaro.stellarodyssey.registry.tiers.RocketTier;
import com.amaro.stellarodyssey.registry.tiers.RocketTierRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defines which component types each rocket tier requires for assembly.
 * <p>
 * Progression mirrors Galacticraft/Ad Astra: higher tiers demand more (and more
 * expensive) components. Tier 1 needs 4 types, Tier 2 needs 6, Tier 3 needs 8.
 * </p>
 */
public final class RocketComponentRegistry {

    /** Canonical component types, ordered by complexity. */
    public static final List<String> COMPONENT_TYPES = List.of(
            "cone", "fin", "tank", "engine", "plate", "thruster", "guidance", "heat_shield"
    );

    private static final Map<Integer, List<String>> REQUIRED_BY_TIER = new ConcurrentHashMap<>();

    static {
        registerBuiltinRequirements();
    }

    private RocketComponentRegistry() {
    }

    private static void registerBuiltinRequirements() {
        RocketTierRegistry.getAllTiers().forEach(RocketComponentRegistry::registerRequirements);
    }

    /**
     * Registers the component set for a tier. The first {@code tier.componentCount()}
     * canonical types are required, in order.
     */
    public static void registerRequirements(RocketTier tier) {
        Objects.requireNonNull(tier, "Tier cannot be null");
        int count = tier.componentCount();
        if (count < 1 || count > COMPONENT_TYPES.size()) {
            throw new IllegalArgumentException(
                    "Tier '" + tier.name() + "' componentCount " + count + " out of range [1, " + COMPONENT_TYPES.size() + "]"
            );
        }
        REQUIRED_BY_TIER.put(tier.tierLevel(), List.copyOf(COMPONENT_TYPES.subList(0, count)));
    }

    /** @return the component types required to assemble a rocket of the given tier. */
    public static List<String> getRequiredComponents(RocketTier tier) {
        List<String> required = REQUIRED_BY_TIER.get(tier.tierLevel());
        if (required == null) {
            throw new IllegalArgumentException("No component requirements registered for tier " + tier.tierLevel());
        }
        return required;
    }

    public static Collection<List<String>> getAllRequirements() {
        return Collections.unmodifiableCollection(REQUIRED_BY_TIER.values());
    }

    public static int getRequirementCount() {
        return REQUIRED_BY_TIER.size();
    }
}
