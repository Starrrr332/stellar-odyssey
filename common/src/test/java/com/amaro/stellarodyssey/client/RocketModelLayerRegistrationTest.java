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

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit test suite verifying Rocket Model Layer Locations and Layer Definitions (Requirement R3).
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
    @DisplayName("Layer definitions compile and bake successfully into ModelPart hierarchies")
    void layerDefinitionsBakeSuccessfully() {
        LayerDefinition def1 = RocketModel.createBodyLayer();
        assertNotNull(def1, "Tier 1 LayerDefinition must not be null");
        ModelPart part1 = def1.bakeRoot();
        assertNotNull(part1, "Tier 1 baked root must not be null");
        RocketModel model1 = new RocketModel(part1);
        assertNotNull(model1);

        LayerDefinition def2 = RocketTier2Model.createBodyLayer();
        assertNotNull(def2, "Tier 2 LayerDefinition must not be null");
        ModelPart part2 = def2.bakeRoot();
        assertNotNull(part2, "Tier 2 baked root must not be null");
        RocketTier2Model model2 = new RocketTier2Model(part2);
        assertNotNull(model2);

        LayerDefinition def3 = RocketTier3Model.createBodyLayer();
        assertNotNull(def3, "Tier 3 LayerDefinition must not be null");
        ModelPart part3 = def3.bakeRoot();
        assertNotNull(part3, "Tier 3 baked root must not be null");
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
}
