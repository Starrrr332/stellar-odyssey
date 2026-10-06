package com.amaro.stellarodyssey.satellites.worldgen;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.registry.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.material.MaterialRules;
import net.minecraft.world.level.levelgen.material.condition.MaterialCondition;
import net.minecraft.world.level.levelgen.material.rule.MaterialRule;
import net.minecraft.world.level.levelgen.placement.CaveSurface;

import java.util.Objects;

/**
 * Procedural surface rule builders for alien planetary terrain.
 * <p>
 * Constructs {@link MaterialRule} chains that dynamically place:
 * <ul>
 *   <li>Bioluminescent alien turf on top surfaces and valley floors</li>
 *   <li>Metamorphic alien stone on steep cliffs and subsurfaces</li>
 *   <li>Exposed and subterranean alien ore crystal pockets on ridge crests and crater basins</li>
 * </ul>
 */
public final class PlanetarySurfaceRules {

    // --- Material Rule ResourceKeys ---

    public static final ResourceKey<MaterialRule> ALIEN_SURFACE_RULE = ResourceKey.create(
            Registries.MATERIAL_RULE,
            StellarOdyssey.id("alien_surface")
    );

    public static final ResourceKey<MaterialRule> PROXIMA_B_SURFACE_RULE = ResourceKey.create(
            Registries.MATERIAL_RULE,
            StellarOdyssey.id("proxima_b_surface")
    );

    public static final ResourceKey<MaterialRule> EXOTIC_PLANET_SURFACE_RULE = ResourceKey.create(
            Registries.MATERIAL_RULE,
            StellarOdyssey.id("exotic_planet_surface")
    );

    public static final ResourceKey<MaterialRule> NEXUS_MOON_SURFACE_RULE = ResourceKey.create(
            Registries.MATERIAL_RULE,
            StellarOdyssey.id("nexus_moon_surface")
    );

    private PlanetarySurfaceRules() {
    }

    /**
     * Creates the complete procedural surface rule for standard alien terrestrial planets.
     *
     * @return Assembled {@link MaterialRule}.
     */
    public static MaterialRule createAlienPlanetSurfaceRule() {
        BlockState turf = ModBlocks.ALIEN_TURF.get().defaultBlockState();
        BlockState stone = ModBlocks.ALIEN_STONE.get().defaultBlockState();
        BlockState ore = ModBlocks.ALIEN_ORE.get().defaultBlockState();

        return buildSurfaceRule(turf, stone, ore, 0.45, 0.85);
    }

    /**
     * Creates the procedural surface rule tailored for Proxima B.
     * Features harsh exposed ridges with crystal outcrops, steep cliff scarps,
     * and patches of bioluminescent turf in protected hollows.
     *
     * @return Assembled {@link MaterialRule} for Proxima B.
     */
    public static MaterialRule createProximaBSurfaceRule() {
        BlockState turf = ModBlocks.ALIEN_TURF.get().defaultBlockState();
        BlockState stone = ModBlocks.ALIEN_STONE.get().defaultBlockState();
        BlockState ore = ModBlocks.ALIEN_ORE.get().defaultBlockState();

        MaterialRule oreState = MaterialRules.state(ore);
        MaterialRule turfState = MaterialRules.state(turf);
        MaterialRule stoneState = MaterialRules.state(stone);

        // Ridge crest exposed crystal veins
        MaterialCondition ridgeExposed = MaterialRules.noiseCondition2d(PlanetaryNoiseSettings.PLANETARY_RIDGE, 0.40, 0.90);
        MaterialCondition surfaceCheck = MaterialRules.stoneDepthCheck(0, false, CaveSurface.FLOOR);
        MaterialRule exposedCrystals = MaterialRules.ifTrue(ridgeExposed, MaterialRules.ifTrue(surfaceCheck, oreState));

        // Subterranean crystal pocket condition
        MaterialCondition craterPocket = MaterialRules.noiseCondition2d(PlanetaryNoiseSettings.EXOTIC_CRATER, 0.50, 0.95);
        MaterialCondition shallowSubsurface = MaterialRules.stoneDepthCheck(3, false, CaveSurface.FLOOR);
        MaterialRule subCrystals = MaterialRules.ifTrue(craterPocket, MaterialRules.ifTrue(shallowSubsurface, oreState));

        // Flat/gentle surface gets alien turf; steep slopes stay bare alien stone
        MaterialCondition isSteep = MaterialRules.steep();
        MaterialRule surfaceLayer = MaterialRules.ifTrue(surfaceCheck,
                MaterialRules.ifTrue(MaterialRules.not(isSteep), turfState));

        // Subsurface layer down to 4 blocks depth
        MaterialCondition subsurfaceCheck = MaterialRules.stoneDepthCheck(4, false, CaveSurface.FLOOR);
        MaterialRule subsurfaceStone = MaterialRules.ifTrue(subsurfaceCheck, stoneState);

        return MaterialRules.sequence(
                exposedCrystals,
                subCrystals,
                surfaceLayer,
                subsurfaceStone,
                stoneState
        );
    }

