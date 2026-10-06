package com.amaro.stellarodyssey.client.renderer.state;

import com.amaro.stellarodyssey.rocket.RocketFlightPhase;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Render state passed to {@link com.amaro.stellarodyssey.client.model.RocketModel}
 * and consumed by {@link com.amaro.stellarodyssey.client.renderer.RocketEntityRenderer}.
 * <p>
 * Mirrors the server-authoritative flight state machine so the client can animate
 * the launch sequence (countdown clench, ignition vibration, ascent thrust, warp
 * charge) without any additional packets.
 */
public class RocketRenderState extends EntityRenderState {
    public boolean launching;
    public int launchTicks;
    public int tierLevel = 1;

    /** Current flight phase mirrored from the entity. */
    public RocketFlightPhase phase = RocketFlightPhase.IDLE;

    /** Progress within the current phase, in the range {@code [0, 1]} (0 when instantaneous). */
    public float phaseProgress;

    /** Visual scale of the tier (taken from {@code RocketTier.modelScale}). */
    public float tierScale = 1.0F;
}
