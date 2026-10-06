package com.amaro.stellarodyssey.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

/**
 * High-performance emissive model layer renderer for Minecraft 26.3.
 * <p>
 * Employs {@link SubmitNodeCollector} to submit full-bright glowing model passes
 * that render without ambient lighting or planetary night/cave darkness attenuation.
 */
public final class EmissiveModelLayer {
    /**
     * Full-bright light coordinate constant representing max block (15) and max sky (15) light levels (0x00F000F0).
     */
    public static final int FULL_BRIGHT = LightCoordsUtil.FULL_BRIGHT; // 0x00F000F0

    private EmissiveModelLayer() {
    }

    /**
     * Submits an emissive model overlay pass with full-bright lighting coordinates (0x00F000F0).
     *
     * @param model           The entity model to render.
     * @param state           The entity render state.
     * @param poseStack       Current matrix transformations.
     * @param collector       The MC 26.3 node collector.
     * @param emissiveTexture Identifier of the emissive overlay texture.
     */
    public static <S extends EntityRenderState, M extends EntityModel<S>> void submitEmissive(
            M model, S state, PoseStack poseStack, SubmitNodeCollector collector, Identifier emissiveTexture) {
        if (model == null || state == null || collector == null || emissiveTexture == null) {
            return;
        }
        collector.submitModel(model, state, poseStack, emissiveTexture, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, 0);
    }

    /**
     * Submits an emissive model overlay pass with custom light coordinates and overlay tint.
     *
     * @param model           The entity model to render.
     * @param state           The entity render state.
     * @param poseStack       Current matrix transformations.
     * @param collector       The MC 26.3 node collector.
     * @param emissiveTexture Identifier of the emissive overlay texture.
     * @param lightCoords     Lighting coordinates (e.g. FULL_BRIGHT or customized glowing level).
     * @param overlayCoords   Overlay texture coordinates (e.g. OverlayTexture.NO_OVERLAY).
     */
    public static <S extends EntityRenderState, M extends EntityModel<S>> void submitEmissive(
            M model, S state, PoseStack poseStack, SubmitNodeCollector collector, Identifier emissiveTexture, int lightCoords, int overlayCoords) {
        if (model == null || state == null || collector == null || emissiveTexture == null) {
            return;
        }
        collector.submitModel(model, state, poseStack, emissiveTexture, lightCoords, overlayCoords, 0);
    }
}
