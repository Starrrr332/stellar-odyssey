package com.amaro.stellarodyssey.client.camera;

import com.amaro.stellarodyssey.client.ClientRocketFlightHandler;
import com.amaro.stellarodyssey.entity.RocketEntity;
import com.amaro.stellarodyssey.rocket.RocketFlightPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Computes deterministic camera amplification applied while the local player
 * rides a rocket through an active {@link RocketFlightPhase} sequence.
 * <p>
 * The vibration is dynamically coupled with:
 * <ul>
 *     <li>FSM flight phase thrust profile</li>
 *     <li>Felt G-acceleration ($G = 1 + a/g_0$)</li>
 *     <li>Aerodynamic dynamic pressure ($Max-Q = \frac{1}{2}\rho v^2$)</li>
 * </ul>
 */
public final class RocketCameraController {

    /** Frames over which the effect attacks to its full amplitude (pops are smoothed). */
    private static final float ATTACK_FRAMES = 10.0F;
    /** Maximum FOV zoom-out ceiling, so the effect can never exceed +12%. */
    private static final float MAX_FOV_SCALE = 1.12F;

    private RocketCameraController() {
    }

    /**
     * Resolves the ridden rocket of the local player and returns the shake for the
     * current frame, coupling camera vibration to real-time G-force and Max-Q dynamic pressure.
     *
     * @param partialTick render-frame partial tick (0 when unknown)
     */
    public static RocketCameraShake currentShake(float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return RocketCameraShake.NONE;
        }
        if (!(player.getVehicle() instanceof RocketEntity rocket)) {
            return RocketCameraShake.NONE;
        }
        RocketFlightPhase phase = rocket.getPhase();
        if (phase == null || !phase.isInFlight()) {
            return RocketCameraShake.NONE;
        }
        int durationTicks = rocket.getSchedule().durationOf(phase);
        ClientRocketFlightHandler.TelemetrySnapshot telemetry = ClientRocketFlightHandler.currentTelemetry();
        return computeShake(phase, rocket.getPhaseTicks(), durationTicks, partialTick,
                telemetry.gForce(), telemetry.dynamicPressure());
    }

    /**
     * Deterministic shake generator with baseline nominal parameters.
     * Backwards-compatible pure function for headless unit testing.
     */
    public static RocketCameraShake computeShake(RocketFlightPhase phase, int phaseTicks,
                                                 int durationTicks, float partialTick) {
        return computeShake(phase, phaseTicks, durationTicks, partialTick, 1.0, 0.0);
    }

    /**
     * Deterministic shake generator dynamically coupled with real-time G-force and Max-Q dynamic pressure:
     * <ul>
     *     <li>Engine vibration during IGNITION</li>
     *     <li>Violent rumble during ASCENT peaking at Max-Q dynamic pressure</li>
     *     <li>Exosphere transition shudder during ATMOSPHERE_EXIT</li>
     *     <li>Relativistic warp shimmer during WARP_CHARGE / WARP</li>
     *     <li>Touchdown settling rumble during ARRIVAL / LANDING</li>
     * </ul>
     *
     * @param phase           the server-authoritative flight phase (never {@code null})
     * @param phaseTicks      elapsed ticks inside the phase (&ge; 0)
     * @param durationTicks   scheduled phase duration in ticks (0 means unproven/terminal)
     * @param partialTick     render-frame partial tick (&ge; 0), adds sub-tick smoothness
     * @param gForce          felt acceleration in Gs
     * @param dynamicPressure normalized dynamic pressure ($q \in [0.0, 1.0]$)
     */
    public static RocketCameraShake computeShake(RocketFlightPhase phase, int phaseTicks,
                                                 int durationTicks, float partialTick,
                                                 double gForce, double dynamicPressure) {
        if (phase == null || !phase.isInFlight()) {
            return RocketCameraShake.NONE;
        }
        if (phaseTicks < 0) {
            phaseTicks = 0;
        }
        if (partialTick < 0.0F) {
            partialTick = 0.0F;
        }

        float t = phaseTicks + partialTick;
        float progress = durationTicks > 0
                ? Math.clamp(t / (float) durationTicks, 0.0F, 1.0F)
                : 1.0F;

        float fovScale = fovScaleFor(phase, progress);

        float amplitude = baseAmplitudeFor(phase);
        // Smooth attack on phase entry to hide the FSM tick boundary.
        amplitude *= Math.clamp(t / ATTACK_FRAMES, 0.0F, 1.0F);
        if (phase == RocketFlightPhase.LANDING) {
            // Settle down as the rocket touches the pad.
            amplitude *= (1.0F - progress);
        }

        // Dynamic aerodynamic Max-Q pressure buffet & G-force scaling during atmospheric ascent
        if (phase == RocketFlightPhase.ASCENT || phase == RocketFlightPhase.ATMOSPHERE_EXIT) {
            float qScale = (float) Math.clamp(dynamicPressure, 0.0, 1.0);
            float gScale = (float) Math.clamp(Math.max(0.0, gForce - 1.0) / 4.0, 0.0, 1.0);
            amplitude *= (1.0F + 0.35F * qScale + 0.15F * gScale);
        }

        if (amplitude <= 0.0F && fovScale == 1.0F) {
            return RocketCameraShake.NONE;
        }

        float yaw = (float) Math.sin(t * 1.9F) * amplitude;
        float pitch = (float) Math.cos(t * 2.3F) * amplitude;
        float roll = (float) Math.sin(t * 1.3F) * amplitude * 0.5F;

        // High-frequency aerodynamic buffet vibration when dynamic pressure crosses Max-Q threshold
        if (dynamicPressure > 0.2 && (phase == RocketFlightPhase.ASCENT || phase == RocketFlightPhase.ATMOSPHERE_EXIT)) {
            float qBuffet = (float) (dynamicPressure * 0.45);
            yaw += (float) Math.sin(t * 4.7F) * qBuffet;
            pitch += (float) Math.cos(t * 5.3F) * qBuffet;
        }

        return new RocketCameraShake(yaw, pitch, roll, fovScale);
    }

    private static float baseAmplitudeFor(RocketFlightPhase phase) {
        return switch (phase) {
            case IGNITION -> 2.0F;
            case ASCENT -> 3.5F;
            case ATMOSPHERE_EXIT -> 3.0F;
            case WARP_CHARGE -> 2.5F;
            case ARRIVAL -> 3.0F;
            case LANDING -> 2.0F;
            default -> 0.0F;
        };
    }

    private static float fovScaleFor(RocketFlightPhase phase, float progress) {
        float pulse = switch (phase) {
            case IGNITION -> 0.05F * (1.0F - progress);
            case ASCENT -> 0.04F;
            case ATMOSPHERE_EXIT -> 0.035F;
            case WARP_CHARGE -> 0.03F * progress;
            case WARP -> 0.08F;
            default -> 0.0F;
        };
        return Math.clamp(1.0F + pulse, 1.0F, MAX_FOV_SCALE);
    }
}