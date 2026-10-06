package com.amaro.stellarodyssey.registry.tiers;

import com.amaro.stellarodyssey.StellarOdyssey;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/**
 * Immutable specification of a rocket tier in Stellar Odyssey.
 * <p>
 * Mirrors {@link AlienMineralTier}: a parametrized record whose values drive
 * fuel capacity, launch altitude, countdown length, component requirements,
 * destination dimension and the visual palette of the rocket.
 * </p>
 */
public record RocketTier(
        int tierLevel,
        String name,
        Identifier id,
        Component displayName,
        ResourceKey<Level> destination,
        int fuelCapacity,
        double launchAltitude,
        int countdownTicks,
        int componentCount,
        int colorHex,
        float modelScale,
        int ascentTicks,
        int warpChargeTicks,
        double atmosphereExitAltitude
) {
    public RocketTier {
        if (tierLevel < 1) {
            throw new IllegalArgumentException("Rocket tier level must be >= 1");
        }
        if (fuelCapacity <= 0) {
            throw new IllegalArgumentException("Rocket tier fuel capacity must be > 0");
        }
        if (componentCount < 1) {
            throw new IllegalArgumentException("Rocket tier component count must be >= 1");
        }
        if (countdownTicks < 1) {
            throw new IllegalArgumentException("Rocket tier countdown must last at least one tick");
        }
        if (ascentTicks < 1) {
            throw new IllegalArgumentException("Rocket tier ascent duration must be >= 1 tick");
        }
        if (warpChargeTicks < 1) {
            throw new IllegalArgumentException("Rocket tier warp charge must last at least one tick");
        }
        if (atmosphereExitAltitude <= 0 || atmosphereExitAltitude >= launchAltitude) {
            throw new IllegalArgumentException(
                    "Rocket tier atmosphere exit altitude must be in (0, launchAltitude)");
        }
    }

    public static RocketTier create(int tierLevel, String name, ResourceKey<Level> destination,
                                    int fuelCapacity, double launchAltitude, int countdownTicks,
                                    int componentCount, int colorHex, float modelScale,
                                    int ascentTicks, int warpChargeTicks, double atmosphereExitAltitude) {
        return new RocketTier(
                tierLevel,
                name,
                StellarOdyssey.id(name),
                Component.translatable("tier.stellarodyssey.rocket." + name),
                destination,
                fuelCapacity,
                launchAltitude,
                countdownTicks,
                componentCount,
                colorHex,
                modelScale,
                ascentTicks,
                warpChargeTicks,
                atmosphereExitAltitude
        );
    }
}
