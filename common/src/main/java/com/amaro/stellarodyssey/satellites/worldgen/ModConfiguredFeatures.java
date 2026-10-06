package com.amaro.stellarodyssey.satellites.worldgen;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.registry.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.BlockReplacement;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockMatchTest;
import net.minecraft.world.level.levelgen.structure.templatesystem.RuleTest;

import java.util.List;
import java.util.Objects;

/**
 * ResourceKeys and factory builders for planetary configured features, crystal formations,
 * and subterranean alien ore distributions.
 */
public final class ModConfiguredFeatures {

    // --- Feature Registry Keys (Configured Features) ---

    public static final ResourceKey<Feature> ALIEN_ORE_VEIN = ResourceKey.create(
            Registries.FEATURE,
            StellarOdyssey.id("alien_ore_vein")
    );

    public static final ResourceKey<Feature> ALIEN_ORE_LARGE_VEIN = ResourceKey.create(
            Registries.FEATURE,
            StellarOdyssey.id("alien_ore_large_vein")
    );

    public static final ResourceKey<Feature> CRYSTAL_SPIRE_FORMATION = ResourceKey.create(
            Registries.FEATURE,
            StellarOdyssey.id("crystal_spire_formation")
    );

    public static final ResourceKey<Feature> EXOTIC_ORE_POCKET = ResourceKey.create(
            Registries.FEATURE,
            StellarOdyssey.id("exotic_ore_pocket")
    );

    public static final ResourceKey<Feature> METEORIC_IRON_CLUSTER = ResourceKey.create(
            Registries.FEATURE,
            StellarOdyssey.id("meteoric_iron_cluster")
    );

    public static final ResourceKey<Feature> BIOLUMINESCENT_CRYSTAL_CLUSTER = ResourceKey.create(
            Registries.FEATURE,
            StellarOdyssey.id("bioluminescent_crystal_cluster")
    );

    // --- Tiered alien mineral ore veins (celidium T1 / verdantite T2 / astralite T3) ---

    public static final ResourceKey<Feature> CELIDIUM_ORE_VEIN = ResourceKey.create(
            Registries.FEATURE,
            StellarOdyssey.id("celidium_ore_vein")
    );

    public static final ResourceKey<Feature> VERDANTITE_ORE_VEIN = ResourceKey.create(
            Registries.FEATURE,
            StellarOdyssey.id("verdantite_ore_vein")
    );

    public static final ResourceKey<Feature> ASTRALITE_ORE_VEIN = ResourceKey.create(
            Registries.FEATURE,
            StellarOdyssey.id("astralite_ore_vein")
    );

    // --- Placed Feature Registry Keys ---

