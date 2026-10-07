package com.amaro.stellarodyssey.client.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Random;

/**
 * 3D Radial Star-Streak Hyperspace Tunnel and Relativistic Warp Jump Renderer.
 * <p>
 * Renders:
 * <ul>
 *     <li>Singularity collapse during {@code WARP_CHARGE} (gravitational lensing rings contracting inward)</li>
 *     <li>Relativistic hyperspace tunnel during {@code WARP} (radial star streaks rushing toward the viewer)</li>
 *     <li>Luminous deceleration shockwave during {@code ARRIVAL} (expanding radiant wave dropping into realspace)</li>
 * </ul>
 */
public final class WarpTunnelRenderer {
    public static final int STREAK_COUNT = 54;
    private static final float[] STREAK_ANGLES = new float[STREAK_COUNT];
    private static final float[] STREAK_INITIAL_R = new float[STREAK_COUNT];
    private static final float[] STREAK_SPEEDS = new float[STREAK_COUNT];
    private static final int[] STREAK_COLORS = new int[STREAK_COUNT];

    static {
        Random rng = new Random(1337L);
        int[] palette = {
                0xFF00E5FF, // Luminous Hyper-Cyan
                0xFFDB72FF, // Relativistic Violet
                0xFF80D8FF, // Spectral Blue
                0xFFFFFFFF, // Pure Stellar White
                0xFFE040FB  // Deep Ultraviolet
        };

        for (int i = 0; i < STREAK_COUNT; i++) {
            float baseAngle = (float) (i * 2.0 * Math.PI / STREAK_COUNT);
            STREAK_ANGLES[i] = baseAngle + (rng.nextFloat() - 0.5F) * 0.12F;
            STREAK_INITIAL_R[i] = rng.nextFloat();
            STREAK_SPEEDS[i] = 0.035F + rng.nextFloat() * 0.045F;
            STREAK_COLORS[i] = palette[rng.nextInt(palette.length)];
        }
    }

    private WarpTunnelRenderer() {
    }

    /**
     * Pure function calculating perspective-stretched streak length.
     * Streaks elongate rapidly as they reach the outer viewport boundary.
     */
    public static double computeStreakLength(double radius, double baseLength, double perspectiveStretch) {
        return Math.max(2.0, baseLength + radius * perspectiveStretch);
    }

    /**
     * Pure function calculating collapsing singularity radius during WARP_CHARGE.
     */
    public static double computeSingularityRadius(double maxRadius, float progress) {
        float clampedProgress = Math.clamp(progress, 0.0F, 1.0F);
        return Math.max(6.0, maxRadius * (1.0F - clampedProgress));
    }

    /**
     * Pure function calculating expanding shockwave radius during ARRIVAL.
     */
    public static double computeShockwaveRadius(double maxRadius, float progress) {
        float clampedProgress = Math.clamp(progress, 0.0F, 1.0F);
        return maxRadius * Math.sqrt(clampedProgress);
    }

    /**
     * Renders the full relativistic 3D warp tunnel during WARP flight.
     */
    public static void renderWarpTunnel(GuiGraphicsExtractor gui, Font font, int width, int height, int ticks) {
        // Deep obsidian hyperspace background
        gui.fill(0, 0, width, height, 0xFF02040B);

        int cx = width / 2;
        int cy = height / 2;
        double maxRadius = Math.sqrt(cx * cx + cy * cy) * 1.05;

        // Radial streaming star-streaks rushing toward the camera
        for (int i = 0; i < STREAK_COUNT; i++) {
            float angle = STREAK_ANGLES[i];
            float speed = STREAK_SPEEDS[i];
            float initialR = STREAK_INITIAL_R[i];

            // Radial progression over time
            double progress = (initialR + ticks * speed) % 1.0;
            double currentR = Math.pow(progress, 1.8) * maxRadius; // Accelerating outward perspective
            double length = computeStreakLength(currentR, 6.0, 0.32);

            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            int xHead = (int) (cx + cos * currentR);
            int yHead = (int) (cy + sin * currentR);

            double tailR = Math.max(0.0, currentR - length);
            int xTail = (int) (cx + cos * tailR);
            int yTail = (int) (cy + sin * tailR);

            int alpha = (int) (Math.clamp(progress * 255.0, 40.0, 240.0));
            int color = (alpha << 24) | (STREAK_COLORS[i] & 0x00FFFFFF);

            // Draw radial streak line
            drawLine(gui, xTail, yTail, xHead, yHead, color);

            // Bright stellar head needle
            if (currentR > 25.0) {
                gui.fill(xHead - 1, yHead - 1, xHead + 1, yHead + 1, 0xFFFFFFFF);
            }
        }

        // Concentric hyperdrive spacetime metric distortion rings
        for (int ring = 1; ring <= 4; ring++) {
            int ringPhase = (int) ((ticks * 3L + ring * 25L) % 100);
            double rFraction = ringPhase / 100.0;
            int r = (int) (Math.pow(rFraction, 1.5) * (Math.min(width, height) * 0.45));
            int ringAlpha = (int) ((1.0 - rFraction) * 160.0);
            if (r > 4 && ringAlpha > 10) {
                int ringColor = (ringAlpha << 24) | 0x00E0F7FA;
                gui.outline(cx - r, cy - r, r * 2, r * 2, ringColor);
            }
        }

        // Central hyperspace wormhole singularity
        int coreR = 14;
        gui.fill(cx - coreR, cy - coreR, cx + coreR, cy + coreR, 0x80DB72FF);
        gui.fill(cx - coreR / 2, cy - coreR / 2, cx + coreR / 2, cy + coreR / 2, 0xCCFFFFFF);

        // Edge chromatic aberration vignette
        gui.fillGradient(0, 0, width, height / 6, 0x55DB72FF, 0x00000000);
        gui.fillGradient(0, height - height / 6, width, height, 0x00000000, 0x5500E5FF);

        gui.centeredText(font, Component.literal("WARP JUMP ENGAGED // HYPERDRIVE TRAVERSAL"), cx, cy - 24, 0xFFFFFFFF);
        gui.centeredText(font, Component.literal("FASTER-THAN-LIGHT TRANSIT"), cx, cy - 10, 0xFF00E5FF);
    }

