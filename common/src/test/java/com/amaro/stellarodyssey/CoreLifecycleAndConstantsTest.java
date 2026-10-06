package com.amaro.stellarodyssey;

import com.amaro.stellarodyssey.core.ModConstants;
import com.amaro.stellarodyssey.core.lifecycle.ModLifecycleManager;
import com.amaro.stellarodyssey.core.lifecycle.ModLifecycleStage;
import com.amaro.stellarodyssey.core.lifecycle.SatelliteModule;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Core Lifecycle and Constants Contract Tests")
class CoreLifecycleAndConstantsTest {

    @BeforeEach
    @AfterEach
    void resetLifecycleManager() {
        ModLifecycleManager.clearModules();
    }

    @Test
    @DisplayName("Verify ModConstants identity values and Identifier factory")
    void testModConstantsAndIdentifierGeneration() {
        assertEquals("stellarodyssey", ModConstants.MOD_ID, "MOD_ID must match canonical mod identifier");
        assertEquals("Stellar Odyssey", ModConstants.MOD_NAME, "MOD_NAME must match expected branding");
        assertNotNull(ModConstants.LOGGER, "Mod logger instance must never be null");

        Identifier id1 = ModConstants.id("warp_drive");
        assertNotNull(id1, "Generated Identifier must not be null");
        assertEquals("stellarodyssey", id1.getNamespace(), "Identifier namespace must be 'stellarodyssey'");
        assertEquals("warp_drive", id1.getPath(), "Identifier path must match input");

        Identifier id2 = ModConstants.id("dimension/exotic_planet");
        assertEquals("stellarodyssey", id2.getNamespace());
        assertEquals("dimension/exotic_planet", id2.getPath());

        // Verify backward compatibility aliases on StellarOdyssey main class
        assertEquals(ModConstants.MOD_ID, StellarOdyssey.MOD_ID);
        assertEquals(ModConstants.id("test"), StellarOdyssey.id("test"));
    }

    @Test
    @DisplayName("Verify ModLifecycleManager orders modules deterministically by priority and ID")
    void testModulePriorityAndOrdering() {
        TestSatelliteModule low = new TestSatelliteModule("low_priority", 1);
        TestSatelliteModule high = new TestSatelliteModule("high_priority", 100);
        TestSatelliteModule midB = new TestSatelliteModule("mid_b", 50);
        TestSatelliteModule midA = new TestSatelliteModule("mid_a", 50);

        ModLifecycleManager.registerModule(low);
        ModLifecycleManager.registerModule(high);
        ModLifecycleManager.registerModule(midB);
        ModLifecycleManager.registerModule(midA);

        List<SatelliteModule> registered = ModLifecycleManager.getRegisteredModules();
        assertEquals(4, registered.size(), "All 4 modules must be registered");

        // Expected order: high (100) -> mid_a (50) -> mid_b (50) -> low (1)
        assertEquals("high_priority", registered.get(0).getId());
        assertEquals("mid_a", registered.get(1).getId(), "Equal priority must break ties alphabetically by ID");
        assertEquals("mid_b", registered.get(2).getId());
        assertEquals("low_priority", registered.get(3).getId());
    }

    @Test
    @DisplayName("Verify ModLifecycleManager replaces modules on duplicate ID registration")
    void testDuplicateModuleReplacement() {
        TestSatelliteModule v1 = new TestSatelliteModule("satellite_alpha", 10);
        TestSatelliteModule v2 = new TestSatelliteModule("satellite_alpha", 99);

        ModLifecycleManager.registerModule(v1);
        assertTrue(ModLifecycleManager.hasModule("satellite_alpha"));
        assertEquals(1, ModLifecycleManager.getRegisteredModules().size());
        assertEquals(10, ModLifecycleManager.getModule("satellite_alpha").orElseThrow().getPriority());

        // Register v2 with same ID
        ModLifecycleManager.registerModule(v2);
        assertEquals(1, ModLifecycleManager.getRegisteredModules().size(), "Duplicate ID must replace existing entry without size inflation");
        assertEquals(99, ModLifecycleManager.getModule("satellite_alpha").orElseThrow().getPriority(), "New priority must take effect");
    }

