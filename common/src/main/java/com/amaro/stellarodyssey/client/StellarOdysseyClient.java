package com.amaro.stellarodyssey.client;

/**
 * Loader-agnostic client entrypoint (called from the Fabric client initializer and the
 * NeoForge {@code Dist.CLIENT} mod class). Future home of: O2 HUD, ship renderers,
 * Blockbench model layers and key bindings.
 */
public final class StellarOdysseyClient {
    private StellarOdysseyClient() {
    }

    public static void init() {
        // Phase 3: ClientGuiEvent.RENDER_HUD.register(OxygenHudOverlay::render);
        // Phase 4: EntityRendererRegistry.register(ModEntities.STARSHIP, StarshipRenderer::new);
    }
}
