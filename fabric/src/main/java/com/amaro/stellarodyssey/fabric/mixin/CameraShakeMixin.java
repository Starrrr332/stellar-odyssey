package com.amaro.stellarodyssey.fabric.mixin;

import com.amaro.stellarodyssey.client.camera.RocketCameraController;
import com.amaro.stellarodyssey.client.camera.RocketCameraShake;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric camera hook. MC 26.3's Fabric API exposes no camera-setup event, so a
 * small mixin is required to move the world view while riding the rocket.
 *
 * <p><b>Rotations</b> are applied at the tail of {@link Camera#update(DeltaTracker)}:
 * the yaw/pitch fields are nudged and the orientation quaternion is rebuilt exactly
 * like vanilla {@code Camera.setRotation} does, then rolled by the shake. Because
 * vanilla extracts {@code CameraRenderState} from the camera later in the same frame
 * (via {@code Camera.extractRenderState}), the shake is picked up by the renderer.
 *
 * <p><b>FOV</b> is a multiplicative pulse on {@link Camera#getFov()}, which the
 * renderer re-reads every frame when it builds the world projection.
 */
@Mixin(Camera.class)
public abstract class CameraShakeMixin {

    private static final float DEG_TO_RAD = (float) (Math.PI / 180.0);

    @Shadow
    private float xRot;

    @Shadow
    private float yRot;

    @Shadow
    @Final
    private Quaternionf rotation;

    @Shadow
    public abstract float getCameraEntityPartialTicks(DeltaTracker deltaTracker);

    @Inject(method = "update", at = @At("TAIL"))
    private void stellarodyssey$applyRocketCameraShake(DeltaTracker deltaTracker, CallbackInfo ci) {
        RocketCameraShake shake = RocketCameraController.currentShake(this.getCameraEntityPartialTicks(deltaTracker));
        if (shake.isZero()) {
            return;
        }

        this.yRot += shake.yawDegrees();
        this.xRot += shake.pitchDegrees();

        // Mirror vanilla Camera#setRotation: rotationYXZ(PI - yaw*deg, -pitch*deg, 0).
        this.rotation.rotationYXZ(
                (float) Math.PI - this.yRot * DEG_TO_RAD,
                -this.xRot * DEG_TO_RAD,
                0.0F);
        if (shake.rollDegrees() != 0.0F) {
            this.rotation.rotateLocalZ(shake.rollDegrees() * DEG_TO_RAD);
        }
    }

    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void stellarodyssey$applyRocketFovPulse(CallbackInfoReturnable<Float> cir) {
        RocketCameraShake shake = RocketCameraController.currentShake(0.0F);
        if (shake.isZero() || Math.abs(shake.fovScale() - 1.0F) < 1.0E-4F) {
            return;
        }
        cir.setReturnValue(cir.getReturnValue() * shake.fovScale());
    }
}