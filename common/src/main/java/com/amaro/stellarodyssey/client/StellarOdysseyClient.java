package com.amaro.stellarodyssey.client;

import com.amaro.stellarodyssey.client.gui.OxygenHudOverlay;
import dev.architectury.event.events.client.ClientGuiEvent;

/**
 * Loader-agnostic client entrypoint (called from the Fabric client initializer and the
 * NeoForge {@code Dist.CLIENT} mod class).
 */
public final class StellarOdysseyClient {
    private StellarOdysseyClient() {
    }

    public static void init() {
        ClientGuiEvent.RENDER_HUD.register(OxygenHudOverlay::render);
    }
}
