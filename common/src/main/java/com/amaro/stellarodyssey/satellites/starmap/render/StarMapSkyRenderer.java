package com.amaro.stellarodyssey.satellites.starmap.render;

import com.amaro.stellarodyssey.api.celestial.ICelestialBody;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.Random;

/**
 * Deterministic spiral-galaxy specification: 4 logarithmic spiral arms,
 * central core bulge, and halo star population.
 * Shared between the background renderer and headless unit tests.
 */
record SpiralGalaxySpec(int armCount, int starCount, float spin, float coreRadius) {
    static final SpiralGalaxySpec DEFAULT = new SpiralGalaxySpec(2, 420, 3.4F, 22.0F);

    static SpiralGalaxySpec defaultSpec() {
        return DEFAULT;
    }
}

/**
 * Orbital sector projector, procedural logarithmic galaxy background,
 * and high-fidelity celestial navigation map renderer.
 * <p>
 * Features:
 * <ul>
 *     <li>Logarithmic spiral density wave galaxy model ($r = a \cdot e^{b \cdot \theta}$)</li>
 *     <li>Precomputed star and nebula dust buffers with OBAFGKM spectral classification</li>
 *     <li>Galactic differential rotation ($\omega(r)$) and core bulge exponential glow</li>
 *     <li>Dynamic orbital propagation ($x(t), z(t)$) around system barycenters</li>
 *     <li>Spherical planetary rasterization with axial rotation, day/night terminator, and rings</li>
 *     <li>Batched span rasterization for grid and orbit lines</li>
 * </ul>
 */
public final class StarMapSkyRenderer {
    /** Number of log-spiral arms of the procedural galaxy background. */
    public static final int GALAXY_ARMS = SpiralGalaxySpec.defaultSpec().armCount();
    /** Total star population of the procedural galaxy background. */
    public static final int GALAXY_STAR_COUNT = SpiralGalaxySpec.defaultSpec().starCount();
    /** Winding tightness of the log-spiral arms (radians per normalized radius). */
    public static final float GALAXY_SPIN = SpiralGalaxySpec.defaultSpec().spin();
    /** Radius of the galactic bulge, in screen pixels at zoom 1.0. */
    public static final float GALAXY_CORE_RADIUS = SpiralGalaxySpec.defaultSpec().coreRadius();

    /** Fixed background starfield for the deep-space skybox canvas. */
    private static final int SKY_STAR_COUNT = 180;
    private static final float[] SKY_STAR_X = new float[SKY_STAR_COUNT];
    private static final float[] SKY_STAR_Y = new float[SKY_STAR_COUNT];
    private static final int[] SKY_STAR_COLORS = new int[SKY_STAR_COUNT];
    private static final float[] SKY_STAR_SPEEDS = new float[SKY_STAR_COUNT];

    /** Precomputed procedural galaxy star buffer. */
    private static final float[] GALAXY_ARM_INDEX = new float[GALAXY_STAR_COUNT];
    private static final float[] GALAXY_RADIAL_T = new float[GALAXY_STAR_COUNT];
    private static final float[] GALAXY_ARM_PHASE = new float[GALAXY_STAR_COUNT];
    private static final float[] GALAXY_RADIAL_DISPERSION = new float[GALAXY_STAR_COUNT];
    private static final int[] GALAXY_STAR_COLORS = new int[GALAXY_STAR_COUNT];
    private static final boolean[] GALAXY_STAR_MAJOR = new boolean[GALAXY_STAR_COUNT];

    /** Precomputed volumetric nebula dust cloud knots along the spiral arms. */
    private static final int NEBULA_KNOT_COUNT = 72;
    private static final float[] NEBULA_ARM = new float[NEBULA_KNOT_COUNT];
    private static final float[] NEBULA_RADIAL_T = new float[NEBULA_KNOT_COUNT];
    private static final float[] NEBULA_ANGULAR_OFFSET = new float[NEBULA_KNOT_COUNT];
    private static final float[] NEBULA_RADIUS = new float[NEBULA_KNOT_COUNT];
    private static final int[] NEBULA_COLORS = new int[NEBULA_KNOT_COUNT];
    private static final int[] BULGE_RADII = {28, 20, 14, 8, 4};
    private static final int[] BULGE_ALPHAS = {14, 24, 38, 65, 110};
    private static final int[] BULGE_COLORS = {0x00FFD54F, 0x00FFE082, 0x00FFF9C4, 0x00FFFFFF, 0x00FFFFFF};
    private static final int[] GRID_RING_RADII = {50, 100, 150, 200, 250};

