package com.amaro.stellarodyssey.client;

import net.minecraft.client.Minecraft;

/**
 * Client-only input facade used by common code. Must only be invoked on the
 * physical client inside a {@code level().isClientSide()} branch.
 */
public final class ClientInputProbe {
    private ClientInputProbe() {
    }

    /** Raw jump-key state; reliable even while riding a vehicle. */
    public static boolean isJumpDown() {
        Minecraft mc = Minecraft.getInstance();
        return mc.options != null && mc.options.keyJump.isDown();
    }
}
