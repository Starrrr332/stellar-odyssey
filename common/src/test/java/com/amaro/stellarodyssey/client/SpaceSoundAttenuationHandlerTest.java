package com.amaro.stellarodyssey.client;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JVM coverage for the space acoustics attenuation model (S3-O1).
 * The decision maths are Minecraft-free so they can be tested headlessly.
 */
@DisplayName("Space Sound Attenuation Tests (S3-O1)")
class SpaceSoundAttenuationHandlerTest {

    @Test
    @DisplayName("Sounds are untouched in a normal atmosphere")
    void atmosphereKeepsFullVolume() {
        assertEquals(1.0F, SpaceSoundAttenuationHandler.attenuationScale(false, false), 1.0E-6F);
        assertEquals(1.0F, SpaceSoundAttenuationHandler.attenuationScale(false, true), 1.0E-6F);
    }

    @Test
    @DisplayName("Vacuum without helmet muffles to the faint hull-hum factor")
    void vacuumWithoutHelmetMuffles() {
        float scale = SpaceSoundAttenuationHandler.attenuationScale(true, false);
        assertEquals(SpaceSoundAttenuationHandler.VACUUM_MUFFLE_SCALE, scale, 1.0E-6F);
        assertTrue(scale > 0.0F && scale < 0.2F, "must be a faint (near-silent) hum");
    }

    @Test
    @DisplayName("Vacuum with a sealed helmet conducts the suit-microphone filter")
    void vacuumWithHelmetFilters() {
        float scale = SpaceSoundAttenuationHandler.attenuationScale(true, true);
        assertEquals(SpaceSoundAttenuationHandler.SUIT_FILTER_SCALE, scale, 1.0E-6F);
        assertTrue(scale > SpaceSoundAttenuationHandler.VACUUM_MUFFLE_SCALE,
                "helmet mic must carry more audio than the bare-vacuum muffle");
        assertTrue(scale < 1.0F, "helmet filter must still attenuate");
    }

    @Test
    @DisplayName("attenuate() zeroes below the silence threshold instead of playing a whisper")
    void silenceThresholdApplies() {
        SpaceSoundAttenuationHandler.setEnvironment(true, false);
        // 0.1 * 0.12 = 0.012 < 0.02 => absolute silence.
        assertEquals(0.0F, SpaceSoundAttenuationHandler.attenuate(0.1F), 1.0E-6F);
        // 1.0 * 0.12 = 0.12 >= 0.02 => faint hum survives.
        assertTrue(SpaceSoundAttenuationHandler.attenuate(1.0F) > 0.0F);
    }

    @Test
    @DisplayName("Attenuation never amplifies a sound")
    void neverAmplifies() {
        for (float volume : new float[]{0.05F, 0.5F, 1.0F, 4.0F}) {
            float scaled = SpaceSoundAttenuationHandler.attenuate(volume);
            assertTrue(scaled <= volume + 1.0E-6F, "must never amplify: " + volume);
        }
    }

    @Test
    @DisplayName("The tick sampler resets to a full-volume atmosphere without a player")
    void resetTickStateClearsVacuum() {
        SpaceSoundAttenuationHandler.resetTickState();
        assertFalse(SpaceSoundAttenuationHandler.isVacuumNow());
        assertFalse(SpaceSoundAttenuationHandler.isHelmetOnNow());
        assertEquals(1.0F, SpaceSoundAttenuationHandler.currentScale(), 1.0E-6F);
        assertFalse(SpaceSoundAttenuationHandler.shouldSilenceAmbient());
    }
}
