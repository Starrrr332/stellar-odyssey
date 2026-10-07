package com.amaro.stellarodyssey.client.gui;

import com.amaro.stellarodyssey.rocket.RocketFlightPhase;

/**
 * Deterministic flight telemetry mathematics and physics model for the launch cinematic HUD.
 * <p>
 * Pure functions with zero Minecraft dependencies so the telemetry panel values,
 * dynamic pressure (Max-Q), Mach number, ionization plasma, and G-acceleration
 * can be unit-tested headlessly.
 *
 * <p><b>Physical model:</b>
 * <ul>
 *     <li>Vertical velocity integrates a fixed gravitational constant ({@value #GRAVITY_BLOCKS_PER_TICK2})
 *         against the FSM phase thrust profile ({@link #thrustProfile(RocketFlightPhase)}).</li>
 *     <li>Mach number is normalized against sound speed at sea level ({@value #SPEED_OF_SOUND_MPS} m/s).</li>
 *     <li>Aerodynamic dynamic pressure ($q = \frac{1}{2}\rho v^2$) models Max-Q atmospheric buffeting.</li>
 *     <li>Felt G-force ($G = 1 + a/g_0$) accounts for engine thrust acceleration.</li>
 *     <li>Atmospheric exit ionization models hypersonic plasma glow during boundary crossing.</li>
 * </ul>
 */
public final class RocketTelemetry {
    /** Vanilla gravity in blocks/tick² — this is 1 g in this model. */
    public static final double GRAVITY_BLOCKS_PER_TICK2 = 0.08;

    /** Standard speed of sound in air at sea level (m/s or blocks/s). */
    public static final double SPEED_OF_SOUND_MPS = 340.0;

    /** Standard Earth-like sea-level acceleration of gravity ($g_0 = 9.81$ m/s²). */
    public static final double STANDARD_G0 = 9.81;

    /** Normalization constant for dynamic pressure Max-Q ceiling. */
    public static final double MAX_Q_REFERENCE = 1800.0;

    /** One-tick vertical velocity sample, in blocks/tick. */
    public record VelocitySample(double blocksPerTick) {
        /** @return the same speed expressed in blocks per second (m/s). */
        public float blocksPerSecond() {
            return (float) (this.blocksPerTick * 20.0);
        }
    }

    private RocketTelemetry() {
    }

    /**
     * Integrates one tick of vertical velocity for the given flight phase.
     *
     * @param phase            the current (non-null) flight phase
     * @param previousVelocity previous one-tick vertical velocity in blocks/tick
     * @param phaseTicks       elapsed ticks inside the phase, used for terminal speed clamping
     * @return the new one-tick vertical velocity sample
     */
    public static VelocitySample computeVelocity(RocketFlightPhase phase, double previousVelocity, int phaseTicks) {
        if (phase == null) {
            return new VelocitySample(previousVelocity);
        }
        double velocity = previousVelocity + thrustProfile(phase) - GRAVITY_BLOCKS_PER_TICK2;
        if (phaseTicks > 30) {
            // Terminal climb: engines throttle back so streaks stay readable at late ascent.
            velocity = Math.min(velocity, 2.2);
        }
        return new VelocitySample(velocity);
    }

    /**
     * Felt acceleration in g units for the current phase: the net (thrust − gravity)
     * in blocks/tick² normalized to 1 g. A rider pinned to their seat during a
     * power ascent reads > 1 g; coasting reads ≈ 0 g.
     *
     * @param phase the current (non-null) flight phase
     * @return felt acceleration in g, signed
     */
    public static double computeGForce(RocketFlightPhase phase) {
        if (phase == null) {
            return 0.0;
        }
        return (thrustProfile(phase) - GRAVITY_BLOCKS_PER_TICK2) / GRAVITY_BLOCKS_PER_TICK2;
    }

    /**
     * Computes the current Mach number from speed in blocks/second (m/s).
     *
     * @param speedBlocksPerSec speed in blocks per second.
     * @return Mach number (dimensionless).
     */
    public static double computeMach(double speedBlocksPerSec) {
        if (speedBlocksPerSec <= 0.0) {
            return 0.0;
        }
        return speedBlocksPerSec / SPEED_OF_SOUND_MPS;
    }

    /**
     * Computes normalized atmospheric density ($\rho \in [0.0, 1.0]$) at a given altitude Y.
     * Sea level ($Y \le 64$) has density 1.0, exponentially decaying toward vacuum at $Y \ge 320$.
     *
     * @param altitudeY current vehicle altitude Y.
     * @return normalized atmospheric density in range [0.0, 1.0].
     */
    public static double computeAtmosphericDensity(double altitudeY) {
        if (altitudeY <= 64.0) {
            return 1.0;
        }
        if (altitudeY >= 320.0) {
            return 0.0;
        }
        double deltaY = altitudeY - 64.0;
        double density = Math.exp(-deltaY / 52.0);
        return Math.clamp(density, 0.0, 1.0);
    }

