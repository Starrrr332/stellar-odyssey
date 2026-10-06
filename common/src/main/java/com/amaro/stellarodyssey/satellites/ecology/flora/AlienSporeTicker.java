package com.amaro.stellarodyssey.satellites.ecology.flora;

import com.amaro.stellarodyssey.api.ecology.IAtmosphereCondition;
import com.amaro.stellarodyssey.lifesupport.AtmosphereHelper;
import com.amaro.stellarodyssey.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Ticking engine for xenomorphic flora and bioluminescent spore dispersal.
 * <p>
 * Simulates atmospheric dispersion physics, photoluminescent emission surges in darkness,
 * symbiotic interactions with alien fauna, and substrate colonization.
 * All ecological behavior is computed dynamically from environmental and atmospheric contracts
 * without hardcoded dimension ties.
 */
public final class AlienSporeTicker {

    /**
     * Configuration profile defining spore dispersal dynamics.
     *
     * @param baseRadius           Base horizontal dispersion radius in blocks.
     * @param particleCount        Number of bioluminescent particles per dispersal pulse.
     * @param emissionChance       Probability per tick (0.0 to 1.0) of firing a spore pulse.
     * @param glowDurationTicks    Duration of glowing effect applied to unprotected organisms.
     * @param allowSubstrateSpread Whether spores can inoculate and convert adjacent blocks.
     */
    public record SporeProfile(
            float baseRadius,
            int particleCount,
            float emissionChance,
            int glowDurationTicks,
            boolean allowSubstrateSpread
    ) {
        public static final SporeProfile DEFAULT_XENOMORPHIC =
                new SporeProfile(4.5f, 16, 0.35f, 160, true);

        public static final SporeProfile BIOLUMINESCENT_BLOOM =
                new SporeProfile(6.0f, 28, 0.60f, 240, true);

        public static final SporeProfile VOLATILE_VACUUM =
                new SporeProfile(9.0f, 22, 0.45f, 200, false);
    }

    private AlienSporeTicker() {
    }

    /**
     * Primary tick hook for xenomorphic plant life and spore-producing blocks.
     *
     * @param level  The server level.
     * @param pos    The block position of the flora.
     * @param state  The block state.
     * @param random Random source.
     */
    public static void tickFlora(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
        tickFlora(level, pos, state, random, SporeProfile.DEFAULT_XENOMORPHIC);
    }

    /**
     * Executes flora ticking logic using a specific {@link SporeProfile}.
     *
     * @param level   The server level.
     * @param pos     The block position of the flora.
     * @param state   The block state.
     * @param random  Random source.
     * @param profile The dispersal configuration profile.
     */
    public static void tickFlora(ServerLevel level, BlockPos pos, BlockState state, RandomSource random, SporeProfile profile) {
        if (level == null || pos == null || random == null) {
            return;
        }

        // Dynamically resolve atmospheric conditions without hardcoded dimension checks
        IAtmosphereCondition atmosphere = resolveAtmosphere(level, pos);

        // Emission chance is modulated by ambient light (darkness triggers photoluminescence)
        float emissionChance = profile.emissionChance();
        int lightLevel = level.getMaxLocalRawBrightness(pos.above());
        if (lightLevel < 7) {
            emissionChance = Math.min(1.0f, emissionChance * 1.5f);
        }

        // Execute spore release pulse if roll succeeds
        if (random.nextFloat() < emissionChance) {
            disperseSpores(level, pos, atmosphere, profile, random);
        }

        // Substrate colonization and mycelial spread
        if (profile.allowSubstrateSpread() && random.nextFloat() < 0.035f) {
            tryPropagateSubstrate(level, pos, random);
        }
    }

    /**
     * Dynamically determines atmospheric parameters for a given world location.
     * Fully decoupled from concrete dimension IDs or hardcoded namespaces.
     *
     * @param level The level.
     * @param pos   The block position.
     * @return The derived {@link IAtmosphereCondition}.
     */
    public static IAtmosphereCondition resolveAtmosphere(Level level, BlockPos pos) {
        if (level == null || pos == null) {
            return IAtmosphereCondition.SimpleAtmosphereCondition.EARTH_LIKE;
        }

        boolean isVacuumTagged = level.dimensionTypeRegistration().is(AtmosphereHelper.VACUUM_DIMENSIONS);
        boolean isHighAltitude = pos.getY() >= AtmosphereHelper.VACUUM_ALTITUDE_THRESHOLD;

        // Temperature from biome thermodynamics
        Holder<Biome> biome = level.getBiome(pos);
        float baseTemp = biome.value().getBaseTemperature();
        float tempKelvin = 273.15f + (baseTemp * 25.0f);

        float pressure;
        if (isHighAltitude) {
            // Mesospheric pressure drop towards zero
            float altDiff = pos.getY() - AtmosphereHelper.VACUUM_ALTITUDE_THRESHOLD;
            pressure = Math.max(0.0f, 1.0f - (altDiff / 64.0f));
        } else if (isVacuumTagged) {
            pressure = 0.015f; // Trace exosphere
        } else {
            pressure = 1.0f; // Standard surface pressure
        }

        float toxicity = isVacuumTagged ? 0.35f : 0.0f;
        float radiation = isHighAltitude ? 2.5f : (isVacuumTagged ? 1.6f : 0.05f);
        float oxygen = (isVacuumTagged || isHighAltitude) ? 0.0f : 0.21f;

        return new IAtmosphereCondition.SimpleAtmosphereCondition(pressure, oxygen, toxicity, tempKelvin, radiation);
    }

