package com.amaro.stellarodyssey;

import com.amaro.stellarodyssey.core.lifecycle.ModLifecycleManager;
import com.amaro.stellarodyssey.core.lifecycle.ModLifecycleStage;
import com.amaro.stellarodyssey.core.lifecycle.SatelliteModule;
import com.amaro.stellarodyssey.satellites.ecology.EcologySatellite;
import com.amaro.stellarodyssey.satellites.starmap.StarMapSatellite;
import com.amaro.stellarodyssey.satellites.worldgen.WorldGenSatellite;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Decoupled Satellites Contract and DAG Architecture Tests")
public class DecoupledSatellitesContractTest {

    @BeforeEach
    @AfterEach
    void resetLifecycleManager() {
        ModLifecycleManager.clearModules();
    }

    @Test
    @DisplayName("Verify WorldGen, Ecology, and StarMap satellites implement SatelliteModule contract")
    void testSatellitesImplementContract() {
        assertTrue(SatelliteModule.class.isAssignableFrom(WorldGenSatellite.class),
                "WorldGenSatellite must implement SatelliteModule SPI");
        assertTrue(SatelliteModule.class.isAssignableFrom(EcologySatellite.class),
                "EcologySatellite must implement SatelliteModule SPI");
        assertTrue(SatelliteModule.class.isAssignableFrom(StarMapSatellite.class),
                "StarMapSatellite must implement SatelliteModule SPI");

        WorldGenSatellite worldGen = WorldGenSatellite.INSTANCE;
        EcologySatellite ecology = EcologySatellite.INSTANCE;
        StarMapSatellite starMap = StarMapSatellite.INSTANCE;

        assertEquals("worldgen", worldGen.getId(), "WorldGenSatellite module ID must be 'worldgen'");
        assertEquals("ecology", ecology.getId(), "EcologySatellite module ID must be 'ecology'");
        assertEquals("starmap", starMap.getId(), "StarMapSatellite module ID must be 'starmap'");

        assertTrue(worldGen.isEnabled(), "WorldGenSatellite must be enabled by default");
        assertTrue(ecology.isEnabled(), "EcologySatellite must be enabled by default");
        assertTrue(starMap.isEnabled(), "StarMapSatellite must be enabled by default");

        assertEquals(10, worldGen.getPriority(), "WorldGenSatellite must have priority 10");
        assertEquals(5, ecology.getPriority(), "EcologySatellite must have priority 5");
        assertEquals(0, starMap.getPriority(), "StarMapSatellite must have priority 0");
    }

    @Test
    @DisplayName("Verify deterministic lifecycle priority order: WorldGen (10) > Ecology (5) > StarMap (0)")
    void testSatellitePriorityHierarchyInLifecycleManager() {
        ModLifecycleManager.registerModule(StarMapSatellite.INSTANCE);
        ModLifecycleManager.registerModule(WorldGenSatellite.INSTANCE);
        ModLifecycleManager.registerModule(EcologySatellite.INSTANCE);

        List<SatelliteModule> ordered = ModLifecycleManager.getRegisteredModules();
        assertEquals(3, ordered.size());

        assertEquals("worldgen", ordered.get(0).getId(), "Priority 10 must execute first");
        assertEquals("ecology", ordered.get(1).getId(), "Priority 5 must execute second");
        assertEquals("starmap", ordered.get(2).getId(), "Priority 0 must execute third");

        // Verify lifecycle stages execute deterministically
        assertDoesNotThrow(() -> ModLifecycleManager.fireStage(ModLifecycleStage.REGISTRY));
        assertDoesNotThrow(() -> ModLifecycleManager.fireStage(ModLifecycleStage.COMMON_SETUP));
        assertDoesNotThrow(() -> ModLifecycleManager.fireStage(ModLifecycleStage.SERVER_STARTING));
    }

    @Test
    @DisplayName("Verify DAG Architectural Decoupling: zero cross-satellite imports between WorldGen, Ecology, and StarMap")
    void testDirectedAcyclicGraphCleanliness() throws IOException {
        Path satellitesDir = findSatellitesDir();
        assertTrue(Files.exists(satellitesDir), "Satellites directory must exist at: " + satellitesDir);

        // 1. Verify WorldGen satellite contains NO imports from Ecology or StarMap
        assertNoCrossPackageImports(
                satellitesDir.resolve("worldgen"),
                List.of("com.amaro.stellarodyssey.satellites.ecology", "com.amaro.stellarodyssey.satellites.starmap"),
                "WorldGen Satellite"
        );

        // 2. Verify Ecology satellite contains NO imports from WorldGen or StarMap
        assertNoCrossPackageImports(
                satellitesDir.resolve("ecology"),
                List.of("com.amaro.stellarodyssey.satellites.worldgen", "com.amaro.stellarodyssey.satellites.starmap"),
                "Ecology Satellite"
        );

        // 3. Verify StarMap satellite contains NO imports from WorldGen or Ecology
        assertNoCrossPackageImports(
                satellitesDir.resolve("starmap"),
                List.of("com.amaro.stellarodyssey.satellites.worldgen", "com.amaro.stellarodyssey.satellites.ecology"),
                "StarMap Satellite"
        );
    }

