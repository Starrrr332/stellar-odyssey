package com.amaro.stellarodyssey.satellites.ecology;

import com.amaro.stellarodyssey.core.lifecycle.ModLifecycleManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EcologySatelliteTest {

    @BeforeAll
    public static void setup() {
        ModLifecycleManager.init();
        EcologySatellite.init();
    }

    @Test
    public void testEcologySatelliteIsRegistered() {
        assertTrue(ModLifecycleManager.hasModule("ecology"), "EcologySatellite should be registered");
        assertNotNull(EcologySatellite.INSTANCE, "EcologySatellite instance should not be null");
    }

    @Test
    public void testPriorityIsCorrect() {
        assertTrue(EcologySatellite.INSTANCE.getPriority() == 5, "EcologySatellite priority should be 5");
    }
}
