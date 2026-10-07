package com.amaro.stellarodyssey.world;

import dev.architectury.event.events.common.TickEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Handles the actual movement physics for players in zero gravity environments.
 */
public final class ZeroGravityPhysicsManager {

    private ZeroGravityPhysicsManager() {}

    public static void init() {
        TickEvent.PLAYER_PRE.register(ZeroGravityPhysicsManager::tickPlayerPhysics);
    }

    private static void tickPlayerPhysics(Player player) {
        if (player.level() == null) return;
        
        double gravity = PlanetaryGravityManager.getGravityMultiplier(player);
        
        // If we are in orbital microgravity (zero-G)
        if (gravity <= PlanetaryGravityManager.ORBITAL_GRAVITY_MULTIPLIER * 1.5) {
            // Apply zero-g flight mechanics
            if (!player.getAbilities().flying) {
                // If the player is jumping (spacebar), give them upward thrust
                if (player.zza > 0) {
                    Vec3 look = player.getLookAngle();
                    Vec3 currentMovement = player.getDeltaMovement();
                    
                    // Add thrust in the direction they are looking
                    double thrust = 0.02;
                    player.setDeltaMovement(
                        currentMovement.x + look.x * thrust,
                        currentMovement.y + look.y * thrust,
                        currentMovement.z + look.z * thrust
                    );
                }
                
                // Add artificial drag (friction) for vacuum so they don't float infinitely fast
                Vec3 movement = player.getDeltaMovement();
                player.setDeltaMovement(movement.scale(0.98));
                
                player.fallDistance = 0.0F; // Prevent fall damage accumulating
            }
        }
    }
}
