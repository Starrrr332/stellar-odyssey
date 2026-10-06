import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Procedural placeholder-art generator for Stellar Odyssey.
 * <p>
 * Run from the project root with any JDK 11+ (no compilation step needed):
 * <pre>  java tools/TextureGen.java</pre>
 * Every texture is deterministic (fixed seeds) so re-running produces identical files.
 * Replace the outputs with hand-painted / Blockbench art whenever you like - file names
 * are what the JSON models reference.
 * <p>
 * Emissive convention: {@code <name>_emissive.png} holds ONLY the glowing pixels (fully
 * opaque) on a fully transparent background. Models render it as a second, full-bright layer.
 */
public class TextureGen {
    static Path assets;

    public static void main(String[] args) throws IOException {
        Path project = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        assets = project.resolve("common/src/main/resources/assets/stellarodyssey");
        alienOre();
        oxygenTank();
        icon();
        System.out.println("Textures written to " + assets);
    }

    // ------------------------------------------------------------------------------------
    //  Alien Ore: violet alien bedrock + bioluminescent crystal shards
    // ------------------------------------------------------------------------------------
    static void alienOre() throws IOException {
        BufferedImage base = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glow = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        int[] stone = {0xFF1C1630, 0xFF261E3D, 0xFF30264C, 0xFF3B2F5C, 0xFF473A6B};
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double n = 0.6 * valueNoise(x / 4.0, y / 4.0, 11) + 0.4 * valueNoise(x / 2.0, y / 2.0, 23);
                base.setRGB(x, y, stone[clamp((int) (n * stone.length), 0, stone.length - 1)]);
            }
        }

        // Crystal shards: {startX, startY, dx, dy, length, palette}
        int[][] shards = {
                {2, 3, 1, 1, 3, 0},
                {11, 2, -1, 1, 3, 0},
                {4, 11, 1, -1, 3, 1},
                {11, 10, 1, 1, 3, 0},
                {8, 7, 0, 1, 2, 1},
        };
        int[][] palettes = {
                // edge, body, core (cyan)
                {0xFF1E9FC4, 0xFF5EF2FF, 0xFFE4FFFF},
                // edge, body, core (magenta)
                {0xFF8B34C9, 0xFFDB72FF, 0xFFFFE3FF},
        };
        int socketDark = 0xFF120D20;
        int socketLit = 0xFF173A4A;

        for (int[] s : shards) {
            int[] pal = palettes[s[5]];
            List<int[]> line = new ArrayList<>();
            for (int i = 0; i < s[4]; i++) {
                line.add(new int[]{s[0] + s[2] * i, s[1] + s[3] * i});
            }
            // edges first (4-neighbourhood of the shard line)
            for (int[] p : line) {
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int ex = p[0] + d[0], ey = p[1] + d[1];
                    if (inside(ex, ey) && glow.getRGB(ex, ey) == 0) {
                        glow.setRGB(ex, ey, pal[0]);
                        base.setRGB(ex, ey, socketDark);
                    }
                }
            }
            for (int i = 0; i < line.size(); i++) {
                int[] p = line.get(i);
                boolean core = i == line.size() / 2;
                glow.setRGB(p[0], p[1], core ? pal[2] : pal[1]);
                base.setRGB(p[0], p[1], socketLit);
            }
        }

        write(base, "textures/block/alien_ore.png");
        write(glow, "textures/block/alien_ore_emissive.png");
    }

    // ------------------------------------------------------------------------------------
    //  Oxygen Tank: brushed-metal capsule with a glowing O2 window + hazard band
    // ------------------------------------------------------------------------------------
    static void oxygenTank() throws IOException {
        BufferedImage base = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glow = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        int outline = 0xFF22262F;
        int[] metal = {0xFFF1F4F8, 0xFFC9D0DA, 0xFFA3ADBB, 0xFF7B8697, 0xFF566072}; // light -> dark
        int hazard = 0xFFFF8A2A;
        int hazardDark = 0xFFC2581A;
        int windowDark = 0xFF0B2A33;

        // body: columns 5..10, rows 3..14 with rounded corners
        for (int y = 3; y <= 14; y++) {
            for (int x = 5; x <= 10; x++) {
                boolean corner = (y == 3 || y == 14) && (x == 5 || x == 10);
                if (corner) continue;
                int shade = switch (x) {
                    case 5 -> 1;
                    case 6 -> 0;
                    case 7 -> 1;
                    case 8 -> 2;
                    case 9 -> 3;
                    default -> 4;
                };
                base.setRGB(x, y, metal[shade]);
            }
        }
        // valve + cap
        for (int x = 7; x <= 8; x++) {
            base.setRGB(x, 1, metal[x == 7 ? 1 : 3]);
            base.setRGB(x, 2, metal[x == 7 ? 2 : 4]);
        }
        base.setRGB(6, 1, metal[2]);
        base.setRGB(9, 1, metal[4]);
        // hazard band
        for (int x = 5; x <= 10; x++) {
            base.setRGB(x, 12, x <= 7 ? hazard : hazardDark);
        }
        // O2 window (rows 6..10, cols 7..8) - dark glass in base, glowing gas in emissive layer
        for (int y = 6; y <= 10; y++) {
            for (int x = 7; x <= 8; x++) {
                base.setRGB(x, y, windowDark);
                glow.setRGB(x, y, y == 6 ? 0xFFE4FFFF : (x == 7 ? 0xFF7DF6FF : 0xFF3FD2F0));
            }
        }
        outline(base, outline);
        write(base, "textures/item/oxygen_tank.png");
        write(glow, "textures/item/oxygen_tank_emissive.png");
    }

    // ------------------------------------------------------------------------------------
    //  Mod icon (128x128): ringed alien planet over a starfield, pixel-art upscaled 2x
    // ------------------------------------------------------------------------------------
    static void icon() throws IOException {
        int s = 64;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(0x57A2);
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                double t = (double) y / s;
                img.setRGB(x, y, lerp(0xFF070B1E, 0xFF1B0F33, t));
            }
        }
        for (int i = 0; i < 70; i++) {
            int x = rnd.nextInt(s), y = rnd.nextInt(s);
            img.setRGB(x, y, rnd.nextInt(5) == 0 ? 0xFFFFFFFF : 0xFF8FA6D8);
        }
        double cx = 32, cy = 34, r = 17;
        int[] planet = {0xFF2B0F4A, 0xFF4A1F7A, 0xFF6B3BB0, 0xFF3FA9C9, 0xFF7BF0E6, 0xFFD8FFF6};
        // back half of ring
        drawRing(img, cx, cy, r, true);
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                double dx = (x - cx) / r, dy = (y - cy) / r;
                double d2 = dx * dx + dy * dy;
                if (d2 > 1) continue;
                double nz = Math.sqrt(1 - d2);
                double light = Math.max(0, -0.55 * dx - 0.55 * dy + 0.63 * nz);
                double bands = 0.15 * Math.sin((dy * 6 + valueNoise(x / 6.0, y / 6.0, 5) * 2.5));
                int idx = clamp((int) ((light + bands) * planet.length), 0, planet.length - 1);
                img.setRGB(x, y, planet[idx]);
            }
        }
        drawRing(img, cx, cy, r, false);

        BufferedImage big = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                big.setRGB(x, y, img.getRGB(x / 2, y / 2));
            }
        }
        write(big, "icon.png");
    }

    static void drawRing(BufferedImage img, double cx, double cy, double r, boolean back) {
        for (double a = 0; a < Math.PI * 2; a += 0.004) {
            for (double w = 1.45; w <= 1.85; w += 0.08) {
                double x = cx + Math.cos(a) * r * w;
                double y = cy + Math.sin(a) * r * w * 0.32 - Math.cos(a) * r * w * 0.18;
                boolean isBack = Math.sin(a) < 0;
                if (isBack != back) continue;
                int ix = (int) Math.round(x), iy = (int) Math.round(y);
                if (ix < 0 || iy < 0 || ix >= img.getWidth() || iy >= img.getHeight()) continue;
                img.setRGB(ix, iy, w < 1.6 ? 0xFFF7C66B : 0xFFE0914A);
            }
        }
    }

    // ------------------------------------------------------------------------------------
    //  helpers
    // ------------------------------------------------------------------------------------
    static void outline(BufferedImage img, int color) {
        int w = img.getWidth(), h = img.getHeight();
        boolean[][] solid = new boolean[w][h];
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                solid[x][y] = (img.getRGB(x, y) >>> 24) != 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (solid[x][y]) continue;
                for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    int nx = x + d[0], ny = y + d[1];
                    if (nx >= 0 && ny >= 0 && nx < w && ny < h && solid[nx][ny]) {
                        img.setRGB(x, y, color);
                        break;
                    }
                }
            }
        }
    }

    static double valueNoise(double x, double y, int seed) {
        int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
        double fx = x - x0, fy = y - y0;
        fx = fx * fx * (3 - 2 * fx);
        fy = fy * fy * (3 - 2 * fy);
        double a = hash(x0, y0, seed), b = hash(x0 + 1, y0, seed);
        double c = hash(x0, y0 + 1, seed), d = hash(x0 + 1, y0 + 1, seed);
        return (a + (b - a) * fx) * (1 - fy) + (c + (d - c) * fx) * fy;
    }

    static double hash(int x, int y, int seed) {
        // wrap at 4 cells so the 16px block texture tiles seamlessly
        x = Math.floorMod(x, 4);
        y = Math.floorMod(y, 4);
        long h = x * 374761393L + y * 668265263L + seed * 2147483647L;
        h = (h ^ (h >>> 13)) * 1274126177L;
        return ((h ^ (h >>> 16)) & 0xFFFF) / 65535.0;
    }

    static int lerp(int c1, int c2, double t) {
        int r = (int) (((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = (int) (((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = (int) ((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static boolean inside(int x, int y) {
        return x >= 0 && y >= 0 && x < 16 && y < 16;
    }

    static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }

    static void write(BufferedImage img, String relative) throws IOException {
        Path out = assets.resolve(relative);
        Files.createDirectories(out.getParent());
        ImageIO.write(img, "png", out.toFile());
    }
}
