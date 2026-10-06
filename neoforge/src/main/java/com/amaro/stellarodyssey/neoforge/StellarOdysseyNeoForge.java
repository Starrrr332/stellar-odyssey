package com.amaro.stellarodyssey.neoforge;

import com.amaro.stellarodyssey.StellarOdyssey;
import net.neoforged.fml.common.Mod;

@Mod(StellarOdyssey.MOD_ID)
public final class StellarOdysseyNeoForge {
    public StellarOdysseyNeoForge() {
        // Architectury hooks the mod event bus automatically on NeoForge.
        StellarOdyssey.init();
    }
}