    /**
     * Creates the airless regolith surface rule for Nexus Moon.
     * Being an airless satellite, there is no biological turf; the surface consists of
     * pulverized alien stone regolith with meteoric crystal deposits.
     *
     * @return Assembled {@link MaterialRule} for Nexus Moon.
     */
    public static MaterialRule createNexusMoonSurfaceRule() {
        BlockState stone = ModBlocks.ALIEN_STONE.get().defaultBlockState();
        BlockState ore = ModBlocks.ALIEN_ORE.get().defaultBlockState();

        MaterialRule stoneState = MaterialRules.state(stone);
        MaterialRule oreState = MaterialRules.state(ore);

        // Meteoric impact crystal veins
        MaterialCondition impactCondition = MaterialRules.noiseCondition2d(PlanetaryNoiseSettings.EXOTIC_CRATER, 0.60, 0.95);
        MaterialCondition surfaceCheck = MaterialRules.stoneDepthCheck(1, false, CaveSurface.FLOOR);
        MaterialRule impactOre = MaterialRules.ifTrue(impactCondition, MaterialRules.ifTrue(surfaceCheck, oreState));

        return MaterialRules.sequence(
                impactOre,
                stoneState
        );
    }

    /**
     * Flexible procedural builder for constructing planetary surface rules from arbitrary block states.
     *
     * @param surfaceBlock    BlockState to place on the primary surface (e.g. alien turf).
     * @param subsurfaceBlock BlockState to place in the subsurface strata (e.g. alien stone).
     * @param orePocketBlock  BlockState to place inside noise-driven crystal pockets (e.g. alien ore).
     * @param oreNoiseMin     Minimum noise threshold for ore vein activation.
     * @param oreNoiseMax     Maximum noise threshold for ore vein activation.
     * @return Assembled {@link MaterialRule}.
     */
    public static MaterialRule buildSurfaceRule(
            BlockState surfaceBlock,
            BlockState subsurfaceBlock,
            BlockState orePocketBlock,
            double oreNoiseMin,
            double oreNoiseMax
    ) {
        Objects.requireNonNull(surfaceBlock, "surfaceBlock cannot be null");
        Objects.requireNonNull(subsurfaceBlock, "subsurfaceBlock cannot be null");
        Objects.requireNonNull(orePocketBlock, "orePocketBlock cannot be null");

        MaterialRule surfaceState = MaterialRules.state(surfaceBlock);
        MaterialRule subsurfaceState = MaterialRules.state(subsurfaceBlock);
        MaterialRule oreState = MaterialRules.state(orePocketBlock);

        // 1. Crystal ore pockets on high-stress noise contours
        MaterialCondition oreNoise = MaterialRules.noiseCondition2d(
                PlanetaryNoiseSettings.PLANETARY_RIDGE,
                oreNoiseMin,
                oreNoiseMax
        );
        MaterialCondition shallowDepth = MaterialRules.stoneDepthCheck(2, false, CaveSurface.FLOOR);
        MaterialRule orePocketRule = MaterialRules.ifTrue(oreNoise, MaterialRules.ifTrue(shallowDepth, oreState));

        // 2. Primary surface layer (alien turf) on non-steep floor
        MaterialCondition isFloor = MaterialRules.stoneDepthCheck(0, false, CaveSurface.FLOOR);
        MaterialCondition isSteep = MaterialRules.steep();
        MaterialRule surfaceRule = MaterialRules.ifTrue(isFloor,
                MaterialRules.ifTrue(MaterialRules.not(isSteep), surfaceState));

        // 3. Subsurface stratum (alien stone)
        MaterialCondition subsurfaceDepth = MaterialRules.stoneDepthCheck(5, false, CaveSurface.FLOOR);
        MaterialRule subsurfaceRule = MaterialRules.ifTrue(subsurfaceDepth, subsurfaceState);

        return MaterialRules.sequence(
                orePocketRule,
                surfaceRule,
                subsurfaceRule,
                subsurfaceState
        );
    }

    /**
     * Initializes and registers procedural surface rule components.
     */
    public static void register() {
        StellarOdyssey.LOGGER.debug("PlanetarySurfaceRules: procedural material rules registered.");
    }
}
