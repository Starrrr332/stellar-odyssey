package com.amaro.stellarodyssey.client;

import com.amaro.stellarodyssey.client.model.RocketModel;
import com.amaro.stellarodyssey.client.model.RocketTier2Model;
import com.amaro.stellarodyssey.client.model.RocketTier3Model;
import com.amaro.stellarodyssey.client.renderer.state.RocketRenderState;
import com.amaro.stellarodyssey.rocket.RocketFlightPhase;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test suite verifying Rocket Model Layer Locations, Layer Definitions,
 * ModelPart hierarchy construction, and generated texture assets (Requirement R3).
 */
@DisplayName("Rocket 3D Model Layer Registration & Geometry Tests (R3)")
class RocketModelLayerRegistrationTest {

    @Test
    @DisplayName("ModelLayerLocations are non-null and belong to stellarodyssey namespace")
    void modelLayerLocationsAreValid() {
        assertNotNull(RocketModel.LAYER_LOCATION, "RocketModel.LAYER_LOCATION must not be null");
        assertNotNull(RocketTier2Model.LAYER_LOCATION, "RocketTier2Model.LAYER_LOCATION must not be null");
        assertNotNull(RocketTier3Model.LAYER_LOCATION, "RocketTier3Model.LAYER_LOCATION must not be null");

        assertEquals("stellarodyssey", RocketModel.LAYER_LOCATION.model().getNamespace());
        assertEquals("stellarodyssey", RocketTier2Model.LAYER_LOCATION.model().getNamespace());
        assertEquals("stellarodyssey", RocketTier3Model.LAYER_LOCATION.model().getNamespace());

        assertEquals("main", RocketModel.LAYER_LOCATION.layer());
        assertEquals("main", RocketTier2Model.LAYER_LOCATION.layer());
        assertEquals("main", RocketTier3Model.LAYER_LOCATION.layer());
    }

    @Test
    @DisplayName("ModelLayerLocations have unique paths across all rocket tiers")
    void modelLayerLocationsHaveUniquePaths() {
        Set<ModelLayerLocation> locations = new HashSet<>();
        assertTrue(locations.add(RocketModel.LAYER_LOCATION), "RocketModel layer location must be unique");
        assertTrue(locations.add(RocketTier2Model.LAYER_LOCATION), "RocketTier2Model layer location must be unique");
        assertTrue(locations.add(RocketTier3Model.LAYER_LOCATION), "RocketTier3Model layer location must be unique");

        assertEquals("rocket", RocketModel.LAYER_LOCATION.model().getPath());
        assertEquals("rocket_t2", RocketTier2Model.LAYER_LOCATION.model().getPath());
        assertEquals("rocket_t3", RocketTier3Model.LAYER_LOCATION.model().getPath());

        assertNotEquals(RocketModel.LAYER_LOCATION, RocketTier2Model.LAYER_LOCATION);
        assertNotEquals(RocketModel.LAYER_LOCATION, RocketTier3Model.LAYER_LOCATION);
        assertNotEquals(RocketTier2Model.LAYER_LOCATION, RocketTier3Model.LAYER_LOCATION);
    }

    @Test
    @DisplayName("Layer definitions compile and bake successfully into ModelPart hierarchies with correct children")
    void layerDefinitionsBakeSuccessfully() {
        LayerDefinition def1 = RocketModel.createBodyLayer();
        assertNotNull(def1, "Tier 1 LayerDefinition must not be null");
        ModelPart part1 = def1.bakeRoot();
        assertNotNull(part1, "Tier 1 baked root must not be null");
        assertNotNull(part1.getChild("body"), "Tier 1 must contain 'body'");
        RocketModel model1 = new RocketModel(part1);
        assertNotNull(model1);

        LayerDefinition def2 = RocketTier2Model.createBodyLayer();
        assertNotNull(def2, "Tier 2 LayerDefinition must not be null");
        ModelPart part2 = def2.bakeRoot();
        assertNotNull(part2, "Tier 2 baked root must not be null");
        ModelPart body2 = part2.getChild("body");
        assertNotNull(body2, "Tier 2 must contain 'body'");
        assertNotNull(body2.getChild("booster_left"), "Tier 2 must have booster_left");
        assertNotNull(body2.getChild("booster_right"), "Tier 2 must have booster_right");
        assertNotNull(body2.getChild("engine_left"), "Tier 2 must have engine_left");
        assertNotNull(body2.getChild("engine_right"), "Tier 2 must have engine_right");
        assertNotNull(body2.getChild("conduit_front"), "Tier 2 must have conduit_front");
        assertNotNull(body2.getChild("fin_front"), "Tier 2 must have fin_front");
        RocketTier2Model model2 = new RocketTier2Model(part2);
        assertNotNull(model2);

        LayerDefinition def3 = RocketTier3Model.createBodyLayer();
        assertNotNull(def3, "Tier 3 LayerDefinition must not be null");
        ModelPart part3 = def3.bakeRoot();
        assertNotNull(part3, "Tier 3 baked root must not be null");
        ModelPart body3 = part3.getChild("body");
        assertNotNull(body3, "Tier 3 must contain 'body'");
        assertNotNull(body3.getChild("nacelle_left"), "Tier 3 must have nacelle_left");
        assertNotNull(body3.getChild("nacelle_right"), "Tier 3 must have nacelle_right");
        assertNotNull(body3.getChild("nacelle_front"), "Tier 3 must have nacelle_front");
        assertNotNull(body3.getChild("nacelle_back"), "Tier 3 must have nacelle_back");
        assertNotNull(body3.getChild("radiator_left"), "Tier 3 must have radiator_left");
        assertNotNull(body3.getChild("engine_rear"), "Tier 3 must have engine_rear");
        RocketTier3Model model3 = new RocketTier3Model(part3);
        assertNotNull(model3);
    }

