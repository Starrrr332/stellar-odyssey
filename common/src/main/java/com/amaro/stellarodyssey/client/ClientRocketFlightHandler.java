package com.amaro.stellarodyssey.client;

import com.amaro.stellarodyssey.client.gui.RocketTelemetry;
import com.amaro.stellarodyssey.entity.RocketEntity;
import com.amaro.stellarodyssey.rocket.RocketFlightPhase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Client-side handler and telemetry provider for rocket flight state.
 * <p>
 * Decouples client-only classes (Minecraft, ClientLevel) from common networking registration
 * and maintains real-time physical telemetry (altitude, velocity, Mach, dynamic G-acceleration,
 * dynamic pressure Max-Q, and atmospheric ionization).
 */
public final class ClientRocketFlightHandler {

    /**
     * Immutable snapshot of current rocket flight telemetry.
     */
    public record TelemetrySnapshot(
            double altitude,
            double verticalSpeed,
            double totalSpeed,
            double mach,
            double gForce,
            double dynamicPressure,
            double ionization,
            RocketFlightPhase phase,
            int phaseTicks
    ) {
        public static final TelemetrySnapshot NOMINAL = new TelemetrySnapshot(
                64.0, 0.0, 0.0, 0.0, 1.0, 0.0, 0.0, RocketFlightPhase.IDLE, 0
        );
    }

    private static volatile TelemetrySnapshot latestTelemetry = TelemetrySnapshot.NOMINAL;
    private static double lastVerticalSpeed = 0.0;
    private static long lastSampleTimeMs = 0L;

    private ClientRocketFlightHandler() {
    }

    /**
     * Updates client flight state upon receiving server-to-client phase broadcast.
     */
    public static void handleFlightPhase(int entityId, RocketFlightPhase phase, int phaseTicks) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != null) {
            Entity entity = level.getEntity(entityId);
            if (entity instanceof RocketEntity rocket) {
                rocket.setPhase(phase);
                rocket.setPhaseTicks(phaseTicks);
                updateTelemetry(rocket);
            }
        }
    }

    /**
     * Samples real-time physical motion from the ridden rocket entity and computes telemetry.
     */
    public static TelemetrySnapshot updateTelemetry(RocketEntity rocket) {
        if (rocket == null) {
            latestTelemetry = TelemetrySnapshot.NOMINAL;
            return latestTelemetry;
        }

        double alt = rocket.getY();
        Vec3 delta = rocket.getDeltaMovement();
        double vy = delta.y * 20.0; // blocks per second
        double totalSpeed = delta.length() * 20.0;
        double mach = RocketTelemetry.computeMach(totalSpeed);
        double dynPressure = RocketTelemetry.computeDynamicPressure(alt, totalSpeed);

        long now = System.currentTimeMillis();
        double dt = (lastSampleTimeMs > 0 && now > lastSampleTimeMs) ? (now - lastSampleTimeMs) / 1000.0 : 0.05;
        dt = Math.clamp(dt, 0.01, 0.2);

        double dynamicG = RocketTelemetry.computeDynamicGForce(lastVerticalSpeed, vy, dt);
        // Fallback or blend with phase thrust model for ultra-smooth HUD display
        double nominalG = RocketTelemetry.computeGForce(rocket.getPhase());
        double blendedG = (dynamicG * 0.7) + (nominalG * 0.3);

        double ionization = RocketTelemetry.computeIonizationIntensity(alt, totalSpeed);

        lastVerticalSpeed = vy;
        lastSampleTimeMs = now;

        latestTelemetry = new TelemetrySnapshot(
                alt, vy, totalSpeed, mach, blendedG, dynPressure, ionization,
                rocket.getPhase(), rocket.getPhaseTicks()
        );
        return latestTelemetry;
    }

    /**
     * Returns the latest flight telemetry snapshot.
     */
    public static TelemetrySnapshot currentTelemetry() {
        return latestTelemetry;
    }

    /**
     * Resets telemetry cache on touchdown or exit.
     */
    public static void reset() {
        latestTelemetry = TelemetrySnapshot.NOMINAL;
        lastVerticalSpeed = 0.0;
        lastSampleTimeMs = 0L;
    }

    /**
     * Ticks client-side effects like plasma friction particles based on flight telemetry.
     */
    public static void tickClient(Minecraft mc) {
        if (mc.isPaused() || mc.player == null || mc.level == null) return;
        if (!(mc.player.getVehicle() instanceof RocketEntity rocket)) return;
        
        TelemetrySnapshot telemetry = currentTelemetry();
        if (telemetry == null || !rocket.getPhase().isInFlight()) return;

        double ionization = telemetry.ionization();
        double pressure = telemetry.dynamicPressure();
        
        if (ionization > 0.05 || pressure > 0.1) {
            int particles = (int) (Math.random() * (ionization * 30 + pressure * 10));
            for (int i = 0; i < particles; i++) {
                double rx = mc.player.getX() + (Math.random() - 0.5) * 5.0;
                double ry = mc.player.getY() + 1.0 + (Math.random() * 2.0);
                double rz = mc.player.getZ() + (Math.random() - 0.5) * 5.0;
                
                double vx = (Math.random() - 0.5) * 0.2;
                double vy = - (telemetry.verticalSpeed() / 20.0) - (Math.random() * 2.0);
                double vz = (Math.random() - 0.5) * 0.2;

                if (ionization > 0.1 && Math.random() > 0.5) {
                    mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.FLAME, rx, ry, rz, vx, vy, vz);
                    if (Math.random() > 0.8) {
                        mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.LAVA, rx, ry, rz, vx, vy, vz);
                    }
                } else if (pressure > 0.1) {
                    mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, rx, ry, rz, vx, vy, vz);
                }
            }
        }
    }
}
