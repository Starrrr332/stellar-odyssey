package com.amaro.stellarodyssey.neoforge.client;

import com.amaro.stellarodyssey.StellarOdyssey;
import com.amaro.stellarodyssey.client.StellarOdysseyClient;
import com.amaro.stellarodyssey.client.camera.RocketCameraController;
import com.amaro.stellarodyssey.client.camera.RocketCameraShake;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Only constructed on the physical client - safe place for renderers, HUD and key bindings. */
@Mod(value = StellarOdyssey.MOD_ID, dist = Dist.CLIENT)
public final class StellarOdysseyNeoForgeClient {
    public StellarOdysseyNeoForgeClient() {
        StellarOdysseyClient.init();

        // Real camera shake + FOV pulse while riding the rocket (F2.3).
        NeoForge.EVENT_BUS.addListener(StellarOdysseyNeoForgeClient::onComputeCameraAngles);
        NeoForge.EVENT_BUS.addListener(StellarOdysseyNeoForgeClient::onComputeFov);
    }

    private static void onComputeCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        RocketCameraShake shake = RocketCameraController.currentShake(event.getPartialTick());
        if (shake.isZero()) {
            return;
        }
        event.setYaw(event.getYaw() + shake.yawDegrees());
        event.setPitch(event.getPitch() + shake.pitchDegrees());
        event.setRoll(event.getRoll() + shake.rollDegrees());
    }

    private static void onComputeFov(ViewportEvent.ComputeFov event) {
        RocketCameraShake shake = RocketCameraController.currentShake(event.getPartialTick());
        if (shake.isZero() || Math.abs(shake.fovScale() - 1.0F) < 1.0E-4F) {
            return;
        }
        event.setFOV(event.getFOV() * shake.fovScale());
    }
}