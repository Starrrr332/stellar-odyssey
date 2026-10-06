package com.amaro.stellarodyssey.client.camera;

import com.amaro.stellarodyssey.rocket.RocketFlightPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JVM coverage for the deterministic rocket camera maths (S1-P0.3).
 * {@link RocketCameraController#computeShake(RocketFlightPhase, int, int, float)}
 * is Minecraft-free on purpose so it can be tested headlessly.
 */
@DisplayName("Rocket Camera Shake Maths Tests (F2.3)")
class RocketCameraShakeTest {

    @Test
    @DisplayName("IDLE and unknown phases produce the identity shake")
    void idlePhasesAreIdentity() {
        RocketCameraShake idle = RocketCameraController.computeShake(RocketFlightPhase.IDLE, 40, 100, 0.0F);
        assertTrue(idle.isZero(), "IDLE must not shake the camera");
        assertEquals(1.0F, idle.fovScale(), 1.0E-6F, "IDLE FOV pulse must be 1.0");
    }

    @Test
    @DisplayName("IGNITION and ASCENT produce measurable yaw/pitch/roll shake")
    void poweredPhasesShake() {
        for (RocketFlightPhase phase : new RocketFlightPhase[]{RocketFlightPhase.IGNITION, RocketFlightPhase.ASCENT}) {
            RocketCameraShake shake = RocketCameraController.computeShake(phase, 40, 120, 0.5F);
            assertFalse(shake.isZero(), phase + " must produce a shake");
            assertTrue(Math.abs(shake.yawDegrees()) > 0.5F, phase + " yaw must be meaningful");
            assertTrue(Math.abs(shake.pitchDegrees()) > 0.5F, phase + " pitch must be meaningful");
        }
    }

    @Test
    @DisplayName("The attack ramp smooths the phase entry: no pop at tick 0")
    void attackRampStartsFromZero() {
        RocketCameraShake start = RocketCameraController.computeShake(RocketFlightPhase.ASCENT, 0, 120, 0.0F);
        RocketCameraShake ramping = RocketCameraController.computeShake(RocketFlightPhase.ASCENT, 5, 120, 0.0F);
        RocketCameraShake full = RocketCameraController.computeShake(RocketFlightPhase.ASCENT, 15, 120, 0.0F);
        assertTrue(start.isZero() || Math.abs(start.yawDegrees()) < Math.abs(ramping.yawDegrees()),
                "tick 0 must be (near) zero");
        assertTrue(Math.abs(ramping.yawDegrees()) < Math.abs(full.yawDegrees()),
                "shake must grow toward full amplitude");
    }

    @Test
    @DisplayName("LANDING settles to zero at touchdown")
    void landingDecays() {
        RocketCameraShake mid = RocketCameraController.computeShake(RocketFlightPhase.LANDING, 30, 60, 0.0F);
        RocketCameraShake end = RocketCameraController.computeShake(RocketFlightPhase.LANDING, 60, 60, 0.0F);
        assertFalse(mid.isZero(), "mid-descent must still rumble");
        assertTrue(mid.rollDegrees() != 0.0F || mid.yawDegrees() != 0.0F, "mid-descent must travel");
        assertTrue(end.isZero(), "touchdown must be perfectly still");
    }

    @Test
    @DisplayName("WARP carries an FOV pulse but no translation shake")
    void warpPulsesFov() {
        RocketCameraShake warp = RocketCameraController.computeShake(RocketFlightPhase.WARP, 2, 20, 0.0F);
        assertTrue(warp.fovScale() > 1.001F, "WARP must zoom out slightly");
        assertEquals(0.0F, warp.yawDegrees(), 1.0E-6F, "WARP has no rotational shake");
        assertEquals(0.0F, warp.pitchDegrees(), 1.0E-6F, "WARP has no rotational shake");
        assertEquals(0.0F, warp.rollDegrees(), 1.0E-6F, "WARP has no rotational shake");
    }

    @Test
    @DisplayName("FOV pulse never exceeds +12% in any phase")
    void fovScaleIsCapped() {
        for (RocketFlightPhase phase : RocketFlightPhase.values()) {
            for (int tick = 0; tick <= 100; tick += 7) {
                RocketCameraShake shake = RocketCameraController.computeShake(phase, tick, 100, 0.5F);
                assertTrue(shake.fovScale() >= 1.0F - 1.0E-6F, phase + " must never zoom in");
                assertTrue(shake.fovScale() <= 1.12F + 1.0E-6F, phase + " FOV ceiling exceeded at tick " + tick);
            }
        }
    }

    @Test
    @DisplayName("The shake is fully deterministic: equal inputs yield equal outputs")
    void deterministic() {
        RocketCameraShake a = RocketCameraController.computeShake(RocketFlightPhase.ASCENT, 42, 120, 0.25F);
        RocketCameraShake b = RocketCameraController.computeShake(RocketFlightPhase.ASCENT, 42, 120, 0.25F);
        assertEquals(a, b, "no randomness must be allowed in the camera maths");
    }

    @Test
    @DisplayName("Unknown duration (0) falls back to full progress without division by zero")
    void zeroDurationIsSafe() {
        RocketCameraShake landing = RocketCameraController.computeShake(RocketFlightPhase.LANDING, 10, 0, 0.0F);
        assertTrue(landing.isZero(), "progress 1 in LANDING must yield stillness");
        RocketCameraShake ascent = RocketCameraController.computeShake(RocketFlightPhase.ASCENT, 10, 0, 0.0F);
        assertFalse(ascent.isZero(), "ASCENT with unknown duration must still shake");
    }

    @Test
    @DisplayName("Negative ticks and partial ticks are clamped, never crash")
    void negativeInputsAreSafe() {
        RocketCameraShake shake = RocketCameraController.computeShake(RocketFlightPhase.ASCENT, -3, -50, -1.0F);
        assertFalse(shake.isZero(), "must produce a sensible (ramped) shake");
        assertTrue(Math.abs(shake.yawDegrees()) < 3.5F, "amplitude must respect the ASCENT ceiling");
    }
}