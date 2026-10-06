package com.amaro.stellarodyssey.client.renderer;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.client.model.RocketModel;
import com.amaro.stellarodyssey.client.model.RocketTier2Model;
import com.amaro.stellarodyssey.client.model.RocketTier3Model;
import com.amaro.stellarodyssey.client.renderer.layer.EmissiveModelLayer;
import com.amaro.stellarodyssey.client.renderer.state.RocketRenderState;
import com.amaro.stellarodyssey.entity.RocketEntity;
import com.amaro.stellarodyssey.registry.tiers.RocketTier;
import com.amaro.stellarodyssey.rocket.RocketFlightPhase;
import com.amaro.stellarodyssey.rocket.RocketFlightSchedule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Modern entity renderer for the tiered rocket using MC 26.3 SubmitNodeCollector.
 * <p>
 * Bakes and switches between RocketModel (T1), RocketTier2Model (T2), and RocketTier3Model (T3).
 * Scales the model according to the tier's {@code modelScale}, and submits a full-bright
 * emissive overlay pass for Tier 2 and Tier 3.
 */
public class RocketEntityRenderer extends EntityRenderer<RocketEntity, RocketRenderState> {
    private final RocketModel modelT1;
    private final RocketTier2Model modelT2;
    private final RocketTier3Model modelT3;

    public RocketEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.modelT1 = new RocketModel(context.bakeLayer(RocketModel.LAYER_LOCATION));
        this.modelT2 = new RocketTier2Model(context.bakeLayer(RocketTier2Model.LAYER_LOCATION));
        this.modelT3 = new RocketTier3Model(context.bakeLayer(RocketTier3Model.LAYER_LOCATION));
        this.shadowRadius = 0.8F;
    }

    public RocketModel getModelT1() {
        return this.modelT1;
    }

    public RocketTier2Model getModelT2() {
        return this.modelT2;
    }

    public RocketTier3Model getModelT3() {
        return this.modelT3;
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

        EntityModel<RocketRenderState> activeModel = switch (state.tierLevel) {
            case 2 -> this.modelT2;
            case 3 -> this.modelT3;
            default -> this.modelT1;
        };

        // 1. Diffuse Base Pass
        Identifier diffuseTexture = StellarOdyssey.id("textures/entity/rocket_t" + state.tierLevel + ".png");
        collector.submitModel(activeModel, state, poseStack, diffuseTexture, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);

        // 2. Full-Bright Emissive Overlay Pass for Tier 2 and Tier 3
        if (state.tierLevel >= 2) {
            Identifier emissiveTexture = StellarOdyssey.id("textures/entity/rocket_t" + state.tierLevel + "_emissive.png");
            EmissiveModelLayer.submitEmissive(activeModel, state, poseStack, collector, emissiveTexture);
        }

        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
