package com.amaro.stellarodyssey.client;

import com.amaro.stellarodyssey.registry.ModSoundEvents;
import com.amaro.stellarodyssey.world.CelestialBodyRegistry;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

/**
 * Plays the eerie alien-world ambience while the local player stands on a charted
 * exoplanet with an unbreathable (but non-vacuum) atmosphere, such as Proxima B,
 * Exotic Prime or Gliese Deep.
 * <p>
 * The ambience track is not a looping SoundInstance, so it is re-triggered on a fixed
 * interval to feel continuous instead of playing once and stopping. Airless bodies
 * (Nexus Moon) stay silent, and playback is routed through
 * {@link SpaceSoundAttenuationHandler}: a helmetless player in hard vacuum hears no
 * ambience, and a sealed visor conducts a filtered version of the world.
 */
public final class AlienAmbienceHandler {
    /** Ticks between ambience re-triggers. 300 ticks = 15 s, matching vanilla biome ambience pacing. */
    public static final int AMBIENCE_INTERVAL_TICKS = 300;

    private static int cooldownTicks = 0;

    private AlienAmbienceHandler() {
    }

    public static void init() {
        ClientTickEvent.CLIENT_LEVEL_POST.register(level -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || level == null) {
                cooldownTicks = 0;
                return;
            }

            if (cooldownTicks > 0) {
                cooldownTicks--;
                return;
            }

            if (!isAlienAtmosphere(level) || SpaceSoundAttenuationHandler.shouldSilenceAmbient()) {
                return;
            }

            float volume = SpaceSoundAttenuationHandler.attenuate(1.0F);
            if (volume > 0.0F) {
                mc.player.playSound(ModSoundEvents.ALIEN_AMBIENCE.get(), volume, 1.0F);
                cooldownTicks = AMBIENCE_INTERVAL_TICKS;
            }
        });
    }

    /**
     * @return {@code true} for charted bodies with an unbreathable, non-vacuum atmosphere
     *         (alien winds and spores can carry sound, unlike hard vacuum).
     */
    private static boolean isAlienAtmosphere(Level level) {
        return CelestialBodyRegistry.getInstance()
                .getBody(level.dimension())
                .map(body -> !body.isVacuum() && !body.hasBreathableAtmosphere())
                .orElse(false);
    }

    /** Test/teardown hook: resumes ambience immediately on the next eligible tick. */
    static void resetCooldown() {
        cooldownTicks = 0;
    }

    static int cooldownTicks() {
        return cooldownTicks;
    }
}
