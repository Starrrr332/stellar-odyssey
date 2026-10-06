package com.amaro.stellarodyssey.client.renderer.state;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Render state passed to {@link com.amaro.stellarodyssey.client.model.RocketModel}.
 */
public class RocketRenderState extends EntityRenderState {
    public boolean launching;
    public int launchTicks;
}
