package com.amaro.stellarodyssey.client.renderer;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.client.model.RocketModel;
import com.amaro.stellarodyssey.client.renderer.state.RocketRenderState;
import com.amaro.stellarodyssey.entity.RocketEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Modern entity renderer for the tiered rocket using MC 26.3 SubmitNodeCollector.
 */
public class RocketEntityRenderer extends EntityRenderer<RocketEntity, RocketRenderState> {
    private static final Identifier TEXTURE = StellarOdyssey.id("textures/entity/rocket/rocket.png");
    private final RocketModel model;

    public RocketEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new RocketModel(context.bakeLayer(RocketModel.LAYER_LOCATION));
        this.shadowRadius = 0.8F;
    }

    @Override
    public RocketRenderState createRenderState() {
        return new RocketRenderState();
    }

    @Override
    public void extractRenderState(RocketEntity entity, RocketRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.launching = entity.isLaunching();
        state.launchTicks = entity.getLaunchTicks();
    }

    @Override
    public void submit(RocketRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, 180.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.0F, 0.0F);

        collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
