package com.amaro.stellarodyssey.world;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression harness for task F1-OC.1 (test infrastructure &amp; multi-loader QA).
 *
 * <p>Validates that the data-driven celestial body datapack JSON files deserialize
 * cleanly through the Mojang {@code Codec} using a {@code DynamicOps<JsonElement>}
 * (the same pipeline {@code CelestialBodyDataLoader} uses at reload time).
 *
 * <p>Runs on a pure JVM: no Minecraft bootstrap, no GLFW, no rendering context.
 */
class CelestialBodyDatapackCodecHarnessTest {

    /** Relative to the Gradle test working directory (the {@code :common} project dir). */
    private static final Path DATAPACK_DIR =
            Path.of("src", "main", "resources", "data", "stellarodyssey", "stellar_odyssey", "celestial_bodies");

    private static final List<String> EXPECTED_DATAPACK_FILES = List.of(
            "proxima_b.json",
            "nexus_moon.json",
            "exotic_prime.json",
            "gliese_deep.json");

    /** The {@code DynamicOps<JsonElement>} backing the JSON codec pipeline. */
    private static DynamicOps<JsonElement> dynamicOps() {
        return JsonOps.INSTANCE;
    }

    private static CelestialBodyRegistry.PlanetaryBody decodeFile(Path file) throws IOException {
        String raw = Files.readString(file, StandardCharsets.UTF_8);
        JsonElement json = JsonParser.parseString(raw);
        var result = CelestialBodyRegistry.PlanetaryBody.CODEC.parse(dynamicOps(), json);
        assertTrue(result.result().isPresent(),
                () -> "Codec must parse " + file.getFileName() + " without errors: " + result.error());
        return result.result().get();
    }

    @Test
    @DisplayName("All four datapack celestial bodies load through DynamicOps<JsonElement>")
    void testAllDatapackBodiesDecode() throws IOException {
        for (String name : EXPECTED_DATAPACK_FILES) {
            Path file = DATAPACK_DIR.resolve(name);
            assertTrue(Files.isRegularFile(file), "Datapack file must exist: " + file);

            CelestialBodyRegistry.PlanetaryBody body = decodeFile(file);

            assertNotNull(body.dimensionKey(), "dimensionKey must decode");
            assertNotNull(body.starSystemName(), "star_system must decode");
            assertNotNull(body.description(), "description must decode");
        }
    }

    @Test
    @DisplayName("Datapack definitions match the built-in CelestialBodyRegistry constants")
    void testDatapackMatchesBuiltinCatalog() throws IOException {
        Map<ResourceKey<Level>, CelestialBodyRegistry.PlanetaryBody> fromDisk = new HashMap<>();
        for (String name : EXPECTED_DATAPACK_FILES) {
            CelestialBodyRegistry.PlanetaryBody body = decodeFile(DATAPACK_DIR.resolve(name));
            fromDisk.put(body.dimensionKey(), body);
        }

        assertEquals(4, fromDisk.size(), "The datapack must define exactly the four charted bodies");

        // PlanetaryBody is a record: equality is structural, so any drift between the
        // datapack JSON and the hard-coded fallback constants fails here.
        assertEquals(CelestialBodyRegistry.PROXIMA_B, fromDisk.get(ModDimensions.PROXIMA_B));
        assertEquals(CelestialBodyRegistry.NEXUS_MOON, fromDisk.get(ModDimensions.NEXUS_MOON));
        assertEquals(CelestialBodyRegistry.EXOTIC_PRIME, fromDisk.get(ModDimensions.EXOTIC_PRIME));
        assertEquals(CelestialBodyRegistry.GLIESE_DEEP, fromDisk.get(ModDimensions.GLIESE_DEEP));
    }

    @Test
    @DisplayName("Per-field values decode with the expected physics/atmosphere parameters")
    void testDecodedFieldValues() throws IOException {
        CelestialBodyRegistry.PlanetaryBody proximaB = decodeFile(DATAPACK_DIR.resolve("proxima_b.json"));
        assertEquals(ModDimensions.PROXIMA_B, proximaB.dimensionKey());
        assertEquals(0.35, proximaB.gravityMultiplier(), 1e-4);
        assertEquals(0.15f, proximaB.atmosphericPressure(), 1e-4f);
        assertFalse(proximaB.hasBreathableAtmosphere());
        assertEquals(2.40f, proximaB.solarRadiation(), 1e-4f);
        assertEquals("Alpha Centauri", proximaB.starSystemName());
        assertEquals(234.0, proximaB.surfaceTemperatureKelvin(), 1e-4);

        CelestialBodyRegistry.PlanetaryBody nexusMoon = decodeFile(DATAPACK_DIR.resolve("nexus_moon.json"));
        assertEquals(ModDimensions.NEXUS_MOON, nexusMoon.dimensionKey());
        assertEquals(0.16, nexusMoon.gravityMultiplier(), 1e-4);
        assertEquals(0.00f, nexusMoon.atmosphericPressure(), 1e-4f);
        assertEquals(3.20f, nexusMoon.solarRadiation(), 1e-4f);
        assertEquals(110.0, nexusMoon.surfaceTemperatureKelvin(), 1e-4);
    }

    @Test
    @DisplayName("A simulated JSON file decodes through the same DynamicOps pipeline")
    void testSimulatedJsonFileDecodes() throws IOException {
        String simulated = """
                {
                  "dimension": "stellarodyssey:simulated_test_world",
                  "gravity": 0.55,
                  "pressure": 0.42,
                  "breathable": true,
                  "radiation": 0.9,
                  "star_system": "Test System",
                  "description": "Simulated body for the F1-OC.1 harness.",
                  "temperature_kelvin": 250.0
                }
                """;

        Path tempFile = Files.createTempFile("stellarodyssey_body_", ".json");
        try {
            Files.writeString(tempFile, simulated, StandardCharsets.UTF_8);

            CelestialBodyRegistry.PlanetaryBody body = decodeFile(tempFile);

            assertEquals(
                    ResourceKey.create(Registries.DIMENSION, com.amaro.stellarodyssey.StellarOdyssey.id("simulated_test_world")),
                    body.dimensionKey());
            assertEquals(0.55, body.gravityMultiplier(), 1e-4);
            assertEquals(0.42f, body.atmosphericPressure(), 1e-4f);
            assertTrue(body.hasBreathableAtmosphere());
            assertEquals(0.9f, body.solarRadiation(), 1e-4f);
            assertEquals("Test System", body.starSystemName());
            assertEquals("Simulated body for the F1-OC.1 harness.", body.description());
            assertEquals(250.0, body.surfaceTemperatureKelvin(), 1e-4);
        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    @DisplayName("Malformed dimension field is rejected by the codec instead of crashing")
    void testMalformedDimensionIsRejected() {
        // Uppercase letters and spaces are invalid in a namespaced identifier,
        // so the ResourceKey codec must reject the input cleanly.
        String malformed = """
                {
                  "dimension": "Stellar Odyssey!",
                  "gravity": 1.0
                }
                """;
        JsonElement json = JsonParser.parseString(malformed);
        var result = CelestialBodyRegistry.PlanetaryBody.CODEC.parse(dynamicOps(), json);
        assertTrue(result.error().isPresent(), "An invalid dimension id must produce a codec error");
        assertFalse(result.result().isPresent(), "No body should be produced from malformed input");
    }
}
