package com.amaro.stellarodyssey.satellites.starmap.render;

import com.amaro.stellarodyssey.api.celestial.ICelestialBody;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Random;

/**
 * Orbital sector projector and star map grid renderer.
 * <p>
 * Projects 3D galactic and celestial coordinates into 2D screen space,
 * renders sci-fi coordinate grids, nebula dust, orbital rings, and celestial nodes.
 */
public final class StarMapSkyRenderer {
    private static final int STAR_COUNT = 140;
    private static final float[] STAR_X = new float[STAR_COUNT];
    private static final float[] STAR_Y = new float[STAR_COUNT];
    private static final int[] STAR_COLORS = new int[STAR_COUNT];
    private static final float[] STAR_SPEEDS = new float[STAR_COUNT];

    static {
        Random rng = new Random(42L);
        int[] palette = {0xFFFFFFFF, 0xFF80DEEA, 0xFFE1BEE7, 0xFFFFE082, 0xFF80CBC4, 0xFFB39DDB};
        for (int i = 0; i < STAR_COUNT; i++) {
            STAR_X[i] = rng.nextFloat();
            STAR_Y[i] = rng.nextFloat();
            STAR_COLORS[i] = palette[rng.nextInt(palette.length)];
            STAR_SPEEDS[i] = 0.02F + rng.nextFloat() * 0.06F;
        }
    }

    private StarMapSkyRenderer() {
    }

    /**
     * 3D galactic sector coordinates record.
     */
    public record SectorCoordinates(double x, double y, double z, String sectorCode) {
    }

    /**
     * Projected 2D point with depth indicator.
     */
    public record ProjectedPoint(double screenX, double screenY, double depth, boolean visible) {
    }

    /**
     * Deterministically derives galactic sector coordinates for a charted celestial body.
     *
     * @param body The celestial body.
     * @return Deterministic sector coordinates in light-years.
     */
    public static SectorCoordinates getCoordinates(ICelestialBody body) {
        if (body == null || body.dimensionKey() == null) {
            return new SectorCoordinates(0.0, 0.0, 0.0, "SEC-00-SOL");
        }
        long hash = (long) body.dimensionKey().identifier().hashCode() ^ ((long) body.starSystemName().hashCode() << 16);
        Random rng = new Random(hash);
        double x = (rng.nextDouble() - 0.5) * 500.0;
        double y = (rng.nextDouble() - 0.5) * 60.0;
        double z = (rng.nextDouble() - 0.5) * 500.0;

        String systemClean = body.starSystemName().replaceAll("[^A-Za-z0-9]", "");
        String prefix = systemClean.length() >= 3 ? systemClean.substring(0, 3).toUpperCase() : "SEC";
        int sectorNum = Math.abs((int) (hash % 100));
        String code = String.format("%s-%02d", prefix, sectorNum);
        return new SectorCoordinates(x, y, z, code);
    }

    /**
     * Projects 3D galactic coordinates onto 2D viewport screen coordinates with pan and zoom.
     *
     * @param worldX  Galactic X in light-years.
     * @param worldY  Galactic Y (declination) in light-years.
     * @param worldZ  Galactic Z in light-years.
     * @param width   Viewport width.
     * @param height  Viewport height.
     * @param panX    Horizontal pan offset.
     * @param panY    Vertical pan offset.
     * @param zoom    Zoom factor.
     * @param pitch   Orbital tilt pitch in degrees.
     * @param yaw     Galactic rotation yaw in degrees.
     * @return Projected 2D screen coordinates.
     */
    public static ProjectedPoint project(double worldX, double worldY, double worldZ,
                                         int width, int height,
                                         double panX, double panY,
                                         float zoom, float pitch, float yaw) {
        double radYaw = Math.toRadians(yaw);
        double cosYaw = Math.cos(radYaw);
        double sinYaw = Math.sin(radYaw);
        double rotX = worldX * cosYaw - worldZ * sinYaw;
        double rotZ = worldX * sinYaw + worldZ * cosYaw;

        double radPitch = Math.toRadians(pitch);
        double cosPitch = Math.cos(radPitch);
        double sinPitch = Math.sin(radPitch);
        double rotY = worldY * cosPitch - rotZ * sinPitch;
        double depth = worldY * sinPitch + rotZ * cosPitch;

        double centerX = width / 2.0;
        double centerY = height / 2.0;

        double screenX = centerX + panX + (rotX * zoom);
        double screenY = centerY + panY + (rotY * zoom);

        boolean visible = screenX >= -100 && screenX <= width + 100 && screenY >= -100 && screenY <= height + 100;
        return new ProjectedPoint(screenX, screenY, depth, visible);
    }

