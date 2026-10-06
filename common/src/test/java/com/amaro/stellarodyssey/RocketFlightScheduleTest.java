package com.amaro.stellarodyssey;

import com.amaro.stellarodyssey.registry.tiers.RocketTier;
import com.amaro.stellarodyssey.registry.tiers.RocketTierRegistry;
import com.amaro.stellarodyssey.rocket.RocketFlightPhase;
import com.amaro.stellarodyssey.rocket.RocketFlightSchedule;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Rocket Flight Schedule Tests (F1)")
class RocketFlightScheduleTest {

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Durations are never negative and instantaneous phases report zero")
    void testDurations() {
        RocketFlightSchedule schedule = RocketFlightSchedule.FALLBACK;
        for (RocketFlightPhase phase : RocketFlightPhase.values()) {
            assertTrue(schedule.durationOf(phase) >= 0, "Negative duration for " + phase);
        }
        assertEquals(0, schedule.durationOf(RocketFlightPhase.IDLE));
        assertEquals(0, schedule.durationOf(RocketFlightPhase.WARP));
        assertTrue(schedule.durationOf(RocketFlightPhase.COUNTDOWN) > 0);
    }

    @Test
    @DisplayName("forTier derives countdown/ascent/warp-charge from the rocket tier")
    void testForTierDerivation() {
        for (int level = 1; level <= RocketTierRegistry.getTierCount(); level++) {
            RocketTier tier = RocketTierRegistry.getTier(level).orElseThrow();
            RocketFlightSchedule schedule = RocketFlightSchedule.forTier(tier);

            assertEquals(tier.countdownTicks(), schedule.countdownTicks());
            assertEquals(tier.ascentTicks(), schedule.ascentTicks());
            assertEquals(tier.warpChargeTicks(), schedule.warpChargeTicks());
            assertEquals(RocketFlightSchedule.DEFAULT_IGNITION_TICKS, schedule.ignitionTicks());
            assertEquals(RocketFlightSchedule.DEFAULT_ATMOSPHERE_EXIT_TICKS, schedule.atmosphereExitTicks());
            assertEquals(RocketFlightSchedule.DEFAULT_ARRIVAL_TICKS, schedule.arrivalTicks());
            assertEquals(RocketFlightSchedule.DEFAULT_LANDING_TICKS, schedule.landingTicks());
        }
    }

    @Test
    @DisplayName("isComplete respects the phase duration boundary")
    void testIsCompleteBoundary() {
        RocketFlightSchedule schedule = new RocketFlightSchedule(100, 40, 200, 60, 20, 80, 60, 40);

        assertFalse(schedule.isComplete(RocketFlightPhase.COUNTDOWN, 99));
        assertTrue(schedule.isComplete(RocketFlightPhase.COUNTDOWN, 100));
        assertTrue(schedule.isComplete(RocketFlightPhase.COUNTDOWN, 101));
        assertFalse(schedule.isComplete(RocketFlightPhase.IGNITION, 39));
        assertTrue(schedule.isComplete(RocketFlightPhase.IGNITION, 40));

        // Instantaneous phases must never report completion.
        assertFalse(schedule.isComplete(RocketFlightPhase.WARP, 5));
        assertFalse(schedule.isComplete(RocketFlightPhase.IDLE, 5));
    }

    @Test
    @DisplayName("Higher tiers have longer total sequences")
    void testTierScaling() {
        int totalT1 = RocketFlightSchedule.forTier(RocketTierRegistry.getTier(1).orElseThrow()).totalTicks();
        int totalT2 = RocketFlightSchedule.forTier(RocketTierRegistry.getTier(2).orElseThrow()).totalTicks();
        int totalT3 = RocketFlightSchedule.forTier(RocketTierRegistry.getTier(3).orElseThrow()).totalTicks();

        assertTrue(totalT1 < totalT2, "Tier 2 sequence must be longer than Tier 1");
        assertTrue(totalT2 < totalT3, "Tier 3 sequence must be longer than Tier 2");
    }

    @Test
    @DisplayName("Tier prerequisites are consistent (atmosphere exit below launch altitude)")
    void testTierAltitudeInvariants() {
        for (int level = 1; level <= RocketTierRegistry.getTierCount(); level++) {
            RocketTier tier = RocketTierRegistry.getTier(level).orElseThrow();
            assertTrue(tier.atmosphereExitAltitude() > 0);
            assertTrue(tier.atmosphereExitAltitude() < tier.launchAltitude(),
                    "Tier " + level + " must cross the atmosphere before reaching launch altitude");
            assertTrue(tier.ascentTicks() > 0);
            assertTrue(tier.warpChargeTicks() > 0);
        }
    }

    @Test
    @DisplayName("Invalid schedules are rejected at construction time")
    void testValidation() {
        assertThrows(IllegalArgumentException.class,
                () -> new RocketFlightSchedule(100, -1, 200, 60, 20, 80, 60, 40));
        assertThrows(IllegalArgumentException.class,
                () -> new RocketFlightSchedule(0, 40, 200, 60, 20, 80, 60, 40));
        assertThrows(NullPointerException.class, () -> RocketFlightSchedule.forTier(null));
    }
}