    /**
     * Renders collapsing singularity rings during WARP_CHARGE.
     */
    public static void renderWarpCharge(GuiGraphicsExtractor gui, Font font, int width, int height, float progress) {
        int cx = width / 2;
        int cy = height / 2;
        double maxR = Math.min(width, height) * 0.45;

        int alpha = Math.round(180.0F * progress);
        if (alpha > 0) {
            gui.fillGradient(0, 0, width, height, (alpha << 24) | 0x0000E5FF, (alpha << 24) | 0x007C4DFF);
        }

        // Concentric gravitational lensing rings contracting inward
        for (int i = 0; i < 3; i++) {
            float phase = Math.clamp(progress - (i * 0.12F), 0.0F, 1.0F);
            int r = (int) computeSingularityRadius(maxR - (i * 20), phase);
            int ringColor = 0x90E0F7FA;
            gui.outline(cx - r, cy - r, r * 2, r * 2, ringColor);
        }

        // Central gravitational well core
        int coreR = Math.round(8.0F + 14.0F * progress);
        gui.fill(cx - coreR, cy - coreR, cx + coreR, cy + coreR, 0xAAFFFFFF);

        gui.centeredText(font, Component.literal("HYPERDRIVE CHARGING // SPOOLING WARP COILS"), cx, cy - 22, 0xFFFFFFFF);
        gui.centeredText(font, Component.literal(Math.round(progress * 100.0F) + "%"), cx, cy - 6, 0xFF00E5FF);
    }

    /**
     * Renders deceleration shockwave expansion during ARRIVAL.
     */
    public static void renderArrival(GuiGraphicsExtractor gui, Font font, int width, int height, float progress) {
        int cx = width / 2;
        int cy = height / 2;
        double maxR = Math.max(width, height) * 0.85;

        // Expanding shockwave burst
        int shockwaveR = (int) computeShockwaveRadius(maxR, progress);
        int alpha = (int) ((1.0F - progress) * 220.0F);
        if (alpha > 5) {
            int shockColor = (alpha << 24) | 0x0000E5FF;
            gui.outline(cx - shockwaveR, cy - shockwaveR, shockwaveR * 2, shockwaveR * 2, shockColor);
            if (shockwaveR > 6) {
                gui.outline(cx - shockwaveR + 2, cy - shockwaveR + 2, (shockwaveR - 2) * 2, (shockwaveR - 2) * 2, (alpha / 2 << 24) | 0x00FFFFFF);
            }
        }

        gui.centeredText(font, Component.literal("EXITING HYPERSPACE // DESTINATION ARRIVAL"), cx, cy - 20, 0xFF00E5FF);
        gui.centeredText(font, Component.literal("SUBSPACE TRANSITION COMPLETE"), cx, cy - 6, 0xFFE0F7FA);
    }

    /**
     * Line drawer helper for radial streaks.
     */
    private static void drawLine(GuiGraphicsExtractor gui, int x0, int y0, int x1, int y1, int color) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1;
        int sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;

        int currX = x0;
        int currY = y0;

        while (true) {
            gui.fill(currX, currY, currX + 1, currY + 1, color);
            if (currX == x1 && currY == y1) {
                break;
            }
            int e2 = 2 * err;
            if (e2 > -dy) {
                err -= dy;
                currX += sx;
            }
            if (e2 < dx) {
                err += dx;
                currY += sy;
            }
        }
    }
}
