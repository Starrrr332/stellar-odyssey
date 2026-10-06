package com.amaro.stellarodyssey.client.gui;

import com.amaro.stellarodyssey.entity.RocketEntity;
import com.amaro.stellarodyssey.rocket.RocketFlightPhase;
import com.amaro.stellarodyssey.rocket.RocketFlightSchedule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

/**
 * Cinematic launch overlay, drawn while the local player rides a rocket through an active
 * {@link RocketFlightPhase} sequence.
 * <p>
 * The phase and its elapsed ticks are synchronised by the entity, so this overlay mirrors the
 * server-authoritative state machine without extra packets: countdown frame, ignition flash,
 * ascent speed streaks, orbital insertion, hyperdrive charge, warp flash and touchdown.
 * <p>
 * <b>Camera shake:</b> actually moving the world view requires a loader-specific camera hook
 * that Architectury does not expose. This overlay therefore shakes its own HUD layer and draws
 * jittering screen streaks to suggest the vibration without touching the camera.
 */
public final class LaunchCinematicOverlay {
    private static final int HUD_PANEL = 0xC0060A14;
    private static final int HUD_TRACK = 0xFF141F30;
    private static final int TITLE_TEXT = 0xFFE0F7FA;
    private static final int CYAN = 0xFF00E5FF;
    private static final int AMBER = 0xFFFFAB00;
    private static final int RED = 0xFFFF1744;
    private static final int WHITE = 0xFFFFFFFF;

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

        int w = gui.guiWidth();
        int h = gui.guiHeight();
        Font font = mc.font;
        int ticks = rocket.getPhaseTicks();
        float progress = phaseProgress(rocket, phase);

        // HUD-level shake: only this overlay vibrates, the camera stays put.
        float amplitude = shakeAmplitude(phase, progress);
        int shakeX = Math.round((float) Math.sin(ticks * 1.9F) * amplitude);
        int shakeY = Math.round((float) Math.cos(ticks * 2.3F) * amplitude);

        switch (phase) {
            case COUNTDOWN -> drawCountdown(gui, font, w, h, ticks, rocket);
            case IGNITION -> drawIgnition(gui, font, w, h, progress);
            case ASCENT, ATMOSPHERE_EXIT -> drawAscent(gui, font, w, h, phase, ticks, progress);
            case ORBIT -> drawOrbit(gui, font, w, h);
            case WARP_CHARGE -> drawWarpCharge(gui, font, w, h, progress);
            case WARP -> drawWarpTunnel(gui, w, h, ticks);
            case ARRIVAL -> drawArrival(gui, font, w, h, progress);
            case LANDING -> drawLanding(gui, font, w, h, progress);
            default -> {
            }
        }

