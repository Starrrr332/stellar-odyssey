package com.amaro.stellarodyssey;

import com.amaro.stellarodyssey.lifesupport.LifeSupportManager;
import com.amaro.stellarodyssey.network.ModNetworking;
import com.amaro.stellarodyssey.registry.ModBlocks;
import com.amaro.stellarodyssey.registry.ModCreativeTabs;
import com.amaro.stellarodyssey.registry.ModItems;
import com.amaro.stellarodyssey.world.PlanetaryGravityManager;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Loader-agnostic entrypoint. Called once by {@code StellarOdysseyFabric} and
 * {@code StellarOdysseyNeoForge}; everything gameplay-related starts here.
 */
public final class StellarOdyssey {
    public static final String MOD_ID = "stellarodyssey";
    public static final Logger LOGGER = LoggerFactory.getLogger("Stellar Odyssey");

    private StellarOdyssey() {
    }

    public static void init() {
        ModCreativeTabs.register();
        ModBlocks.register();
        ModItems.register();

        ModNetworking.init();
        LifeSupportManager.init();
        PlanetaryGravityManager.init();

        LOGGER.info("Stellar Odyssey initialised - preparing for launch.");
    }

    /** {@code stellarodyssey:<path>} */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
