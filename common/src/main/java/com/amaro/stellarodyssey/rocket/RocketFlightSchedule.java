package com.amaro.stellarodyssey.rocket;

import com.amaro.stellarodyssey.registry.tiers.RocketTier;

import java.util.Objects;

/**
 * Immutable per-phase timing schedule for a rocket launch sequence.
 * <p>
 * Derived from a {@link RocketTier} so higher tiers take longer to ignite and
 * charge their hyperdrive, matching their advertised launch altitude and fuel
 * capacity. Pure data (no Minecraft types), which keeps the flight state machine
 * unit-testable headlessly.
 *
 * @param countdownTicks       Number of ticks spent counting down on the pad.
 * @param ignitionTicks        Engine spool-up duration.
 * @param ascentTicks          Powered ascent duration (lower atmosphere).
 * @param atmosphereExitTicks  Atmospheric boundary crossing duration.
 * @param orbitTicks           Time held in orbit before charging the hyperdrive.
 * @param warpChargeTicks      Hyperdrive charge-up duration.
 * @param arrivalTicks         Descent through the destination atmosphere.
 * @param landingTicks         Final touchdown duration.
 */
public record RocketFlightSchedule(
        int countdownTicks,
        int ignitionTicks,
        int ascentTicks,
        int atmosphereExitTicks,
        int orbitTicks,
        int warpChargeTicks,
        int arrivalTicks,
        int landingTicks
) {
    /** Engine spool-up is a fixed dramatic beat regardless of tier. */
    public static final int DEFAULT_IGNITION_TICKS = 40;
    /** Crossing the atmosphere boundary is a short transition. */
    public static final int DEFAULT_ATMOSPHERE_EXIT_TICKS = 60;
    /** Brief orbital hold before the warp charge begins. */
    public static final int DEFAULT_ORBIT_TICKS = 20;
    /** Descent through the destination atmosphere. */
    public static final int DEFAULT_ARRIVAL_TICKS = 60;
    /** Final touchdown settle time. */
    public static final int DEFAULT_LANDING_TICKS = 40;

    /** Fallback schedule used when a rocket has no resolvable tier. */
    public static final RocketFlightSchedule FALLBACK = new RocketFlightSchedule(
            100, DEFAULT_IGNITION_TICKS, 200, DEFAULT_ATMOSPHERE_EXIT_TICKS,
            DEFAULT_ORBIT_TICKS, 80, DEFAULT_ARRIVAL_TICKS, DEFAULT_LANDING_TICKS
    );

    public RocketFlightSchedule {
        if (countdownTicks < 0 || ignitionTicks < 0 || ascentTicks < 0 || atmosphereExitTicks < 0
                || orbitTicks < 0 || warpChargeTicks < 0 || arrivalTicks < 0 || landingTicks < 0) {
            throw new IllegalArgumentException("Rocket flight schedule durations cannot be negative");
        }
        if (countdownTicks == 0) {
            throw new IllegalArgumentException("Countdown must last at least one tick");
        }
    }

    /**
     * Builds the schedule for a concrete rocket tier.
     *
     * @param tier The rocket tier driving ascent and warp-charge timings.
     * @return The tier's flight schedule.
     */
    public static RocketFlightSchedule forTier(RocketTier tier) {
        Objects.requireNonNull(tier, "Rocket tier cannot be null");
        return new RocketFlightSchedule(
                tier.countdownTicks(),
                DEFAULT_IGNITION_TICKS,
                tier.ascentTicks(),
                DEFAULT_ATMOSPHERE_EXIT_TICKS,
                DEFAULT_ORBIT_TICKS,
                tier.warpChargeTicks(),
                DEFAULT_ARRIVAL_TICKS,
                DEFAULT_LANDING_TICKS
        );
    }

    /**
     * Duration in ticks of a given phase. Instantaneous phases ({@link RocketFlightPhase#IDLE},
     * {@link RocketFlightPhase#WARP}) report {@code 0}.
     *
     * @param phase The phase to query.
     * @return Duration in ticks.
     */
    public int durationOf(RocketFlightPhase phase) {
        Objects.requireNonNull(phase, "Phase cannot be null");
        return switch (phase) {
            case IDLE, WARP -> 0;
            case COUNTDOWN -> countdownTicks;
            case IGNITION -> ignitionTicks;
            case ASCENT -> ascentTicks;
            case ATMOSPHERE_EXIT -> atmosphereExitTicks;
            case ORBIT -> orbitTicks;
            case WARP_CHARGE -> warpChargeTicks;
            case ARRIVAL -> arrivalTicks;
            case LANDING -> landingTicks;
        };
    }

    /**
     * Whether the given phase has elapsed its allotted duration.
     *
     * @param phase        The phase being ticked.
     * @param elapsedTicks Ticks already spent in that phase (inclusive count).
     * @return {@code true} when the phase should advance.
     */
    public boolean isComplete(RocketFlightPhase phase, int elapsedTicks) {
        int duration = durationOf(phase);
        return duration > 0 && elapsedTicks >= duration;
    }

    /**
     * Total ticks a full launch sequence occupies, useful for UI progress bars
     * and for tests asserting tier scaling.
     *
     * @return Sum of every phase duration.
     */
    public int totalTicks() {
        return countdownTicks + ignitionTicks + ascentTicks + atmosphereExitTicks
                + orbitTicks + warpChargeTicks + arrivalTicks + landingTicks;
    }
}
