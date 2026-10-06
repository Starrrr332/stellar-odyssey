package com.amaro.stellarodyssey.client;

/**
 * Client-side cached state for the local player's life support systems.
 * Updated whenever an {@link com.amaro.stellarodyssey.network.OxygenSyncPayload} is received.
 */
public final class ClientOxygenData {
    private static int oxygen = 0;
    private static int maxOxygen = 0;
    private static boolean inHazard = false;

    private ClientOxygenData() {
    }

    public static void set(int currentOxygen, int currentMaxOxygen, boolean currentInHazard) {
        oxygen = currentOxygen;
        maxOxygen = currentMaxOxygen;
        inHazard = currentInHazard;
    }

    public static int getOxygen() {
        return oxygen;
    }

    public static int getMaxOxygen() {
        return maxOxygen;
    }

    public static boolean isInHazard() {
        return inHazard;
    }

    public static float getPercentage() {
        if (maxOxygen <= 0) {
            return 0.0F;
        }
        return Math.clamp((float) oxygen / (float) maxOxygen, 0.0F, 1.0F);
    }
}
