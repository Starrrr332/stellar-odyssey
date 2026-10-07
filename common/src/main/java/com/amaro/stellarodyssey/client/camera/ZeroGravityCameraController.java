package com.amaro.stellarodyssey.client.camera;

import com.amaro.stellarodyssey.world.PlanetaryGravityManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;

/**
 * Handles camera roll and 6-DOF visual feedback when the player is in zero-gravity / orbital altitude.
 */
public final class ZeroGravityCameraController {

    private static float currentRoll = 0.0F;
    private static float targetRoll = 0.0F;

    private ZeroGravityCameraController() {}

    /**
     * Computes the camera roll to apply for zero gravity feedback.
     * When moving laterally in zero-G, the camera gently rolls to simulate weightlessness.
     */
    public static float computeCameraRoll(float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return 0.0F;
        }

        // Check if player is in orbital microgravity
        double gravity = PlanetaryGravityManager.getGravityMultiplier(player);
        if (gravity > PlanetaryGravityManager.ORBITAL_GRAVITY_MULTIPLIER * 1.5) {
            // Not in zero-G, return to upright
            targetRoll = 0.0F;
        } else {
            // In zero-G, apply roll based on sideways movement (strafing) and yaw rotation
            float strafe = player.xxa;
            targetRoll = strafe * 5.0F; // max 5 degrees tilt
        }

        // Smoothly interpolate current roll towards target roll
        currentRoll = Mth.lerp(partialTicks * 0.1F, currentRoll, targetRoll);

        return currentRoll;
    }
}
