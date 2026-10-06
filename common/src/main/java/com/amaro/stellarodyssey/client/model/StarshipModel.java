package com.amaro.stellarodyssey.client.model;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.client.renderer.state.StarshipRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * 3D Java Entity Model for the exploratory Starship.
 * Compatible with Blockbench Java Entity export hierarchy.
 */
public class StarshipModel extends EntityModel<StarshipRenderState> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(StellarOdyssey.id("starship"), "main");

    private final ModelPart root;
    private final ModelPart hull;
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    public StarshipModel(ModelPart root) {
        super(root);
        this.root = root;
        this.hull = root.getChild("hull");
        this.leftWing = this.hull.getChild("left_wing");
        this.rightWing = this.hull.getChild("right_wing");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition hull = root.addOrReplaceChild("hull",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-10.0F, -4.0F, -16.0F, 20.0F, 8.0F, 32.0F) // main fuselage
                        .texOffs(0, 40).addBox(-6.0F, -3.0F, -24.0F, 12.0F, 6.0F, 8.0F)  // aerodynamic nose
                        .texOffs(0, 54).addBox(-6.0F, -8.0F, -8.0F, 12.0F, 4.0F, 14.0F), // pressurized canopy
                PartPose.ZERO);

        hull.addOrReplaceChild("left_wing",
                CubeListBuilder.create()
                        .texOffs(40, 40).addBox(0.0F, -1.0F, -6.0F, 16.0F, 2.0F, 20.0F),
                PartPose.offset(10.0F, 0.0F, 0.0F));

        hull.addOrReplaceChild("right_wing",
                CubeListBuilder.create()
                        .texOffs(40, 40).mirror().addBox(-16.0F, -1.0F, -6.0F, 16.0F, 2.0F, 20.0F),
                PartPose.offset(-10.0F, 0.0F, 0.0F));

        hull.addOrReplaceChild("left_engine",
                CubeListBuilder.create()
                        .texOffs(72, 0).addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 10.0F),
                PartPose.offset(8.0F, 0.0F, 16.0F));

        hull.addOrReplaceChild("right_engine",
                CubeListBuilder.create()
                        .texOffs(72, 0).mirror().addBox(-3.0F, -3.0F, 0.0F, 6.0F, 6.0F, 10.0F),
                PartPose.offset(-8.0F, 0.0F, 16.0F));

        hull.addOrReplaceChild("left_fin",
                CubeListBuilder.create()
                        .texOffs(104, 0).addBox(-1.0F, -8.0F, -4.0F, 2.0F, 8.0F, 10.0F),
                PartPose.offset(8.0F, -4.0F, 10.0F));

        hull.addOrReplaceChild("right_fin",
                CubeListBuilder.create()
                        .texOffs(104, 0).mirror().addBox(-1.0F, -8.0F, -4.0F, 2.0F, 8.0F, 10.0F),
                PartPose.offset(-8.0F, -4.0F, 10.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(StarshipRenderState state) {
        super.setupAnim(state);
        // Subtle aerodynamic wing flexing during active high-thrust flight
        float flex = state.thrusting ? 0.05F : 0.0F;
        this.leftWing.zRot = flex;
        this.rightWing.zRot = -flex;
    }
}