    public static final ResourceKey<PlacedFeature> ALIEN_ORE_VEIN_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE,
            StellarOdyssey.id("alien_ore_vein_placed")
    );

    public static final ResourceKey<PlacedFeature> ALIEN_ORE_LARGE_VEIN_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE,
            StellarOdyssey.id("alien_ore_large_vein_placed")
    );

    public static final ResourceKey<PlacedFeature> CRYSTAL_SPIRE_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE,
            StellarOdyssey.id("crystal_spire_placed")
    );

    public static final ResourceKey<PlacedFeature> EXOTIC_ORE_POCKET_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE,
            StellarOdyssey.id("exotic_ore_pocket_placed")
    );

    public static final ResourceKey<PlacedFeature> METEORIC_IRON_CLUSTER_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE,
            StellarOdyssey.id("meteoric_iron_cluster_placed")
    );

    public static final ResourceKey<PlacedFeature> CELIDIUM_ORE_VEIN_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE,
            StellarOdyssey.id("celidium_ore_vein_placed")
    );

    public static final ResourceKey<PlacedFeature> VERDANTITE_ORE_VEIN_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE,
            StellarOdyssey.id("verdantite_ore_vein_placed")
    );

    public static final ResourceKey<PlacedFeature> ASTRALITE_ORE_VEIN_PLACED = ResourceKey.create(
            Registries.PLACED_FEATURE,
            StellarOdyssey.id("astralite_ore_vein_placed")
    );

    // --- Planetary Ore Distribution Profiles ---

    /**
     * Immutable specification for planetary ore generation parameters.
     *
     * @param veinSize                   Target number of ore blocks per vein cluster.
     * @param veinsPerChunk              Spawn attempts per chunk column.
     * @param minY                       Minimum world Y elevation.
     * @param maxY                       Maximum world Y elevation.
     * @param discardChanceOnAirExposure Probability of skipping placement when exposed directly to open air/vacuum.
     */
    public record OreDistributionConfig(
            int veinSize,
            int veinsPerChunk,
            int minY,
            int maxY,
            float discardChanceOnAirExposure
    ) {
        public OreDistributionConfig {
            if (veinSize <= 0) {
                throw new IllegalArgumentException("veinSize must be > 0");
            }
            if (veinsPerChunk < 0) {
                throw new IllegalArgumentException("veinsPerChunk cannot be negative");
            }
            if (minY >= maxY) {
                throw new IllegalArgumentException("minY must be strictly less than maxY");
            }
        }
    }

    public static final OreDistributionConfig STANDARD_ALIEN_ORE = new OreDistributionConfig(
            9,    // 9 blocks per cluster
            14,   // 14 veins per chunk
            -48,  // Deep subterranean lower bound
            112,  // Mid-altitude upper bound
            0.15f // 15% air exposure discard
    );

    public static final OreDistributionConfig LARGE_CRYSTAL_VEIN = new OreDistributionConfig(
            18,   // 18 blocks per mega-vein
            4,    // 4 clusters per chunk
            -64,  // Bedrock floor
            32,   // Lower crust
            0.05f // 5% air exposure discard
    );

    public static final OreDistributionConfig METEORIC_CLUSTER = new OreDistributionConfig(
            12,   // 12 blocks per meteoric pocket
            8,    // 8 attempts per chunk
            40,   // Surface to high ridge
            180,  // High mountain peak craters
            0.35f // 35% air exposure discard
    );

    public static final OreDistributionConfig DEEP_EXOTIC_POCKET = new OreDistributionConfig(
            24,   // 24 blocks concentrated pocket
            2,    // Rare (2 per chunk)
            -64,  // Deep mantle
            -16,  // Deep underground
            0.00f // 0% discard (full subterranean protection)
    );

    /** Celidium (Tier 1): the most common alien mineral, spread across the upper crust. */
    public static final OreDistributionConfig CELIDIUM_DISTRIBUTION = new OreDistributionConfig(
            9,     // 9 blocks per vein
            8,     // 8 veins per chunk
            -64,   // Bedrock floor
            112,   // Mid-altitude upper bound
            0.10f  // 10% air exposure discard
    );

    /** Verdantite (Tier 2): rarer, confined to the middle strata. */
    public static final OreDistributionConfig VERDANTITE_DISTRIBUTION = new OreDistributionConfig(
            6,     // 6 blocks per vein
            4,     // 4 veins per chunk
            -48,   // Deep lower bound
            48,    // Shallow upper bound
            0.05f  // 5% air exposure discard
    );

    /** Astralite (Tier 3): the rarest tier, only deep subterranean pockets. */
    public static final OreDistributionConfig ASTRALITE_DISTRIBUTION = new OreDistributionConfig(
            4,     // 4 blocks per vein
            2,     // 2 veins per chunk
            -64,   // Deep mantle
            8,     // Deep crust only
            0.00f  // 0% discard (full subterranean protection)
    );

    private ModConfiguredFeatures() {
    }

    /**
     * Builds a genuine {@link OreFeature} targeting alien stone and depositing alien crystalline ore.
     *
     * @param veinSize                   Number of ore blocks per vein.
     * @param discardChanceOnAirExposure Air exposure discard probability.
     * @return Configured {@link OreFeature} instance.
     */
    public static OreFeature createAlienOreFeature(int veinSize, float discardChanceOnAirExposure) {
        RuleTest stoneTarget = new BlockMatchTest(ModBlocks.ALIEN_STONE.get());
        BlockReplacement replacement = BlockReplacement.replace(
                stoneTarget,
                ModBlocks.ALIEN_ORE.get().defaultBlockState()
        );
        return new OreFeature(List.of(replacement), veinSize, discardChanceOnAirExposure);
    }

    /**
     * Builds a deep concentrated exotic ore pocket feature.
     *
     * @param pocketSize Size of the deep ore pocket.
     * @return Configured {@link OreFeature} instance.
     */
    public static OreFeature createExoticPocketFeature(int pocketSize) {
        return createAlienOreFeature(pocketSize, 0.0f);
    }

    /**
     * Builds a genuine {@link OreFeature} that deposits a specific alien mineral ore into both
     * vanilla stone strata and the mod's own alien stone, so the tiered minerals spawn on every
     * procedural planet regardless of which surface rules are active.
     *
     * @param oreBlock                   The mineral ore block to deposit.
     * @param veinSize                   Number of ore blocks per vein.
     * @param discardChanceOnAirExposure Air exposure discard probability.
     * @return Configured {@link OreFeature} instance.
     */
    public static OreFeature createMineralOreFeature(
            net.minecraft.world.level.block.Block oreBlock,
            int veinSize,
            float discardChanceOnAirExposure
    ) {
        Objects.requireNonNull(oreBlock, "oreBlock cannot be null");
        List<BlockReplacement> replacements = List.of(
                BlockReplacement.replace(
                        new BlockMatchTest(ModBlocks.ALIEN_STONE.get()),
                        oreBlock.defaultBlockState()
                ),
                BlockReplacement.replace(
                        new BlockMatchTest(net.minecraft.world.level.block.Blocks.STONE),
                        oreBlock.defaultBlockState()
                ),
                BlockReplacement.replace(
                        new BlockMatchTest(net.minecraft.world.level.block.Blocks.DEEPSLATE),
                        oreBlock.defaultBlockState()
                )
        );
        return new OreFeature(replacements, veinSize, discardChanceOnAirExposure);
    }

    /**
     * Builds the configured ore feature for a built-in alien mineral tier.
     *
     * @param featureKey The configured feature {@link ResourceKey} identifying the mineral.
     * @return Configured {@link OreFeature} instance for the matching tier.
     */
    public static OreFeature createMineralOreFeature(ResourceKey<Feature> featureKey) {
        Objects.requireNonNull(featureKey, "featureKey cannot be null");
        if (featureKey.equals(VERDANTITE_ORE_VEIN)) {
            return createMineralOreFeature(ModBlocks.VERDANTITE_ORE.get(),
                    VERDANTITE_DISTRIBUTION.veinSize(), VERDANTITE_DISTRIBUTION.discardChanceOnAirExposure());
        } else if (featureKey.equals(ASTRALITE_ORE_VEIN)) {
            return createMineralOreFeature(ModBlocks.ASTRALITE_ORE.get(),
                    ASTRALITE_DISTRIBUTION.veinSize(), ASTRALITE_DISTRIBUTION.discardChanceOnAirExposure());
        }
        return createMineralOreFeature(ModBlocks.CELIDIUM_ORE.get(),
                CELIDIUM_DISTRIBUTION.veinSize(), CELIDIUM_DISTRIBUTION.discardChanceOnAirExposure());
    }

    /**
     * Retrieves the recommended ore distribution config for the given configured feature key.
     *
     * @param featureKey The feature resource key.
     * @return The associated {@link OreDistributionConfig}.
     */
    public static OreDistributionConfig getDistributionConfig(ResourceKey<Feature> featureKey) {
        Objects.requireNonNull(featureKey, "featureKey cannot be null");
        if (featureKey.equals(ALIEN_ORE_LARGE_VEIN)) {
            return LARGE_CRYSTAL_VEIN;
        } else if (featureKey.equals(METEORIC_IRON_CLUSTER)) {
            return METEORIC_CLUSTER;
        } else if (featureKey.equals(EXOTIC_ORE_POCKET)) {
            return DEEP_EXOTIC_POCKET;
        } else if (featureKey.equals(CELIDIUM_ORE_VEIN)) {
            return CELIDIUM_DISTRIBUTION;
        } else if (featureKey.equals(VERDANTITE_ORE_VEIN)) {
            return VERDANTITE_DISTRIBUTION;
        } else if (featureKey.equals(ASTRALITE_ORE_VEIN)) {
            return ASTRALITE_DISTRIBUTION;
        }
        return STANDARD_ALIEN_ORE;
    }

    /**
     * Initializes and registers configured feature definitions.
     */
    public static void register() {
        StellarOdyssey.LOGGER.debug("ModConfiguredFeatures: configured features and ore distribution profiles ready.");
    }
}
