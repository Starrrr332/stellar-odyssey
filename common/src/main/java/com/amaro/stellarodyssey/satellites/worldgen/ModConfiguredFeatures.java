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
