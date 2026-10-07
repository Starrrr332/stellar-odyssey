package com.amaro.stellarodyssey.client;

import net.minecraft.sounds.SoundSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Headless pure-JVM test suite for SpaceSoundAttenuationHandler acoustics logic (Front B / Sprint S3).
 */
@DisplayName("Space Sound Attenuation Acoustics Tests (Front B / S3)")
class SpaceSoundAttenuationTest {

    @BeforeEach
    void setUp() {
        SpaceSoundAttenuationHandler.resetGainsState();
    }

    @Test
    @DisplayName("Normal atmosphere produces unattenuated standard volume across all categories")
    void normalAtmosphereIsUnattenuated() {
        for (SoundSource source : SoundSource.values()) {
            float gain = SpaceSoundAttenuationHandler.computeTargetGain(source, false, false, false);
            assertEquals(1.0F, gain, 1.0E-5F, "Atmospheric sound for " + source + " must be 1.0F");
        }
    }

    @Test
    @DisplayName("Pressurized sealed habitat room in vacuum protects sound transmission (1.0F)")
    void sealedRoomInVacuumIsProtected() {
        for (SoundSource source : SoundSource.values()) {
            float gain = SpaceSoundAttenuationHandler.computeTargetGain(source, true, false, true);
            assertEquals(1.0F, gain, 1.0E-5F, "Sealed room sound for " + source + " must be 1.0F");
        }
    }

    @Test
    @DisplayName("Bare vacuum without helmet silences external sounds while preserving music/UI")
    void bareVacuumSilencesExternalSounds() {
        // Non-diegetic audio
        assertEquals(1.0F, SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.MASTER, true, false, false), 1.0E-5F);
        assertEquals(1.0F, SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.MUSIC, true, false, false), 1.0E-5F);
        assertEquals(1.0F, SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.RECORDS, true, false, false), 1.0E-5F);
        assertEquals(1.0F, SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.VOICE, true, false, false), 1.0E-5F);

        // Diegetic airborne sounds completely dead in space
        assertEquals(0.0F, SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.AMBIENT, true, false, false), 1.0E-5F);
        assertEquals(0.0F, SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.WEATHER, true, false, false), 1.0E-5F);
        assertEquals(0.0F, SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.HOSTILE, true, false, false), 1.0E-5F);
        assertEquals(0.0F, SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.NEUTRAL, true, false, false), 1.0E-5F);
        assertEquals(0.0F, SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.PLAYERS, true, false, false), 1.0E-5F);

        // Blocks: faint bone conduction (0.02)
        assertEquals(SpaceSoundAttenuationHandler.BARE_VACUUM_BLOCK_GAIN,
                SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.BLOCKS, true, false, false), 1.0E-5F);
    }

    @Test
    @DisplayName("Pressurized helmet in vacuum muffles environment and filters contact vibration")
    void vacuumWithHelmetProvidesSuitAcoustics() {
        // Airborne sounds cannot reach helmet through vacuum
        assertEquals(0.0F, SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.AMBIENT, true, true, false), 1.0E-5F);
        assertEquals(0.0F, SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.WEATHER, true, true, false), 1.0E-5F);

        // Contact / suit structure conduction
        assertEquals(SpaceSoundAttenuationHandler.HELMET_BLOCK_GAIN,
                SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.BLOCKS, true, true, false), 1.0E-5F);
        assertEquals(SpaceSoundAttenuationHandler.HELMET_PLAYER_GAIN,
                SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.PLAYERS, true, true, false), 1.0E-5F);
        assertEquals(SpaceSoundAttenuationHandler.HELMET_MOB_GAIN,
                SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.HOSTILE, true, true, false), 1.0E-5F);
        assertEquals(SpaceSoundAttenuationHandler.HELMET_MOB_GAIN,
                SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.NEUTRAL, true, true, false), 1.0E-5F);

        // Soundtrack and UI unaffected
        assertEquals(1.0F, SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.MUSIC, true, true, false), 1.0E-5F);
        assertEquals(1.0F, SpaceSoundAttenuationHandler.computeTargetGain(SoundSource.MASTER, true, true, false), 1.0E-5F);
    }

    @Test
    @DisplayName("Tick smoothing transitions smoothly from full volume toward silence without pops")
    void tickSmoothingTransitionsGradually() {
        float initialAmbient = SpaceSoundAttenuationHandler.getCurrentGain(SoundSource.AMBIENT);
        assertEquals(1.0F, initialAmbient, 1.0E-5F);

        // Tick 1 in bare vacuum
        SpaceSoundAttenuationHandler.tick(null, true, false, false);
        float tick1 = SpaceSoundAttenuationHandler.getCurrentGain(SoundSource.AMBIENT);
        assertTrue(tick1 < 1.0F, "Tick 1 must decrease gain");
        assertTrue(tick1 > 0.0F, "Tick 1 must not instantly pop to 0");

        // Several ticks to converge
        for (int i = 0; i < 30; i++) {
            SpaceSoundAttenuationHandler.tick(null, true, false, false);
        }
        float converged = SpaceSoundAttenuationHandler.getCurrentGain(SoundSource.AMBIENT);
        assertEquals(0.0F, converged, 1.0E-4F, "After 30 ticks, ambient must converge to 0");
        assertTrue(SpaceSoundAttenuationHandler.isCurrentlyAttenuated(), "Must report active attenuation");
    }

    @Test
    @DisplayName("Resetting restores all category gains back to 1.0F")
    void resetRestoresStandardGains() {
        for (int i = 0; i < 20; i++) {
            SpaceSoundAttenuationHandler.tick(null, true, false, false);
        }
        assertTrue(SpaceSoundAttenuationHandler.getCurrentGain(SoundSource.AMBIENT) < 0.5F);

        SpaceSoundAttenuationHandler.resetAll(null);

        for (SoundSource source : SoundSource.values()) {
            assertEquals(1.0F, SpaceSoundAttenuationHandler.getCurrentGain(source), 1.0E-5F);
        }
        assertFalse(SpaceSoundAttenuationHandler.isCurrentlyAttenuated());
    }
}