    /**
     * Renders deep space obsidian backdrop, nebula dust, and procedural twinkling stars.
     *
     * @param gui       The graphics extractor.
     * @param width     Screen width.
     * @param height    Screen height.
     * @param tickCount Frame / tick counter for animations.
     */
    public static void renderDeepSpaceBackground(GuiGraphicsExtractor gui, int width, int height, long tickCount) {
        // 1. Deep space obsidian canvas
        gui.fillGradient(0, 0, width, height, 0xFF050914, 0xFF020409);

        // 2. Cosmic nebula gas dust layers (subtle atmospheric gradients)
        gui.fillGradient(0, 0, width / 2, height, 0x1400E5FF, 0x00000000);
        gui.fillGradient(width / 3, 0, width, height / 2, 0x107C4DFF, 0x00000000);
        gui.fillGradient(width / 4, height / 3, width, height, 0x1200B0FF, 0x00000000);

        // 3. Twinkling background starfield
        for (int i = 0; i < STAR_COUNT; i++) {
            int sx = (int) (STAR_X[i] * width);
            int sy = (int) (STAR_Y[i] * height);

            double sin = Math.sin(tickCount * STAR_SPEEDS[i] + i);
            int alpha = (int) (140 + 115 * sin);
            int baseColor = STAR_COLORS[i] & 0x00FFFFFF;
            int starColor = (Math.clamp(alpha, 40, 255) << 24) | baseColor;

            gui.fill(sx, sy, sx + 1, sy + 1, starColor);
            if (i % 7 == 0) {
                // Slightly larger major star
                gui.fill(sx - 1, sy, sx + 2, sy + 1, (Math.clamp(alpha / 2, 20, 180) << 24) | baseColor);
            }
        }
    }

