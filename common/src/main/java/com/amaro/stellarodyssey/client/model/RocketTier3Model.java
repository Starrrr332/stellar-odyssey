package com.amaro.stellarodyssey.client.model;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.client.renderer.state.RocketRenderState;
import com.amaro.stellarodyssey.rocket.RocketFlightPhase;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * 3D Java Entity Model for the Tier 3 "Odyssey" Interstellar Flagship.
 * <p>
 * Forged from Astralite and Verdantite. Features a heavy command fuselage,
 * hyperdrive crystalline focal dome, quad radial outboard warp nacelles,
 * four Astralite crystalline radiator wings, a heavy tri-engine thruster cluster,
 * and a panoramic pressurized forward bridge cupola.
 */
public class RocketTier3Model extends EntityModel<RocketRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(StellarOdyssey.id("rocket_t3"), "main");

    private final ModelPart root;
    private final ModelPart body;

    public RocketTier3Model(ModelPart root) {
        super(root);
        this.root = root;
        this.body = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Main command fuselage and forward hyperdrive focal assembly
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // Heavy reinforced hexagonal command fuselage (16x28x16)
                        .texOffs(0, 0).addBox(-8.0F, -28.0F, -8.0F, 16.0F, 28.0F, 16.0F)
                        // Crystalline hyperdrive focal dome (12x10x12)
                        .texOffs(0, 44).addBox(-6.0F, -38.0F, -6.0F, 12.0F, 10.0F, 12.0F)
                        // Hyperdrive focal emitter crystal (6x6x6)
                        .texOffs(0, 66).addBox(-3.0F, -44.0F, -3.0F, 6.0F, 6.0F, 6.0F)
                        // Panoramic pressurized bridge cupola (8x4x2)
                        .texOffs(48, 44).addBox(-4.0F, -22.0F, -10.0F, 8.0F, 4.0F, 2.0F),
                PartPose.ZERO);

        // Quad radial outboard warp nacelles (6x22x6 each)
        body.addOrReplaceChild("nacelle_left",
                CubeListBuilder.create().texOffs(64, 0).addBox(-15.0F, -22.0F, -3.0F, 6.0F, 22.0F, 6.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("nacelle_right",
                CubeListBuilder.create().texOffs(64, 0).addBox(9.0F, -22.0F, -3.0F, 6.0F, 22.0F, 6.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("nacelle_front",
                CubeListBuilder.create().texOffs(64, 0).addBox(-3.0F, -22.0F, -15.0F, 6.0F, 22.0F, 6.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("nacelle_back",
                CubeListBuilder.create().texOffs(64, 0).addBox(-3.0F, -22.0F, 9.0F, 6.0F, 22.0F, 6.0F),
                PartPose.ZERO);

        // Astralite crystalline radiator wings (1x16x8 each)
        body.addOrReplaceChild("radiator_left",
                CubeListBuilder.create().texOffs(88, 0).addBox(-16.0F, -19.0F, -4.0F, 1.0F, 16.0F, 8.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("radiator_right",
                CubeListBuilder.create().texOffs(88, 0).addBox(15.0F, -19.0F, -4.0F, 1.0F, 16.0F, 8.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("radiator_front",
                CubeListBuilder.create().texOffs(88, 0).addBox(-4.0F, -19.0F, -16.0F, 8.0F, 16.0F, 1.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("radiator_back",
                CubeListBuilder.create().texOffs(88, 0).addBox(-4.0F, -19.0F, 15.0F, 8.0F, 16.0F, 1.0F),
                PartPose.ZERO);

        // Tri-engine heavy ion cluster in delta formation (5x4x5 each)
        body.addOrReplaceChild("engine_rear",
                CubeListBuilder.create().texOffs(64, 28).addBox(-2.5F, 0.0F, 1.5F, 5.0F, 4.0F, 5.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("engine_front_left",
                CubeListBuilder.create().texOffs(64, 28).addBox(-6.0F, 0.0F, -4.5F, 5.0F, 4.0F, 5.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("engine_front_right",
                CubeListBuilder.create().texOffs(64, 28).addBox(1.0F, 0.0F, -4.5F, 5.0F, 4.0F, 5.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(RocketRenderState state) {
        super.setupAnim(state);
        RocketFlightPhase phase = state.phase != null ? state.phase : RocketFlightPhase.IDLE;
        float intensity = shakeIntensity(phase, state.phaseProgress);
        float t = state.launchTicks;

        if (intensity <= 0.0F) {
            this.root.xRot = 0.0F;
            this.root.zRot = 0.0F;
            this.body.xRot = 0.0F;
            this.body.zRot = 0.0F;
            return;
        }

        // Heavy interstellar flagship low-frequency massive vibration and warp harmonics
        this.root.xRot = (float) Math.sin(t * 0.70F) * intensity;
        this.root.zRot = (float) Math.sin(t * 0.55F + 1.2F) * intensity * 0.9F;

        this.body.xRot = switch (phase) {
            case ASCENT, ATMOSPHERE_EXIT -> 0.05F;
            case ARRIVAL -> -0.04F;
            default -> 0.0F;
        };
        this.body.zRot = (float) Math.sin(t * 0.45F + 0.2F) * intensity * 0.5F;
    }

    private static float shakeIntensity(RocketFlightPhase phase, float progress) {
        return switch (phase) {
            case COUNTDOWN -> 0.006F + 0.016F * progress;
            case IGNITION -> 0.028F;
            case ASCENT -> 0.030F;
            case ATMOSPHERE_EXIT -> 0.025F;
            case ORBIT -> 0.003F;
            case WARP_CHARGE -> 0.015F + 0.022F * progress; // High-energy hyperdrive harmonic oscillation
            case ARRIVAL -> 0.024F;
            case LANDING -> 0.020F * (1.0F - progress);
            default -> 0.0F;
        };
    }
}