        drawStatusReadout(gui, font, phase, progress, shakeX, shakeY);
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
        gui.centeredText(font, Component.literal("ENGINES SPOOLING UP"), w / 2, h / 2 - 4, TITLE_TEXT);
    }

    private static void drawAscent(GuiGraphicsExtractor gui, Font font, int w, int h,
                                   RocketFlightPhase phase, int ticks, float progress) {
        if (phase == RocketFlightPhase.ATMOSPHERE_EXIT) {
            // Atmospheric haze lerps from sky blue toward deep space as the rocket crosses the boundary.
            int skyBlue = (int) (120 * (1F - progress));
            int spaceBlack = (int) (180 * progress);
            int skyColor = (Math.min(255, spaceBlack) << 24) | (skyBlue << 16) | (skyBlue << 8);
            gui.fill(0, 0, w, h, skyColor);
        }
        // Radial speed streaks rushing outward, longest toward mid-screen.
        int streakColor = 0x33E0F7FA;
        int count = 28;
        for (int i = 0; i < count; i++) {
            int x = (int) ((i + 0.5F) * w / count);
            int grow = (int) (h * 0.18F * (0.4F + progress) * (0.5F + 0.5F * ((i * 37) % 100) / 100.0F));
            int offset = (int) ((ticks * (6L + (i % 5) * 3L)) % Math.max(1, h / 2));
            gui.fill(x, offset, x + 2, offset + grow, streakColor);
            gui.fill(x, h - offset - grow, x + 2, h - offset, streakColor);
        }

        closeVignette(gui, w, h, 0x66000A14);
        frameBorder(gui, w, h, 0x6600E5FF, 2);

        String title = phase == RocketFlightPhase.ATMOSPHERE_EXIT
                ? "LEAVING ATMOSPHERE"
                : "POWERED ASCENT";
        gui.centeredText(font, Component.literal(title), w / 2, h / 2 - 24, CYAN);
    }

    private static void drawOrbit(GuiGraphicsExtractor gui, Font font, int w, int h) {
        frameBorder(gui, w, h, 0xAA00E5FF, 2);
        gui.centeredText(font, Component.literal("ORBITAL INSERTION COMPLETE"), w / 2, h / 2 - 16, CYAN);
        gui.centeredText(font, Component.literal("HOLDING STATION"), w / 2, h / 2, TITLE_TEXT);
    }

    private static void drawWarpCharge(GuiGraphicsExtractor gui, Font font, int w, int h, float progress) {
        int alpha = Math.round(200.0F * progress);
        if (alpha > 0) {
            gui.fillGradient(0, 0, w, h, (alpha << 24) | 0x0000E5FF, (alpha << 24) | 0x007C4DFF);
        }
        // Collapsing bright ring around the reticle.
        int radius = Math.round(40.0F + 160.0F * (1.0F - progress));
        int cx = w / 2;
        int cy = h / 2;
        gui.outline(cx - radius, cy - radius, radius * 2, radius * 2, 0x88E0F7FA);
        gui.centeredText(font, Component.literal("HYPERDRIVE CHARGING"), w / 2, h / 2 - 16, WHITE);
        gui.centeredText(font, Component.literal(Math.round(progress * 100.0F) + "%"), w / 2, h / 2, CYAN);
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
        gui.centeredText(font, Component.literal("DESCENT SETTLE"), w / 2, h / 2, TITLE_TEXT);
    }

    /** Full-screen warp tunnel: bright streaks rushing outward plus a colored vignette/collapsing rings. */
    private static void drawWarpTunnel(GuiGraphicsExtractor gui, int w, int h, int ticks) {
        gui.fill(0, 0, w, h, 0xFF020610);
        // Vertical hyper-tunnel streaks streaming toward the screen.
        int streakColor = 0x55DB72FF;
        int count = 36;
        for (int i = 0; i < count; i++) {
            int x = (int) ((i + 0.5F) * w / count);
            int grow = (int) (h * 0.25F);
            int offset = (int) ((ticks * (8L + (i % 5) * 4L)) % Math.max(1, h / 2));
            gui.fill(x, offset, x + 2, offset + grow, streakColor);
            gui.fill(x, h - offset - grow, x + 2, h - offset, streakColor);
        }
        // Collapsing bright ring rings
        int cx = w / 2;
        int cy = h / 2;
        for (int ring = 1; ring <= 3; ring++) {
            int r = Math.max(10, (int) (8.0F + ring * 24.0F - (ticks % 12) * 2.0F));
            gui.outline(cx - r, cy - r, r * 2, r * 2, 0x88E0F7FA);
        }
        gui.centeredText(Minecraft.getInstance().font, Component.literal("WARP JUMP ENGAGED"), cx, cy - 6, WHITE);
    }

    // --- Status readout -------------------------------------------------------------------

    private static void drawStatusReadout(GuiGraphicsExtractor gui, Font font, RocketFlightPhase phase,
                                          float progress, int shakeX, int shakeY) {
        int accent = accentFor(phase);
        int x = 12 + shakeX;
        int y = 52 + shakeY; // sits just below the oxygen HUD panel and its hazard banner
        int width = 168;
        int height = 26;

        gui.fill(x, y, x + width, y + height, HUD_PANEL);
        gui.fill(x, y, x + 2, y + height, accent);
        gui.text(font, Component.literal("▲ " + phaseLabel(phase)), x + 6, y + 3, TITLE_TEXT);
        drawProgressBar(gui, x + 6, y + 15, width - 12, 5, progress, accent);
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
