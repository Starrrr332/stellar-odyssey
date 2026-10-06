package com.amaro.stellarodyssey.client.model;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.client.renderer.state.RocketRenderState;
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
        // Slight vibration during countdown / liftoff
        if (state.launching) {
            float shake = (float) Math.sin(state.launchTicks * 0.8F) * 0.01F;
            this.root.xRot = shake;
            this.root.zRot = shake * 0.7F;
        } else {
            this.root.xRot = 0.0F;
            this.root.zRot = 0.0F;
        }
    }
}
