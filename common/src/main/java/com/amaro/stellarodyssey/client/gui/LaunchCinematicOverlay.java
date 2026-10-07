package com.amaro.stellarodyssey.client.gui;

import com.amaro.stellarodyssey.client.ClientRocketFlightHandler;
import com.amaro.stellarodyssey.client.renderer.WarpTunnelRenderer;
import com.amaro.stellarodyssey.entity.RocketEntity;
import com.amaro.stellarodyssey.rocket.RocketFlightPhase;
import com.amaro.stellarodyssey.rocket.RocketFlightSchedule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * High-fidelity cinematic launch overlay drawn while the local player rides an in-flight rocket.
 * <p>
 * Features:
 * <ul>
 *     <li>Comprehensive flight telemetry HUD (Altitude Y, velocity, Mach number, dynamic G-meter bar)</li>
 *     <li>Max-Q aerodynamic dynamic pressure buffet indicators</li>
 *     <li>Atmosphere-to-space transition visuals (Rayleigh thinning, planetary horizon curvature, emergent starfield)</li>
 *     <li>Hypersonic atmospheric exit ionization plasma glow</li>
 *     <li>3D radial relativistic hyperspace warp tunnel and shockwave visuals</li>
 * </ul>
 */
public final class LaunchCinematicOverlay {
    private static final int HUD_PANEL = 0xD0060A14;
    private static final int HUD_TRACK = 0xFF141F30;
    private static final int TITLE_TEXT = 0xFFE0F7FA;
    private static final int CYAN = 0xFF00E5FF;
    private static final int AMBER = 0xFFFFAB00;
    private static final int RED = 0xFFFF1744;
    private static final int GREEN = 0xFF00E676;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int ORANGE = 0xFFFF6D00;

    private LaunchCinematicOverlay() {
    }

    public static void render(GuiGraphicsExtractor gui, DeltaTracker deltaTracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gui.hud.isHidden() || mc.player.isSpectator()) {
            return;
        }
        if (!(mc.player.getVehicle() instanceof RocketEntity rocket)) {
            return;
        }

        RocketFlightPhase phase = rocket.getPhase();
        if (!phase.isInFlight()) {
            return;
        }

        // Update and cache physical telemetry
        ClientRocketFlightHandler.TelemetrySnapshot telemetry = ClientRocketFlightHandler.updateTelemetry(rocket);

        int w = gui.guiWidth();
        int h = gui.guiHeight();
        Font font = mc.font;
        int ticks = rocket.getPhaseTicks();
        float progress = phaseProgress(rocket, phase);

        // HUD-level shake coupled with dynamic pressure and G-force
        float baseAmp = shakeAmplitude(phase, progress);
        float coupledAmp = (float) (baseAmp * (1.0 + telemetry.dynamicPressure() * 0.45));
        int shakeX = Math.round((float) Math.sin(ticks * 1.9F) * coupledAmp);
        int shakeY = Math.round((float) Math.cos(ticks * 2.3F) * coupledAmp);

        switch (phase) {
            case COUNTDOWN -> drawCountdown(gui, font, w, h, ticks, rocket);
            case IGNITION -> drawIgnition(gui, font, w, h, progress);
            case ASCENT, ATMOSPHERE_EXIT -> drawAscent(gui, font, w, h, phase, ticks, progress, telemetry);
            case ORBIT -> drawOrbit(gui, font, w, h);
            case WARP_CHARGE -> WarpTunnelRenderer.renderWarpCharge(gui, font, w, h, progress);
            case WARP -> WarpTunnelRenderer.renderWarpTunnel(gui, font, w, h, ticks);
            case ARRIVAL -> {
                drawArrival(gui, font, w, h, progress);
                WarpTunnelRenderer.renderArrival(gui, font, w, h, progress);
            }
            case LANDING -> drawLanding(gui, font, w, h, progress);
            default -> {
            }
        }

