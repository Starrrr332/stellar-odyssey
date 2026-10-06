package com.amaro.stellarodyssey.world;

import com.amaro.stellarodyssey.api.celestial.ICelestialBody;
import com.amaro.stellarodyssey.lifesupport.AtmosphereHelper;
import net.minecraft.SharedConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Milestone M2: Adaptive Planetary Gravity (R2) & Atmospheric Physics Tests")
public class AdaptivePlanetaryGravityTest {

    private static final double EPSILON = 1.0E-6;

    @BeforeAll
    static void initBootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Nested
    @DisplayName("R2 Celestial Catalog Gravity Parameters")
    class CelestialCatalogTests {

        @Test
        @DisplayName("Verify PROXIMA_B gravity multiplier is strictly 0.35g and NEXUS_MOON is 0.16g")
        void testRequiredGravityMultipliers() {
            assertEquals(0.35, CelestialBodyRegistry.PROXIMA_B.gravityMultiplier(), EPSILON,
                    "R2 mandates Proxima B surface gravity must be exactly 0.35g");
            assertEquals(0.16, CelestialBodyRegistry.NEXUS_MOON.gravityMultiplier(), EPSILON,
                    "R2 mandates Nexus Moon surface gravity must be exactly 0.16g");
        }

        @Test
        @DisplayName("Verify complete catalog gravity distribution across charted bodies")
        void testAllCelestialBodiesGravity() {
            CelestialBodyRegistry registry = CelestialBodyRegistry.getInstance();

            Optional<ICelestialBody> moon = registry.getBody(ModDimensions.NEXUS_MOON);
            assertTrue(moon.isPresent(), "Nexus Moon must be present in registry");
            assertEquals(0.16, moon.get().gravityMultiplier(), EPSILON);

            Optional<ICelestialBody> proxima = registry.getBody(ModDimensions.PROXIMA_B);
            assertTrue(proxima.isPresent(), "Proxima B must be present in registry");
            assertEquals(0.35, proxima.get().gravityMultiplier(), EPSILON);

            Optional<ICelestialBody> exotic = registry.getBody(ModDimensions.EXOTIC_PRIME);
            assertTrue(exotic.isPresent(), "Exotic Prime must be present in registry");
            assertEquals(0.75, exotic.get().gravityMultiplier(), EPSILON);

            Optional<ICelestialBody> gliese = registry.getBody(ModDimensions.GLIESE_DEEP);
            assertTrue(gliese.isPresent(), "Gliese Deep must be present in registry");
            assertEquals(1.35, gliese.get().gravityMultiplier(), EPSILON);
        }
    }

    @Nested
    @DisplayName("Dynamic Gravity Modifier Calculations")
    class GravityModifierMathTests {

        @Test
        @DisplayName("Verify modifier calculation formula: multiplier - 1.0")
        void testModifierFormula() {
            // Nexus Moon: 0.16g -> modifier -0.84
            assertEquals(-0.84, PlanetaryGravityManager.gravityModifierAmount(0.16), EPSILON);

            // Proxima B: 0.35g -> modifier -0.65
            assertEquals(-0.65, PlanetaryGravityManager.gravityModifierAmount(0.35), EPSILON);

            // Exotic Prime: 0.75g -> modifier -0.25
            assertEquals(-0.25, PlanetaryGravityManager.gravityModifierAmount(0.75), EPSILON);

            // Gliese Deep: 1.35g -> modifier +0.35
            assertEquals(0.35, PlanetaryGravityManager.gravityModifierAmount(1.35), EPSILON);

            // Earth / Overworld: 1.0g -> modifier 0.0
            assertEquals(0.0, PlanetaryGravityManager.gravityModifierAmount(1.0), EPSILON);

            // Orbital microgravity: 0.08g -> modifier -0.92
            assertEquals(-0.92, PlanetaryGravityManager.gravityModifierAmount(PlanetaryGravityManager.ORBITAL_GRAVITY_MULTIPLIER), EPSILON);
        }

        @Test
        @DisplayName("Verify effective acceleration in blocks/tick^2")
        void testEffectiveAcceleration() {
            double baseGravity = PlanetaryGravityManager.STANDARD_GRAVITY; // 0.08

            // Nexus Moon: 0.08 * 0.16 = 0.0128
            double moonAcceleration = baseGravity * (1.0 + PlanetaryGravityManager.gravityModifierAmount(0.16));
            assertEquals(0.0128, moonAcceleration, EPSILON);

            // Proxima B: 0.08 * 0.35 = 0.0280
            double proximaAcceleration = baseGravity * (1.0 + PlanetaryGravityManager.gravityModifierAmount(0.35));
            assertEquals(0.0280, proximaAcceleration, EPSILON);

            // Orbit: 0.08 * 0.08 = 0.0064
            double orbitAcceleration = baseGravity * (1.0 + PlanetaryGravityManager.gravityModifierAmount(PlanetaryGravityManager.ORBITAL_GRAVITY_MULTIPLIER));
            assertEquals(0.0064, orbitAcceleration, EPSILON);
        }

