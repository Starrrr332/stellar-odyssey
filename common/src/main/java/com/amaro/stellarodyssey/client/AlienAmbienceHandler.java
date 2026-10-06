package com.amaro.stellarodyssey.client;

import com.amaro.stellarodyssey.registry.ModSoundEvents;
import com.amaro.stellarodyssey.world.ModDimensions;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;

/**
 * Plays the eerie alien-world ambience loop while the local player is on
 * Proxima B, and fades it out when they leave the dimension.
 */
public final class AlienAmbienceHandler {
    private static boolean playing = false;

    private AlienAmbienceHandler() {
    }

    public static void init() {
        ClientTickEvent.CLIENT_LEVEL_POST.register(level -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) {
                return;
            }
            boolean inAlienWorld = level.dimension().equals(ModDimensions.PROXIMA_B);
            if (inAlienWorld && !playing) {
                playing = true;
                mc.player.playSound(ModSoundEvents.ALIEN_AMBIENCE.get(), 1.0F, 1.0F);
            } else if (!inAlienWorld && playing) {
                playing = false;
            }
        });
    }
}