    @Test
    @DisplayName("Tier 2 and Tier 3 models execute animation setup without errors across all flight phases")
    void modelsAnimateAcrossFlightPhases() {
        RocketTier2Model model2 = new RocketTier2Model(RocketTier2Model.createBodyLayer().bakeRoot());
        RocketTier3Model model3 = new RocketTier3Model(RocketTier3Model.createBodyLayer().bakeRoot());

        RocketRenderState state = new RocketRenderState();
        for (RocketFlightPhase phase : RocketFlightPhase.values()) {
            state.phase = phase;
            state.launchTicks = 45;
            state.phaseProgress = 0.5F;
            state.tierScale = 1.0F;

            model2.setupAnim(state);
            model3.setupAnim(state);
        }

        // Test boundary values: tick 0, tick 1000, null phase
        state.phase = null;
        state.launchTicks = 0;
        state.phaseProgress = 0.0F;
        model2.setupAnim(state);
        model3.setupAnim(state);

        state.phase = RocketFlightPhase.WARP_CHARGE;
        state.launchTicks = 1000;
        state.phaseProgress = 1.0F;
        model2.setupAnim(state);
        model3.setupAnim(state);
    }

    @Test
    @DisplayName("Diffuse and Emissive texture files exist on disk with valid dimensions and transparency")
    void textureFilesAreValid() throws IOException {
        String baseDir = "src/main/resources/assets/stellarodyssey/textures/entity/";
        File t2Diffuse = new File(baseDir + "rocket_t2.png");
        File t2Emissive = new File(baseDir + "rocket_t2_emissive.png");
        File t3Diffuse = new File(baseDir + "rocket_t3.png");
        File t3Emissive = new File(baseDir + "rocket_t3_emissive.png");

        assertTrue(t2Diffuse.exists(), "rocket_t2.png must exist");
        assertTrue(t2Emissive.exists(), "rocket_t2_emissive.png must exist");
        assertTrue(t3Diffuse.exists(), "rocket_t3.png must exist");
        assertTrue(t3Emissive.exists(), "rocket_t3_emissive.png must exist");

        BufferedImage imgT2Diffuse = ImageIO.read(t2Diffuse);
        assertNotNull(imgT2Diffuse);
        assertEquals(128, imgT2Diffuse.getWidth());
        assertEquals(128, imgT2Diffuse.getHeight());

        BufferedImage imgT2Emissive = ImageIO.read(t2Emissive);
        assertNotNull(imgT2Emissive);
        assertEquals(128, imgT2Emissive.getWidth());
        assertEquals(128, imgT2Emissive.getHeight());
        // Verify background is transparent
        int cornerAlphaT2 = (imgT2Emissive.getRGB(0, 0) >>> 24);
        assertEquals(0, cornerAlphaT2, "Emissive texture background should be transparent at (0,0)");

        BufferedImage imgT3Diffuse = ImageIO.read(t3Diffuse);
        assertNotNull(imgT3Diffuse);
        assertEquals(128, imgT3Diffuse.getWidth());
        assertEquals(128, imgT3Diffuse.getHeight());

        BufferedImage imgT3Emissive = ImageIO.read(t3Emissive);
        assertNotNull(imgT3Emissive);
        assertEquals(128, imgT3Emissive.getWidth());
        assertEquals(128, imgT3Emissive.getHeight());
        int cornerAlphaT3 = (imgT3Emissive.getRGB(0, 0) >>> 24);
        assertEquals(0, cornerAlphaT3, "Emissive texture background should be transparent at (0,0)");
    }
}