        @Test
        @DisplayName("Verify safe fall distance inverse scaling: (3.0 / multiplier) - 3.0")
        void testSafeFallDistanceScaling() {
            // Nexus Moon (0.16g): 3.0 / 0.16 = 18.75 blocks threshold -> +15.75 bonus blocks
            assertEquals(15.75, PlanetaryGravityManager.safeFallDistanceBonus(0.16), EPSILON);

            // Proxima B (0.35g): 3.0 / 0.35 = 8.571428... -> +5.571428... bonus blocks
            assertEquals((3.0 / 0.35) - 3.0, PlanetaryGravityManager.safeFallDistanceBonus(0.35), EPSILON);

            // Overworld (1.0g): 0.0 bonus blocks
            assertEquals(0.0, PlanetaryGravityManager.safeFallDistanceBonus(1.0), EPSILON);

            // High gravity Gliese Deep (1.35g): reduces safe distance (negative bonus)
            assertTrue(PlanetaryGravityManager.safeFallDistanceBonus(1.35) < 0.0,
                    "High gravity must decrease safe fall distance threshold");

            // Zero or negative protection
            assertEquals(0.0, PlanetaryGravityManager.safeFallDistanceBonus(0.0), EPSILON);
            assertEquals(0.0, PlanetaryGravityManager.safeFallDistanceBonus(-1.0), EPSILON);
        }

        @Test
        @DisplayName("Verify fall damage multiplier scaling: multiplier - 1.0")
        void testFallDamageMultiplierScaling() {
            // Nexus Moon: -0.84 (84% damage reduction)
            assertEquals(-0.84, PlanetaryGravityManager.fallDamageMultiplierAmount(0.16), EPSILON);

            // Proxima B: -0.65 (65% damage reduction)
            assertEquals(-0.65, PlanetaryGravityManager.fallDamageMultiplierAmount(0.35), EPSILON);

            // Overworld: 0.0 (no damage change)
            assertEquals(0.0, PlanetaryGravityManager.fallDamageMultiplierAmount(1.0), EPSILON);
        }
    }

    @Nested
    @DisplayName("Dimension & Orbital Altitude Resolution")
    class DimensionAndAltitudeTests {

        @Test
        @DisplayName("Verify dimension resource key gravity multiplier resolution")
        void testDimensionKeyResolution() {
            assertEquals(0.16, PlanetaryGravityManager.resolveBodyGravityMultiplier(ModDimensions.NEXUS_MOON), EPSILON);
            assertEquals(0.35, PlanetaryGravityManager.resolveBodyGravityMultiplier(ModDimensions.PROXIMA_B), EPSILON);
            assertEquals(0.75, PlanetaryGravityManager.resolveBodyGravityMultiplier(ModDimensions.EXOTIC_PRIME), EPSILON);
            assertEquals(1.35, PlanetaryGravityManager.resolveBodyGravityMultiplier(ModDimensions.GLIESE_DEEP), EPSILON);

            // Uncharted or null defaults to 1.0 (Earth standard)
            assertEquals(1.0, PlanetaryGravityManager.resolveBodyGravityMultiplier((ResourceKey<Level>) null), EPSILON);
            ResourceKey<Level> overworld = ResourceKey.create(Registries.DIMENSION, Identifier.parse("minecraft:overworld"));
            assertEquals(1.0, PlanetaryGravityManager.resolveBodyGravityMultiplier(overworld), EPSILON);
        }

        @Test
        @DisplayName("Verify orbital altitude decay at Y >= 320 blocks")
        void testOrbitalAltitudeTransitions() {
            // Below threshold: surface gravity of body applies
            assertEquals(0.35, PlanetaryGravityManager.gravityMultiplierAt(0, 0.35), EPSILON);
            assertEquals(0.35, PlanetaryGravityManager.gravityMultiplierAt(319, 0.35), EPSILON);
            assertEquals(1.35, PlanetaryGravityManager.gravityMultiplierAt(256, 1.35), EPSILON);

            // At or above threshold: orbital microgravity applies regardless of body
            assertEquals(PlanetaryGravityManager.ORBITAL_GRAVITY_MULTIPLIER,
                    PlanetaryGravityManager.gravityMultiplierAt(320, 0.35), EPSILON);
            assertEquals(PlanetaryGravityManager.ORBITAL_GRAVITY_MULTIPLIER,
                    PlanetaryGravityManager.gravityMultiplierAt(500, 1.35), EPSILON);
            assertEquals(PlanetaryGravityManager.ORBITAL_GRAVITY_MULTIPLIER,
                    PlanetaryGravityManager.gravityMultiplierAt(1000, 0.16), EPSILON);
        }
    }

