package com.amaro.stellarodyssey.satellites.worldgen;

import com.amaro.stellarodyssey.core.lifecycle.ModLifecycleManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WorldGenSatelliteTest {

    @BeforeAll
    public static void setup() {
        ModLifecycleManager.init();
        WorldGenSatellite.init();
    }

    @Test
    public void testWorldGenSatelliteIsRegistered() {
        assertTrue(ModLifecycleManager.hasModule("worldgen"), "WorldGenSatellite should be registered in the ModLifecycleManager");
        assertNotNull(WorldGenSatellite.INSTANCE, "WorldGenSatellite instance should not be null");
    }

    @Test
    public void testPriorityIsCorrect() {
        assertTrue(WorldGenSatellite.INSTANCE.getPriority() == 10, "WorldGenSatellite priority should be 10 to run before other satellites");
    }
}
