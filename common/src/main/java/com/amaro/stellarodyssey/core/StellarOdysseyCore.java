package com.amaro.stellarodyssey.core;

import com.amaro.stellarodyssey.core.lifecycle.ModLifecycleManager;
import com.amaro.stellarodyssey.core.lifecycle.ModLifecycleStage;

/**
 * Core lifecycle orchestrator for internal subsystems.
 * Provides direct lifecycle triggers for satellite modules.
 */
public final class StellarOdysseyCore {
    private StellarOdysseyCore() {
    }

    public static void fireRegistry() {
        ModLifecycleManager.fireStage(ModLifecycleStage.REGISTRY);
    }

    public static void fireCommonSetup() {
        ModLifecycleManager.fireStage(ModLifecycleStage.COMMON_SETUP);
    }

    public static void fireClientSetup() {
        ModLifecycleManager.fireStage(ModLifecycleStage.CLIENT_SETUP);
    }

    public static void fireServerStarting() {
        ModLifecycleManager.fireStage(ModLifecycleStage.SERVER_STARTING);
    }
}
