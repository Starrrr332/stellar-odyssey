package com.amaro.stellarodyssey.client;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.client.gui.LaunchCinematicOverlay;
import com.amaro.stellarodyssey.client.gui.OxygenHudOverlay;
import com.amaro.stellarodyssey.client.model.RocketModel;
import com.amaro.stellarodyssey.client.model.RocketTier2Model;
import com.amaro.stellarodyssey.client.model.RocketTier3Model;
import com.amaro.stellarodyssey.client.model.StarshipModel;
import com.amaro.stellarodyssey.client.renderer.RocketEntityRenderer;
import com.amaro.stellarodyssey.client.renderer.StarshipEntityRenderer;
import com.amaro.stellarodyssey.client.screen.AssemblyTableScreen;
import com.amaro.stellarodyssey.network.FlightPhasePayload;
import com.amaro.stellarodyssey.network.ModNetworking;
import com.amaro.stellarodyssey.network.OxygenSyncPayload;
import com.amaro.stellarodyssey.registry.ModEntities;
import com.amaro.stellarodyssey.satellites.starmap.StarMapSatellite;
import com.amaro.stellarodyssey.satellites.starmap.screen.StarMapScreen;
import com.amaro.stellarodyssey.registry.ModMenuTypes;
import dev.architectury.event.events.client.ClientGuiEvent;
import dev.architectury.registry.client.gui.MenuScreenRegistry;
import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;

/**
 * Loader-agnostic client entrypoint (called from the Fabric client initializer and the
 * NeoForge {@code Dist.CLIENT} mod class).
 */
public final class StellarOdysseyClient {
    private static boolean initialized;

    private StellarOdysseyClient() {
    }

    public static synchronized void init() {
        if (initialized) {
            return;
        }

        ModNetworking.registerClientHandlers(
                payload -> ClientOxygenData.set(payload.oxygen(), payload.maxOxygen(), payload.inHazard()),
                payload -> ClientRocketFlightHandler.handleFlightPhase(
                        payload.entityId(), payload.phase(), payload.phaseTicks())
        );
        StarMapSatellite.registerScreenOpener((catalog, rocketTier, rocketEntityId) ->
                net.minecraft.client.Minecraft.getInstance().setScreenAndShow(
                        new StarMapScreen(catalog, rocketTier, rocketEntityId)));

        ClientGuiEvent.RENDER_HUD.register(OxygenHudOverlay::render);
        ClientGuiEvent.RENDER_HUD.register(LaunchCinematicOverlay::render);

        EntityModelLayerRegistry.register(StarshipModel.LAYER_LOCATION, StarshipModel::createBodyLayer);
        EntityRendererRegistry.register(ModEntities.STARSHIP, StarshipEntityRenderer::new);

        EntityModelLayerRegistry.register(RocketModel.LAYER_LOCATION, RocketModel::createBodyLayer);
        EntityModelLayerRegistry.register(RocketTier2Model.LAYER_LOCATION, RocketTier2Model::createBodyLayer);
        EntityModelLayerRegistry.register(RocketTier3Model.LAYER_LOCATION, RocketTier3Model::createBodyLayer);
        EntityRendererRegistry.register(ModEntities.ROCKET, RocketEntityRenderer::new);

        MenuScreenRegistry.registerScreenFactory(ModMenuTypes.ASSEMBLY_TABLE.get(), AssemblyTableScreen::new);

        AlienAmbienceHandler.init();
        SpaceSoundAttenuationHandler.init();
        
        dev.architectury.event.events.client.ClientTickEvent.CLIENT_POST.register(
            com.amaro.stellarodyssey.client.ClientRocketFlightHandler::tickClient
        );

        StellarOdyssey.clientInit();
        initialized = true;
    }
}