    @Test
    @DisplayName("Verify ModLifecycleManager dispatches lifecycle stages in priority order")
    void testDeterministicStageDispatch() {
        List<String> executionLog = new ArrayList<>();

        TestSatelliteModule mod1 = new TrackingSatelliteModule("mod_primary", 10, executionLog);
        TestSatelliteModule mod2 = new TrackingSatelliteModule("mod_secondary", 5, executionLog);

        ModLifecycleManager.registerModule(mod1);
        ModLifecycleManager.registerModule(mod2);

        // 1. REGISTRY Stage
        ModLifecycleManager.fireStage(ModLifecycleStage.REGISTRY);
        assertEquals(List.of("mod_primary:REGISTRY", "mod_secondary:REGISTRY"), executionLog);

        // 2. COMMON_SETUP Stage
        executionLog.clear();
        ModLifecycleManager.fireStage(ModLifecycleStage.COMMON_SETUP);
        assertEquals(List.of("mod_primary:COMMON_SETUP", "mod_secondary:COMMON_SETUP"), executionLog);

        // 3. CLIENT_SETUP Stage
        executionLog.clear();
        ModLifecycleManager.fireStage(ModLifecycleStage.CLIENT_SETUP);
        assertEquals(List.of("mod_primary:CLIENT_SETUP", "mod_secondary:CLIENT_SETUP"), executionLog);

        // 4. SERVER_STARTING Stage
        executionLog.clear();
        ModLifecycleManager.fireStage(ModLifecycleStage.SERVER_STARTING);
        assertEquals(List.of("mod_primary:SERVER_STARTING", "mod_secondary:SERVER_STARTING"), executionLog);
    }

    @Test
    @DisplayName("Verify disabled modules skip lifecycle stage execution")
    void testDisabledModuleSkipping() {
        List<String> executionLog = new ArrayList<>();

        DisabledSatelliteModule disabled = new DisabledSatelliteModule("disabled_mod", 50, executionLog);
        TestSatelliteModule active = new TrackingSatelliteModule("active_mod", 10, executionLog);

        ModLifecycleManager.registerModule(disabled);
        ModLifecycleManager.registerModule(active);

        ModLifecycleManager.fireStage(ModLifecycleStage.COMMON_SETUP);

        assertEquals(1, executionLog.size());
        assertEquals("active_mod:COMMON_SETUP", executionLog.getFirst(), "Disabled module must be skipped completely");
    }

    @Test
    @DisplayName("Verify error resilience: module exception does not crash lifecycle loop")
    void testFaultyModuleErrorResilience() {
        List<String> executionLog = new ArrayList<>();

        FaultySatelliteModule faulty = new FaultySatelliteModule("faulty_mod", 100);
        TestSatelliteModule survivor = new TrackingSatelliteModule("survivor_mod", 10, executionLog);

        ModLifecycleManager.registerModule(faulty);
        ModLifecycleManager.registerModule(survivor);

        assertDoesNotThrow(() -> ModLifecycleManager.fireStage(ModLifecycleStage.COMMON_SETUP),
                "Lifecycle dispatch must swallow module exceptions and continue executing subsequent modules");

        assertEquals(1, executionLog.size());
        assertEquals("survivor_mod:COMMON_SETUP", executionLog.getFirst());
    }

    @Test
    @DisplayName("Verify boundary conditions: null handling and queries")
    void testBoundaryConditions() {
        assertThrows(NullPointerException.class, () -> ModLifecycleManager.registerModule(null));
        assertThrows(NullPointerException.class, () -> ModLifecycleManager.fireStage(null));

        assertFalse(ModLifecycleManager.hasModule(null));
        assertFalse(ModLifecycleManager.hasModule("non_existent"));

        assertEquals(Optional.empty(), ModLifecycleManager.getModule(null));
        assertEquals(Optional.empty(), ModLifecycleManager.getModule("unknown_id"));

        TestSatelliteModule mod = new TestSatelliteModule("test_id", 0);
        ModLifecycleManager.registerModule(mod);
        assertTrue(ModLifecycleManager.hasModule("test_id"));

        ModLifecycleManager.clearModules();
        assertFalse(ModLifecycleManager.hasModule("test_id"));
        assertTrue(ModLifecycleManager.getRegisteredModules().isEmpty());
    }

    // --- Helper Test Satellite Implementations ---

    private static class TestSatelliteModule implements SatelliteModule {
        private final String id;
        private final int priority;

        TestSatelliteModule(String id, int priority) {
            this.id = id;
            this.priority = priority;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public int getPriority() {
            return priority;
        }
    }

    private static class TrackingSatelliteModule extends TestSatelliteModule {
        private final List<String> log;

        TrackingSatelliteModule(String id, int priority, List<String> log) {
            super(id, priority);
            this.log = log;
        }

        @Override
        public void onRegister() {
            log.add(getId() + ":REGISTRY");
        }

        @Override
        public void onCommonSetup() {
            log.add(getId() + ":COMMON_SETUP");
        }

        @Override
        public void onClientSetup() {
            log.add(getId() + ":CLIENT_SETUP");
        }

        @Override
        public void onServerStarting() {
            log.add(getId() + ":SERVER_STARTING");
        }
    }

    private static class DisabledSatelliteModule extends TrackingSatelliteModule {
        DisabledSatelliteModule(String id, int priority, List<String> log) {
            super(id, priority, log);
        }

        @Override
        public boolean isEnabled() {
            return false;
        }
    }

    private static class FaultySatelliteModule extends TestSatelliteModule {
        FaultySatelliteModule(String id, int priority) {
            super(id, priority);
        }

        @Override
        public void onCommonSetup() {
            throw new IllegalStateException("Intentional test fault in satellite module");
        }
    }
}
