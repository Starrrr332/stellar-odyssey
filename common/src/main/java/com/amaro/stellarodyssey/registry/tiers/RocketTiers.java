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
            0.8F
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
            1.0F
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
            1.3F
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

    static {
        registerBuiltinTiers();
    }
}
