package com.amaro.stellarodyssey.world;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test verifying Mojang Codec serialization and deserialization
 * for celestial body definitions.
 */
class CelestialBodyCodecTest {

    @Test
    @DisplayName("PlanetaryBody serializes and deserializes cleanly through Mojang Codec")
    void testCodecRoundTrip() {
        String jsonString = """
        {
          "dimension": "stellarodyssey:proxima_b",
          "gravity": 0.35,
          "pressure": 0.15,
          "breathable": false,
          "radiation": 2.4,
          "star_system": "Alpha Centauri",
          "description": "Tidally locked rocky world.",
          "temperature_kelvin": 234.0
        }
        """;

        var jsonElement = JsonParser.parseString(jsonString);
        var parseResult = CelestialBodyRegistry.PlanetaryBody.CODEC.parse(JsonOps.INSTANCE, jsonElement);

        assertTrue(parseResult.result().isPresent(), "Parsing should succeed without errors");
        CelestialBodyRegistry.PlanetaryBody body = parseResult.result().get();

        assertEquals(ModDimensions.PROXIMA_B, body.dimensionKey());
        assertEquals(0.35, body.gravityMultiplier(), 1e-4);
        assertEquals(0.15f, body.atmosphericPressure(), 1e-4f);
        assertFalse(body.hasBreathableAtmosphere());
        assertEquals(2.4f, body.solarRadiation(), 1e-4f);
        assertEquals("Alpha Centauri", body.starSystemName());
        assertEquals("Tidally locked rocky world.", body.description());
        assertEquals(234.0, body.surfaceTemperatureKelvin(), 1e-4);

        // Test serialization back to JsonElement
        var encodeResult = CelestialBodyRegistry.PlanetaryBody.CODEC.encodeStart(JsonOps.INSTANCE, body);
        assertTrue(encodeResult.result().isPresent(), "Encoding should succeed");
    }

    @Test
    @DisplayName("PlanetaryBody applies default values when optional fields are omitted")
    void testCodecDefaultValues() {
        String minimalJson = """
        {
          "dimension": "stellarodyssey:exotic_prime"
        }
        """;

        var jsonElement = JsonParser.parseString(minimalJson);
        var parseResult = CelestialBodyRegistry.PlanetaryBody.CODEC.parse(JsonOps.INSTANCE, jsonElement);

        assertTrue(parseResult.result().isPresent(), "Minimal JSON should parse with defaults");
        CelestialBodyRegistry.PlanetaryBody body = parseResult.result().get();

        assertEquals(ModDimensions.EXOTIC_PRIME, body.dimensionKey());
        assertEquals(1.0, body.gravityMultiplier());
        assertEquals(1.0f, body.atmosphericPressure());
        assertFalse(body.hasBreathableAtmosphere());
        assertEquals(1.0f, body.solarRadiation());
        assertEquals("Sol", body.starSystemName());
        assertEquals("", body.description());
        assertEquals(288.0, body.surfaceTemperatureKelvin());
    }
}
