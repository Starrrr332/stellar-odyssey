package com.amaro.stellarodyssey.client;

import com.amaro.stellarodyssey.lifesupport.AtmosphereHelper;
import dev.architectury.event.events.client.ClientLifecycleEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.EnumMap;
import java.util.Map;

/**
 * Client-side space acoustics handler for Stellar Odyssey.
 * <p>
 * <b>Acoustic Physics Contract:</b>
 * Sound waves require a physical medium to propagate. In open space, high orbit (&ge; 320m),
 * or airless celestial bodies (such as Nexus Moon), sound propagation is physically altered:
 * <ul>
 *   <li><b>Vacuum without Pressurized Helmet:</b> External acoustic transmission drops to zero.
 *       Air-borne ambient sounds, weather, and distant mob/player sounds are completely silenced (0.0).
 *       A faint bone-conduction / physical impact vibration is retained for blocks (0.02).
 *       Non-diegetic audio (Music, UI Master, Jukebox) remains intact.</li>
 *   <li><b>Vacuum with Pressurized Helmet:</b> The sealed visor and helmet isolate the player from
 *       open space. Air ambience is silent (0.0), while contact vibrations and internal suit mechanics
 *       are filtered and muffled (Blocks 0.25, Players 0.20, Mobs 0.05).</li>
 *   <li><b>Pressurized Habitat / Breathable Atmosphere:</b> When inside an active sealed room
 *       pressurized by an Oxygen Sealer or on an atmospheric planet, standard 1.0 gain is maintained.</li>
 * </ul>
 * <p>
 * <b>Implementation Architecture:</b>
 * Operates via runtime category volume modulation using vanilla {@link SoundManager#updateCategoryVolume(SoundSource, float)}.
 * This smoothly modulates OpenAL channel gains via {@code SoundEngine} without modifying user options or preferences.
 * Fully decoupled and multiloader compatible across Fabric and NeoForge.
 */
public final class SpaceSoundAttenuationHandler {

    /** Muffle factor for block vibrations in bare vacuum without helmet. */
    public static final float BARE_VACUUM_BLOCK_GAIN = 0.02F;
    /** Muffle factor for suit structure-borne block vibrations with helmet. */
    public static final float HELMET_BLOCK_GAIN = 0.25F;
    /** Muffle factor for suit structure-borne player vibrations with helmet. */
    public static final float HELMET_PLAYER_GAIN = 0.20F;
    /** Muffle factor for contact mob impacts with helmet. */
    public static final float HELMET_MOB_GAIN = 0.05F;

    /** Exponential smoothing factor per tick for smooth audio transitions. */
    public static final float LERP_FACTOR = 0.20F;
    /** Epsilon threshold to snap gain and eliminate micro-adjustments. */
    public static final float EPSILON = 0.005F;

    // Track active interpolated gains per category
    private static final Map<SoundSource, Float> CURRENT_GAINS = new EnumMap<>(SoundSource.class);
    private static final Map<SoundSource, Float> APPLIED_GAINS = new EnumMap<>(SoundSource.class);

    private static boolean isAttenuated = false;

    static {
        resetGainsState();
    }

    private SpaceSoundAttenuationHandler() {
    }

