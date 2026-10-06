package com.amaro.stellarodyssey.fabric.client;

import com.amaro.stellarodyssey.client.StellarOdysseyClient;
import net.fabricmc.api.ClientModInitializer;

public final class StellarOdysseyFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        StellarOdysseyClient.init();
    }
}
