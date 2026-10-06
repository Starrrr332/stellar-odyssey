package com.amaro.stellarodyssey.network;

import com.amaro.stellarodyssey.client.ClientOxygenData;
import com.amaro.stellarodyssey.entity.RocketEntity;
import com.amaro.stellarodyssey.rocket.RocketFlightPhase;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

public final class ModNetworking {
    private ModNetworking() {
    }

    public static void init() {
        NetworkManager.registerS2C(OxygenSyncPayload.TYPE, OxygenSyncPayload.CODEC, (payload, context) -> {
            // Client side receiver executed on render/client thread
            context.queue(() -> {
                ClientOxygenData.set(payload.oxygen(), payload.maxOxygen(), payload.inHazard());
            });
        });

        // S2C: flight phase broadcast to the rider (overlay, camera, audio)
        NetworkManager.registerS2C(FlightPhasePayload.TYPE, FlightPhasePayload.CODEC, (payload, context) -> {
            context.queue(() -> {
                ClientLevel level = Minecraft.getInstance().level;
                if (level != null) {
                    Entity entity = level.getEntity(payload.entityId());
                    if (entity instanceof RocketEntity rocket) {
                        rocket.setPhase(RocketFlightPhase.fromOrdinal(payload.phaseOrdinal(), rocket.getPhase()));
                        rocket.setPhaseTicks(payload.phaseTicks());
                    }
                }
            });
        });

        // C2S: destination selected in the Star Map UI by the rider
        NetworkManager.registerC2S(SelectDestinationPayload.TYPE, SelectDestinationPayload.CODEC, (payload, context) -> {
            context.queue(() -> {
                Player player = context.getPlayer();
                if (player instanceof ServerPlayer serverPlayer) {
                    Entity entity = serverPlayer.level().getEntity(payload.entityId());
                    if (entity instanceof RocketEntity rocket
                            && rocket.getPhase() == RocketFlightPhase.IDLE
                            && rocket.getFirstPassenger() == serverPlayer) {
                        rocket.setDestination(payload.dimensionKey());
                        if (rocket.validateLaunchPad(serverPlayer)) {
                            rocket.beginLaunchSequence();
                        }
                    }
                }
            });
        });
    }
}