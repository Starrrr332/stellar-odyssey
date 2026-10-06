package com.amaro.stellarodyssey.satellites.ecology;

import com.amaro.stellarodyssey.core.ModConstants;
import com.amaro.stellarodyssey.core.lifecycle.ModLifecycleManager;
import com.amaro.stellarodyssey.core.lifecycle.SatelliteModule;
import com.amaro.stellarodyssey.satellites.ecology.ai.LowGravityJumpGoal;
import com.amaro.stellarodyssey.satellites.ecology.ai.VacuumFleeGoal;
import net.minecraft.world.entity.PathfinderMob;

/**
 * Alien Ecology & AI Satellite Module for Stellar Odyssey.
 * <p>
 * Decoupled expansion subsystem providing planetary fauna behavioral goals (low-gravity high leaping,
 * vacuum decompression evasion) and xenomorphic flora spore dispersal routines without hardcoded
 * dimension coupling or circular dependencies with WorldGen or GUI layers.
 */
public final class EcologySatellite implements SatelliteModule {
    public static final String MODULE_ID = "ecology";
    public static final int PRIORITY = 5;

    public static final EcologySatellite INSTANCE = new EcologySatellite();

    private boolean initialized = false;

    public EcologySatellite() {
    }

    /**
     * Initializes and registers the Ecology Satellite into the central lifecycle manager.
     */
    public static void init() {
        if (!ModLifecycleManager.hasModule(MODULE_ID)) {
            ModLifecycleManager.registerModule(INSTANCE);
        }
    }

    static {
        // Auto-register with ModLifecycleManager if loaded
        try {
            init();
        } catch (Throwable t) {
            ModConstants.LOGGER.debug("Deferred registration of EcologySatellite until lifecycle bootstrap: {}", t.getMessage());
        }
    }

    @Override
    public String getId() {
        return MODULE_ID;
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    @Override
    public void onRegister() {
        ModConstants.LOGGER.info("EcologySatellite: registering alien ecological behavioral foundations (priority={}).", PRIORITY);
    }

    @Override
    public void onCommonSetup() {
        ModConstants.LOGGER.info("EcologySatellite: common setup complete. Low-gravity AI and spore dispersal active.");
        this.initialized = true;
    }

    @Override
    public void onClientSetup() {
        ModConstants.LOGGER.debug("EcologySatellite: client setup completed.");
    }

    @Override
    public void onServerStarting() {
        ModConstants.LOGGER.info("EcologySatellite: server starting hook dispatched.");
    }

    /**
     * Attaches core alien ecology behavior goals to the given planetary pathfinder mob.
     * <p>
     * Equips the entity with:
     * 1. {@link VacuumFleeGoal} (High priority emergency survival goal)
     * 2. {@link LowGravityJumpGoal} (Locomotion and terrain navigation goal)
     *
     * @param mob The entity to equip with alien ecological goals.
     */
    public static void attachAlienFaunaAI(PathfinderMob mob) {
        if (mob == null || mob.level().isClientSide()) {
            return;
        }

        // Priority 1: Vacuum Decompression Survival (immediate panic response)
        mob.getGoalSelector().addGoal(1, new VacuumFleeGoal(mob));

        // Priority 3: Low-Gravity High Leaping & Impulse Navigation
        mob.getGoalSelector().addGoal(3, new LowGravityJumpGoal(mob));
    }

    /**
     * Returns whether this satellite module has completed common setup.
     *
     * @return {@code true} if initialized.
     */
    public boolean isInitialized() {
        return this.initialized;
    }
}
