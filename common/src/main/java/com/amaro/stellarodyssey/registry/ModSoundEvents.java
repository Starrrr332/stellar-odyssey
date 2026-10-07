package com.amaro.stellarodyssey.registry;

import com.amaro.stellarodyssey.core.ModConstants;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;

/**
 * Centralized SoundEvent registry for Stellar Odyssey.
 */
public final class ModSoundEvents {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ModConstants.MOD_ID, Registries.SOUND_EVENT);

    /** Sound played during starship thruster burn and acceleration. */
    public static final RegistrySupplier<SoundEvent> STARSHIP_THRUST =
            register("entity.starship.thrust");

    /** Alarm klaxon triggered during cabin breach or oxygen loss. */
    public static final RegistrySupplier<SoundEvent> DECOMPRESSION_ALARM =
            register("hazard.decompression_alarm");

    /** Ambient atmospheric sound loop on hostile alien exoplanets. */
    public static final RegistrySupplier<SoundEvent> ALIEN_AMBIENCE =
            register("ambient.alien_world");

    /** Crystalline harmonic resonance emitted by alien ores and anomalous crystals. */
    public static final RegistrySupplier<SoundEvent> RESONANCE_CRYSTAL =
            register("block.alien_ore.resonate");

    /** Rising beep repeated during pre-flight countdown. */
    public static final RegistrySupplier<SoundEvent> LAUNCH_COUNTDOWN_BEEP =
            register("entity.rocket.launch_beep");

    /** Engine spool-up at IGNITION. */
    public static final RegistrySupplier<SoundEvent> ENGINE_IGNITION =
            register("entity.rocket.engine_ignition");

    /** Tier 1 rocket engine thrust roar (Pioneer solid booster growl) during IGNITION / ASCENT. */
    public static final RegistrySupplier<SoundEvent> ROCKET_THRUST_T1 =
            register("entity.rocket.thrust_t1");

    /** Backward-compatible alias for Tier 1 rocket engine roar. */
    public static final RegistrySupplier<SoundEvent> ENGINE_ROAR_T1 = ROCKET_THRUST_T1;

    /** Tier 2 rocket engine thrust roar (Voyager clustered liquid ion burn). */
    public static final RegistrySupplier<SoundEvent> ROCKET_THRUST_T2 =
            register("entity.rocket.thrust_t2");

    /** Backward-compatible alias for Tier 2 rocket engine roar. */
    public static final RegistrySupplier<SoundEvent> ENGINE_ROAR_T2 = ROCKET_THRUST_T2;

    /** Tier 3 rocket engine thrust roar (Odyssey deep antimatter/warp thruster). */
    public static final RegistrySupplier<SoundEvent> ROCKET_THRUST_T3 =
            register("entity.rocket.thrust_t3");

    /** Backward-compatible alias for Tier 3 rocket engine roar. */
    public static final RegistrySupplier<SoundEvent> ENGINE_ROAR_T3 = ROCKET_THRUST_T3;

    /** Atmospheric entry thunder while descending into a destination atmosphere (ARRIVAL). */
    public static final RegistrySupplier<SoundEvent> ATMOSPHERIC_REENTRY =
            register("entity.rocket.atmospheric_reentry");

    /** Pressurization hiss and pneumatic seal confirmation of the Oxygen Sealer habitat. */
    public static final RegistrySupplier<SoundEvent> OXYGEN_SEALER_PRESSURIZE =
            register("block.oxygen_sealer.pressurize");

    /** Pressurization cycle sound for airlocks and sealed habitats. */
    public static final RegistrySupplier<SoundEvent> AIRLOCK_CYCLE =
            register("block.airlock.cycle");

    /** Backward-compatible alias for habitat pressurization. */
    public static final RegistrySupplier<SoundEvent> HABITAT_PRESSURIZE = OXYGEN_SEALER_PRESSURIZE;

    /** Whoosh of the hyperdrive as the rocket bends space (WARP). */
    public static final RegistrySupplier<SoundEvent> WARP_WHOOSH =
            register("entity.rocket.warp_whoosh");

    private ModSoundEvents() {
    }

    /**
     * Resolves the appropriate engine thrust supplier for a given rocket tier (1 to 3).
     *
     * @param tier Rocket tier level (1, 2, or 3).
     * @return The corresponding RegistrySupplier for SoundEvent.
     */
    public static RegistrySupplier<SoundEvent> getRocketThrustSupplier(int tier) {
        return switch (tier) {
            case 3 -> ROCKET_THRUST_T3;
            case 2 -> ROCKET_THRUST_T2;
            default -> ROCKET_THRUST_T1;
        };
    }

    /**
     * Resolves the appropriate engine thrust SoundEvent for a given rocket tier (1 to 3).
     *
     * @param tier Rocket tier level (1, 2, or 3).
     * @return The corresponding SoundEvent.
     */
    public static SoundEvent getRocketThrustSound(int tier) {
        return getRocketThrustSupplier(tier).get();
    }

    private static RegistrySupplier<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ModConstants.id(name)));
    }

    public static void register() {
        SOUND_EVENTS.register();
    }
}
