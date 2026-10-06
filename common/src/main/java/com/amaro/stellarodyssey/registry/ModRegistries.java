package com.amaro.stellarodyssey.registry;

import com.amaro.stellarodyssey.core.ModConstants;

/**
 * Unified registry binder for Stellar Odyssey.
 * Binds all subsystem {@link dev.architectury.registry.registries.DeferredRegister} instances
 * to the mod event bus in a single coordinated call.
 */
public final class ModRegistries {
    private ModRegistries() {
    }

    /**
     * Registers all DeferredRegister instances for creative tabs, blocks, items, entities,
     * and sound events in their required deterministic order.
     */
    public static void registerAll() {
        ModConstants.LOGGER.info("Registering all Stellar Odyssey deferred registries...");
        ModCreativeTabs.register();
        ModBlocks.register();
        ModItems.register();
        ModBlockEntityTypes.register();
        ModMenuTypes.register();
        ModEntities.register();
        ModSoundEvents.register();
    }
}