    @Nested
    @DisplayName("AtmosphereHelper Vacuum vs Toxic Differentiation Across Dimensions")
    class AtmosphereDifferentiationTests {

        @Test
        @DisplayName("Verify hard vacuum vs toxic exoplanetary atmosphere categorization")
        void testAtmosphereHelperCategorization() {
            // Nexus Moon: 0.00 atm -> hard vacuum, NOT toxic atmosphere
            assertTrue(AtmosphereHelper.isHardVacuum(ModDimensions.NEXUS_MOON),
                    "Nexus Moon (0.00 atm) must be categorized as hard vacuum");
            assertFalse(AtmosphereHelper.isToxicOrUnbreathable(ModDimensions.NEXUS_MOON),
                    "Nexus Moon is vacuum, not toxic exoplanetary atmosphere");

            // Proxima B: 0.15 atm -> toxic/unbreathable exoplanet, NOT hard vacuum
            assertFalse(AtmosphereHelper.isHardVacuum(ModDimensions.PROXIMA_B),
                    "Proxima B (0.15 atm) must NOT be categorized as hard vacuum");
            assertTrue(AtmosphereHelper.isToxicOrUnbreathable(ModDimensions.PROXIMA_B),
                    "Proxima B must be categorized as toxic/unbreathable exoplanetary atmosphere");

            // Exotic Prime: 0.85 atm -> toxic/unbreathable exoplanet, NOT hard vacuum
            assertFalse(AtmosphereHelper.isHardVacuum(ModDimensions.EXOTIC_PRIME),
                    "Exotic Prime (0.85 atm) must NOT be categorized as hard vacuum");
            assertTrue(AtmosphereHelper.isToxicOrUnbreathable(ModDimensions.EXOTIC_PRIME),
                    "Exotic Prime must be categorized as toxic/unbreathable exoplanetary atmosphere");

            // Gliese Deep: 2.50 atm -> high pressure toxic exoplanet, NOT hard vacuum
            assertFalse(AtmosphereHelper.isHardVacuum(ModDimensions.GLIESE_DEEP),
                    "Gliese Deep (2.50 atm) must NOT be categorized as hard vacuum");
            assertTrue(AtmosphereHelper.isToxicOrUnbreathable(ModDimensions.GLIESE_DEEP),
                    "Gliese Deep must be categorized as toxic/unbreathable exoplanetary atmosphere");
        }
    }

    @Nested
    @DisplayName("Architectural Decoupling & Modifier Integrity")
    class ArchitecturalDecouplingTests {

        @Test
        @DisplayName("Verify attribute modifier IDs are unique and scoped to stellarodyssey namespace")
        void testModifierIds() {
            assertNotNull(PlanetaryGravityManager.GRAVITY_MOD_ID);
            assertNotNull(PlanetaryGravityManager.SAFE_FALL_MOD_ID);
            assertNotNull(PlanetaryGravityManager.FALL_DAMAGE_MOD_ID);

            assertEquals("stellarodyssey", PlanetaryGravityManager.GRAVITY_MOD_ID.getNamespace());
            assertEquals("stellarodyssey", PlanetaryGravityManager.SAFE_FALL_MOD_ID.getNamespace());
            assertEquals("stellarodyssey", PlanetaryGravityManager.FALL_DAMAGE_MOD_ID.getNamespace());

            assertNotEquals(PlanetaryGravityManager.GRAVITY_MOD_ID, PlanetaryGravityManager.SAFE_FALL_MOD_ID);
            assertNotEquals(PlanetaryGravityManager.GRAVITY_MOD_ID, PlanetaryGravityManager.FALL_DAMAGE_MOD_ID);
            assertNotEquals(PlanetaryGravityManager.SAFE_FALL_MOD_ID, PlanetaryGravityManager.FALL_DAMAGE_MOD_ID);
        }

        @Test
        @DisplayName("Verify PlanetaryGravityManager contains zero static entity or level leaks")
        void testNoStaticEntityLeaks() {
            Field[] fields = PlanetaryGravityManager.class.getDeclaredFields();
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    assertFalse(Level.class.isAssignableFrom(field.getType()),
                            "Static field " + field.getName() + " leaks Level instance");
                    assertFalse(field.getType().getName().contains("Entity"),
                            "Static field " + field.getName() + " leaks Entity instance");
                }
            }
        }
    }
}
