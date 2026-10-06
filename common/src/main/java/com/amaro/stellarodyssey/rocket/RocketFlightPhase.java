package com.amaro.stellarodyssey.rocket;

import java.util.List;

/**
 * Server-authoritative flight phases of a rocket launch sequence.
 * <p>
 * The sequence is strictly linear and every phase transitions to the next one:

 * <pre>
 * IDLE → COUNTDOWN → IGNITION → ASCENT → ATMOSPHERE_EXIT → ORBIT
 *      → WARP_CHARGE → WARP → ARRIVAL → LANDING → IDLE
 * </pre>
 *
 * <p>
 * The enum is intentionally free of any Minecraft dependency so the sequencing
 * can be unit-tested headlessly; the concrete durations live in
 * {@link RocketFlightSchedule}. The visual client-side effects (camera shake,
 * sky lerp, warp tunnel, overlays) are driven by mirroring this state, which is
 * synchronised through the entity's synched data.
 */
public enum RocketFlightPhase {
    /** Resting on the launch pad, idle. */
    IDLE,
    /** Countdown announced to the crew; clamps still engaged. */
    COUNTDOWN,
    /** Engines spooling up; the rocket is still anchored to the pad. */
    IGNITION,
    /** Powered vertical ascent through the lower atmosphere. */
    ASCENT,
    /** Crossing the atmosphere boundary; sky fades to space. */
    ATMOSPHERE_EXIT,
    /** Holding a stable orbit above the launch body. */
    ORBIT,
    /** Hyperdrive charging before the interplanetary jump. */
    WARP_CHARGE,
    /** The instantaneous jump itself (dimension transfer). */
    WARP,
    /** Descending through the destination atmosphere. */
    ARRIVAL,
    /** Final touchdown; the sequence is about to end. */
    LANDING;

    private static final List<RocketFlightPhase> SEQUENCE = List.of(values());

    /**
     * The next phase of the launch sequence. {@link #LANDING} wraps around to
     * {@link #IDLE} (the sequence is over).
     */
    public RocketFlightPhase next() {
        int index = ordinal() + 1;
        if (index >= SEQUENCE.size()) {
            return IDLE;
        }
        return SEQUENCE.get(index);
    }

    /** Whether the rocket is anywhere inside a launch sequence (i.e. not resting). */
    public boolean isInFlight() {
        return this != IDLE;
    }

    /** Whether this is a powered, moving phase (ascent / atmospheric escape / arrival). */
    public boolean isMoving() {
        return this == ASCENT || this == ATMOSPHERE_EXIT || this == ARRIVAL;
    }

    /** Whether this is the final phase of the sequence. */
    public boolean isTerminal() {
        return this == LANDING;
    }

    /**
     * Backwards-compatible view of the legacy {@code isLaunching} flag: any phase
     * other than {@link #IDLE} means a launch sequence is in progress.
     */
    public boolean countsAsLaunching() {
        return isInFlight();
    }

    /**
     * Resolves a phase from a persisted ordinal, falling back to {@code fallback}
     * when the value is out of range (e.g. an older save file).
     */
    public static RocketFlightPhase fromOrdinal(int ordinal, RocketFlightPhase fallback) {
        if (ordinal >= 0 && ordinal < SEQUENCE.size()) {
            return SEQUENCE.get(ordinal);
        }
        return fallback;
    }
}
