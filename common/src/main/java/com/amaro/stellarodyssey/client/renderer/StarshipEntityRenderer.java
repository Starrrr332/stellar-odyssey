package com.amaro.stellarodyssey.client.renderer;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.client.model.StarshipModel;
import com.amaro.stellarodyssey.client.renderer.layer.EmissiveModelLayer;
import com.amaro.stellarodyssey.client.renderer.state.StarshipRenderState;
import com.amaro.stellarodyssey.entity.StarshipEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Modern entity renderer for the Starship vehicle using MC 26.3 SubmitNodeCollector.
 */
public class StarshipEntityRenderer extends EntityRenderer<StarshipEntity, StarshipRenderState> {
    private static final Identifier TEXTURE = StellarOdyssey.id("textures/entity/starship/starship.png");
    private static final Identifier EMISSIVE_TEXTURE = StellarOdyssey.id("textures/entity/starship/starship_emissive.png");
    private final StarshipModel model;

    public StarshipEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new StarshipModel(context.bakeLayer(StarshipModel.LAYER_LOCATION));
        this.shadowRadius = 1.2F;
    }

    @Override
    public StarshipRenderState createRenderState() {
        return new StarshipRenderState();
    }

    @Override
    public void extractRenderState(StarshipEntity entity, StarshipRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.yRot = entity.getViewYRot(partialTick);
        state.xRot = entity.getViewXRot(partialTick);
        state.fuel = entity.getFuel();
        state.thrusting = entity.isThrusting();
    }

    @Override
    public void submit(StarshipRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
        poseStack.rotateDegrees(Axis.XP, -state.xRot);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -0.6F, 0.0F);

        // Diffuse base model pass
        collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        // Full-bright emissive overlay pass (thruster glow, navigation lights, energy conduits)
        EmissiveModelLayer.submitEmissive(this.model, state, poseStack, collector, EMISSIVE_TEXTURE);

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