    /**
     * Initializes client-side event listeners. Called from {@link StellarOdysseyClient#init()}.
     */
    public static void init() {
        // 1. Client level tick: evaluate environment and smoothly apply category volume modulation
        ClientTickEvent.CLIENT_LEVEL_POST.register(level -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || level == null) {
                resetAll(mc);
                return;
            }

            boolean inVacuum = AtmosphereHelper.isVacuumEnvironment(mc.player);
            boolean hasHelmet = AtmosphereHelper.hasPressurizedHelmet(mc.player);
            boolean inSealed = AtmosphereHelper.isRoomSealed(level, mc.player.blockPosition());

            tick(mc, inVacuum, hasHelmet, inSealed);
        });

        // 2. Client level unload / disconnect: ensure volume returns to standard 1.0F
        try {
            ClientLifecycleEvent.CLIENT_LEVEL_STATE.register((client, level) -> {
                if (level == null) {
                    resetAll(client);
                }
            });
        } catch (Throwable ignored) {
            // Guard against test harnesses without lifecycle registry
        }
    }

    /**
     * Pure, deterministic computation of target category gain.
     * Fully unit-testable headlessly without Minecraft or OpenAL dependencies.
     *
     * @param source The sound category.
     * @param inVacuum Whether the player is in vacuum / orbit.
     * @param hasHelmet Whether the player has a pressurized spacesuit helmet.
     * @param inSealedRoom Whether the player is inside an active sealed habitat.
     * @return Target gain multiplier in [0.0, 1.0].
     */
    public static float computeTargetGain(SoundSource source, boolean inVacuum, boolean hasHelmet, boolean inSealedRoom) {
        // Non-diegetic audio (UI, Music, Jukebox, Voice) is never attenuated
        if (source == SoundSource.MASTER || source == SoundSource.MUSIC ||
            source == SoundSource.RECORDS || source == SoundSource.VOICE) {
            return 1.0F;
        }

        // Inside a sealed pressurized room: normal atmospheric acoustic transmission
        if (inSealedRoom) {
            return 1.0F;
        }

        // Atmospheric environment: normal transmission
        if (!inVacuum) {
            return 1.0F;
        }

        // Hard vacuum environment:
        if (!hasHelmet) {
            // Bare vacuum without helmet: absolute silence for airborne sounds
            if (source == SoundSource.BLOCKS) {
                return BARE_VACUUM_BLOCK_GAIN;
            }
            return 0.0F;
        } else {
            // Pressurized helmet in vacuum: structure-borne filtered acoustic transmission
            return switch (source) {
                case BLOCKS -> HELMET_BLOCK_GAIN;
                case PLAYERS -> HELMET_PLAYER_GAIN;
                case HOSTILE, NEUTRAL -> HELMET_MOB_GAIN;
                case AMBIENT, WEATHER -> 0.0F; // No airborne ambient wind or weather in vacuum
                default -> 1.0F;
            };
        }
    }

    /**
     * Advances interpolation by one tick and updates SoundManager when changed.
     */
    public static void tick(Minecraft mc, boolean inVacuum, boolean hasHelmet, boolean inSealed) {
        boolean anyAttenuated = false;
        SoundManager soundManager = mc != null ? mc.getSoundManager() : null;

        for (SoundSource source : SoundSource.values()) {
            float target = computeTargetGain(source, inVacuum, hasHelmet, inSealed);
            if (target < 0.999F) {
                anyAttenuated = true;
            }

            float current = CURRENT_GAINS.getOrDefault(source, 1.0F);
            float updated;

            if (Math.abs(current - target) <= EPSILON) {
                updated = target;
            } else {
                updated = Mth.lerp(LERP_FACTOR, current, target);
            }

            CURRENT_GAINS.put(source, updated);

            float lastApplied = APPLIED_GAINS.getOrDefault(source, 1.0F);
            if (Math.abs(lastApplied - updated) > EPSILON || (updated == target && lastApplied != target)) {
                if (soundManager != null) {
                    try {
                        soundManager.updateCategoryVolume(source, updated);
                    } catch (Throwable ignored) {
                        // Guard against mock/headless test environments
                    }
                }
                APPLIED_GAINS.put(source, updated);
            }
        }

        isAttenuated = anyAttenuated;
    }

    /**
     * Resets all category volumes to 1.0F on level unload or disconnect.
     */
    public static void resetAll(Minecraft mc) {
        if (!isAttenuated && APPLIED_GAINS.values().stream().allMatch(g -> g >= 0.999F)) {
            return;
        }

        SoundManager soundManager = mc != null ? mc.getSoundManager() : null;
        for (SoundSource source : SoundSource.values()) {
            CURRENT_GAINS.put(source, 1.0F);
            APPLIED_GAINS.put(source, 1.0F);
            if (soundManager != null) {
                try {
                    soundManager.updateCategoryVolume(source, 1.0F);
                } catch (Throwable ignored) {
                }
            }
        }
        isAttenuated = false;
    }

    public static void resetGainsState() {
        for (SoundSource source : SoundSource.values()) {
            CURRENT_GAINS.put(source, 1.0F);
            APPLIED_GAINS.put(source, 1.0F);
        }
        isAttenuated = false;
    }

    public static float getCurrentGain(SoundSource source) {
        return CURRENT_GAINS.getOrDefault(source, 1.0F);
    }

    public static float getAppliedGain(SoundSource source) {
        return APPLIED_GAINS.getOrDefault(source, 1.0F);
    }

    public static boolean isCurrentlyAttenuated() {
        return isAttenuated;
    }
}