        drawCrosshairAndPitchLadder(gui, font, w, h, mc.player, shakeX, shakeY);
        drawStatusReadout(gui, font, phase, progress, telemetry, shakeX, shakeY);
        drawResourceGauges(gui, font, w, h, telemetry, shakeX, shakeY);
    }

    // --- Phase effects --------------------------------------------------------------------

    private static void drawCountdown(GuiGraphicsExtractor gui, Font font, int w, int h,
                                      int ticks, RocketEntity rocket) {
        boolean pulse = (ticks / 10) % 2 == 0;
        int border = pulse ? 0xAAFF1744 : 0x66FF1744;
        frameBorder(gui, w, h, border, 3);

        int countdownTicks = rocket.getSchedule().durationOf(RocketFlightPhase.COUNTDOWN);
        int seconds = Math.max(0, (int) Math.ceil((countdownTicks - ticks) / 20.0));
        gui.centeredText(font, Component.literal("T-" + seconds), w / 2, h / 2 - 34,
                pulse ? RED : AMBER);
        gui.centeredText(font, Component.literal("LAUNCH SEQUENCE ENGAGED"), w / 2, h / 2 - 18, TITLE_TEXT);

        float progress = countdownTicks > 0
                ? Math.clamp((float) ticks / countdownTicks, 0.0F, 1.0F) : 1.0F;
        drawProgressBar(gui, w / 2 - 60, h / 2 + 2, 120, 4, progress, RED);
    }

    private static void drawIgnition(GuiGraphicsExtractor gui, Font font, int w, int h, float progress) {
        int alpha = Math.round(170.0F * (1.0F - progress));
        if (alpha > 0) {
            gui.fill(0, 0, w, h, (alpha << 24) | 0x00FF8A2A);
        }
        frameBorder(gui, w, h, 0x88FFAB00, 3);
        gui.centeredText(font, Component.literal("IGNITION"), w / 2, h / 2 - 20, progress < 0.5F ? WHITE : AMBER);
        gui.centeredText(font, Component.literal("ENGINES SPOOLING UP // THRUST STABILIZING"), w / 2, h / 2 - 4, TITLE_TEXT);
    }

    private static void drawAscent(GuiGraphicsExtractor gui, Font font, int w, int h,
                                   RocketFlightPhase phase, int ticks, float progress,
                                   ClientRocketFlightHandler.TelemetrySnapshot telemetry) {
        double altitude = telemetry.altitude();

        // 1. Multi-layer Rayleigh thinning gradient (atmosphere-to-space transition)
        if (phase == RocketFlightPhase.ATMOSPHERE_EXIT || altitude > 75.0) {
            float exitFactor = (float) Math.clamp((altitude - 75.0) / 225.0, 0.0, 1.0);
            int skyAlpha = (int) (160.0F * (1.0F - exitFactor));
            if (skyAlpha > 0) {
                gui.fill(0, 0, w, h, (skyAlpha << 24) | 0x000D47A1);
            }
            int spaceAlpha = (int) (220.0F * exitFactor);
            if (spaceAlpha > 0) {
                gui.fill(0, 0, w, h, (spaceAlpha << 24) | 0x00010206);
            }

            // Emergent stars visible through the thinning atmosphere
            if (exitFactor > 0.35F) {
                int starAlpha = (int) ((exitFactor - 0.35F) / 0.65F * 200.0F);
                for (int s = 0; s < 24; s++) {
                    int sx = (int) (((s * 73L + 19) % w));
                    int sy = (int) (((s * 41L + 11) % (h / 2)));
                    gui.fill(sx, sy, sx + 1, sy + 1, (starAlpha << 24) | 0x00FFFFFF);
                }
            }

            // 2. Curvature of planetary limb (horizon curvature bowing downward at high altitude)
            if (altitude > 130.0) {
                float curveProg = (float) Math.clamp((altitude - 130.0) / 170.0, 0.0, 1.0);
                int horizonY = (int) (h * 0.76F + (curveProg * h * 0.12F));
                int horizonR = (int) (w * 0.95F);
                // Lower planetary darkness
                gui.fill(0, horizonY + 8, w, h, 0xDD08111A);
                // Luminous atmospheric limb rim arc
                gui.outline(w / 2 - horizonR, horizonY - horizonR / 4, horizonR * 2, horizonR / 2, 0x8000E5FF);
                gui.outline(w / 2 - horizonR, horizonY - horizonR / 4 + 1, horizonR * 2, horizonR / 2, 0x4080DEEA);
            }
        }

        // 3. Hypersonic Atmospheric Exit Ionization Plasma Glow
        if (telemetry.ionization() > 0.05) {
            int ionAlpha = (int) (telemetry.ionization() * 180.0);
            int flicker = (ticks % 3 == 0) ? 18 : -10;
            int flameAlpha = Math.clamp(ionAlpha + flicker, 0, 255);
            // Fiery plasma shockwave on top and sides
            gui.fillGradient(0, 0, w, h / 4, (flameAlpha << 24) | 0x00FF6D00, 0x00000000);
            gui.fillGradient(0, 0, w / 7, h, (flameAlpha / 2 << 24) | 0x0000E5FF, 0x00000000);
            gui.fillGradient(w - w / 7, 0, w, h, 0x00000000, (flameAlpha / 2 << 24) | 0x0000E5FF);
        }

        // 4. Radial speed streaks rushing outward
        int streakColor = 0x33E0F7FA;
        int count = 30;
        for (int i = 0; i < count; i++) {
            int x = (int) ((i + 0.5F) * w / count);
            int grow = (int) (h * 0.20F * (0.4F + progress) * (0.5F + 0.5F * ((i * 37) % 100) / 100.0F));
            int offset = (int) ((ticks * (6L + (i % 5) * 3L)) % Math.max(1, h / 2));
            gui.fill(x, offset, x + 2, offset + grow, streakColor);
            gui.fill(x, h - offset - grow, x + 2, h - offset, streakColor);
        }

        closeVignette(gui, w, h, 0x66000A14);
        frameBorder(gui, w, h, 0x6600E5FF, 2);

        String title = phase == RocketFlightPhase.ATMOSPHERE_EXIT
                ? "LEAVING ATMOSPHERE // EXOSPHERE TRANSITION"
                : "POWERED ASCENT // MAX THRUST VECTOR";
        gui.centeredText(font, Component.literal(title), w / 2, h / 2 - 24, CYAN);
    }

    private static void drawOrbit(GuiGraphicsExtractor gui, Font font, int w, int h) {
        frameBorder(gui, w, h, 0xAA00E5FF, 2);
        gui.centeredText(font, Component.literal("ORBITAL INSERTION COMPLETE"), w / 2, h / 2 - 16, CYAN);
        gui.centeredText(font, Component.literal("HOLDING ZERO-G STATION"), w / 2, h / 2, TITLE_TEXT);
    }

    private static void drawArrival(GuiGraphicsExtractor gui, Font font, int w, int h, float progress) {
        closeVignette(gui, w, h, 0x77000812);
        frameBorder(gui, w, h, 0x6600E5FF, 2);
        gui.centeredText(font, Component.literal("ENTERING DESTINATION SPACE"), w / 2, h / 2 - 16, CYAN);
        drawProgressBar(gui, w / 2 - 60, h / 2 + 4, 120, 4, progress, CYAN);
    }

    private static void drawLanding(GuiGraphicsExtractor gui, Font font, int w, int h, float progress) {
        closeVignette(gui, w, h, 0x88000000);
        gui.centeredText(font, Component.literal("TOUCHDOWN"), w / 2, h / 2 - 16, AMBER);
        gui.centeredText(font, Component.literal("DESCENT SETTLE // THRUST SHUTDOWN"), w / 2, h / 2, TITLE_TEXT);
    }

    // --- Status readout & Telemetry HUD ----------------------------------------------------

    private static void drawStatusReadout(GuiGraphicsExtractor gui, Font font, RocketFlightPhase phase,
                                          float progress, ClientRocketFlightHandler.TelemetrySnapshot telemetry,
                                          int shakeX, int shakeY) {
        int accent = accentFor(phase);
        int x = 12 + shakeX;
        int y = 52 + shakeY;
        int width = 184;
        int height = 26;

        gui.fill(x, y, x + width, y + height, HUD_PANEL);
        gui.fill(x, y, x + 2, y + height, accent);
        for (int i = 0; i < width; i += 4) {
            gui.fill(x + i, y - 6, x + i + 2, y - 4, 0x6600E5FF);
        }
        gui.text(font, Component.literal("▲ " + phaseLabel(phase)), x + 6, y + 3, TITLE_TEXT);
        drawProgressBar(gui, x + 6, y + 15, width - 12, 5, progress, accent);

        // Comprehensive flight telemetry HUD panel below phase indicator
        drawTelemetryPanel(gui, font, x, y + height + 3, telemetry);
    }

    // --- Advanced HUD Elements -------------------------------------------------------------

    private static void drawCrosshairAndPitchLadder(GuiGraphicsExtractor gui, Font font, int w, int h, net.minecraft.world.entity.player.Player player, int shakeX, int shakeY) {
        int cx = w / 2 + shakeX;
        int cy = h / 2 + shakeY;
        
        gui.fill(cx - 30, cy - 20, cx - 20, cy - 18, CYAN);
        gui.fill(cx - 30, cy - 20, cx - 28, cy - 10, CYAN);
        gui.fill(cx + 20, cy - 20, cx + 30, cy - 18, CYAN);
        gui.fill(cx + 28, cy - 20, cx + 30, cy - 10, CYAN);
        gui.fill(cx - 30, cy + 18, cx - 20, cy + 20, CYAN);
        gui.fill(cx - 30, cy + 10, cx - 28, cy + 20, CYAN);
        gui.fill(cx + 20, cy + 18, cx + 30, cy + 20, CYAN);
        gui.fill(cx + 28, cy + 10, cx + 30, cy + 20, CYAN);
        gui.fill(cx - 1, cy - 1, cx + 1, cy + 1, GREEN);
        
        float pitch = player.getXRot();
        int pitchOffset = (int) (pitch * 2.0F); 
        for (int i = -9; i <= 9; i++) {
            int deg = i * 10;
            if (deg == 0) continue;
            int yPos = cy - pitchOffset + (deg * 2);
            if (Math.abs(yPos - cy) < 100) {
                int lineW = (deg % 30 == 0) ? 60 : 30;
                int alpha = (int) (255 * (1.0F - (Math.abs(yPos - cy) / 100.0F)));
                int col = (alpha << 24) | 0x00E0F7FA;
                gui.fill(cx - lineW, yPos, cx - 20, yPos + 1, col);
                gui.fill(cx + 20, yPos, cx + lineW, yPos + 1, col);
                if (deg % 30 == 0) {
                    gui.text(font, Component.literal(String.valueOf(-deg)), cx - lineW - 16, yPos - 3, col);
                    gui.text(font, Component.literal(String.valueOf(-deg)), cx + lineW + 4, yPos - 3, col);
                }
            }
        }
    }

    private static void drawResourceGauges(GuiGraphicsExtractor gui, Font font, int w, int h, ClientRocketFlightHandler.TelemetrySnapshot telemetry, int shakeX, int shakeY) {
        int x = w - 196 + shakeX;
        int y = h - 52 + shakeY;
        gui.fill(x, y, x + 184, y + 40, HUD_PANEL);
        gui.fill(x + 182, y, x + 184, y + 40, AMBER);
        
        double fuelPerc = 1.0;
        if (telemetry.phase() == RocketFlightPhase.ASCENT || telemetry.phase() == RocketFlightPhase.ATMOSPHERE_EXIT) {
            fuelPerc = Math.max(0.1, 1.0 - (telemetry.altitude() / 300.0));
        } else if (telemetry.phase() == RocketFlightPhase.LANDING) {
            fuelPerc = 0.05;
        } else if (telemetry.phase() != RocketFlightPhase.IDLE && telemetry.phase() != RocketFlightPhase.COUNTDOWN) {
            fuelPerc = 0.1;
        }
        
        gui.text(font, Component.literal("PROPELLANT"), x + 6, y + 6, TITLE_TEXT);
        drawProgressBar(gui, x + 70, y + 8, 100, 4, (float) fuelPerc, fuelPerc > 0.25 ? CYAN : RED);
        
        gui.text(font, Component.literal("HULL INTGRTY"), x + 6, y + 20, TITLE_TEXT);
        double hullPerc = 1.0;
        if (telemetry.dynamicPressure() > 0.4) hullPerc = 0.95 - (telemetry.dynamicPressure() * 0.1);
        if (telemetry.ionization() > 0.3) hullPerc -= telemetry.ionization() * 0.15;
        drawProgressBar(gui, x + 70, y + 22, 100, 4, (float) Math.max(0.0, hullPerc), hullPerc > 0.5 ? GREEN : RED);
        
        for (int i = 0; i < 184; i += 4) {
            gui.fill(x + i, y - 6, x + i + 2, y - 4, 0x6600E5FF);
        }
    }

    /**
     * Draws the comprehensive flight telemetry instrument panel:
     * <ul>
     *   <li><b>ALT</b> — Altitude in meters or kilometers</li>
     *   <li><b>VEL</b> — Total velocity vector magnitude in m/s</li>
     *   <li><b>MACH</b> — Mach number and speed regime (SUBSONIC / TRANSONIC / SUPERSONIC / HYPERSONIC)</li>
     *   <li><b>G-FORCE</b> — Dynamic felt acceleration with segmented color G-meter bar</li>
     *   <li><b>MAX-Q / IONIZATION</b> — Real-time dynamic pressure and plasma shield badges</li>
     * </ul>
     */
    private static void drawTelemetryPanel(GuiGraphicsExtractor gui, Font font, int panelX, int panelY,
                                           ClientRocketFlightHandler.TelemetrySnapshot telemetry) {
        int x = panelX;
        int y = panelY;
        int width = 184;
        int height = 62;

        gui.fill(x, y, x + width, y + height, HUD_PANEL);
        gui.fill(x, y, x + 2, y + height, CYAN);

        // Line 1: Altitude
        String altText = "ALT  " + RocketTelemetry.formatAltitude(telemetry.altitude());
        gui.text(font, Component.literal(altText), x + 6, y + 3, TITLE_TEXT);

        // Line 2: Velocity
        String velText = "VEL  " + RocketTelemetry.formatVelocity(telemetry.totalSpeed());
        gui.text(font, Component.literal(velText), x + 6, y + 13, CYAN);

        // Line 3: Mach Number
        String machText = RocketTelemetry.formatMach(telemetry.mach());
        gui.text(font, Component.literal(machText), x + 6, y + 23, TITLE_TEXT);

        // Line 4: G-Force and visual segmented bar
        double gForce = telemetry.gForce();
        int gColor = (gForce >= 4.5) ? RED : (gForce >= 3.5 ? AMBER : (gForce >= 2.0 ? TITLE_TEXT : GREEN));
        gui.text(font, Component.literal(String.format("G    %+5.2f g", gForce)), x + 6, y + 33, gColor);
        drawGMeterBar(gui, x + 84, y + 35, 92, 5, gForce);

        // Line 5: Dynamic badges (Max-Q buffet warning or Ionization shield)
        if (telemetry.dynamicPressure() > 0.35) {
            String qText = String.format("⚠ MAX-Q BUFFET: %2.0f%%", telemetry.dynamicPressure() * 100.0);
            gui.text(font, Component.literal(qText), x + 6, y + 46, AMBER);
        } else if (telemetry.ionization() > 0.1) {
            String ionText = String.format("● PLASMA SHIELD: %2.0f%%", telemetry.ionization() * 100.0);
            gui.text(font, Component.literal(ionText), x + 6, y + 46, ORANGE);
        } else {
            gui.text(font, Component.literal("● TELEMETRY: NOMINAL"), x + 6, y + 46, GREEN);
        }
    }

    /**
     * Draws a segmented G-meter bar representing G-load from 0G to 6G.
     */
    private static void drawGMeterBar(GuiGraphicsExtractor gui, int x, int y, int w, int h, double gForce) {
        gui.fill(x, y, x + w, y + h, HUD_TRACK);
        int segments = 10;
        int segW = (w - (segments - 1)) / segments;
        int activeSegments = (int) Math.clamp(Math.round((gForce / 5.5) * segments), 0, segments);

        for (int i = 0; i < segments; i++) {
            int segX = x + i * (segW + 1);
            if (i < activeSegments) {
                int col = (i >= 8) ? RED : (i >= 6 ? AMBER : (i >= 3 ? CYAN : GREEN));
                gui.fill(segX, y, segX + segW, y + h, col);
            } else {
                gui.fill(segX, y, segX + segW, y + h, 0x33FFFFFF);
            }
        }
    }

    private static String phaseLabel(RocketFlightPhase phase) {
        return switch (phase) {
            case COUNTDOWN -> "COUNTDOWN";
            case IGNITION -> "IGNITION";
            case ASCENT -> "ASCENT";
            case ATMOSPHERE_EXIT -> "ATMOSPHERE EXIT";
            case ORBIT -> "ORBIT";
            case WARP_CHARGE -> "WARP CHARGE";
            case WARP -> "WARP JUMP";
            case ARRIVAL -> "ARRIVAL";
            case LANDING -> "LANDING";
            case IDLE -> "STANDBY";
        };
    }

    private static int accentFor(RocketFlightPhase phase) {
        return switch (phase) {
            case COUNTDOWN, IGNITION, LANDING -> AMBER;
            case WARP_CHARGE, WARP -> 0xFFDB72FF;
            default -> CYAN;
        };
    }

    // --- Helpers --------------------------------------------------------------------------

    private static float phaseProgress(RocketEntity rocket, RocketFlightPhase phase) {
        RocketFlightSchedule schedule = rocket.getSchedule();
        int duration = schedule.durationOf(phase);
        if (duration <= 0) {
            return 1.0F;
        }
        return Math.clamp((float) rocket.getPhaseTicks() / (float) duration, 0.0F, 1.0F);
    }

    private static float shakeAmplitude(RocketFlightPhase phase, float progress) {
        return switch (phase) {
            case IGNITION -> 2.0F;
            case ASCENT -> 3.5F;
            case ATMOSPHERE_EXIT -> 3.0F;
            case WARP_CHARGE -> 2.5F;
            case ARRIVAL -> 3.0F;
            case LANDING -> 2.0F * (1.0F - progress);
            default -> 0.0F;
        };
    }

    private static void drawProgressBar(GuiGraphicsExtractor gui, int x, int y, int w, int h,
                                        float fraction, int fillColor) {
        gui.fill(x, y, x + w, y + h, HUD_TRACK);
        int filled = Math.round(w * Math.clamp(fraction, 0.0F, 1.0F));
        if (filled > 0) {
            gui.fill(x, y, x + filled, y + h, fillColor);
            gui.fill(x, y, x + filled, y + 1, 0x80FFFFFF);
        }
    }

    private static void frameBorder(GuiGraphicsExtractor gui, int w, int h, int color, int thickness) {
        gui.fill(0, 0, w, thickness, color);
        gui.fill(0, h - thickness, w, h, color);
        gui.fill(0, 0, thickness, h, color);
        gui.fill(w - thickness, 0, w, h, color);
    }

    private static void closeVignette(GuiGraphicsExtractor gui, int w, int h, int edgeColor) {
        gui.fillGradient(0, 0, w, h / 5, edgeColor, 0x00000000);
        gui.fillGradient(0, h - h / 5, w, h, 0x00000000, edgeColor);
    }
}