    /**
     * Renders the sci-fi galactic coordinate grid, axis lines, range rings, and core beacon.
     */
    public static void renderGalacticGrid(GuiGraphicsExtractor gui, int width, int height,
                                         double panX, double panY, float zoom, int baseGridSpacing) {
        double centerX = width / 2.0 + panX;
        double centerY = height / 2.0 + panY;
        double step = baseGridSpacing * zoom;

        int gridColor = 0x1A00E5FF; // Subtle neon cyan
        int axisColor = 0x4D00E5FF; // Stronger primary axis

        if (step > 15) {
            // Vertical grid lines
            for (double x = centerX % step; x < width; x += step) {
                gui.fill((int) x, 0, (int) x + 1, height, gridColor);
            }
            // Horizontal grid lines
            for (double y = centerY % step; y < height; y += step) {
                gui.fill(0, (int) y, width, (int) y + 1, gridColor);
            }
        }

        // Major galactic axes
        if (centerX >= 0 && centerX <= width) {
            gui.fill((int) centerX, 0, (int) centerX + 1, height, axisColor);
        }
        if (centerY >= 0 && centerY <= height) {
            gui.fill(0, (int) centerY, width, (int) centerY + 1, axisColor);
        }

        // Concentric sector range rings (50 ly, 100 ly, 150 ly, 200 ly)
        int[] ringRadii = {50, 100, 150, 200};
        for (int r : ringRadii) {
            double scaledR = r * zoom;
            if (scaledR > 10 && scaledR < Math.max(width, height) * 1.5) {
                renderOrbitRing(gui, centerX, centerY, scaledR, scaledR * 0.7, 0x2400E5FF, 32);
            }
        }

        // Galactic Core reticle
        int cx = (int) centerX;
        int cy = (int) centerY;
        if (cx >= -20 && cx <= width + 20 && cy >= -20 && cy <= height + 20) {
            gui.fill(cx - 5, cy, cx + 6, cy + 1, 0xFF00E5FF);
            gui.fill(cx, cy - 5, cx + 1, cy + 6, 0xFF00E5FF);
            gui.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0x80FFFFFF);
        }
    }

    /**
     * Renders an orbital ellipse ring with segmented line approximations.
     */
    public static void renderOrbitRing(GuiGraphicsExtractor gui, double cx, double cy,
                                       double rx, double ry, int color, int segments) {
        double prevX = cx + rx;
        double prevY = cy;

        for (int i = 1; i <= segments; i++) {
            double angle = (2.0 * Math.PI * i) / segments;
            double curX = cx + Math.cos(angle) * rx;
            double curY = cy + Math.sin(angle) * ry;

            drawLine(gui, (int) prevX, (int) prevY, (int) curX, (int) curY, color);

            prevX = curX;
            prevY = curY;
        }
    }

    /**
     * Renders a hyperspace navigation lane between two galactic points.
     */
    public static void renderHyperspaceLane(GuiGraphicsExtractor gui, double x1, double y1,
                                            double x2, double y2, int color, long tickCount) {
        drawLine(gui, (int) x1, (int) y1, (int) x2, (int) y2, color);

        // Animated pulse packet traveling along the lane
        double progress = (tickCount % 60) / 60.0;
        double px = x1 + (x2 - x1) * progress;
        double py = y1 + (y2 - y1) * progress;
        gui.fill((int) px - 2, (int) py - 2, (int) px + 3, (int) py + 3, 0xFFFFFFFF);
    }

    /**
     * Renders a celestial body node with glowing halo, hazard badge, and selection reticle.
     */
    public static void renderCelestialNode(GuiGraphicsExtractor gui, Font font,
                                           double sx, double sy,
                                           ICelestialBody body,
                                           boolean selected, boolean hovered,
                                           long tickCount) {
        int ix = (int) sx;
        int iy = (int) sy;

        // Base node color based on hazard and atmosphere
        int coreColor;
        int haloColor;
        if (body.hasBreathableAtmosphere()) {
            coreColor = 0xFF00E676; // Cyan/Green for life-supporting habitable worlds
            haloColor = 0x4000E676;
        } else if (body.isHazardous()) {
            coreColor = 0xFFFF5252; // Red for extreme hazards / vacuum / radiation
            haloColor = 0x40FF5252;
        } else {
            coreColor = 0xFF00E5FF; // Signature sci-fi cyan
            haloColor = 0x4000E5FF;
        }

        // Outer glow halo
        int radius = selected ? 7 : (hovered ? 6 : 4);
        gui.fill(ix - radius - 2, iy - radius - 2, ix + radius + 3, iy + radius + 3, haloColor);

        // Core planetary sphere
        gui.fill(ix - radius, iy - radius, ix + radius + 1, iy + radius + 1, coreColor);
        gui.fill(ix - 1, iy - 1, ix + 2, iy + 2, 0xFFFFFFFF);

        // Selected targeting brackets / reticle
        if (selected) {
            int bracketSize = 12;
            int reticleColor = (tickCount % 20 < 10) ? 0xFF00E5FF : 0xFFFFFFFF;
            // Top-left
            gui.fill(ix - bracketSize, iy - bracketSize, ix - bracketSize + 4, iy - bracketSize + 1, reticleColor);
            gui.fill(ix - bracketSize, iy - bracketSize, ix - bracketSize + 1, iy - bracketSize + 4, reticleColor);
            // Top-right
            gui.fill(ix + bracketSize - 4, iy - bracketSize, ix + bracketSize + 1, iy - bracketSize + 1, reticleColor);
            gui.fill(ix + bracketSize, iy - bracketSize, ix + bracketSize + 1, iy - bracketSize + 4, reticleColor);
            // Bottom-left
            gui.fill(ix - bracketSize, iy + bracketSize, ix - bracketSize + 4, iy + bracketSize + 1, reticleColor);
            gui.fill(ix - bracketSize, iy + bracketSize - 3, ix - bracketSize + 1, iy + bracketSize + 1, reticleColor);
            // Bottom-right
            gui.fill(ix + bracketSize - 4, iy + bracketSize, ix + bracketSize + 1, iy + bracketSize + 1, reticleColor);
            gui.fill(ix + bracketSize, iy + bracketSize - 3, ix + bracketSize + 1, iy + bracketSize + 1, reticleColor);
        }

        // Hover highlight
        if (hovered && !selected) {
            gui.fill(ix - 8, iy - 8, ix + 9, iy - 7, 0x80FFFFFF);
            gui.fill(ix - 8, iy + 8, ix + 9, iy + 9, 0x80FFFFFF);
            gui.fill(ix - 8, iy - 8, ix - 7, iy + 9, 0x80FFFFFF);
            gui.fill(ix + 8, iy - 8, ix + 9, iy + 9, 0x80FFFFFF);
        }

        // Planet text label
        String label = body.name().toUpperCase();
        int labelColor = selected ? 0xFF00E5FF : (hovered ? 0xFFFFFFFF : 0xFFB0BEC5);
        gui.text(font, Component.literal(label), ix + radius + 4, iy - 4, labelColor);

        // Hazard badge indicator
        if (body.isHazardous()) {
            boolean pulse = (tickCount / 15) % 2 == 0;
            int badgeColor = pulse ? 0xFFFF1744 : 0xFFFF8A80;
            gui.text(font, Component.literal("⚠"), ix - radius - 10, iy - 4, badgeColor);
        }
    }

    /**
     * Bresenham-style line rasterizer using small filled rects.
     */
    public static void drawLine(GuiGraphicsExtractor gui, int x0, int y0, int x1, int y1, int color) {
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
