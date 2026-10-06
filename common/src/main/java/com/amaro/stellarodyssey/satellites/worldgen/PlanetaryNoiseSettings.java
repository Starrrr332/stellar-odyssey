package com.amaro.stellarodyssey.satellites.worldgen;

import com.amaro.stellarodyssey.StellarOdyssey;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.synth.NormalNoise;

import java.util.Arrays;
import java.util.Objects;

/**
 * Noise parameters, curves, and procedural terrain configuration for planetary bodies.
 * <p>
 * Provides decoupled registration keys for noise samplers (continentalness, erosion,
 * ridges, craters) and mathematical elevation profiles for realistic multi-frequency
 * alien planetary landscapes.
 */
public final class PlanetaryNoiseSettings {

    // --- Noise Parameter Registry Keys ---

    public static final ResourceKey<NormalNoise> PLANETARY_CONTINENTALNESS = ResourceKey.create(
            Registries.NOISE,
            StellarOdyssey.id("planetary_continentalness")
    );

    public static final ResourceKey<NormalNoise> PLANETARY_EROSION = ResourceKey.create(
            Registries.NOISE,
            StellarOdyssey.id("planetary_erosion")
    );

    public static final ResourceKey<NormalNoise> PLANETARY_RIDGE = ResourceKey.create(
            Registries.NOISE,
            StellarOdyssey.id("planetary_ridge")
    );

    public static final ResourceKey<NormalNoise> EXOTIC_CRATER = ResourceKey.create(
            Registries.NOISE,
            StellarOdyssey.id("exotic_crater")
    );

    public static final ResourceKey<NormalNoise> ALIEN_CANYON = ResourceKey.create(
            Registries.NOISE,
            StellarOdyssey.id("alien_canyon")
    );

    public static final ResourceKey<NormalNoise> CRYSTAL_RESONANCE = ResourceKey.create(
            Registries.NOISE,
            StellarOdyssey.id("crystal_resonance")
    );

    // --- Noise Generator Settings Registry Keys ---

    public static final ResourceKey<NoiseGeneratorSettings> PROXIMA_B_NOISE = ResourceKey.create(
            Registries.NOISE_SETTINGS,
            StellarOdyssey.id("proxima_b_noise")
    );

    public static final ResourceKey<NoiseGeneratorSettings> EXOTIC_PLANET_NOISE = ResourceKey.create(
            Registries.NOISE_SETTINGS,
            StellarOdyssey.id("exotic_planet_noise")
    );

    public static final ResourceKey<NoiseGeneratorSettings> NEXUS_MOON_NOISE = ResourceKey.create(
            Registries.NOISE_SETTINGS,
            StellarOdyssey.id("nexus_moon_noise")
    );

    // --- Configurable Noise Curves ---

    /**
     * Immutable representation of an octave noise curve configuration.
     *
     * @param firstOctave The lowest octave frequency index.
     * @param amplitudes  Array of octave amplitudes defining curve harmonic decay.
     */
    public record NoiseCurve(int firstOctave, double[] amplitudes) {
        public NoiseCurve {
            Objects.requireNonNull(amplitudes, "Amplitudes cannot be null");
            if (amplitudes.length == 0) {
                throw new IllegalArgumentException("Amplitudes array cannot be empty");
            }
            amplitudes = amplitudes.clone();
        }

        public static NoiseCurve of(int firstOctave, double... amplitudes) {
            return new NoiseCurve(firstOctave, amplitudes);
        }

        /**
         * Evaluates cumulative weight of harmonics.
         *
         * @return Sum of all amplitude coefficients.
         */
        public double totalAmplitude() {
            double sum = 0.0;
            for (double a : amplitudes) {
                sum += Math.abs(a);
            }
            return sum;
        }

        @Override
        public String toString() {
            return "NoiseCurve[firstOctave=" + firstOctave + ", amplitudes=" + Arrays.toString(amplitudes) + "]";
        }
    }

