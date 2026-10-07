package com.amaro.stellarodyssey.network;

import com.amaro.stellarodyssey.core.ModConstants;
import com.amaro.stellarodyssey.entity.RocketEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Objects;
import java.util.function.Consumer;

public final class ModNetworking {
    private static volatile ClientHandlers clientHandlers;
    private static boolean payloadsRegistered;

    private ModNetworking() {
    }

    private record ClientHandlers(
            Consumer<OxygenSyncPayload> oxygenSync,
            Consumer<FlightPhasePayload> flightPhase
    ) {
    }

    /** Installs client-only packet consumers from the physical client entrypoint. */
    public static void registerClientHandlers(
            Consumer<OxygenSyncPayload> oxygenSync,
            Consumer<FlightPhasePayload> flightPhase
    ) {
        clientHandlers = new ClientHandlers(
                Objects.requireNonNull(oxygenSync, "oxygenSync handler cannot be null"),
                Objects.requireNonNull(flightPhase, "flightPhase handler cannot be null")
        );
    }

    public static void init() {
        registerPayloads();
    }

    public static synchronized void registerPayloads() {
        if (payloadsRegistered) {
            return;
        }

        NetworkManager.registerS2C(OxygenSyncPayload.TYPE, OxygenSyncPayload.CODEC, (payload, context) ->
                context.queue(() -> {
                    ClientHandlers handlers = clientHandlers;
                    if (handlers != null) {
                        handlers.oxygenSync().accept(payload);
                    } else {
                        ModConstants.LOGGER.warn("Received oxygen sync before client packet handlers were initialized.");
                    }
                }));

        // S2C: flight phase broadcast to the rider (overlay, camera, audio)
        NetworkManager.registerS2C(FlightPhasePayload.TYPE, FlightPhasePayload.CODEC, (payload, context) ->
                context.queue(() -> {
                    ClientHandlers handlers = clientHandlers;
                    if (handlers != null) {
                        handlers.flightPhase().accept(payload);
                    } else {
                        ModConstants.LOGGER.warn("Received flight phase before client packet handlers were initialized.");
                    }
                }));

        // C2S: destination selected in the Star Map UI by the rider
        NetworkManager.registerC2S(SelectDestinationPayload.TYPE, SelectDestinationPayload.CODEC, (payload, context) -> {
            context.queue(() -> {
                Player player = context.getPlayer();
                if (player instanceof ServerPlayer serverPlayer) {
                    Entity entity = serverPlayer.level().getEntity(payload.entityId());
                    if (entity instanceof RocketEntity rocket) {
                        rocket.handleSelectDestination(serverPlayer, payload.destinationDimension());
                    }
                }
            });
        });

        payloadsRegistered = true;
    }
}