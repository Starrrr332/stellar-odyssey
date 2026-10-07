package com.amaro.stellarodyssey.assembly;

import com.amaro.stellarodyssey.rocket.AssemblyLogic;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cross-checks the shipped rocket component recipes against the canonical mineral
 * bill-of-materials declared in {@link AssemblyLogic}.
 * <p>
 * The recipe JSON files are human-authored resources, while the tier economy is expressed
 * once in code. When the two drift apart the progression silently breaks (for example a
 * Tier 3 component that only consumes Verdantite and never requires its Astralite core).
 * This test makes that class of regression fail loudly instead of shipping.
 */
@DisplayName("Recipe Material Consistency: component recipes match the AssemblyLogic BOM")
class RecipeMaterialConsistencyTest {

    /** Canonical component types, mirroring {@code RocketComponentRegistry}. */
    private static final List<String> COMPONENTS = List.of(
            "cone", "fin", "tank", "engine", "plate", "thruster", "guidance", "heat_shield"
    );

    @BeforeAll
    static void initMinecraft() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Every Tier 1 component recipe consumes vanilla iron and no alien mineral")
    void tier1ComponentsUseIronOnly() throws IOException {
        for (String component : COMPONENTS) {
            String json = loadRecipe("rocket_" + component + "_t1.json");
            assertContainsIngot(json, "minecraft:iron_ingot", component, "t1");
            assertNoAlienIngot(json, component, "t1");
        }
    }

    @Test
    @DisplayName("Every Tier 2 component recipe consumes Celidium")
    void tier2ComponentsUseCelidium() throws IOException {
        for (String component : COMPONENTS) {
            String json = loadRecipe("rocket_" + component + "_t2.json");
            assertContainsIngot(json, "stellarodyssey:celidium_ingot", component, "t2");
            assertFalse(json.contains("\"stellarodyssey:astralite_ingot\""),
                    "Tier 2 component " + component + " must not require Astralite");
        }
    }

    @Test
    @DisplayName("Every Tier 3 component recipe consumes both Verdantite and Astralite")
    void tier3ComponentsUseVerdantiteAndAstralite() throws IOException {
        for (String component : COMPONENTS) {
            String json = loadRecipe("rocket_" + component + "_t3.json");
            assertContainsIngot(json, "stellarodyssey:verdantite_ingot", component, "t3");
            assertContainsIngot(json, "stellarodyssey:astralite_ingot", component, "t3");
        }
    }

    @Test
    @DisplayName("Recipes agree with the ingredient identifiers reported by AssemblyLogic")
    void recipesMatchAssemblyLogicBillOfMaterials() throws IOException {
        for (int tier = 1; tier <= 3; tier++) {
            List<String> expected = AssemblyLogic.getRequiredIngotIds(tier).stream()
                    .map(Identifier::toString)
                    .toList();
            assertFalse(expected.isEmpty(), "AssemblyLogic must declare a BOM for tier " + tier);

            String suffix = "_t" + tier + ".json";
            for (String component : COMPONENTS) {
                String json = loadRecipe("rocket_" + component + suffix);
                for (String ingot : expected) {
                    assertContainsIngot(json, ingot, component, "t" + tier);
                }
            }
        }
    }

    private static void assertContainsIngot(String json, String ingot, String component, String tier) {
        assertTrue(json.contains("\"" + ingot + "\""),
                "Tier " + tier + " component " + component + " must consume " + ingot);
    }

    private static void assertNoAlienIngot(String json, String component, String tier) {
        assertFalse(json.contains("stellarodyssey:celidium_ingot")
                        || json.contains("stellarodyssey:verdantite_ingot")
                        || json.contains("stellarodyssey:astralite_ingot"),
                "Tier " + tier + " component " + component + " must not require alien minerals");
    }

    private static String loadRecipe(String fileName) throws IOException {
        String path = "/data/stellarodyssey/recipe/" + fileName;
        try (InputStream in = RecipeMaterialConsistencyTest.class.getResourceAsStream(path)) {
            assertNotNull(in, "Missing recipe resource on the test classpath: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