    /**
     * Computes aerodynamic dynamic pressure ($q = \frac{1}{2}\rho v^2$) normalized
     * for Max-Q assessment. Peaks during mid-ascent ($Y \approx 130–180$).
     *
     * @param altitudeY         current vehicle altitude Y.
     * @param speedBlocksPerSec speed in blocks per second.
     * @return normalized dynamic pressure in range [0.0, 1.0].
     */
    public static double computeDynamicPressure(double altitudeY, double speedBlocksPerSec) {
        if (speedBlocksPerSec <= 0.0) {
            return 0.0;
        }
        double density = computeAtmosphericDensity(altitudeY);
        if (density <= 0.0) {
            return 0.0;
        }
        double q = 0.5 * density * (speedBlocksPerSec * speedBlocksPerSec);
        return Math.clamp(q / MAX_Q_REFERENCE, 0.0, 1.0);
    }

    /**
     * Computes dynamic G-acceleration from consecutive velocity readings.
     * $a = \Delta v / \Delta t$, $G = 1 + a / g_0$.
     *
     * @param prevVyBlocksPerSec previous vertical velocity (m/s).
     * @param currVyBlocksPerSec current vertical velocity (m/s).
     * @param dtSeconds          elapsed time in seconds (typically 0.05s for 1 tick).
     * @return dynamic G-force reading.
     */
    public static double computeDynamicGForce(double prevVyBlocksPerSec, double currVyBlocksPerSec, double dtSeconds) {
        if (dtSeconds <= 0.0) {
            return 1.0;
        }
        double accel = (currVyBlocksPerSec - prevVyBlocksPerSec) / dtSeconds;
        return 1.0 + (accel / STANDARD_G0);
    }

    /**
     * Computes hypersonic atmospheric exit / re-entry ionization plasma glow intensity.
     * Forms a plasma sheath when vehicle speed is high and crossing the mesosphere/thermosphere ($Y \in [110, 310]$).
     *
     * @param altitudeY         vehicle altitude Y.
     * @param speedBlocksPerSec vehicle velocity in blocks/second.
     * @return plasma ionization intensity in range [0.0, 1.0].
     */
    public static double computeIonizationIntensity(double altitudeY, double speedBlocksPerSec) {
        if (speedBlocksPerSec < 15.0 || altitudeY < 110.0 || altitudeY > 310.0) {
            return 0.0;
        }
        double speedFactor = Math.clamp((speedBlocksPerSec - 15.0) / 25.0, 0.0, 1.0);
        double layerProgress = (altitudeY - 110.0) / 200.0; // 0.0 at 110, 1.0 at 310
        double bellCurve = Math.sin(layerProgress * Math.PI);
        return Math.clamp(speedFactor * bellCurve, 0.0, 1.0);
    }

    /**
     * Formats altitude as readable meters or kilometers.
     */
    public static String formatAltitude(double altitudeY) {
        if (altitudeY < 1000.0) {
            return String.format("%6.0f m", altitudeY);
        } else {
            return String.format("%6.2f km", altitudeY / 1000.0);
        }
    }

    /**
     * Formats velocity as readable meters/second.
     */
    public static String formatVelocity(double speedBlocksPerSec) {
        return String.format("%6.1f m/s", speedBlocksPerSec);
    }

    /**
     * Formats Mach number with flight regime label (SUBSONIC / TRANSONIC / SUPERSONIC / HYPERSONIC).
     */
    public static String formatMach(double mach) {
        String regime;
        if (mach < 0.8) {
            regime = "SUBSONIC";
        } else if (mach <= 1.2) {
            regime = "TRANSONIC";
        } else if (mach < 5.0) {
            regime = "SUPERSONIC";
        } else {
            regime = "HYPERSONIC";
        }
        return String.format("MACH %4.2f [%s]", mach, regime);
    }

    /** Vertical thrust per tick contributed by the rocket engines for a phase. */
    static double thrustProfile(RocketFlightPhase phase) {
        return switch (phase) {
            case IGNITION -> 0.06;
            case ASCENT -> 0.21;
            case ATMOSPHERE_EXIT -> 0.18;
            case ARRIVAL -> -0.02;
            case LANDING -> 0.005;
            default -> 0.0;
        };
    }
}
