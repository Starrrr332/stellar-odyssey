package com.amaro.stellarodyssey;

import com.amaro.stellarodyssey.world.CelestialBodyRegistry;
import com.amaro.stellarodyssey.world.PlanetaryGravityManager;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Adaptive Planetary Gravity Tests (R2)")
class AdaptivePlanetaryGravityTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Celestial catalog exposes the required per-body gravity multipliers")
    void testCatalogGravityMultipliers() {
        assertEquals(0.16, CelestialBodyRegistry.NEXUS_MOON.gravityMultiplier(), 1e-9);
        assertEquals(0.35, CelestialBodyRegistry.PROXIMA_B.gravityMultiplier(), 1e-9);
        assertEquals(0.75, CelestialBodyRegistry.EXOTIC_PRIME.gravityMultiplier(), 1e-9);
        assertEquals(1.35, CelestialBodyRegistry.GLIESE_DEEP.gravityMultiplier(), 1e-9);
    }

    @Test
    @DisplayName("Gravity modifier math produces the expected per-body amounts")
    void testGravityModifierMath() {
        assertEquals(-0.84, PlanetaryGravityManager.gravityModifierAmount(0.16), 1e-9);
        assertEquals(-0.65, PlanetaryGravityManager.gravityModifierAmount(0.35), 1e-9);
        assertEquals(0.0, PlanetaryGravityManager.gravityModifierAmount(1.0), 1e-9);

        double effectiveGravity = PlanetaryGravityManager.STANDARD_GRAVITY
                * (1.0 + PlanetaryGravityManager.gravityModifierAmount(0.16));
        assertEquals(0.0128, effectiveGravity, 1e-9);
    }

    @Test
    @DisplayName("Safe fall distance scales inversely with gravity")
    void testSafeFallDistanceScaling() {
        assertEquals(15.75, PlanetaryGravityManager.safeFallDistanceBonus(0.16), 1e-6);
        assertEquals(3.0 / 0.35 - 3.0, PlanetaryGravityManager.safeFallDistanceBonus(0.35), 1e-6);
        assertEquals(0.0, PlanetaryGravityManager.safeFallDistanceBonus(1.0), 1e-9);
    }

    @Test
    @DisplayName("Orbital altitude resolves to microgravity regardless of the body")
    void testOrbitalMicrogravity() {
        assertEquals(PlanetaryGravityManager.ORBITAL_GRAVITY_MULTIPLIER,
                PlanetaryGravityManager.gravityMultiplierAt(320, 1.35), 1e-9);
        assertEquals(0.35, PlanetaryGravityManager.gravityMultiplierAt(64, 0.35), 1e-9);
    }

    @Test
    @DisplayName("Fall damage multiplier becomes the gravity multiplier")
    void testFallDamageScaling() {
        assertEquals(-0.84, PlanetaryGravityManager.fallDamageMultiplierAmount(0.16), 1e-9);
        assertEquals(-0.35, PlanetaryGravityManager.fallDamageMultiplierAmount(0.65), 1e-9);
    }
}
