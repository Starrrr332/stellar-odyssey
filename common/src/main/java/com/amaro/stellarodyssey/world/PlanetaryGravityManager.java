package com.amaro.stellarodyssey.world;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.api.celestial.ICelestialBody;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Handles realistic celestial gravity and orbital physics.
 * <p>
 * Gravity is resolved dynamically from the {@link CelestialBodyRegistry} instead of a single
 * hardcoded vacuum modifier, so every celestial body exposes its own surface gravity
 * (Nexus Moon {@code 0.16g}, Proxima B {@code 0.35g}, Exotic Prime {@code 0.75g},
 * Gliese Deep {@code 1.35g}). Above {@link #ORBITAL_ALTITUDE} the local gravity decays into
 * orbital microgravity.
 * <p>
 * Three vanilla syncable attributes are scaled together so that low gravity feels physically
 * coherent: {@code GRAVITY} (floaty arcs), {@code SAFE_FALL_DISTANCE} and
 * {@code FALL_DAMAGE_MULTIPLIER} (a gentle low-gravity landing is no longer lethal). Changing
 * these on the server auto-synchronises to tracking clients, so no custom packets are required.
 */
public final class PlanetaryGravityManager {
    public static final Identifier GRAVITY_MOD_ID = StellarOdyssey.id("planetary_gravity");
    public static final Identifier SAFE_FALL_MOD_ID = StellarOdyssey.id("planetary_safe_fall");
    public static final Identifier FALL_DAMAGE_MOD_ID = StellarOdyssey.id("planetary_fall_damage");

    /** Vanilla gravity acceleration in blocks/tick^2 (Earth standard, 1.0g). */
    public static final double STANDARD_GRAVITY = 0.08;

    /** Vanilla base safe fall distance in blocks. */
    public static final double VANILLA_SAFE_FALL_DISTANCE = 3.0;

    /** Altitude above which surface gravity fades into orbital microgravity. */
    public static final int ORBITAL_ALTITUDE = 320;

    /** Effective gravity multiplier experienced in orbit / free fall. */
    public static final double ORBITAL_GRAVITY_MULTIPLIER = 0.08;

    private static final double NEUTRAL_EPSILON = 1.0E-4;

    private PlanetaryGravityManager() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(PlanetaryGravityManager::tickPlayer);
        EntityEvent.ADD.register(PlanetaryGravityManager::onEntityAdded);
        try {
            PlayerEvent.CHANGE_DIMENSION.register((player, oldDim, newDim) -> applyAdaptiveGravity(player));
        } catch (Throwable ignored) {
            // Guard against environments where PlayerEvent hooks differ
        }
    }

    // --- Pure physics helpers (no game state, directly unit-testable) -----------------------

    /**
     * Resolves the effective gravity multiplier for an entity at a given altitude above a body.
     *
     * @param y              Entity world Y elevation.
     * @param bodyMultiplier Surface gravity multiplier of the body ({@code 1.0} = Earth).
     * @return The altitude-adjusted gravity multiplier.
     */
    public static double gravityMultiplierAt(double y, double bodyMultiplier) {
        if (y >= ORBITAL_ALTITUDE) {
            return ORBITAL_GRAVITY_MULTIPLIER;
        }
        return bodyMultiplier;
    }

    /**
     * Looks up the surface gravity multiplier of the celestial body owning the given level.
     * Dimensions absent from the catalog (e.g. the Overworld) default to standard {@code 1.0g}.
     *
     * @param level The level whose dimension is queried.
     * @return The body gravity multiplier, or {@code 1.0} when the dimension is not charted.
     */
    public static double resolveBodyGravityMultiplier(Level level) {
        if (level == null) {
            return 1.0;
        }
        return resolveBodyGravityMultiplier(level.dimension());
    }

    /**
     * Looks up the surface gravity multiplier of the celestial body for a given dimension key.
     * Dimensions absent from the catalog default to standard {@code 1.0g}.
     *
     * @param dimensionKey The dimension resource key.
     * @return The body gravity multiplier, or {@code 1.0} when the dimension is not charted.
     */
    public static double resolveBodyGravityMultiplier(ResourceKey<Level> dimensionKey) {
        if (dimensionKey == null) {
            return 1.0;
        }
        return CelestialBodyRegistry.getInstance()
                .getBody(dimensionKey)
                .map(ICelestialBody::gravityMultiplier)
                .orElse(1.0);
    }

    /**
     * Gets the effective gravity multiplier for a level and world altitude.
     */
    public static double getGravityMultiplier(Level level, double y) {
        return gravityMultiplierAt(y, resolveBodyGravityMultiplier(level));
    }

    /**
     * Gets the effective gravity multiplier for a level and position.
     */
    public static double getGravityMultiplier(Level level, BlockPos pos) {
        double y = pos != null ? pos.getY() : 0.0;
        return getGravityMultiplier(level, y);
    }

    /**
     * Gets the effective gravity multiplier for an entity.
     */
    public static double getGravityMultiplier(Entity entity) {
        if (entity == null) {
            return 1.0;
        }
        return gravityMultiplierAt(entity.getY(), resolveBodyGravityMultiplier(entity.level()));
    }

    /**
     * {@code ADD_MULTIPLIED_TOTAL} amount that turns vanilla gravity into the requested multiplier.
     *
     * @param multiplier Target gravity multiplier ({@code 0.35} -> {@code -0.65}).
     * @return The modifier amount to apply.
     */
    public static double gravityModifierAmount(double multiplier) {
        return multiplier - 1.0;
    }

    /**
     * Extra safe fall distance so that low gravity also means soft landings.
     * Scaled as {@code (3.0 / multiplier) - 3.0}: {@code +15.75} blocks on Nexus Moon,
     * {@code +5.57} on Proxima B.
     *
     * @param multiplier Target gravity multiplier.
     * @return Bonus blocks of safe fall distance.
     */
    public static double safeFallDistanceBonus(double multiplier) {
        if (multiplier <= 0.0) {
            return 0.0;
        }
        return (VANILLA_SAFE_FALL_DISTANCE / multiplier) - VANILLA_SAFE_FALL_DISTANCE;
    }

    /**
     * {@code ADD_MULTIPLIED_TOTAL} amount for {@code FALL_DAMAGE_MULTIPLIER} so it becomes the
     * requested multiplier ({@code 0.16} -> {@code -0.84}, i.e. 84% less fall damage).
     *
     * @param multiplier Target gravity multiplier.
     * @return The modifier amount to apply.
     */
    public static double fallDamageMultiplierAmount(double multiplier) {
        return multiplier - 1.0;
    }

    // --- Runtime application -----------------------------------------------------------------

    /**
     * Applies (or clears) the adaptive gravity modifiers on a living entity.
     * Server-side only; the vanilla attribute sync engine propagates the change to clients.
     */
    public static void applyAdaptiveGravity(LivingEntity entity) {
        if (entity == null || (entity.level() != null && entity.level().isClientSide())) {
            return;
        }

        double multiplier = gravityMultiplierAt(entity.getY(), resolveBodyGravityMultiplier(entity.level()));

        AttributeInstance gravity = entity.getAttribute(Attributes.GRAVITY);
        AttributeInstance safeFall = entity.getAttribute(Attributes.SAFE_FALL_DISTANCE);
        AttributeInstance fallDamage = entity.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER);

        if (Math.abs(multiplier - 1.0) < NEUTRAL_EPSILON) {
            clearAdaptiveGravity(entity);
            return;
        }

        if (gravity != null) {
            applyOrUpdateModifier(
                    gravity,
                    GRAVITY_MOD_ID,
                    gravityModifierAmount(multiplier),
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
        }
        if (safeFall != null) {
            applyOrUpdateModifier(
                    safeFall,
                    SAFE_FALL_MOD_ID,
                    safeFallDistanceBonus(multiplier),
                    AttributeModifier.Operation.ADD_VALUE
            );
        }
        if (fallDamage != null) {
            applyOrUpdateModifier(
                    fallDamage,
                    FALL_DAMAGE_MOD_ID,
                    fallDamageMultiplierAmount(multiplier),
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
            );
        }
    }

    /**
     * Applies or updates an attribute modifier only if the current value or operation differs,
     * avoiding redundant attribute recalculation and client packet spam every tick.
     */
    private static void applyOrUpdateModifier(AttributeInstance instance, Identifier id, double amount, AttributeModifier.Operation operation) {
        if (instance == null) {
            return;
        }
        AttributeModifier existing = instance.getModifier(id);
        if (existing != null && Math.abs(existing.amount() - amount) < NEUTRAL_EPSILON && existing.operation() == operation) {
            return;
        }
        instance.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
    }

    /**
     * Clears all adaptive planetary gravity modifiers from an entity.
     */
    public static void clearAdaptiveGravity(LivingEntity entity) {
        if (entity == null) {
            return;
        }
        clearModifier(entity.getAttribute(Attributes.GRAVITY), GRAVITY_MOD_ID);
        clearModifier(entity.getAttribute(Attributes.SAFE_FALL_DISTANCE), SAFE_FALL_MOD_ID);
        clearModifier(entity.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER), FALL_DAMAGE_MOD_ID);
    }

    public static void clearModifier(AttributeInstance instance, Identifier id) {
        if (instance != null && instance.hasModifier(id)) {
            instance.removeModifier(id);
        }
    }

    /**
     * Re-evaluates gravity every player tick so dimension changes and orbital altitude
     * transitions take effect immediately.
     */
    private static void tickPlayer(Player player) {
        applyAdaptiveGravity(player);
    }

    /**
     * Applies gravity as soon as any living entity spawns or is loaded into a level
     * (mobs, alien fauna, and passengers included).
     */
    private static EventResult onEntityAdded(Entity entity, Level level) {
        if (entity instanceof LivingEntity living && level != null && !level.isClientSide()) {
            applyAdaptiveGravity(living);
        }
        return EventResult.pass();
    }
}
