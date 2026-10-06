package com.amaro.stellarodyssey.registry.tiers;

import com.amaro.stellarodyssey.world.ModDimensions;

/**
 * Canonical rocket tiers of Stellar Odyssey.
 * <p>
 * Progression loop (Galacticraft style): each tier's destination holds the
 * mineral that unlocks the next tier's components.
 * </p>
 * <ul>
 *   <li><b>Tier 1 "Pioneer"</b> -&gt; Nexus Moon. Overworld materials (iron/copper).</li>
 *   <li><b>Tier 2 "Voyager"</b> -&gt; Proxima B. Celidium (mined on Nexus Moon / Proxima B).</li>
 *   <li><b>Tier 3 "Odyssey"</b> -&gt; Exotic Prime. Verdantite + Astralite (mined on Proxima B / Exotic Prime).</li>
 * </ul>
 */
public enum RocketTiers {
    TIER_1(RocketTier.create(
            1,
            "pioneer",
            ModDimensions.NEXUS_MOON,
            3000,
            400.0,
            100,
            4,
            0xE2E8F0,
            0.8F,
            200,   // ascentTicks
            80,    // warpChargeTicks
            300.0  // atmosphereExitAltitude
    )),
    TIER_2(RocketTier.create(
            2,
            "voyager",
            ModDimensions.PROXIMA_B,
            6000,
            600.0,
            160,
            6,
            0xD97724,
            1.0F,
            240,
            100,
            450.0
    )),
    TIER_3(RocketTier.create(
            3,
            "odyssey",
            ModDimensions.EXOTIC_PRIME,
            10000,
            800.0,
            240,
            8,
            0x06B6D4,
            1.3F,
            280,
            120,
            600.0
    ));

    private final RocketTier tier;
    private static volatile boolean registered = false;

    RocketTiers(RocketTier tier) {
        this.tier = tier;
    }

    public RocketTier get() {
        return this.tier;
    }

    public static synchronized void registerBuiltinTiers() {
        if (registered) {
            return;
        }
        registered = true;
        for (RocketTiers t : values()) {
            RocketTierRegistry.registerTier(t.tier);
        }
    }

    /**
     * Determines the minimum rocket tier required to reach the given destination dimension.
     * <ul>
     *   <li>NEXUS_MOON -&gt; Tier 1 (Pioneer)</li>
     *   <li>OVERWORLD -&gt; Tier 1 (Any)</li>
     *   <li>PROXIMA_B -&gt; Tier 2 (Voyager)</li>
     *   <li>EXOTIC_PRIME -&gt; Tier 3 (Odyssey)</li>
     *   <li>GLIESE_DEEP -&gt; Tier 3 (Odyssey)</li>
     * </ul>
     * Returns -1 if the destination is null or unknown/unregistered.
     */
    public static int getRequiredTier(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> destination) {
        if (destination == null) {
            return -1;
        }
        net.minecraft.resources.Identifier id = destination.identifier();
        if (id.equals(ModDimensions.NEXUS_MOON.identifier()) || id.equals(net.minecraft.world.level.Level.OVERWORLD.identifier())) {
            return 1;
        }
        if (id.equals(ModDimensions.PROXIMA_B.identifier())) {
            return 2;
        }
        if (id.equals(ModDimensions.EXOTIC_PRIME.identifier()) || id.equals(ModDimensions.GLIESE_DEEP.identifier())) {
            return 3;
        }
        return -1;
    }

    /**
     * Checks whether a rocket of the given tier level is permitted to travel to the specified destination.
     */
    public static boolean isDestinationAllowed(int tierLevel, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> destination) {
        if (destination == null || tierLevel < 1) {
            return false;
        }
        int required = getRequiredTier(destination);
        if (required <= 0) {
            return false;
        }
        return tierLevel >= required;
    }

    static {
        registerBuiltinTiers();
    }
}
