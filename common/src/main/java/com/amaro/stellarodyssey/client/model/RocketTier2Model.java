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
 * 3D Java Entity Model for the Tier 2 "Voyager" Interplanetary Rocket.
 * <p>
 * Reinforced with Celidium amber alloy. Features dual auxiliary side booster pods,
 * an enlarged stepped aerodynamic heat nose cone, dual heavy engine nozzles,
 * vertical Celidium conduit ribs along the hull, and heavy swept aerodynamic fins.
 */
public class RocketTier2Model extends EntityModel<RocketRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(StellarOdyssey.id("rocket_t2"), "main");

    private final ModelPart root;
    private final ModelPart body;

    public RocketTier2Model(ModelPart root) {
        super(root);
        this.root = root;
        this.body = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Main body containing fuselage, nose cone, engines, conduits, boosters and fins
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // Center Fuselage column (14x26x14)
                        .texOffs(0, 0).addBox(-7.0F, -26.0F, -7.0F, 14.0F, 26.0F, 14.0F)
                        // Stepped aerodynamic heat cone (10x10x10)
                        .texOffs(0, 40).addBox(-5.0F, -36.0F, -5.0F, 10.0F, 10.0F, 10.0F)
                        // Nose cone aerodynamic sensor cap (4x4x4)
                        .texOffs(0, 60).addBox(-2.0F, -40.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.ZERO);

        // Vertical Celidium energy conduit ribs running along hull faces
        body.addOrReplaceChild("conduit_front",
                CubeListBuilder.create().texOffs(24, 60).addBox(-1.0F, -24.0F, -8.0F, 2.0F, 22.0F, 1.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("conduit_back",
                CubeListBuilder.create().texOffs(24, 60).addBox(-1.0F, -24.0F, 7.0F, 2.0F, 22.0F, 1.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("conduit_left",
                CubeListBuilder.create().texOffs(30, 60).addBox(-8.0F, -24.0F, -1.0F, 1.0F, 22.0F, 2.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("conduit_right",
                CubeListBuilder.create().texOffs(30, 60).addBox(7.0F, -24.0F, -1.0F, 1.0F, 22.0F, 2.0F),
                PartPose.ZERO);

        // Dual heavy vector engine nozzles (4x4x4 each)
        body.addOrReplaceChild("engine_left",
                CubeListBuilder.create().texOffs(80, 0).addBox(-6.0F, 0.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("engine_right",
                CubeListBuilder.create().texOffs(80, 0).addBox(2.0F, 0.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.ZERO);

        // Auxiliary side booster pods (6x18x6 each) with nose cones (4x4x4)
        body.addOrReplaceChild("booster_left",
                CubeListBuilder.create()
                        .texOffs(56, 0).addBox(-13.0F, -20.0F, -3.0F, 6.0F, 18.0F, 6.0F)
                        .texOffs(56, 24).addBox(-12.0F, -24.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("booster_right",
                CubeListBuilder.create()
                        .texOffs(56, 0).addBox(7.0F, -20.0F, -3.0F, 6.0F, 18.0F, 6.0F)
                        .texOffs(56, 24).addBox(8.0F, -24.0F, -2.0F, 4.0F, 4.0F, 4.0F),
                PartPose.ZERO);

        // Swept aerodynamic stabilizer fins (4 heavy fins)
        body.addOrReplaceChild("fin_front",
                CubeListBuilder.create().texOffs(40, 40).addBox(-1.0F, -16.0F, -13.0F, 2.0F, 14.0F, 6.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("fin_back",
                CubeListBuilder.create().texOffs(40, 40).addBox(-1.0F, -16.0F, 7.0F, 2.0F, 14.0F, 6.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("fin_left",
                CubeListBuilder.create().texOffs(40, 40).addBox(-17.0F, -16.0F, -1.0F, 4.0F, 14.0F, 2.0F),
                PartPose.ZERO);
        body.addOrReplaceChild("fin_right",
                CubeListBuilder.create().texOffs(40, 40).addBox(13.0F, -16.0F, -1.0F, 4.0F, 14.0F, 2.0F),
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

        // Heavy dual-booster resonant shudder
        this.root.xRot = (float) Math.sin(t * 0.75F) * intensity;
        this.root.zRot = (float) Math.sin(t * 0.58F + 1.5F) * intensity * 0.85F;

        this.body.xRot = switch (phase) {
            case ASCENT, ATMOSPHERE_EXIT -> 0.05F;
            case ARRIVAL -> -0.04F;
            default -> 0.0F;
        };
        this.body.zRot = (float) Math.sin(t * 0.48F + 0.3F) * intensity * 0.5F;
    }

    private static float shakeIntensity(RocketFlightPhase phase, float progress) {
        return switch (phase) {
            case COUNTDOWN -> 0.005F + 0.014F * progress;
            case IGNITION -> 0.025F;
            case ASCENT -> 0.028F;
            case ATMOSPHERE_EXIT -> 0.024F;
            case ORBIT -> 0.003F;
            case WARP_CHARGE -> 0.012F + 0.018F * progress;
            case ARRIVAL -> 0.022F;
            case LANDING -> 0.018F * (1.0F - progress);
            default -> 0.0F;
        };
    }
}
