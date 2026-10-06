package com.amaro.stellarodyssey;

import com.amaro.stellarodyssey.core.ModConstants;
import com.amaro.stellarodyssey.core.lifecycle.ModLifecycleManager;
import com.amaro.stellarodyssey.core.lifecycle.ModLifecycleStage;
import com.amaro.stellarodyssey.lifesupport.LifeSupportManager;
import com.amaro.stellarodyssey.network.ModNetworking;
import com.amaro.stellarodyssey.registry.ModRegistries;
import com.amaro.stellarodyssey.world.PlanetaryGravityManager;
import dev.architectury.event.events.common.LifecycleEvent;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

/**
 * Loader-agnostic entrypoint. Called once by {@code StellarOdysseyFabric} and
 * {@code StellarOdysseyNeoForge}; everything gameplay-related starts here.
 */
public final class StellarOdyssey {
    public static final String MOD_ID = ModConstants.MOD_ID;
    public static final Logger LOGGER = ModConstants.LOGGER;

    private StellarOdyssey() {
    }

    public static void init() {
        ModLifecycleManager.init();
        ModRegistries.registerAll();
        ModLifecycleManager.fireStage(ModLifecycleStage.REGISTRY);

        ModNetworking.init();
        LifeSupportManager.init();
        PlanetaryGravityManager.init();

        ModLifecycleManager.fireStage(ModLifecycleStage.COMMON_SETUP);

        LifecycleEvent.SERVER_STARTING.register(server -> serverStarting());

        LOGGER.info("Stellar Odyssey initialised - preparing for launch.");
    }

    /** Dispatches client setup to all registered satellite modules. */
    public static void clientInit() {
        ModLifecycleManager.fireStage(ModLifecycleStage.CLIENT_SETUP);
    }

    /** Dispatches server starting to all registered satellite modules. */
    public static void serverStarting() {
        ModLifecycleManager.fireStage(ModLifecycleStage.SERVER_STARTING);
    }

    /** {@code stellarodyssey:<path>} */
    public static Identifier id(String path) {
        return ModConstants.id(path);
    }
}

