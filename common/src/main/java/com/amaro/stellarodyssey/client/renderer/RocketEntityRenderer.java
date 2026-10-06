package com.amaro.stellarodyssey.client.renderer;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.client.model.RocketModel;
import com.amaro.stellarodyssey.client.renderer.state.RocketRenderState;
import com.amaro.stellarodyssey.entity.RocketEntity;
import com.amaro.stellarodyssey.registry.tiers.RocketTier;
import com.amaro.stellarodyssey.rocket.RocketFlightPhase;
import com.amaro.stellarodyssey.rocket.RocketFlightSchedule;
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
 * <p>
 * Reads the synchronised flight phase from the entity so the model can animate the
 * launch sequence, and scales the rocket by the tier's {@code modelScale}.
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
        state.tierLevel = entity.getTierLevel();
        state.phase = entity.getPhase();
        state.tierScale = entity.getTier().map(RocketTier::modelScale).orElse(1.0F);
        state.phaseProgress = computePhaseProgress(entity, state.phase);
    }

    private static float computePhaseProgress(RocketEntity entity, RocketFlightPhase phase) {
        RocketFlightSchedule schedule = entity.getSchedule();
        int duration = schedule.durationOf(phase);
        if (duration <= 0) {
            return 0.0F;
        }
        float progress = (float) entity.getPhaseTicks() / (float) duration;
        return Math.clamp(progress, 0.0F, 1.0F);
    }

    @Override
    public void submit(RocketRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, 180.0F);
        float scale = state.tierScale > 0.0F ? state.tierScale : 1.0F;
        poseStack.scale(-scale, -scale, scale);
        poseStack.translate(0.0F, -1.0F, 0.0F);

        Identifier texture = StellarOdyssey.id("textures/entity/rocket_t" + state.tierLevel + ".png");
        collector.submitModel(this.model, state, poseStack, texture, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
