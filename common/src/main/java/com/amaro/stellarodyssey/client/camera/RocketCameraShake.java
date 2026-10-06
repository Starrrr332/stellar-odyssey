package com.amaro.stellarodyssey.client.camera;

/**
 * Immutable per-frame camera amplification for a ridden rocket launch sequence.
 *
 * <p>All values are expressed in degrees (yaw / pitch / roll) plus a unitless
 * FOV scale factor (&gt; 1 means the view "zooms out" slightly). A zero shake
 * carries an FOV scale of exactly {@code 1.0F}.
 */
public record RocketCameraShake(float yawDegrees, float pitchDegrees, float rollDegrees, float fovScale) {

    /** The identity shake: no rotation, no zoom. */
    public static final RocketCameraShake NONE = new RocketCameraShake(0.0F, 0.0F, 0.0F, 1.0F);

    /** Whether this shake would visually change nothing. */
    public boolean isZero() {
        return this.yawDegrees == 0.0F
                && this.pitchDegrees == 0.0F
                && this.rollDegrees == 0.0F
                && this.fovScale == 1.0F;
    }
}