package com.amaro.stellarodyssey.client.camera;

import com.amaro.stellarodyssey.entity.RocketEntity;
import com.amaro.stellarodyssey.rocket.RocketFlightPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

/**
 * Computes the deterministic camera amplification applied while the local player
 * rides a rocket through an active {@link RocketFlightPhase} sequence.
 *
 * <p>The maths live in {@link #computeShake(RocketFlightPhase, int, int, float)}
 * and are deliberately free of any Minecraft dependency so they can be unit-tested
 * headlessly. {@link #currentShake(float)} is the thin client accessor used by the
 * loader-specific hooks:
 * <ul>
 *     <li><b>Fabric</b>: a mixin on {@code net.minecraft.client.Camera} (no Fabric API
 *         camera event exists on 26.3).</li>
 *     <li><b>NeoForge</b>: {@code ViewportEvent.ComputeCameraAngles} + {@code ComputeFov}.</li>
 * </ul>
 *
 * <p>Reference amplitudes mirror the HUD shake already drawn by
 * {@code LaunchCinematicOverlay}, but applied to the actual world view.
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
     * current frame. Returns {@link RocketCameraShake#NONE} when the player is not
     * riding a rocket whose FSM is in flight.
     *
     * @param partialTick render-frame partial tick (0 when unknown), used only for
     *                    sub-tick smoothness of the trigger/shake waves
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
        return computeShake(phase, rocket.getPhaseTicks(), durationTicks, partialTick);
    }

    /**
     * Deterministic shake generator: engine vibration during IGNITION, violent rumble
     * through ASCENT / ATMOSPHERE_EXIT, a warp shimmer during WARP_CHARGE / WARP and a
     * settling rumble on ARRIVAL / LANDING that decays toward touchdown.
     *
     * @param phase         the server-authoritative flight phase (never {@code null})
     * @param phaseTicks    elapsed ticks inside the phase (&ge; 0)
     * @param durationTicks scheduled phase duration in ticks (0 means unproven/terminal)
     * @param partialTick   render-frame partial tick (&ge; 0), adds sub-tick smoothness
     */
    public static RocketCameraShake computeShake(RocketFlightPhase phase, int phaseTicks,
                                                 int durationTicks, float partialTick) {
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

        if (amplitude <= 0.0F && fovScale == 1.0F) {
            return RocketCameraShake.NONE;
        }

        float yaw = (float) Math.sin(t * 1.9F) * amplitude;
        float pitch = (float) Math.cos(t * 2.3F) * amplitude;
        float roll = (float) Math.sin(t * 1.3F) * amplitude * 0.5F;
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