    @Test
    @DisplayName("Verify individual satellite lifecycle methods execute cleanly")
    void testIndividualSatelliteLifecycleExecution() {
        WorldGenSatellite worldGen = WorldGenSatellite.INSTANCE;
        EcologySatellite ecology = EcologySatellite.INSTANCE;
        StarMapSatellite starMap = StarMapSatellite.INSTANCE;

        assertDoesNotThrow(worldGen::onRegister);
        assertDoesNotThrow(worldGen::onCommonSetup);
        assertDoesNotThrow(worldGen::onServerStarting);

        assertDoesNotThrow(ecology::onRegister);
        assertDoesNotThrow(ecology::onCommonSetup);
        assertDoesNotThrow(ecology::onServerStarting);

        assertDoesNotThrow(starMap::onRegister);
        assertDoesNotThrow(starMap::onCommonSetup);
        assertDoesNotThrow(starMap::onServerStarting);
    }

    @Test
    @DisplayName("Verify automatic discovery and priority ordering of all 3 satellites via Java ServiceLoader SPI")
    void testSatelliteDiscoveryViaSPI() {
        ModLifecycleManager.clearModules();
        assertEquals(0, ModLifecycleManager.getRegisteredModules().size(), "Modules must be empty after clear");

        ModLifecycleManager.discoverModules();

        List<SatelliteModule> discovered = ModLifecycleManager.getRegisteredModules();
        assertEquals(3, discovered.size(), "SPI discovery must automatically load all 3 satellite modules");

        assertTrue(ModLifecycleManager.hasModule("worldgen"), "WorldGen satellite must be discovered via SPI");
        assertTrue(ModLifecycleManager.hasModule("ecology"), "Ecology satellite must be discovered via SPI");
        assertTrue(ModLifecycleManager.hasModule("starmap"), "StarMap satellite must be discovered via SPI");

        assertEquals("worldgen", discovered.get(0).getId(), "WorldGenSatellite (priority 10) must be first");
        assertEquals("ecology", discovered.get(1).getId(), "EcologySatellite (priority 5) must be second");
        assertEquals("starmap", discovered.get(2).getId(), "StarMapSatellite (priority 0) must be third");
    }

    /**
     * Test harness helper to prepare BuiltInRegistries.BLOCK intrusive holders
     * in headless unit test environments where Bootstrap.bootStrap() has frozen the registry.
     */
    public static void prepareTestRegistry() {
        try {
            var registry = net.minecraft.core.registries.BuiltInRegistries.BLOCK;
            Class<?> clazz = registry.getClass();
            while (clazz != null && clazz != Object.class) {
                try {
                    java.lang.reflect.Field holdersField = clazz.getDeclaredField("unregisteredIntrusiveHolders");
                    holdersField.setAccessible(true);
                    if (holdersField.get(registry) == null) {
                        holdersField.set(registry, new java.util.IdentityHashMap<>());
                        java.lang.reflect.Field frozenField = clazz.getDeclaredField("frozen");
                        frozenField.setAccessible(true);
                        frozenField.setBoolean(registry, false);
                    }
                    break;
                } catch (NoSuchFieldException ignored) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Throwable ignored) {
        }
    }

    // --- Helper Validation Methods ---

    private static void assertNoCrossPackageImports(Path packageDir, List<String> forbiddenPackages, String moduleName) throws IOException {
        if (!Files.exists(packageDir)) {
            return;
        }

        try (Stream<Path> files = Files.walk(packageDir)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(javaFile -> {
                try {
                    List<String> lines = Files.readAllLines(javaFile);
                    for (int lineNum = 0; lineNum < lines.size(); lineNum++) {
                        String line = lines.get(lineNum).trim();
                        if (line.startsWith("import ")) {
                            for (String forbidden : forbiddenPackages) {
                                assertFalse(line.contains(forbidden),
                                        String.format("%s class '%s' violates DAG decoupling at line %d by importing forbidden package '%s': %s",
                                                moduleName, javaFile.getFileName(), lineNum + 1, forbidden, line));
                            }
                        }
                    }
                } catch (IOException e) {
                    fail("Failed reading source file for architecture verification: " + javaFile, e);
                }
            });
        }
    }

    private static Path findSatellitesDir() {
        Path current = Path.of("").toAbsolutePath();
        if (Files.exists(current.resolve("src/main/java/com/amaro/stellarodyssey/satellites"))) {
            return current.resolve("src/main/java/com/amaro/stellarodyssey/satellites");
        }
        if (Files.exists(current.resolve("common/src/main/java/com/amaro/stellarodyssey/satellites"))) {
            return current.resolve("common/src/main/java/com/amaro/stellarodyssey/satellites");
        }
        for (int i = 0; i < 5; i++) {
            if (Files.exists(current.resolve("common/src/main/java/com/amaro/stellarodyssey/satellites"))) {
                return current.resolve("common/src/main/java/com/amaro/stellarodyssey/satellites");
            }
            if (current.getParent() == null) {
                break;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Could not find satellites directory from " + Path.of("").toAbsolutePath());
    }
}
