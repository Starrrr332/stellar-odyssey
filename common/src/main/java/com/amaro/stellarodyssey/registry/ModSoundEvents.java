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

    private ModSoundEvents() {
    }

    private static RegistrySupplier<SoundEvent> register(String name) {
        return SOUND_EVENTS.register(name, () -> SoundEvent.createVariableRangeEvent(ModConstants.id(name)));
    }

    public static void register() {
        SOUND_EVENTS.register();
    }
}