    public static final NoiseCurve CONTINENTALNESS_CURVE = NoiseCurve.of(-8, 1.0, 1.5, 2.0, 1.0, 0.5);
    public static final NoiseCurve EROSION_CURVE = NoiseCurve.of(-7, 1.0, 1.2, 0.8, 0.4);
    public static final NoiseCurve RIDGE_CURVE = NoiseCurve.of(-6, 1.0, 2.0, 1.5, 0.8, 0.3);
    public static final NoiseCurve CRATER_CURVE = NoiseCurve.of(-5, 1.5, 1.0, 0.5);

    // --- Planetary Terrain Profiles ---

    /**
     * Mathematical profile defining how noise parameters combine to generate planetary elevation.
     *
     * @param continentalnessWeight Macro-continental elevation scale.
     * @param erosionWeight          Factor by which erosion flattens or smooths peaks.
     * @param ridgeWeight            Exaggeration factor for exotic crystalline mountain ridges.
     * @param craterInfluence        Impact crater depression intensity.
     * @param baselineY              Base sea-level or datum elevation in world coordinates.
     * @param verticalScale          Total vertical amplitude dynamic range in blocks.
     * @param ridgeSharpness         Power exponent governing the steepness of planetary ridges.
     */
    public record TerrainProfile(
            double continentalnessWeight,
            double erosionWeight,
            double ridgeWeight,
            double craterInfluence,
            double baselineY,
            double verticalScale,
            double ridgeSharpness
    ) {
        /**
         * Computes the target surface elevation for a column given noise samples.
         *
         * @param continentalness Sample value in range [-1.0, 1.0].
         * @param erosion         Sample value in range [-1.0, 1.0].
         * @param ridge           Sample value in range [-1.0, 1.0].
         * @param crater          Sample value in range [-1.0, 1.0].
         * @return Computed world block Y height.
         */
        public double computeElevation(double continentalness, double erosion, double ridge, double crater) {
            double base = continentalness * continentalnessWeight;

            // Erosion dampens peaks when positive
            double normalizedErosion = Math.clamp(erosion, -1.0, 1.0);
            double erosionDamping = 1.0 - (Math.max(0.0, normalizedErosion) * erosionWeight * 0.5);

            // Non-linear power curve creates folded alien ridges and knife-edge peaks
            double ridgeAbs = Math.abs(ridge);
            double ridgeElevation = Math.pow(ridgeAbs, Math.max(0.1, ridgeSharpness)) * ridgeWeight;

            // Crater negative depression with sharp rim walls
            double craterDepression = 0.0;
            if (crater > 0.25) {
                double craterDelta = crater - 0.25;
                craterDepression = Math.pow(craterDelta, 1.5) * craterInfluence;
            }

            double combined = (base * erosionDamping) + ridgeElevation - craterDepression;
            return baselineY + (combined * verticalScale);
        }
    }

    // Planetary presets
    public static final TerrainProfile PROXIMA_B_PROFILE = new TerrainProfile(
            1.25,  // High continental relief
            0.30,  // Low weathering (radiation-stripped atmosphere)
            1.85,  // Massive sharp crystalline ridges
            0.80,  // Moderate meteoric impact craters
            64.0,  // Base datum
            50.0,  // Vertical scale (+/- 50 blocks)
            2.20   // Sharp folded peaks
    );

    public static final TerrainProfile EXOTIC_PLANET_PROFILE = new TerrainProfile(
            1.00,  // Balanced continental shelves
            0.75,  // Smooth alien sediment erosion
            1.20,  // Rolling crystalline dune ridges
            0.40,  // Minor cratering
            70.0,  // Higher alien sea/plain datum
            38.0,  // Moderate rolling hills
            1.60   // Softer ridge gradients
    );

    public static final TerrainProfile NEXUS_MOON_PROFILE = new TerrainProfile(
            0.60,  // Low continental variation (crustal plate tectonics absent)
            0.05,  // Near-zero erosion (hard vacuum)
            1.40,  // Impact ejecta crater rim walls
            2.10,  // Massive deep impact basins
            54.0,  // Low datum
            56.0,  // High vertical extremes
            2.60   // Extremely jagged vacuum-preserved scarps
    );

    private PlanetaryNoiseSettings() {
    }

    /**
     * Initializes and verifies all noise setting keys.
     */
    public static void register() {
        StellarOdyssey.LOGGER.debug("PlanetaryNoiseSettings: noise keys and terrain profiles ready.");
    }
}
