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
 * 3D Java Entity Model for the tiered rocket. Vertical stack: nose cone,
 * fuselage, fins and engine bell.
 */
public class RocketModel extends EntityModel<RocketRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(StellarOdyssey.id("rocket"), "main");

    private final ModelPart root;
    private final ModelPart body;

    public RocketModel(ModelPart root) {
        super(root);
        this.root = root;
        this.body = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6.0F, -24.0F, -6.0F, 12.0F, 24.0F, 12.0F)   // fuselage
                        .texOffs(0, 36).addBox(-4.0F, -32.0F, -4.0F, 8.0F, 8.0F, 8.0F)     // nose cone
                        .texOffs(48, 0).addBox(-2.0F, -2.0F, -2.0F, 4.0F, 2.0F, 4.0F),     // engine bell
                PartPose.ZERO);

        // Four fins
        body.addOrReplaceChild("fin_front",
                CubeListBuilder.create().texOffs(36, 36).addBox(-1.0F, -16.0F, -10.0F, 2.0F, 12.0F, 4.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("fin_back",
                CubeListBuilder.create().texOffs(36, 36).addBox(-1.0F, -16.0F, 6.0F, 2.0F, 12.0F, 4.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("fin_left",
                CubeListBuilder.create().texOffs(36, 36).addBox(-10.0F, -16.0F, -1.0F, 4.0F, 12.0F, 2.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("fin_right",
                CubeListBuilder.create().texOffs(36, 36).addBox(6.0F, -16.0F, -1.0F, 4.0F, 12.0F, 2.0F),
                PartPose.ZERO);

        // Window band
        body.addOrReplaceChild("window_band",
                CubeListBuilder.create().texOffs(0, 52).addBox(-5.0F, -20.0F, -5.0F, 10.0F, 4.0F, 10.0F),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(RocketRenderState state) {
        super.setupAnim(state);
        // The render state mirrors the server-authoritative flight phase, so the airframe
        // can be animated per phase: countdown tremble, ignition roar, thrust rumble, warp
        // charge shiver and a damped settle on touchdown.
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

        // Two slightly detuned sines give a believable mechanical shudder rather than a
        // flat wobble, and the amplitude is driven by the phase (and its progress).
        this.root.xRot = (float) Math.sin(t * 0.82F) * intensity;
        this.root.zRot = (float) Math.sin(t * 0.63F + 1.7F) * intensity * 0.8F;

        // Nose into the direction of travel while under power.
        this.body.xRot = switch (phase) {
            case ASCENT, ATMOSPHERE_EXIT -> 0.05F;
            case ARRIVAL -> -0.04F;
            default -> 0.0F;
        };
        this.body.zRot = (float) Math.sin(t * 0.51F + 0.4F) * intensity * 0.5F;
    }

    /** Vibration amplitude (radians) for a given phase, scaled by its progress where relevant. */
    private static float shakeIntensity(RocketFlightPhase phase, float progress) {
        return switch (phase) {
            case COUNTDOWN -> 0.004F + 0.012F * progress;   // rumble builds as T-0 approaches
            case IGNITION -> 0.022F;
            case ASCENT -> 0.026F;
            case ATMOSPHERE_EXIT -> 0.022F;
            case ORBIT -> 0.003F;                             // near-still; holding station
            case WARP_CHARGE -> 0.010F + 0.016F * progress;   // hyperdrive spin-up
            case ARRIVAL -> 0.020F;
            case LANDING -> 0.016F * (1.0F - progress);       // damped settle
            default -> 0.0F;
        };
    }
}
