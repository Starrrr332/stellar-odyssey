package com.amaro.stellarodyssey.network;

import com.amaro.stellarodyssey.client.ClientOxygenData;
import com.amaro.stellarodyssey.client.ClientRocketFlightHandler;
import com.amaro.stellarodyssey.entity.RocketEntity;
import dev.architectury.networking.NetworkManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void init() {
        registerPayloads();
    }

    public static void registerPayloads() {
        NetworkManager.registerS2C(OxygenSyncPayload.TYPE, OxygenSyncPayload.CODEC, (payload, context) -> {
            // Client side receiver executed on render/client thread
            context.queue(() -> {
                dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                    ClientOxygenData.set(payload.oxygen(), payload.maxOxygen(), payload.inHazard());
                });
            });
        });

        // S2C: flight phase broadcast to the rider (overlay, camera, audio)
        NetworkManager.registerS2C(FlightPhasePayload.TYPE, FlightPhasePayload.CODEC, (payload, context) -> {
            context.queue(() -> {
                dev.architectury.utils.EnvExecutor.runInEnv(dev.architectury.utils.Env.CLIENT, () -> () -> {
                    ClientRocketFlightHandler.handleFlightPhase(payload.entityId(), payload.phase(), payload.phaseTicks());
                });
            });
        });

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
    }
}