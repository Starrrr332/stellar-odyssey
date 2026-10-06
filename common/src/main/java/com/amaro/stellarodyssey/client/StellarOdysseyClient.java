package com.amaro.stellarodyssey.client;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.client.gui.OxygenHudOverlay;
import com.amaro.stellarodyssey.client.model.StarshipModel;
import com.amaro.stellarodyssey.client.renderer.StarshipEntityRenderer;
import com.amaro.stellarodyssey.registry.ModEntities;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;

/**
 * Loader-agnostic client entrypoint (called from the Fabric client initializer and the
 * NeoForge {@code Dist.CLIENT} mod class).
 */
public final class StellarOdysseyClient {
    private StellarOdysseyClient() {
    }

    public static void init() {
        ClientGuiEvent.RENDER_HUD.register(OxygenHudOverlay::render);

        EntityModelLayerRegistry.register(StarshipModel.LAYER_LOCATION, StarshipModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntities.STARSHIP, StarshipEntityRenderer::new);

        StellarOdyssey.clientInit();
    }
}
