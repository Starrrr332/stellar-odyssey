package com.amaro.stellarodyssey.world;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.lifesupport.AtmosphereHelper;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;

/**
 * Handles realistic celestial gravity and orbital physics.
 * In orbit (Y >= 320) or on alien planet dimensions, applies reduced gravity
 * and increased safe fall distance so players experience low-gravity physics.
 */
public final class PlanetaryGravityManager {
    public static final Identifier GRAVITY_MOD_ID = StellarOdyssey.id("planetary_gravity");
    public static final Identifier SAFE_FALL_MOD_ID = StellarOdyssey.id("planetary_safe_fall");

    private static final AttributeModifier LOW_GRAVITY_MODIFIER = new AttributeModifier(
            GRAVITY_MOD_ID,
            -0.60, // Reduces gravity by 60% (40% Earth gravity)
            AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
    );

    private static final AttributeModifier SAFE_FALL_MODIFIER = new AttributeModifier(
            SAFE_FALL_MOD_ID,
            12.0, // +12 blocks safe fall threshold
            AttributeModifier.Operation.ADD_VALUE
    );

    private PlanetaryGravityManager() {
    }

    public static void init() {
        TickEvent.PLAYER_POST.register(PlanetaryGravityManager::tickPlayer);
    }

    private static void tickPlayer(Player player) {
        if (player.level().isClientSide()) {
            return;
        }

        boolean inLowGravity = AtmosphereHelper.isVacuumEnvironment(player);

        AttributeInstance gravity = player.getAttribute(Attributes.GRAVITY);
        AttributeInstance safeFall = player.getAttribute(Attributes.SAFE_FALL_DISTANCE);

        if (gravity != null) {
            if (inLowGravity) {
                if (!gravity.hasModifier(GRAVITY_MOD_ID)) {
                    gravity.addOrUpdateTransientModifier(LOW_GRAVITY_MODIFIER);
                }
            } else if (gravity.hasModifier(GRAVITY_MOD_ID)) {
                gravity.removeModifier(GRAVITY_MOD_ID);
            }
        }

        if (safeFall != null) {
            if (inLowGravity) {
                if (!safeFall.hasModifier(SAFE_FALL_MOD_ID)) {
                    safeFall.addOrUpdateTransientModifier(SAFE_FALL_MODIFIER);
                }
            } else if (safeFall.hasModifier(SAFE_FALL_MOD_ID)) {
                safeFall.removeModifier(SAFE_FALL_MOD_ID);
            }
        }
    }
}
