package com.amaro.stellarodyssey.client.gui;

import com.amaro.stellarodyssey.rocket.RocketFlightPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JVM coverage for the launch HUD telemetry maths (S3-A2).
 * {@link RocketTelemetry} is Minecraft-free on purpose so it can be tested headlessly.
 */
@DisplayName("Rocket Flight Telemetry Tests (S3-A2)")
class RocketTelemetryTest {

    @Test
    @DisplayName("ASCENT gains velocity every tick under net-positive thrust")
    void ascentAccelerates() {
        double v = 0.0;
        for (int tick = 0; tick < 20; tick++) {
            v = RocketTelemetry.computeVelocity(RocketFlightPhase.ASCENT, v, tick).blocksPerTick();
        }
        assertTrue(v > 0.0, "ASCENT must gain vertical speed, got " + v);
    }

    @Test
    @DisplayName("Velocity never exceeds the terminal climb clamp")
    void terminalClampHolds() {
        double v = 2.19;
        for (int tick = 31; tick < 90; tick++) {
            v = RocketTelemetry.computeVelocity(RocketFlightPhase.ASCENT, v, tick).blocksPerTick();
            assertTrue(v <= 2.2 + 1.0E-9, "terminal clamp exceeded: " + v + " at tick " + tick);
        }
    }

    @Test
    @DisplayName("1 g in this model equals the vanilla gravity constant")
    void oneGMatchesGravityConstant() {
        // Coasting phases have zero engine thrust: felt = -gravity = exactly -1 g.
        assertEquals(-1.0, RocketTelemetry.computeGForce(RocketFlightPhase.ORBIT), 1.0E-9,
                "coasting phases must read 1 g of passive gravity");
        assertEquals(-1.0, RocketTelemetry.computeGForce(RocketFlightPhase.IDLE), 1.0E-9);
    }

    @Test
    @DisplayName("Power ascent pulls more than 1 g, landing pull is mild")
    void thrustProfileRanks() {
        double ascent = RocketTelemetry.computeGForce(RocketFlightPhase.ASCENT);
        double exit = RocketTelemetry.computeGForce(RocketFlightPhase.ATMOSPHERE_EXIT);
        double ignition = RocketTelemetry.computeGForce(RocketFlightPhase.IGNITION);
        double landing = RocketTelemetry.computeGForce(RocketFlightPhase.LANDING);
        double arrival = RocketTelemetry.computeGForce(RocketFlightPhase.ARRIVAL);
        assertTrue(ascent > 1.0, "ASCENT must exceed 1 g");
        assertTrue(exit > 1.0, "ATMOSPHERE_EXIT must exceed 1 g");
        // IGNITION spools below weight (engines still anchored): net pull is negative but
        // milder than the ARRIVAL reentry dip, and always weaker than full ASCENT.
        assertTrue(ignition < ascent, "IGNITION is a milder pull than ASCENT");
        assertTrue(ignition > arrival, "IGNITION pull must exceed the ARRIVAL reentry dip");
        assertTrue(landing < ascent, "LANDING must pull less than ASCENT");
    }

    @Test
    @DisplayName("Blocks-per-second conversion multiplies by 20")
    void velocityConversion() {
        RocketTelemetry.VelocitySample sample = new RocketTelemetry.VelocitySample(0.5);
        assertEquals(10.0F, sample.blocksPerSecond(), 1.0E-6F);
    }

    @Test
    @DisplayName("The telemetry is fully deterministic: equal inputs yield equal outputs")
    void deterministic() {
        RocketTelemetry.VelocitySample a = RocketTelemetry.computeVelocity(RocketFlightPhase.ASCENT, 1.1, 42);
        RocketTelemetry.VelocitySample b = RocketTelemetry.computeVelocity(RocketFlightPhase.ASCENT, 1.1, 42);
        assertEquals(a, b, "no randomness must be allowed in the telemetry maths");
    }

    @Test
    @DisplayName("Null phase never crashes")
    void nullPhaseIsSafe() {
        RocketTelemetry.VelocitySample sample = RocketTelemetry.computeVelocity(null, 0.4, 5);
        assertEquals(0.4, sample.blocksPerTick(), 1.0E-9);
        assertEquals(0.0, RocketTelemetry.computeGForce(null), 1.0E-9);
        assertFalse(Double.isNaN(RocketTelemetry.computeGForce(null)));
    }
}