    static {
        Random rng = new Random(42L);

        // 1. Deep space skybox background stars
        int[] skyPalette = {0xFFFFFFFF, 0xFF90CAF9, 0xFFE1BEE7, 0xFFFFE082, 0xFF80CBC4, 0xFFB39DDB};
        for (int i = 0; i < SKY_STAR_COUNT; i++) {
            SKY_STAR_X[i] = rng.nextFloat();
            SKY_STAR_Y[i] = rng.nextFloat();
            SKY_STAR_COLORS[i] = skyPalette[rng.nextInt(skyPalette.length)];
            SKY_STAR_SPEEDS[i] = 0.02F + rng.nextFloat() * 0.05F;
        }

        // 2. Realistic spectral stellar classification palette for spiral galaxy
        // O/B (blue/white), A (white), F (yellow-white), G (yellow), K (orange), M (red-orange)
        int[] spectralPalette = {
                0xFF80D8FF, // O-type luminous cyan-blue
                0xFFB3E5FC, // B-type blue-white
                0xFFFFFFFF, // A-type pure white
                0xFFFFF9C4, // F-type yellow-white
                0xFFFFEE58, // G-type solar yellow
                0xFFFFB74D, // K-type orange
                0xFFFF8A80  // M-type red dwarf
        };

        SpiralGalaxySpec spec = SpiralGalaxySpec.defaultSpec();
        for (int i = 0; i < GALAXY_STAR_COUNT; i++) {
            GALAXY_ARM_INDEX[i] = i % spec.armCount();
            float rawT = rng.nextFloat();
            // Quadratic/exponential clustering toward the galactic core
            GALAXY_RADIAL_T[i] = (float) Math.pow(rawT, 0.65);
            // Angular dispersion perpendicular to logarithmic spiral arm
            GALAXY_ARM_PHASE[i] = (rng.nextFloat() - 0.5F) * 0.42F;
            GALAXY_RADIAL_DISPERSION[i] = (rng.nextFloat() - 0.5F) * 0.15F;
            GALAXY_STAR_COLORS[i] = spectralPalette[rng.nextInt(spectralPalette.length)];
            GALAXY_STAR_MAJOR[i] = (rng.nextFloat() < 0.12F);
        }

        // 3. Volumetric nebula gas dust clouds along spiral arms
        int[] nebulaPalette = {
                0x2800E5FF, // Luminous cyan reflection nebula
                0x287C4DFF, // Deep violet ionization pocket
                0x24BA68C8, // Hydrogen-alpha magenta cloud
                0x2200B0FF, // Cobalt interstellar gas
                0x20FFD54F  // Warm golden core haze
        };
        for (int i = 0; i < NEBULA_KNOT_COUNT; i++) {
            NEBULA_ARM[i] = i % spec.armCount();
            NEBULA_RADIAL_T[i] = 0.15F + rng.nextFloat() * 0.8F;
            NEBULA_ANGULAR_OFFSET[i] = (rng.nextFloat() - 0.5F) * 0.35F;
            NEBULA_RADIUS[i] = 12.0F + rng.nextFloat() * 26.0F;
            NEBULA_COLORS[i] = nebulaPalette[rng.nextInt(nebulaPalette.length)];
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
     * Projected 2D star point for procedural galaxy background with alpha intensity.
     */
    public record GalaxyStarPoint(double x, double y, int alpha) {
    }

    /**
     * Pure, deterministic projection of a procedural spiral galaxy star.
     */
    public static GalaxyStarPoint galaxyPoint(int starIndex, long tickCount, float cx, float cy, float scale) {
        int idx = Math.floorMod(starIndex, GALAXY_STAR_COUNT);
        float radialT = GALAXY_RADIAL_T[idx];
        float arm = GALAXY_ARM_INDEX[idx];
        float phase = GALAXY_ARM_PHASE[idx];

        float logSpiralTheta = (float) Math.log(1.0 + radialT * 9.0) * (GALAXY_SPIN * 0.65F);
        float baseAngle = arm * (2.0F * (float) Math.PI / GALAXY_ARMS) + phase + logSpiralTheta;

        float rotation = tickCount * 0.015F;
        float diffRot = rotation * (1.2F - radialT * 0.4F);
        float angle = baseAngle + diffRot;

        float maxRadius = 150.0F * scale;
        float radius = GALAXY_CORE_RADIUS * scale + radialT * maxRadius;

        double sx = cx + Math.cos(angle) * radius;
        double sy = cy + Math.sin(angle) * radius * 0.42F;

        float twinkle = 0.65F + 0.35F * (float) Math.sin(tickCount * (0.04F + radialT * 0.05F) + idx);
        int alpha = (int) Math.clamp(110.0F * (1.0F - radialT * 0.5F) * twinkle, 25.0F, 230.0F);
        alpha = Math.clamp(alpha, 24, 210);

        return new GalaxyStarPoint(sx, sy, alpha);
    }

    /**
     * Deterministically derives static galactic sector coordinates for a charted celestial body.
     * Backwards-compatible overload delegating to tick 0.
     *
     * @param body The celestial body.
     * @return Deterministic sector coordinates in light-years.
     */
    public static SectorCoordinates getCoordinates(ICelestialBody body) {
        return getCoordinates(body, 0L);
    }

    /**
     * Deterministically derives galactic sector coordinates for a charted celestial body,
     * including orbital propagation around its star system barycenter over time.
     *
     * @param body      The celestial body.
     * @param tickCount World/animation ticks.
     * @return Dynamic sector coordinates in light-years.
     */
    public static SectorCoordinates getCoordinates(ICelestialBody body, long tickCount) {
        return getCoordinates(body, (double) tickCount);
    }

    /** Smoothly propagated coordinates for sub-tick rendering interpolation. */
    public static SectorCoordinates getCoordinates(ICelestialBody body, double tickCount) {
        if (body == null || body.dimensionKey() == null) {
            return new SectorCoordinates(0.0, 0.0, 0.0, "SEC-00-SOL");
        }

        String systemName = body.starSystemName() != null ? body.starSystemName() : "SOL";
        long systemHash = systemName.hashCode();
        double sysX = (unitHash(systemHash) - 0.5) * 440.0;
        double sysY = (unitHash(systemHash + 0x9E3779B97F4A7C15L) - 0.5) * 40.0;
        double sysZ = (unitHash(systemHash + 0x3C6EF372FE94F82AL) - 0.5) * 440.0;

        long bodyHash = (long) body.dimensionKey().identifier().hashCode() ^ (systemHash << 16);
        double orbitRadius = 18.0 + unitHash(bodyHash) * 45.0;
        double orbitSpeed = 0.0035 + (50.0 / Math.max(10.0, orbitRadius)) * 0.002;
        double initialPhase = unitHash(bodyHash + 0x9E3779B97F4A7C15L) * Math.PI * 2.0;
        double currentAngle = initialPhase + (tickCount * orbitSpeed);

        double posX = sysX + Math.cos(currentAngle) * orbitRadius;
        double posY = sysY + Math.sin(currentAngle) * (orbitRadius * 0.12);
        double posZ = sysZ + Math.sin(currentAngle) * orbitRadius;

        String systemClean = alphanumericPrefix(systemName);
        String prefix = systemClean.length() >= 3 ? systemClean.substring(0, 3).toUpperCase() : "SEC";
        int sectorNum = (int) Math.floorMod(bodyHash, 100L);
        String code = prefix + "-" + (sectorNum < 10 ? "0" : "") + sectorNum;

        return new SectorCoordinates(posX, posY, posZ, code);
    }

    private static double unitHash(long value) {
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return (value >>> 11) * 0x1.0p-53;
    }

    private static String alphanumericPrefix(String value) {
        StringBuilder prefix = new StringBuilder(3);
        for (int i = 0; i < value.length() && prefix.length() < 3; i++) {
            char c = value.charAt(i);
            if ((c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                prefix.append(c);
            }
        }
        return prefix.toString();
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

        boolean visible = screenX >= -120 && screenX <= width + 120 && screenY >= -120 && screenY <= height + 120;
        return new ProjectedPoint(screenX, screenY, depth, visible);
    }

    /**
     * Renders deep space obsidian backdrop, nebula dust, and procedural twinkling stars.
     * Backwards-compatible overload delegating with default pan and zoom.
     */
    public static void renderDeepSpaceBackground(GuiGraphicsExtractor gui, int width, int height, long tickCount) {
        renderDeepSpaceBackground(gui, width, height, 0.0, 0.0, 1.0F, tickCount);
    }

    /**
     * Renders deep space obsidian backdrop, procedural logarithmic spiral galaxy,
     * volumetric nebula dust lanes, and twinkling stars coupled with viewport pan and zoom.
     *
     * @param gui       The graphics extractor.
     * @param width     Screen width.
     * @param height    Screen height.
     * @param panX      Viewport horizontal pan.
     * @param panY      Viewport vertical pan.
     * @param zoom      Viewport zoom level.
     * @param tickCount Frame / tick counter for animations.
     */
    public static void renderDeepSpaceBackground(GuiGraphicsExtractor gui, int width, int height,
                                                 double panX, double panY, float zoom, long tickCount) {
        renderDeepSpaceBackground(gui, width, height, panX, panY, zoom, (double) tickCount);
    }

    public static void renderDeepSpaceBackground(GuiGraphicsExtractor gui, int width, int height,
                                                 double panX, double panY, float zoom, double tickCount) {
        // 1. Deep space obsidian canvas
        gui.fillGradient(0, 0, width, height, 0xFF040711, 0xFF010206);

        // 2. Distant cosmic background nebula haze
        gui.fillGradient(0, 0, width / 2, height, 0x1200E5FF, 0x00000000);
        gui.fillGradient(width / 3, 0, width, height / 2, 0x107C4DFF, 0x00000000);
        gui.fillGradient(width / 4, height / 3, width, height, 0x1400B0FF, 0x00000000);

        // 3. Procedural Logarithmic Spiral Galaxy (with pan parallax and zoom coupling)
        renderSpiralGalaxy(gui, width, height, panX, panY, zoom, tickCount);

        // 4. Twinkling background starfield
        for (int i = 0; i < SKY_STAR_COUNT; i++) {
            int sx = (int) (SKY_STAR_X[i] * width);
            int sy = (int) (SKY_STAR_Y[i] * height);

            double sin = Math.sin(tickCount * SKY_STAR_SPEEDS[i] + i);
            int alpha = (int) (140 + 115 * sin);
            int baseColor = SKY_STAR_COLORS[i] & 0x00FFFFFF;
            int starColor = (Math.clamp(alpha, 35, 255) << 24) | baseColor;

            gui.fill(sx, sy, sx + 1, sy + 1, starColor);
            if (i % 8 == 0) {
                // Cross-spike major star
                int coreAlpha = Math.clamp(alpha / 2, 20, 180);
                gui.fill(sx - 1, sy, sx + 2, sy + 1, (coreAlpha << 24) | baseColor);
                gui.fill(sx, sy - 1, sx + 1, sy + 2, (coreAlpha << 24) | baseColor);
            }
        }
    }

    /**
     * Backwards-compatible overload for spiral galaxy rendering.
     */
    public static void renderSpiralGalaxy(GuiGraphicsExtractor gui, int width, int height, long tickCount) {
        renderSpiralGalaxy(gui, width, height, 0.0, 0.0, 1.0F, tickCount);
    }

    /**
     * Procedural logarithmic spiral galaxy renderer:
     * <ul>
     *     <li>Logarithmic spiral density wave ($r = a \cdot e^{b \cdot \theta}$)</li>
     *     <li>Galactic core bulge with soft radial exponential glow</li>
     *     <li>Volumetric nebula gas dust clouds along spiral arms</li>
     *     <li>Galactic differential rotation ($\omega(r) = \omega_0 \cdot (0.75 + 0.35 / (0.2 + r))$)</li>
     *     <li>Smooth scaling and parallax with screen pan and zoom</li>
     * </ul>
     */
    public static void renderSpiralGalaxy(GuiGraphicsExtractor gui, int width, int height,
                                          double panX, double panY, float zoom, long tickCount) {
        renderSpiralGalaxy(gui, width, height, panX, panY, zoom, (double) tickCount);
    }

    public static void renderSpiralGalaxy(GuiGraphicsExtractor gui, int width, int height,
                                          double panX, double panY, float zoom, double tickCount) {
        // Parallax factor: distant galaxy shifts subtly with viewport pan for great depth
        float cx = (float) (width * 0.5F + panX * 0.32);
        float cy = (float) (height * 0.44F + panY * 0.32);

        // Macro-to-micro scale factor based on screen size and zoom
        float baseScale = Math.min(width, height) * 0.0035F;
        float scale = baseScale * (0.75F + 0.25F * zoom);
        float rotation = (float) (tickCount * 0.015F);

        // 1. Galactic Core Bulge (luminous supermassive central cluster)
        for (int b = 0; b < BULGE_RADII.length; b++) {
            float r = BULGE_RADII[b] * scale;
            int rx = (int) r;
            int ry = (int) (r * 0.48F); // tilted galactic plane disk
            int color = (BULGE_ALPHAS[b] << 24) | BULGE_COLORS[b];
            gui.fill((int) (cx - rx), (int) (cy - ry), (int) (cx + rx + 1), (int) (cy + ry + 1), color);
        }

        // 2. Volumetric Nebula Dust Clouds along spiral arms
        for (int k = 0; k < NEBULA_KNOT_COUNT; k++) {
            float radialT = NEBULA_RADIAL_T[k];
            float arm = NEBULA_ARM[k];
            float angOffset = NEBULA_ANGULAR_OFFSET[k];

            // Logarithmic spiral angle calculation
            float logAngle = arm * (2.0F * (float) Math.PI / GALAXY_ARMS)
                    + (float) Math.log(1.0 + radialT * 8.0) * (GALAXY_SPIN * 0.7F)
                    + angOffset;

            // Differential rotation
            float diffRot = rotation * (1.35F - radialT * 0.7F);
            float angle = logAngle + diffRot;

            float radius = GALAXY_CORE_RADIUS * scale + radialT * Math.max(width, height) * 0.46F * (0.8F + 0.2F * zoom);
            int nx = (int) (cx + Math.cos(angle) * radius);
            int ny = (int) (cy + Math.sin(angle) * radius * 0.44F);

            if (nx >= -40 && nx <= width + 40 && ny >= -40 && ny <= height + 40) {
                int knotR = (int) (NEBULA_RADIUS[k] * scale * 0.85F);
                int knotColor = NEBULA_COLORS[k];
                // Soft dual-layer dust stamp
                gui.fill(nx - knotR, ny - knotR / 2, nx + knotR, ny + knotR / 2, knotColor);
                if (knotR > 4) {
                    gui.fill(nx - knotR / 2, ny - knotR / 4, nx + knotR / 2, ny + knotR / 4, knotColor);
                }
            }
        }

        // 3. Precomputed Logarithmic Spiral Star Buffer
        for (int i = 0; i < GALAXY_STAR_COUNT; i++) {
            float radialT = GALAXY_RADIAL_T[i];
            float arm = GALAXY_ARM_INDEX[i];
            float phase = GALAXY_ARM_PHASE[i];
            float disp = GALAXY_RADIAL_DISPERSION[i];

            // Logarithmic spiral density wave: r = a * exp(b * theta)
            float logSpiralTheta = (float) Math.log(1.0 + radialT * 9.0) * (GALAXY_SPIN * 0.65F);
            float baseAngle = arm * (2.0F * (float) Math.PI / GALAXY_ARMS) + phase + logSpiralTheta;

            // Keplerian / flat rotation curve differential velocity
            float diffRot = rotation * (1.4F - radialT * 0.75F);
            float angle = baseAngle + diffRot;

            float maxRadius = Math.max(width, height) * 0.47F * (0.8F + 0.2F * zoom);
            float radius = (GALAXY_CORE_RADIUS + radialT * maxRadius) + disp * 15.0F;

            int sx = (int) (cx + Math.cos(angle) * radius);
            int sy = (int) (cy + Math.sin(angle) * radius * 0.44F); // Inclination foreshortening

            if (sx < -4 || sx > width + 4 || sy < -4 || sy > height + 4) {
                continue;
            }

            // Scintillation / twinkle wave
            float twinkle = 0.65F + 0.35F * (float) Math.sin(tickCount * (0.04F + radialT * 0.05F) + i);
            int alpha = (int) Math.clamp(110.0F * (1.0F - radialT * 0.5F) * twinkle, 25.0F, 230.0F);
            int color = (alpha << 24) | (GALAXY_STAR_COLORS[i] & 0x00FFFFFF);

            gui.fill(sx, sy, sx + 1, sy + 1, color);

            // Major stellar knot / bright star spike
            if (GALAXY_STAR_MAJOR[i]) {
                int glowColor = (Math.clamp(alpha / 3, 15, 120) << 24) | (GALAXY_STAR_COLORS[i] & 0x00FFFFFF);
                gui.fill(sx - 1, sy, sx + 2, sy + 1, glowColor);
                gui.fill(sx, sy - 1, sx + 1, sy + 2, glowColor);
            }
        }
    }

    /**
     * Renders Cartesian galactic grid, prime galactic axes, and concentric range rings.
     */
    public static void renderGalacticGrid(GuiGraphicsExtractor gui, int width, int height,
                                          double panX, double panY, float zoom, int baseGridSpacing) {
        double centerX = width / 2.0 + panX;
        double centerY = height / 2.0 + panY;
        double step = baseGridSpacing * zoom;

        int gridColor = 0x1800E5FF; // Subtle neon cyan
        int axisColor = 0x4800E5FF; // Primary galactic coordinate meridian

        if (step > 15) {
            // Batched vertical grid lines
            for (double x = centerX % step; x < width; x += step) {
                gui.fill((int) x, 0, (int) x + 1, height, gridColor);
            }
            // Batched horizontal grid lines
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

        // Concentric sector range rings (50 ly, 100 ly, 150 ly, 200 ly, 250 ly)
        for (int r : GRID_RING_RADII) {
            double scaledR = r * zoom;
            if (scaledR > 12 && scaledR < Math.max(width, height) * 1.6) {
                renderOrbitRing(gui, centerX, centerY, scaledR, scaledR * 0.72, 0x2200E5FF, 36);
            }
        }

        // Galactic Core reticle
        int cx = (int) centerX;
        int cy = (int) centerY;
        if (cx >= -20 && cx <= width + 20 && cy >= -20 && cy <= height + 20) {
            gui.fill(cx - 6, cy, cx + 7, cy + 1, 0xFF00E5FF);
            gui.fill(cx, cy - 6, cx + 1, cy + 7, 0xFF00E5FF);
            gui.fill(cx - 2, cy - 2, cx + 3, cy + 3, 0x90FFFFFF);
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
     * Renders a hyperspace navigation lane between two galactic points with animated pulse packets.
     */
    public static void renderHyperspaceLane(GuiGraphicsExtractor gui, double x1, double y1,
                                            double x2, double y2, int color, long tickCount) {
        renderHyperspaceLane(gui, x1, y1, x2, y2, color, (double) tickCount);
    }

    public static void renderHyperspaceLane(GuiGraphicsExtractor gui, double x1, double y1,
                                            double x2, double y2, int color, double tickCount) {
        drawLine(gui, (int) x1, (int) y1, (int) x2, (int) y2, color);

        // Animated pulse packet traveling smoothly along the lane
        double progress = (tickCount % 60.0) / 60.0;
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
        renderCelestialNode(gui, font, sx, sy, body, selected, hovered, tickCount, 0);
    }

    /**
     * Renders a high-fidelity celestial body node:
     * <ul>
     *     <li>Spherical rasterized body with dynamic axial rotation</li>
     *     <li>Day/night terminator shadow crescent</li>
     *     <li>Atmospheric limb glow and specular highlight</li>
     *     <li>Tilted planetary rings for gaseous / ringed celestial bodies</li>
     *     <li>Animated selection brackets, hover glow, and rocket tier clearance badge</li>
     * </ul>
     */
    public static void renderCelestialNode(GuiGraphicsExtractor gui, Font font,
                                           double sx, double sy,
                                           ICelestialBody body,
                                           boolean selected, boolean hovered,
                                           long tickCount,
                                           int rocketTier) {
        renderCelestialNode(gui, font, sx, sy, body, selected, hovered, (double) tickCount, rocketTier);
    }

    public static void renderCelestialNode(GuiGraphicsExtractor gui, Font font,
                                           double sx, double sy,
                                           ICelestialBody body,
                                           boolean selected, boolean hovered,
                                           double tickCount,
                                           int rocketTier) {
        int ix = (int) sx;
        int iy = (int) sy;

        // Base node color based on atmosphere and hazard profile
        int coreColor;
        int haloColor;
        int atmosphereColor;
        if (body.hasBreathableAtmosphere()) {
            coreColor = 0xFF00E676; // Emerald/Cyan habitable world
            haloColor = 0x4000E676;
            atmosphereColor = 0x6080E8A8;
        } else if (body.isHazardous()) {
            coreColor = 0xFFFF5252; // Extreme radiation / volcanic / toxic hazard
            haloColor = 0x40FF5252;
            atmosphereColor = 0x50FF8A80;
        } else {
            coreColor = 0xFF00E5FF; // Signature sci-fi icy/exotic cyan
            haloColor = 0x4000E5FF;
            atmosphereColor = 0x6080DEEA;
        }

        int radius = selected ? 7 : (hovered ? 6 : 5);
        long bodyHash = body.dimensionKey() != null ? (long) body.dimensionKey().identifier().hashCode() : 0L;
        boolean hasRings = Math.abs(bodyHash % 3) == 0;

        // 1. Back arc of planetary rings (behind the planetary sphere)
        if (hasRings) {
            renderPlanetaryRingArc(gui, ix, iy, radius + 5, radius + 2, 0x6000E5FF, true);
        }

        // 2. Outer atmospheric aura / halo
        gui.fill(ix - radius - 2, iy - radius - 2, ix + radius + 3, iy + radius + 3, haloColor);

        // 3. Spherical planetary rasterization with rounded horizontal spans
        for (int dy = -radius; dy <= radius; dy++) {
            int dx = (int) Math.round(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            gui.fill(ix - dx, iy + dy, ix + dx + 1, iy + dy + 1, coreColor);
        }

        // 4. Dynamic axial rotation: day/night terminator shading
        // Simulates day/night hemispherical shadow based on tick rotation
        double axialAngle = ((tickCount * 0.05 + (bodyHash % 100)) % (Math.PI * 2));
        int shadowOffset = (int) Math.round(Math.sin(axialAngle) * (radius * 0.5));
        for (int dy = -radius + 1; dy <= radius - 1; dy++) {
            int dx = (int) Math.round(Math.sqrt(Math.max(0, radius * radius - dy * dy)));
            if (shadowOffset > 0) {
                // Night hemisphere on right side
                gui.fill(ix + shadowOffset, iy + dy, ix + dx + 1, iy + dy + 1, 0x55000000);
            } else {
                // Night hemisphere on left side
                gui.fill(ix - dx, iy + dy, ix + shadowOffset, iy + dy + 1, 0x55000000);
            }
        }

        // 5. Specular planetary surface highlight / atmospheric rim
        gui.fill(ix - radius / 2, iy - radius / 2, ix - radius / 2 + 2, iy - radius / 2 + 2, 0xAAFFFFFF);

        // 6. Front arc of planetary rings (in front of the planetary sphere)
        if (hasRings) {
            renderPlanetaryRingArc(gui, ix, iy, radius + 5, radius + 2, 0x9000E5FF, false);
        }

        // 7. Selected targeting reticle brackets
        if (selected) {
            int bracketSize = 13;
            int reticleColor = (Math.sin(tickCount * 0.16) > 0.0) ? 0xFF00E5FF : 0xFFFFFFFF;
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

        // 8. Hover highlight boundary
        if (hovered && !selected) {
            gui.fill(ix - 8, iy - 8, ix + 9, iy - 7, 0x80FFFFFF);
            gui.fill(ix - 8, iy + 8, ix + 9, iy + 9, 0x80FFFFFF);
            gui.fill(ix - 8, iy - 8, ix - 7, iy + 9, 0x80FFFFFF);
            gui.fill(ix + 8, iy - 8, ix + 9, iy + 9, 0x80FFFFFF);
        }

        // 9. Celestial body text label and rocket tier clearance badge
        String label = body.name().toUpperCase();
        int labelColor = selected ? 0xFF00E5FF : (hovered ? 0xFFFFFFFF : 0xFFB0BEC5);

        if (rocketTier > 0) {
            int reqTier = com.amaro.stellarodyssey.registry.tiers.RocketTiers.getRequiredTier(body.dimensionKey());
            if (reqTier > 0) {
                if (rocketTier >= reqTier) {
                    label += " [UNLOCKED]";
                } else {
                    label += " [LOCKED - REQUIRES TIER " + reqTier + "]";
                    if (!selected && !hovered) {
                        labelColor = 0xFFFF5252;
                    }
                }
            }
        }

        gui.text(font, Component.literal(label), ix + radius + 5, iy - 4, labelColor);

        // 10. Environmental hazard badge
        if (body.isHazardous()) {
            boolean pulse = Math.sin(tickCount * 0.20) > 0.0;
            int badgeColor = pulse ? 0xFFFF1744 : 0xFFFF8A80;
            gui.text(font, Component.literal("⚠"), ix - radius - 11, iy - 4, badgeColor);
        }
    }

    /**
     * Renders either the back arc or the front arc of tilted planetary rings.
     */
    private static void renderPlanetaryRingArc(GuiGraphicsExtractor gui, int cx, int cy,
                                               int rx, int ry, int color, boolean backArc) {
        int segments = 24;
        int startIdx = backArc ? segments / 2 : 0;
        int endIdx = backArc ? segments : segments / 2;

        double prevX = cx + Math.cos((2.0 * Math.PI * startIdx) / segments) * rx;
        double prevY = cy + Math.sin((2.0 * Math.PI * startIdx) / segments) * ry;

        for (int i = startIdx + 1; i <= endIdx; i++) {
            double angle = (2.0 * Math.PI * i) / segments;
            double curX = cx + Math.cos(angle) * rx;
            double curY = cy + Math.sin(angle) * ry;

            drawLine(gui, (int) prevX, (int) prevY, (int) curX, (int) curY, color);

            prevX = curX;
            prevY = curY;
        }
    }

    /**
     * Optimized line rasterizer using batched spans and segment rectangles.
     */
    public static void drawLine(GuiGraphicsExtractor gui, int x0, int y0, int x1, int y1, int color) {
        // Fast horizontal line
        if (y0 == y1) {
            int minX = Math.min(x0, x1);
            int maxX = Math.max(x0, x1);
            gui.fill(minX, y0, maxX + 1, y0 + 1, color);
            return;
        }
        // Fast vertical line
        if (x0 == x1) {
            int minY = Math.min(y0, y1);
            int maxY = Math.max(y0, y1);
            gui.fill(x0, minY, x0 + 1, maxY + 1, color);
            return;
        }

        // Diagonal Bresenham with run-length batching
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
