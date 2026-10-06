package com.amaro.stellarodyssey.client.renderer.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Render state passed to {@link com.amaro.stellarodyssey.client.model.StarshipModel}.
 */
public class StarshipRenderState extends EntityRenderState {
    public float yRot;
    public float xRot;
    public float fuel;
    public boolean thrusting;
}
