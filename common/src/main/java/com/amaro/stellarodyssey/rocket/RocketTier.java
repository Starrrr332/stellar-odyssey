package com.amaro.stellarodyssey.rocket;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Tier destination validation helper in the rocket subsystem.
 */
public final class RocketTier {
    private RocketTier() {
    }

    public static int getRequiredTier(ResourceKey<Level> destination) {
        return com.amaro.stellarodyssey.registry.tiers.RocketTiers.getRequiredTier(destination);
    }

    public static int minTierForDestination(ResourceKey<Level> destination) {
        return com.amaro.stellarodyssey.registry.tiers.RocketTiers.getRequiredTier(destination);
    }

    public static boolean isDestinationAllowed(int tierLevel, ResourceKey<Level> destination) {
        return com.amaro.stellarodyssey.registry.tiers.RocketTiers.isDestinationAllowed(tierLevel, destination);
    }
}
