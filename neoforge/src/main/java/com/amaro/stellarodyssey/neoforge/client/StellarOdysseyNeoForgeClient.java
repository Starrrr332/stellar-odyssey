package com.amaro.stellarodyssey.neoforge.client;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.client.StellarOdysseyClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;

/** Only constructed on the physical client - safe place for renderers, HUD and key bindings. */
@Mod(value = StellarOdyssey.MOD_ID, dist = Dist.CLIENT)
public final class StellarOdysseyNeoForgeClient {
    public StellarOdysseyNeoForgeClient() {
        StellarOdysseyClient.init();
    }
}
