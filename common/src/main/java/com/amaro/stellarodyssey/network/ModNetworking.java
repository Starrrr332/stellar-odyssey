package com.amaro.stellarodyssey.network;

import com.amaro.stellarodyssey.client.ClientOxygenData;
import dev.architectury.networking.NetworkManager;

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
    }
}
