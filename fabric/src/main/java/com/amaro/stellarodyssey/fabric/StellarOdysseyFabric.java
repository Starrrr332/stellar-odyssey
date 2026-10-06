package com.amaro.stellarodyssey.fabric;

import com.amaro.stellarodyssey.StellarOdyssey;
import net.fabricmc.api.ModInitializer;

public final class StellarOdysseyFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        StellarOdyssey.init();
    }
}