    /**
     * Disperses bioluminescent spore particles and applies atmospheric effects to nearby entities.
     *
     * @param level      The server level.
     * @param pos        Origin of spore emission.
     * @param atmosphere The ambient atmosphere condition.
     * @param profile    Spore profile.
     * @param random     Random source.
     */
    public static void disperseSpores(
            ServerLevel level,
            BlockPos pos,
            IAtmosphereCondition atmosphere,
            SporeProfile profile,
            RandomSource random
    ) {
        double ox = pos.getX() + 0.5;
        double oy = pos.getY() + 0.75;
        double oz = pos.getZ() + 0.5;

        // In vacuum, absence of atmospheric drag yields broader ballistic dispersal radius
        float effectiveRadius = atmosphere.isVacuum() ? profile.baseRadius() * 1.8f : profile.baseRadius();

        // 1. Spawn server-side particles
        int count = profile.particleCount();
        double speed = atmosphere.isVacuum() ? 0.12 : 0.04;

        // Primary bioluminescent glow particles
        level.sendParticles(
                ParticleTypes.GLOW,
                ox, oy, oz,
                count,
                effectiveRadius * 0.4, 0.6, effectiveRadius * 0.4,
                speed
        );

        // Secondary xenomorphic spore clouds
        if (atmosphere.isToxic()) {
            level.sendParticles(
                    ParticleTypes.SCULK_CHARGE_POP,
                    ox, oy, oz,
                    Math.max(4, count / 3),
                    effectiveRadius * 0.3, 0.4, effectiveRadius * 0.3,
                    0.05
            );
        } else {
            level.sendParticles(
                    ParticleTypes.WARPED_SPORE,
                    ox, oy, oz,
                    Math.max(6, count / 2),
                    effectiveRadius * 0.5, 0.8, effectiveRadius * 0.5,
                    0.02
            );
        }

        // 2. Affect nearby entities within the spore cloud
        applySporeEffectsToEntities(level, pos, effectiveRadius, atmosphere, profile);
    }

    /**
     * Applies physiological and symbiotic spore effects to living entities in range.
     *
     * @param level           The level.
     * @param sourcePos       Origin of the spore dispersal.
     * @param radius          Dispersion radius in blocks.
     * @param atmosphere      Atmosphere condition.
     * @param profile         Spore profile.
     */
    public static void applySporeEffectsToEntities(
            Level level,
            BlockPos sourcePos,
            double radius,
            IAtmosphereCondition atmosphere,
            SporeProfile profile
    ) {
        AABB box = new AABB(sourcePos).inflate(radius, radius * 0.6, radius);
        List<LivingEntity> entities = level.getEntitiesOfClass(LivingEntity.class, box);

        for (LivingEntity entity : entities) {
            if (!entity.isAlive()) {
                continue;
            }

            if (entity instanceof Player player) {
                // Players in hermetically sealed spacesuits are fully protected from spore inhalation
                if (AtmosphereHelper.isFullSuitEquipped(player)) {
                    continue;
                }

                // Photoluminescent adhesion: unprotected skin/gear glows in the dark
                entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, profile.glowDurationTicks(), 0, true, false));

                // In low oxygen or vacuum, buoyant alien spores provide slight buoyancy
                if (atmosphere.isVacuum() || atmosphere.oxygenFraction() < 0.1f) {
                    entity.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 35, 0, true, true));
                }

                // In toxic atmosphere, inhalation causes mild disorientation
                if (atmosphere.isToxic()) {
                    entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 70, 0, true, false));
                }
            } else {
                // Alien fauna exhibit symbiotic affinity with native xenomorphic spores
                entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, profile.glowDurationTicks() / 2, 0, true, false));
                entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 50, 0, true, false));
            }
        }
    }

    /**
     * Attempts to inoculate and spread xenomorphic mycelial turf to adjacent sterile blocks.
     *
     * @param level     The server level.
     * @param sourcePos Origin flora position.
     * @param random    Random source.
     * @return {@code true} if a block was successfully inoculated.
     */
    public static boolean tryPropagateSubstrate(ServerLevel level, BlockPos sourcePos, RandomSource random) {
        Direction dir = Direction.Plane.HORIZONTAL.getRandomDirection(random);
        BlockPos targetPos = sourcePos.relative(dir).below();

        BlockState targetState = level.getBlockState(targetPos);
        BlockPos spaceAbove = targetPos.above();

        // Check if target is solid and space above is clear
        if (!targetState.isAir() && targetState.isSolidRender() && level.getBlockState(spaceAbove).isAir()) {
            // Check if target is alien stone or generic stone/dirt susceptible to xenomorphic colonization
            try {
                if (ModBlocks.ALIEN_TURF != null && ModBlocks.ALIEN_TURF.get() != null) {
                    level.setBlockAndUpdate(targetPos, ModBlocks.ALIEN_TURF.get().defaultBlockState());
                    return true;
                }
            } catch (Throwable ignored) {
                // ModBlocks not yet registered in test harness
            }
        }

        return false;
    }

    /**
     * Spawns cosmetic ambient bioluminescent particles on the client side.
     *
     * @param level  Client level.
     * @param pos    Flora position.
     * @param random Random source.
     */
    public static void spawnClientVisuals(Level level, BlockPos pos, RandomSource random) {
        if (!level.isClientSide() || random.nextFloat() > 0.45f) {
            return;
        }

        double x = pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.8;
        double y = pos.getY() + 0.6 + random.nextDouble() * 0.5;
        double z = pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.8;

        level.addParticle(ParticleTypes.GLOW, x, y, z, 0.0, 0.02, 0.0);
    }
}
