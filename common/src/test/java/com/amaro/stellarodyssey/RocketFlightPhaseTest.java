package com.amaro.stellarodyssey;

import com.amaro.stellarodyssey.rocket.RocketFlightPhase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Rocket Flight Phase Enum Tests (F1)")
class RocketFlightPhaseTest {

    @Test
    @DisplayName("The launch sequence is strictly linear and wraps LANDING back to IDLE")
    void testLinearSequence() {
        List<RocketFlightPhase> visited = new ArrayList<>();
        RocketFlightPhase phase = RocketFlightPhase.IDLE;
        for (int i = 0; i < RocketFlightPhase.values().length; i++) {
            visited.add(phase);
            phase = phase.next();
        }

        assertEquals(RocketFlightPhase.values().length, visited.size());
        assertEquals(RocketFlightPhase.IDLE, visited.get(0));
        assertEquals(RocketFlightPhase.COUNTDOWN, visited.get(1));
        assertEquals(RocketFlightPhase.IGNITION, visited.get(2));
        assertEquals(RocketFlightPhase.ASCENT, visited.get(3));
        assertEquals(RocketFlightPhase.ATMOSPHERE_EXIT, visited.get(4));
        assertEquals(RocketFlightPhase.ORBIT, visited.get(5));
        assertEquals(RocketFlightPhase.WARP_CHARGE, visited.get(6));
        assertEquals(RocketFlightPhase.WARP, visited.get(7));
        assertEquals(RocketFlightPhase.ARRIVAL, visited.get(8));
        assertEquals(RocketFlightPhase.LANDING, visited.get(9));
        assertEquals(RocketFlightPhase.IDLE, phase, "LANDING must wrap back to IDLE");

        for (RocketFlightPhase p : RocketFlightPhase.values()) {
            assertTrue(visited.contains(p), "Every phase must be reachable: " + p);
        }
    }

    @Test
    @DisplayName("Flight-state flags are internally consistent")
    void testFlags() {
        assertFalse(RocketFlightPhase.IDLE.isInFlight());
        assertFalse(RocketFlightPhase.IDLE.countsAsLaunching());

        for (RocketFlightPhase p : RocketFlightPhase.values()) {
            if (p != RocketFlightPhase.IDLE) {
                assertTrue(p.isInFlight(), p + " must be in flight");
                assertTrue(p.countsAsLaunching(), p + " must count as launching");
            }
        }

        assertTrue(RocketFlightPhase.LANDING.isTerminal());
        assertFalse(RocketFlightPhase.ORBIT.isTerminal());

        assertTrue(RocketFlightPhase.ASCENT.isMoving());
        assertTrue(RocketFlightPhase.ATMOSPHERE_EXIT.isMoving());
        assertTrue(RocketFlightPhase.ARRIVAL.isMoving());
        assertFalse(RocketFlightPhase.WARP.isMoving());
        assertFalse(RocketFlightPhase.COUNTDOWN.isMoving());
    }

    @Test
    @DisplayName("fromOrdinal is resilient to out-of-range and legacy values")
    void testFromOrdinal() {
        assertEquals(RocketFlightPhase.COUNTDOWN, RocketFlightPhase.fromOrdinal(1, RocketFlightPhase.IDLE));
        assertEquals(RocketFlightPhase.LANDING,
                RocketFlightPhase.fromOrdinal(RocketFlightPhase.LANDING.ordinal(), RocketFlightPhase.IDLE));
        assertEquals(RocketFlightPhase.IDLE, RocketFlightPhase.fromOrdinal(-1, RocketFlightPhase.IDLE));
        assertEquals(RocketFlightPhase.IDLE, RocketFlightPhase.fromOrdinal(999, RocketFlightPhase.IDLE));
    }
}
