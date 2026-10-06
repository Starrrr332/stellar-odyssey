import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

/**
 * High-quality procedural art generator for Stellar Odyssey.
 * <p>
 * Run from the project root with any JDK 11+ (no compilation step needed):
 * <pre>  java tools/TextureGen.java</pre>
 * Every texture is deterministic (fixed seeds) so re-running produces identical files.
 * <p>
 * Art direction: "bioluminescent deep space" - dark indigo basalt, neon cyan/magenta
 * crystals, white high-tech suit & ship with cyan emissive accents. Block and item
 * textures are 32x32 (2x vanilla) for crisp detail; entity textures keep the exact
 * sizes their models' UVs require (64x64 armor, 128x128 starship).
 * <p>
 * Emissive convention: {@code <name>_emissive.png} holds ONLY the glowing pixels (fully
 * opaque) on a fully transparent background. Models render it as a second, full-bright layer.
 */
public class TextureGen {
    static Path assets;
    static final int S = 32; // block/item texture resolution (2x vanilla)

    public static void main(String[] args) throws IOException {
        Path project = Path.of(args.length > 0 ? args[0] : ".").toAbsolutePath().normalize();
        assets = project.resolve("common/src/main/resources/assets/stellarodyssey");
        alienOre();
        alienStone();
        alienTurf();
        oxygenTank();
        spacesuit();
        starship();
        rocket();
        rocketItems();
        machineBlocks();
        assemblyTableGui();
        icon();
        newMinerals();
        rawAlien();
        System.out.println("Textures written to " + assets);
    }

    // ------------------------------------------------------------------------------------
    //  Alien Ore: violet alien bedrock + bioluminescent crystal shards
    // ------------------------------------------------------------------------------------
    static void alienOre() throws IOException {
        BufferedImage base = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glow = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);

        int[] stone = {0xFF141024, 0xFF1C1730, 0xFF241D3C, 0xFF2C2448, 0xFF352B55};
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double n = 0.55 * valueNoise(x / 6.0, y / 6.0, 11) + 0.3 * valueNoise(x / 3.0, y / 3.0, 23)
                        + 0.15 * valueNoise(x / 1.5, y / 1.5, 37);
                base.setRGB(x, y, stone[clamp((int) (n * stone.length), 0, stone.length - 1)]);
            }
        }
        // subtle cracks
        for (int i = 0; i < 5; i++) {
            int cx = (i * 7 + 3) % S, cy = (i * 11 + 5) % S;
            for (int k = 0; k < 6; k++) {
                int x = (cx + k * 2) % S, y = (cy + k) % S;
                base.setRGB(x, y, 0xFF0D0A18);
                base.setRGB((x + 1) % S, y, 0xFF0D0A18);
            }
        }

        // Crystal clusters: {centerX, centerY, palette, shardCount}
        int[][] clusters = {
                {7, 9, 0, 3},
                {24, 7, 1, 2},
                {15, 24, 0, 2},
                {27, 22, 1, 1},
        };
        int[][] palettes = {
                // edge, body, core (cyan)
                {0xFF1E9FC4, 0xFF5EF2FF, 0xFFE4FFFF},
                // edge, body, core (magenta)
                {0xFF8B34C9, 0xFFDB72FF, 0xFFFFE3FF},
        };
        int socketDark = 0xFF0D0A18;
        int socketLit = 0xFF173A4A;

        for (int[] c : clusters) {
            int[] pal = palettes[c[2]];
            Random rnd = new Random(c[0] * 31L + c[1] * 17L + c[2] * 7L);
            for (int s = 0; s < c[3]; s++) {
                int len = 3 + rnd.nextInt(3);
                int dx = rnd.nextBoolean() ? 1 : -1;
                int dy = rnd.nextBoolean() ? 1 : -1;
                int sx = c[0] + rnd.nextInt(3) - 1;
                int sy = c[1] + rnd.nextInt(3) - 1;
                for (int i = 0; i < len; i++) {
                    int x = sx + dx * i, y = sy + dy * i;
                    if (!inside(x, y)) break;
                    boolean core = i == len / 2;
                    glow.setRGB(x, y, core ? pal[2] : pal[1]);
                    base.setRGB(x, y, socketLit);
                    // 4-neighbour edge glow
                    for (int[] d : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                        int ex = x + d[0], ey = y + d[1];
                        if (inside(ex, ey) && glow.getRGB(ex, ey) == 0) {
                            glow.setRGB(ex, ey, pal[0]);
                            base.setRGB(ex, ey, socketDark);
                        }
                    }
                }
            }
            // soft halo around cluster
            for (int dy = -3; dy <= 3; dy++) {
                for (int dx = -3; dx <= 3; dx++) {
                    int x = c[0] + dx, y = c[1] + dy;
                    if (!inside(x, y)) continue;
                    double d = Math.sqrt(dx * dx + dy * dy);
                    if (d > 3.2) continue;
                    if (glow.getRGB(x, y) == 0 && rnd.nextDouble() < 0.25 * (1 - d / 3.2)) {
                        glow.setRGB(x, y, pal[0]);
                    }
                }
            }
        }

        write(base, "textures/block/alien_ore.png");
        write(glow, "textures/block/alien_ore_emissive.png");
    }

    // ------------------------------------------------------------------------------------
    //  Alien Stone: Dense dark basaltic crust of exoplanets
    // ------------------------------------------------------------------------------------
    static void alienStone() throws IOException {
        BufferedImage base = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        int[] stone = {0xFF141024, 0xFF1C1730, 0xFF241D3C, 0xFF2C2448, 0xFF352B55, 0xFF3E3262};
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double n = 0.5 * valueNoise(x / 6.0, y / 6.0, 42) + 0.3 * valueNoise(x / 3.0, y / 3.0, 77)
                        + 0.2 * valueNoise(x / 1.5, y / 1.5, 13);
                base.setRGB(x, y, stone[clamp((int) (n * stone.length), 0, stone.length - 1)]);
            }
        }
        // mineral veins (subtle lighter streaks)
        for (int i = 0; i < 4; i++) {
            int cx = (i * 13 + 2) % S, cy = (i * 9 + 4) % S;
            for (int k = 0; k < 8; k++) {
                int x = (cx + k) % S, y = (cy + (k * 3) / 2) % S;
                base.setRGB(x, y, 0xFF4A3D73);
                base.setRGB((x + 1) % S, (y + 1) % S, 0xFF3E3262);
            }
        }
        // sparse embedded crystal flecks
        Random rnd = new Random(0x51E);
        for (int i = 0; i < 6; i++) {
            int x = rnd.nextInt(S), y = rnd.nextInt(S);
            base.setRGB(x, y, 0xFF5EF2FF);
            base.setRGB((x + 1) % S, y, 0xFF1E9FC4);
        }
        write(base, "textures/block/alien_stone.png");
    }

    // ------------------------------------------------------------------------------------
    //  Alien Turf: Bioluminescent neon surface cover
    // ------------------------------------------------------------------------------------
    static void alienTurf() throws IOException {
        BufferedImage top = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        BufferedImage topGlow = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        BufferedImage side = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        BufferedImage sideGlow = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);

        int[] turfBase = {0xFF003840, 0xFF004D54, 0xFF00666B, 0xFF008082};
        int[] neonCyan = {0xFF00E5FF, 0xFF76FFFF, 0xFFE0F7FA};
        int[] neonMagenta = {0xFFDB72FF, 0xFFFFE3FF};
        int[] stone = {0xFF141024, 0xFF1C1730, 0xFF241D3C};

        // Top face: dense turf with spore glow dots and faint magenta spores
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double n = valueNoise(x / 5.0, y / 5.0, 99) * 0.7 + valueNoise(x / 2.5, y / 2.5, 31) * 0.3;
                top.setRGB(x, y, turfBase[clamp((int) (n * turfBase.length), 0, turfBase.length - 1)]);
                if ((x * 7 + y * 13) % 11 == 0) {
                    topGlow.setRGB(x, y, neonCyan[(x + y) % neonCyan.length]);
                } else if ((x * 5 + y * 17) % 23 == 0) {
                    topGlow.setRGB(x, y, neonMagenta[(x + y) % neonMagenta.length]);
                }
            }
        }

        // Side face: turf overhang with hanging glowing tendrils over stone
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                int drop = 6 + (int) (4.5 * Math.sin(x * 0.35 + 0.7) + 2.5 * Math.sin(x * 0.9 + 2.0));
                if (y <= drop) {
                    double n = valueNoise(x / 4.0, y / 4.0, 55);
                    side.setRGB(x, y, turfBase[clamp((int) (n * turfBase.length), 0, turfBase.length - 1)]);
                    if (y == drop && (x % 4 == 0)) {
                        sideGlow.setRGB(x, y, neonCyan[0]);
                    }
                } else {
                    side.setRGB(x, y, stone[(x * 3 + y * 7) % stone.length]);
                }
            }
        }
        // hanging tendrils with glowing tips
        Random rnd = new Random(0x7A7);
        for (int i = 0; i < 7; i++) {
            int x = rnd.nextInt(S);
            int start = 6 + (int) (4.5 * Math.sin(x * 0.35 + 0.7) + 2.5 * Math.sin(x * 0.9 + 2.0));
            int len = 3 + rnd.nextInt(4);
            for (int k = 1; k <= len; k++) {
                int y = start + k;
                if (y >= S) break;
                side.setRGB(x, y, 0xFF00666B);
                if (k == len) {
                    sideGlow.setRGB(x, y, neonCyan[0]);
                }
            }
        }

        write(top, "textures/block/alien_turf_top.png");
        write(topGlow, "textures/block/alien_turf_top_emissive.png");
        write(side, "textures/block/alien_turf_side.png");
        write(sideGlow, "textures/block/alien_turf_side_emissive.png");
    }

    // ------------------------------------------------------------------------------------
    //  Oxygen Tank: brushed-metal capsule with a glowing O2 window + hazard band
    // ------------------------------------------------------------------------------------
    static void oxygenTank() throws IOException {
        BufferedImage base = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glow = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);

        int outline = 0xFF1A1E26;
        int[] metal = {0xFFF4F7FB, 0xFFD3DAE4, 0xFFAEB8C6, 0xFF8793A5, 0xFF5F6B7E, 0xFF3E4857};
        int hazard = 0xFFFF8A2A;
        int hazardDark = 0xFFB24E12;
        int windowDark = 0xFF0B2A33;

        // body: columns 10..21, rows 6..25 with rounded corners
        for (int y = 6; y <= 25; y++) {
            for (int x = 10; x <= 21; x++) {
                boolean corner = (y == 6 || y == 25) && (x == 10 || x == 21);
                if (corner) continue;
                int shade = switch (x) {
                    case 10 -> 1;
                    case 11 -> 0;
                    case 12 -> 0;
                    case 13 -> 1;
                    case 14 -> 1;
                    case 15 -> 2;
                    case 16 -> 2;
                    case 17 -> 3;
                    case 18 -> 3;
                    case 19 -> 4;
                    default -> 5;
                };
                base.setRGB(x, y, metal[shade]);
            }
        }
        // brushed-metal vertical streaks
        Random rnd = new Random(0x0A2);
        for (int i = 0; i < 14; i++) {
            int x = 10 + rnd.nextInt(12);
            int y0 = 7 + rnd.nextInt(16);
            for (int k = 0; k < 4; k++) {
                int y = y0 + k;
                if (y > 24) break;
                int c = base.getRGB(x, y);
                base.setRGB(x, y, shade(c, rnd.nextBoolean() ? 10 : -10));
            }
        }
        // valve + cap
        for (int x = 14; x <= 17; x++) {
            base.setRGB(x, 3, metal[x <= 15 ? 1 : 4]);
            base.setRGB(x, 4, metal[x <= 15 ? 2 : 5]);
            base.setRGB(x, 5, metal[x <= 15 ? 1 : 4]);
        }
        base.setRGB(13, 4, metal[2]);
        base.setRGB(18, 4, metal[5]);
        base.setRGB(15, 2, metal[2]);
        base.setRGB(16, 2, metal[4]);
        // hazard band (diagonal stripes)
        for (int x = 10; x <= 21; x++) {
            for (int y = 22; y <= 24; y++) {
                boolean stripe = ((x + y) / 2) % 2 == 0;
                base.setRGB(x, y, stripe ? hazard : hazardDark);
            }
        }
        // O2 window (rows 12..18, cols 14..17) - dark glass in base, glowing gas in emissive
        for (int y = 12; y <= 18; y++) {
            for (int x = 14; x <= 17; x++) {
                base.setRGB(x, y, windowDark);
                glow.setRGB(x, y, y == 12 ? 0xFFE4FFFF : (x == 14 ? 0xFF7DF6FF : 0xFF3FD2F0));
            }
        }
        // window frame
        for (int x = 13; x <= 18; x++) {
            base.setRGB(x, 11, metal[3]);
            base.setRGB(x, 19, metal[3]);
        }
        for (int y = 12; y <= 18; y++) {
            base.setRGB(13, y, metal[3]);
            base.setRGB(18, y, metal[3]);
        }
        // hose connector at bottom
        for (int x = 14; x <= 17; x++) {
            base.setRGB(x, 26, metal[4]);
            base.setRGB(x, 27, metal[5]);
        }
        base.setRGB(15, 28, metal[5]);
        base.setRGB(16, 28, metal[5]);
        outline(base, outline);
        write(base, "textures/item/oxygen_tank.png");
        write(glow, "textures/item/oxygen_tank_emissive.png");
    }

    // ------------------------------------------------------------------------------------
    //  Modular Spacesuit: High-tech astronaut gear with emissive visor & manifold
    // ------------------------------------------------------------------------------------
    static void spacesuit() throws IOException {
        spacesuitHelmet();
        spacesuitChestplate();
        spacesuitLeggings();
        spacesuitBoots();
        spacesuitEntityArmor();
    }

    static void spacesuitHelmet() throws IOException {
        BufferedImage base = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glow = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        int[] whiteSuit = {0xFFFFFFFF, 0xFFE6EBF2, 0xFFC3CBD8, 0xFF97A1B2, 0xFF6B7688};
        int darkTrim = 0xFF232833;
        int visorDark = 0xFF00363A;

        // Helmet shell: rows 4..27, cols 5..26
        for (int y = 4; y <= 27; y++) {
            for (int x = 5; x <= 26; x++) {
                if ((y == 4 && (x <= 7 || x >= 24)) || (y == 27 && (x <= 7 || x >= 24))) continue;
                int shade = (x == 5 || x == 26 || y == 4) ? 1 : (y == 27 ? 3 : 0);
                base.setRGB(x, y, whiteSuit[shade]);
            }
        }
        // dome highlight
        for (int y = 5; y <= 9; y++) {
            for (int x = 9; x <= 15; x++) {
                base.setRGB(x, y, whiteSuit[0]);
            }
        }
        // Visor cutout (rows 10..19, cols 9..22)
        for (int y = 10; y <= 19; y++) {
            for (int x = 9; x <= 22; x++) {
                base.setRGB(x, y, visorDark);
                glow.setRGB(x, y, (x == 9 || y == 10) ? 0xFFE0F7FA : 0xFF00E5FF);
            }
        }
        // visor reflection streak
        for (int x = 12; x <= 19; x++) {
            base.setRGB(x, 12, 0xFF0E6E75);
        }
        // Chin breather vent
        for (int x = 12; x <= 19; x++) {
            base.setRGB(x, 22, darkTrim);
            base.setRGB(x, 23, darkTrim);
        }
        // side comm lights (emissive)
        base.setRGB(7, 14, 0xFF00E5FF);
        glow.setRGB(7, 14, 0xFF00E5FF);
        base.setRGB(24, 14, 0xFF00E5FF);
        glow.setRGB(24, 14, 0xFF00E5FF);
        // top antenna light
        base.setRGB(15, 3, 0xFFFF5A5A);
        glow.setRGB(15, 3, 0xFFFF5A5A);
        outline(base, darkTrim);
        write(base, "textures/item/spacesuit_helmet.png");
        write(glow, "textures/item/spacesuit_helmet_emissive.png");
    }

    static void spacesuitChestplate() throws IOException {
        BufferedImage base = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glow = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        int[] whiteSuit = {0xFFFFFFFF, 0xFFE6EBF2, 0xFFC3CBD8, 0xFF97A1B2, 0xFF6B7688};
        int darkTrim = 0xFF232833;
        int cyanGlow = 0xFF00E5FF;

        // Shoulders & Chest
        for (int y = 4; y <= 27; y++) {
            for (int x = 3; x <= 28; x++) {
                if (y <= 8 && (x >= 12 && x <= 19)) continue; // Neck opening
                if (y >= 16 && (x <= 5 || x >= 26)) continue; // Arm openings
                int shade = (y == 27 || x == 3 || x == 28) ? 2 : 1;
                base.setRGB(x, y, whiteSuit[shade]);
            }
        }
        // chest highlight
        for (int y = 9; y <= 13; y++) {
            for (int x = 10; x <= 21; x++) {
                base.setRGB(x, y, whiteSuit[0]);
            }
        }
        // Oxygen Manifold chest connection & HUD status indicator (rows 13..15, cols 13..18)
        for (int y = 13; y <= 15; y++) {
            for (int x = 13; x <= 18; x++) {
                base.setRGB(x, y, darkTrim);
            }
        }
        base.setRGB(14, 14, cyanGlow);
        base.setRGB(15, 14, cyanGlow);
        base.setRGB(16, 14, cyanGlow);
        base.setRGB(17, 14, 0xFFE0F7FA);
        glow.setRGB(14, 14, cyanGlow);
        glow.setRGB(15, 14, cyanGlow);
        glow.setRGB(16, 14, cyanGlow);
        glow.setRGB(17, 14, 0xFFE0F7FA);
        // collar
        for (int x = 10; x <= 21; x++) {
            base.setRGB(x, 8, darkTrim);
        }
        // Utility belt
        for (int x = 7; x <= 24; x++) {
            base.setRGB(x, 24, darkTrim);
            base.setRGB(x, 25, darkTrim);
        }
        // belt buckle
        base.setRGB(15, 24, 0xFF97A1B2);
        base.setRGB(16, 24, 0xFF97A1B2);
        base.setRGB(15, 25, 0xFF97A1B2);
        base.setRGB(16, 25, 0xFF97A1B2);
        outline(base, darkTrim);
        write(base, "textures/item/spacesuit_chestplate.png");
        write(glow, "textures/item/spacesuit_chestplate_emissive.png");
    }

    static void spacesuitLeggings() throws IOException {
        BufferedImage base = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        int[] whiteSuit = {0xFFFFFFFF, 0xFFE6EBF2, 0xFFC3CBD8, 0xFF97A1B2, 0xFF6B7688};
        int darkTrim = 0xFF232833 | 0xFF000000;
        int cyanStripe = 0xFF00E5FF;

        // Waist
        for (int y = 4; y <= 8; y++) {
            for (int x = 6; x <= 25; x++) {
                base.setRGB(x, y, whiteSuit[1]);
            }
        }
        // Legs (cols 6..13 and cols 18..25)
        for (int y = 9; y <= 27; y++) {
            for (int x = 6; x <= 13; x++) base.setRGB(x, y, whiteSuit[2]);
            for (int x = 18; x <= 25; x++) base.setRGB(x, y, whiteSuit[2]);
        }
        // inner leg shading
        for (int y = 9; y <= 27; y++) {
            base.setRGB(6, y, whiteSuit[3]);
            base.setRGB(25, y, whiteSuit[3]);
        }
        // Knee armor pads with cyan stripe
        for (int x = 8; x <= 11; x++) {
            base.setRGB(x, 16, darkTrim);
            base.setRGB(x, 17, darkTrim);
            base.setRGB(x, 18, darkTrim);
        }
        for (int x = 20; x <= 23; x++) {
            base.setRGB(x, 16, darkTrim);
            base.setRGB(x, 17, darkTrim);
            base.setRGB(x, 18, darkTrim);
        }
        base.setRGB(9, 17, cyanStripe);
        base.setRGB(10, 17, cyanStripe);
        base.setRGB(21, 17, cyanStripe);
        base.setRGB(22, 17, cyanStripe);
        outline(base, darkTrim);
        write(base, "textures/item/spacesuit_leggings.png");
    }

    static void spacesuitBoots() throws IOException {
        BufferedImage base = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glow = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        int[] whiteSuit = {0xFFFFFFFF, 0xFFE6EBF2, 0xFFC3CBD8, 0xFF97A1B2, 0xFF6B7688};
        int darkTrim = 0xFF232833;
        int cyanGlow = 0xFF00E5FF;

        // Left & Right boots
        for (int y = 14; y <= 27; y++) {
            for (int x = 4; x <= 12; x++) {
                if (y == 14 && x == 4) continue;
                base.setRGB(x, y, whiteSuit[1]);
            }
            for (int x = 19; x <= 27; x++) {
                if (y == 14 && x == 27) continue;
                base.setRGB(x, y, whiteSuit[1]);
            }
        }
        // boot highlights
        for (int x = 6; x <= 9; x++) base.setRGB(x, 15, whiteSuit[0]);
        for (int x = 21; x <= 24; x++) base.setRGB(x, 15, whiteSuit[0]);
        // Heavy reinforced soles
        for (int x = 4; x <= 12; x++) {
            base.setRGB(x, 26, darkTrim);
            base.setRGB(x, 27, darkTrim);
        }
        for (int x = 19; x <= 27; x++) {
            base.setRGB(x, 26, darkTrim);
            base.setRGB(x, 27, darkTrim);
        }
        // ankle trim
        for (int x = 5; x <= 11; x++) base.setRGB(x, 20, darkTrim);
        for (int x = 20; x <= 26; x++) base.setRGB(x, 20, darkTrim);

        // Micro-thruster exhaust ports (lateral)
        base.setRGB(4, 22, cyanGlow);
        glow.setRGB(4, 22, cyanGlow);
        base.setRGB(27, 22, cyanGlow);
        glow.setRGB(27, 22, cyanGlow);
        base.setRGB(4, 23, 0xFFE0F7FA);
        glow.setRGB(4, 23, 0xFFE0F7FA);
        base.setRGB(27, 23, 0xFFE0F7FA);
        glow.setRGB(27, 23, 0xFFE0F7FA);

        outline(base, darkTrim);
        write(base, "textures/item/spacesuit_boots.png");
        write(glow, "textures/item/spacesuit_boots_emissive.png");
    }

    // ------------------------------------------------------------------------------------
    //  Spacesuit entity armor (64x64, vanilla humanoid UV layout)
    // ------------------------------------------------------------------------------------
    static void spacesuitEntityArmor() throws IOException {
        BufferedImage l1 = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        BufferedImage l1Glow = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        BufferedImage l2 = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);

        int suitColor = 0xFFE6EBF2;
        int suitLight = 0xFFFFFFFF;
        int suitDark = 0xFF97A1B2;
        int trimColor = 0xFF232833;
        int visorColor = 0xFF00E5FF;
        int visorBright = 0xFFE0F7FA;

        // ---- Layer 1 ----
        // Head (0,0)-(32,16): top (8,0) 8x8, bottom (16,0) 8x8, right (0,8) 8x8,
        //                     front (8,8) 8x8, left (16,8) 8x8, back (24,8) 8x8
        paintArmorFace(l1, 8, 0, 8, 8, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 16, 0, 8, 8, suitDark, suitColor, trimColor, 1);
        paintArmorFace(l1, 0, 8, 8, 8, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 8, 8, 8, 8, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 16, 8, 8, 8, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 24, 8, 8, 8, suitColor, suitDark, trimColor, 1);
        // visor on head front (9..14, 9..14)
        for (int y = 9; y <= 14; y++) {
            for (int x = 9; x <= 14; x++) {
                l1.setRGB(x, y, 0xFF00363A);
                l1Glow.setRGB(x, y, (x == 9 || y == 9) ? visorBright : visorColor);
            }
        }
        // head side vents
        l1.setRGB(2, 12, trimColor);
        l1.setRGB(3, 12, trimColor);
        l1.setRGB(28, 12, trimColor);
        l1.setRGB(29, 12, trimColor);

        // Body (16,16)-(40,32): top (20,16) 8x4, bottom (28,16) 8x4, right (16,20) 4x12,
        //                       front (20,20) 8x12, left (28,20) 4x12, back (32,20) 8x12
        paintArmorFace(l1, 20, 16, 8, 4, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 28, 16, 8, 4, suitDark, suitColor, trimColor, 1);
        paintArmorFace(l1, 16, 20, 4, 12, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 20, 20, 8, 12, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 28, 20, 4, 12, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 32, 20, 8, 12, suitColor, suitDark, trimColor, 1);
        // chest LED on front (24,22)
        l1.setRGB(24, 22, visorColor);
        l1Glow.setRGB(24, 22, visorColor);
        l1.setRGB(23, 22, visorBright);
        l1Glow.setRGB(23, 22, visorBright);
        // belt on front
        for (int x = 21; x <= 27; x++) {
            l1.setRGB(x, 29, trimColor);
            l1.setRGB(x, 30, trimColor);
        }

        // Right arm (40,16)-(56,32): top (44,16) 4x4, bottom (48,16) 4x4, right (40,20) 4x12,
        //                            front (44,20) 4x12, left (48,20) 4x12, back (52,20) 4x12
        paintArmorFace(l1, 44, 16, 4, 4, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 48, 16, 4, 4, suitDark, suitColor, trimColor, 1);
        paintArmorFace(l1, 40, 20, 4, 12, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 44, 20, 4, 12, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 48, 20, 4, 12, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 52, 20, 4, 12, suitColor, suitDark, trimColor, 1);
        // elbow pad
        for (int x = 44; x <= 47; x++) {
            l1.setRGB(x, 26, trimColor);
            l1.setRGB(x, 27, trimColor);
        }

        // Right leg (0,16)-(16,32): top (4,16) 4x4, bottom (8,16) 4x4, right (0,20) 4x12,
        //                           front (4,20) 4x12, left (8,20) 4x12, back (12,20) 4x12
        paintArmorFace(l1, 4, 16, 4, 4, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 8, 16, 4, 4, suitDark, suitColor, trimColor, 1);
        paintArmorFace(l1, 0, 20, 4, 12, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 4, 20, 4, 12, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 8, 20, 4, 12, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 12, 20, 4, 12, suitColor, suitDark, trimColor, 1);
        // boot sole
        for (int x = 4; x <= 7; x++) {
            l1.setRGB(x, 30, trimColor);
            l1.setRGB(x, 31, trimColor);
        }

        // Left leg (16,48)-(32,64): top (20,48) 4x4, bottom (24,48) 4x4, right (16,52) 4x12,
        //                           front (20,52) 4x12, left (24,52) 4x12, back (28,52) 4x12
        paintArmorFace(l1, 20, 48, 4, 4, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 24, 48, 4, 4, suitDark, suitColor, trimColor, 1);
        paintArmorFace(l1, 16, 52, 4, 12, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 20, 52, 4, 12, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 24, 52, 4, 12, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 28, 52, 4, 12, suitColor, suitDark, trimColor, 1);
        // boot sole
        for (int x = 20; x <= 23; x++) {
            l1.setRGB(x, 62, trimColor);
            l1.setRGB(x, 63, trimColor);
        }

        // Left arm (32,48)-(48,64): top (36,48) 4x4, bottom (40,48) 4x4, right (32,52) 4x12,
        //                           front (36,52) 4x12, left (40,52) 4x12, back (44,52) 4x12
        paintArmorFace(l1, 36, 48, 4, 4, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 40, 48, 4, 4, suitDark, suitColor, trimColor, 1);
        paintArmorFace(l1, 32, 52, 4, 12, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 36, 52, 4, 12, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 40, 52, 4, 12, suitColor, suitDark, trimColor, 1);
        paintArmorFace(l1, 44, 52, 4, 12, suitColor, suitDark, trimColor, 1);
        // elbow pad
        for (int x = 36; x <= 39; x++) {
            l1.setRGB(x, 58, trimColor);
            l1.setRGB(x, 59, trimColor);
        }

        // ---- Layer 2 (leggings) ----
        paintArmorFace(l2, 20, 16, 8, 4, suitColor, suitDark, trimColor, 2);
        paintArmorFace(l2, 28, 16, 8, 4, suitDark, suitColor, trimColor, 2);
        paintArmorFace(l2, 16, 20, 4, 12, suitColor, suitDark, trimColor, 2);
        paintArmorFace(l2, 20, 20, 8, 12, suitColor, suitDark, trimColor, 2);
        paintArmorFace(l2, 28, 20, 4, 12, suitColor, suitDark, trimColor, 2);
        paintArmorFace(l2, 32, 20, 8, 12, suitColor, suitDark, trimColor, 2);
        paintArmorFace(l2, 4, 16, 4, 4, suitColor, suitDark, trimColor, 2);
        paintArmorFace(l2, 8, 16, 4, 4, suitDark, suitColor, trimColor, 2);
        paintArmorFace(l2, 0, 20, 4, 12, suitColor, suitDark, trimColor, 2);
        paintArmorFace(l2, 4, 20, 4, 12, suitColor, suitDark, trimColor, 2);
        paintArmorFace(l2, 8, 20, 4, 12, suitColor, suitDark, trimColor, 2);
        paintArmorFace(l2, 12, 20, 4, 12, suitColor, suitDark, trimColor, 2);
        paintArmorFace(l2, 20, 48, 4, 4, suitColor, suitDark, trimColor, 2);
        paintArmorFace(l2, 24, 48, 4, 4, suitDark, suitColor, trimColor, 2);
        paintArmorFace(l2, 16, 52, 4, 12, suitColor, suitDark, trimColor, 2);
        paintArmorFace(l2, 20, 52, 4, 12, suitColor, suitDark, trimColor, 2);
        paintArmorFace(l2, 24, 52, 4, 12, suitColor, suitDark, trimColor, 2);
        paintArmorFace(l2, 28, 52, 4, 12, suitColor, suitDark, trimColor, 2);
        // knee pads on leggings front
        for (int x = 5; x <= 6; x++) {
            l2.setRGB(x, 26, trimColor);
            l2.setRGB(x, 27, trimColor);
        }
        for (int x = 21; x <= 22; x++) {
            l2.setRGB(x, 58, trimColor);
            l2.setRGB(x, 59, trimColor);
        }

        write(l1, "textures/entity/equipment/humanoid/spacesuit.png");
        write(l1Glow, "textures/entity/equipment/humanoid/spacesuit_emissive.png");
        write(l2, "textures/entity/equipment/humanoid_leggings/spacesuit.png");
    }

    /** Fills a vanilla armor face with a subtle checker + trim pattern. */
    static void paintArmorFace(BufferedImage img, int x0, int y0, int w, int h,
                               int light, int dark, int trim, int layer) {
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                boolean edge = x == 0 || y == 0 || x == w - 1 || y == h - 1;
                img.setRGB(x0 + x, y0 + y, edge ? trim : ((x + y) % 2 == 0 ? light : dark));
            }
        }
    }

    // ------------------------------------------------------------------------------------
    //  Starship: Sleek exploratory spacecraft with emissive ion engines and cockpit
    // ------------------------------------------------------------------------------------
    static void starship() throws IOException {
        starshipItem();
        starshipItem3D();
        starshipEntity();
    }

    static void starshipItem() throws IOException {
        BufferedImage base = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glow = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        int[] hull = {0xFFF8FAFC, 0xFFE2E8F0, 0xFFCBD5E1, 0xFF94A3B8, 0xFF475569};
        int darkTrim = 0xFF1E293B;
        int cyanIon = 0xFF00E5FF;
        int canopyGlass = 0xFF006064;

        // Delta wing starship silhouette (facing up)
        for (int y = 2; y <= 29; y++) {
            int span = (y <= 8) ? (y / 2) : Math.min(12, (y - 2) / 2 + 2);
            for (int dx = -span; dx <= span; dx++) {
                int x = 15 + dx;
                if (x < 0 || x >= S) continue;
                int shade = Math.abs(dx) <= 2 ? 0 : (Math.abs(dx) <= 5 ? 1 : (Math.abs(dx) <= 8 ? 2 : 3));
                base.setRGB(x, y, hull[shade]);
            }
        }
        // hull panel lines
        for (int y = 6; y <= 28; y += 6) {
            int span = (y <= 8) ? (y / 2) : Math.min(12, (y - 2) / 2 + 2);
            for (int dx = -span + 1; dx <= span - 1; dx++) {
                int x = 15 + dx;
                if (x >= 0 && x < S) base.setRGB(x, y, darkTrim);
            }
        }
        // Cockpit canopy (rows 8..15, center)
        for (int y = 8; y <= 15; y++) {
            for (int x = 13; x <= 17; x++) {
                base.setRGB(x, y, canopyGlass);
                glow.setRGB(x, y, (y == 8 || y == 9) ? 0xFFE0F7FA : cyanIon);
            }
        }
        // canopy frame
        for (int x = 12; x <= 18; x++) {
            base.setRGB(x, 7, darkTrim);
            base.setRGB(x, 16, darkTrim);
        }
        // Twin rear ion thrusters (row 29, cols 11, 19)
        base.setRGB(11, 29, darkTrim);
        base.setRGB(19, 29, darkTrim);
        base.setRGB(11, 30, cyanIon);
        base.setRGB(19, 30, cyanIon);
        base.setRGB(11, 31, 0xFFE0F7FA);
        base.setRGB(19, 31, 0xFFE0F7FA);
        glow.setRGB(11, 30, cyanIon);
        glow.setRGB(19, 30, cyanIon);
        glow.setRGB(11, 31, 0xFFE0F7FA);
        glow.setRGB(19, 31, 0xFFE0F7FA);
        // wingtip nav lights
        base.setRGB(3, 20, 0xFFFF5A5A);
        glow.setRGB(3, 20, 0xFFFF5A5A);
        base.setRGB(27, 20, 0xFF00E5FF);
        glow.setRGB(27, 20, 0xFF00E5FF);

        outline(base, darkTrim);
        write(base, "textures/item/starship.png");
        write(glow, "textures/item/starship_emissive.png");
    }

    /**
     * 64x64 texture for the 3D item model (models/item/starship.json).
     * UV regions match the cube layout of that model exactly; glow is baked in
     * so the 3D ship reads as lit without needing a shader emissive pass.
     */
    static void starshipItem3D() throws IOException {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        int hullWhite = 0xFFE2E8F0;
        int hullLight = 0xFFF8FAFC;
        int hullSlate = 0xFF94A3B8;
        int darkPlating = 0xFF1E293B;
        int ionCyan = 0xFF00E5FF;
        int ionBright = 0xFFE0F7FA;
        int glassCyan = 0xFF00838F;

        // hull: texOffs(0,0) 8x4x12
        paintCube(img, null, 0, 0, 8, 4, 12, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean panel = (x % 4 == 0) || (y % 4 == 0);
                    int c = panel ? darkPlating : ((x + y) % 8 < 4 ? hullWhite : hullLight);
                    if (face.equals("bottom")) c = panel ? darkPlating : hullSlate;
                    b.setRGB(x0 + x, y0 + y, c);
                }
            }
            for (int x = 0; x < w; x += 4) {
                for (int y = 0; y < h; y += 4) {
                    b.setRGB(x0 + x, y0 + y, 0xFF0F172A);
                }
            }
        });

        // nose: texOffs(0,16) 4x2x4
        paintCube(img, null, 0, 16, 4, 2, 4, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    b.setRGB(x0 + x, y0 + y, x < 2 ? hullWhite : darkPlating);
                }
            }
        });

        // canopy: texOffs(0,22) 4x2x6 - glass with baked glow streaks
        paintCube(img, null, 0, 22, 4, 2, 6, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean streak = (x + y) % 5 == 0;
                    b.setRGB(x0 + x, y0 + y, streak ? ionBright : glassCyan);
                }
            }
        });

        // left wing: texOffs(0,30) 6x1x8
        paintCube(img, null, 0, 30, 6, 1, 8, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean edge = x == 0 || x == w - 1 || y == 0 || y == h - 1;
                    boolean tip = x >= w - 2;
                    int c = edge ? darkPlating : (tip ? hullSlate : hullWhite);
                    if (face.equals("bottom")) c = edge ? darkPlating : hullSlate;
                    b.setRGB(x0 + x, y0 + y, c);
                }
            }
        });

        // right wing: texOffs(0,50) 6x1x8 - mirrored content in its own region
        paintCube(img, null, 0, 50, 6, 1, 8, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean edge = x == 0 || x == w - 1 || y == 0 || y == h - 1;
                    boolean tip = x <= 1;
                    int c = edge ? darkPlating : (tip ? hullSlate : hullWhite);
                    if (face.equals("bottom")) c = edge ? darkPlating : hullSlate;
                    b.setRGB(x0 + x, y0 + y, c);
                }
            }
        });

        // engines: texOffs(0,39) 2x2x3 - exhaust glow baked on back face
        paintCube(img, null, 0, 39, 2, 2, 3, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    b.setRGB(x0 + x, y0 + y, darkPlating);
                }
            }
            if (face.equals("back")) {
                for (int y = 0; y < h; y++) {
                    for (int x = 0; x < w; x++) {
                        boolean ring = x == 0 || x == w - 1 || y == 0 || y == h - 1;
                        b.setRGB(x0 + x, y0 + y, ring ? ionCyan : ionBright);
                    }
                }
            }
        });

        // fins: texOffs(0,44) 1x2x3
        paintCube(img, null, 0, 44, 1, 2, 3, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean edge = x == 0 || x == w - 1 || y == 0 || y == h - 1;
                    b.setRGB(x0 + x, y0 + y, edge ? darkPlating : hullSlate);
                }
            }
        });

        write(img, "textures/item/starship_3d.png");
    }

    // ------------------------------------------------------------------------------------
    //  Starship entity texture (128x128) - UV regions computed from StarshipModel.java
    //  so panel lines, rivets and glow align exactly with the 3D model's faces.
    // ------------------------------------------------------------------------------------
    static void starshipEntity() throws IOException {
        BufferedImage base = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        BufferedImage glow = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);

        int hullWhite = 0xFFE2E8F0;
        int hullLight = 0xFFF8FAFC;
        int hullSlate = 0xFF94A3B8;
        int darkPlating = 0xFF1E293B;
        int ionCyan = 0xFF00E5FF;
        int ionBright = 0xFFE0F7FA;
        int glassCyan = 0xFF00838F;

        // hull: texOffs(0,0) 20x8x32
        paintCube(base, glow, 0, 0, 20, 8, 32, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean panel = (x % 4 == 0) || (y % 4 == 0);
                    int c = panel ? darkPlating : ((x + y) % 8 < 4 ? hullWhite : hullLight);
                    if (face.equals("bottom")) c = panel ? darkPlating : hullSlate;
                    b.setRGB(x0 + x, y0 + y, c);
                }
            }
            // rivets at panel intersections
            for (int x = 0; x < w; x += 4) {
                for (int y = 0; y < h; y += 4) {
                    b.setRGB(x0 + x, y0 + y, 0xFF0F172A);
                }
            }
        });

        // nose: texOffs(0,40) 12x6x8
        paintCube(base, glow, 0, 40, 12, 6, 8, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean tip = x < 2;
                    b.setRGB(x0 + x, y0 + y, tip ? hullWhite : darkPlating);
                }
            }
        });

        // canopy: texOffs(0,54) 12x4x14
        paintCube(base, glow, 0, 54, 12, 4, 14, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    b.setRGB(x0 + x, y0 + y, glassCyan);
                    boolean streak = (x + y) % 5 == 0;
                    g.setRGB(x0 + x, y0 + y, streak ? ionBright : ionCyan);
                }
            }
        });

        // wings: texOffs(40,40) 16x2x20
        paintCube(base, glow, 40, 40, 16, 2, 20, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean edge = x == 0 || x == w - 1 || y == 0 || y == h - 1;
                    boolean tip = x >= w - 3;
                    int c = edge ? darkPlating : (tip ? hullSlate : hullWhite);
                    if (face.equals("bottom")) c = edge ? darkPlating : hullSlate;
                    b.setRGB(x0 + x, y0 + y, c);
                }
            }
            for (int x = 0; x < w; x += 4) {
                for (int y = 0; y < h; y += 4) {
                    b.setRGB(x0 + x, y0 + y, 0xFF0F172A);
                }
            }
        });

        // engines: texOffs(72,0) 6x6x10
        paintCube(base, glow, 72, 0, 6, 6, 10, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    b.setRGB(x0 + x, y0 + y, darkPlating);
                }
            }
            if (face.equals("back")) {
                // exhaust nozzle glow ring
                for (int x = 0; x < w; x++) {
                    for (int y = 0; y < h; y++) {
                        boolean ring = x == 0 || x == w - 1 || y == 0 || y == h - 1;
                        g.setRGB(x0 + x, y0 + y, ring ? ionCyan : ionBright);
                    }
                }
            }
        });

        // fins: texOffs(104,0) 2x8x10
        paintCube(base, glow, 104, 0, 2, 8, 10, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean edge = x == 0 || x == w - 1 || y == 0 || y == h - 1;
                    b.setRGB(x0 + x, y0 + y, edge ? darkPlating : hullSlate);
                }
            }
        });

        write(base, "textures/entity/starship/starship.png");
        write(glow, "textures/entity/starship/starship_emissive.png");
    }

    /** Paints all six faces of a cube at their Minecraft UV positions. */
    interface Material {
        void paint(BufferedImage base, BufferedImage glow, String face, int x0, int y0, int w, int h);
    }

    static void paintCube(BufferedImage base, BufferedImage glow, int u, int v, int w, int h, int d, Material mat) {
        mat.paint(base, glow, "top", u + d, v, w, d);
        mat.paint(base, glow, "bottom", u + d + w, v, w, d);
        mat.paint(base, glow, "right", u, v + d, d, h);
        mat.paint(base, glow, "front", u + d, v + d, w, h);
        mat.paint(base, glow, "left", u + d + w, v + d, d, h);
        mat.paint(base, glow, "back", u + d + w + d, v + d, w, h);
    }

    // ------------------------------------------------------------------------------------
    //  Rocket entity texture (64x64) - UVs match RocketModel.java
    // ------------------------------------------------------------------------------------
    static void rocket() throws IOException {
        BufferedImage img = new BufferedImage(128, 64, BufferedImage.TYPE_INT_ARGB);
        int hullWhite = 0xFFE2E8F0;
        int hullLight = 0xFFF8FAFC;
        int hullSlate = 0xFF94A3B8;
        int darkPlating = 0xFF1E293B;
        int accentCyan = 0xFF00E5FF;
        int accentDark = 0xFF00838F;
        int engineBell = 0xFF475569;

        // fuselage: texOffs(0,0) 12x24x12
        paintCube(img, null, 0, 0, 12, 24, 12, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean panel = (x % 4 == 0) || (y % 4 == 0);
                    int c = panel ? darkPlating : ((x + y) % 8 < 4 ? hullWhite : hullLight);
                    if (face.equals("bottom")) c = panel ? darkPlating : hullSlate;
                    b.setRGB(x0 + x, y0 + y, c);
                }
            }
        });

        // nose cone: texOffs(0,36) 8x8x8
        paintCube(img, null, 0, 36, 8, 8, 8, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean tip = y < 2;
                    b.setRGB(x0 + x, y0 + y, tip ? accentCyan : hullWhite);
                }
            }
        });

        // engine bell: texOffs(48,0) 4x2x4
        paintCube(img, null, 48, 0, 4, 2, 4, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean edge = x == 0 || x == w - 1 || y == 0 || y == h - 1;
                    b.setRGB(x0 + x, y0 + y, edge ? darkPlating : engineBell);
                }
            }
        });

        // fins: texOffs(64,0) 2x12x4
        paintCube(img, null, 64, 0, 2, 12, 4, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean edge = x == 0 || x == w - 1 || y == 0 || y == h - 1;
                    b.setRGB(x0 + x, y0 + y, edge ? darkPlating : hullSlate);
                }
            }
        });

        // window band: texOffs(80,0) 8x4x8
        paintCube(img, null, 80, 0, 8, 4, 8, (b, g, face, x0, y0, w, h) -> {
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    boolean window = (x % 3 == 1) && (y % 2 == 0);
                    b.setRGB(x0 + x, y0 + y, window ? accentDark : hullWhite);
                }
            }
        });

        write(img, "textures/entity/rocket/rocket.png");
    }

    // ------------------------------------------------------------------------------------
    //  Rocket component & rocket item textures (32x32) - tier-tinted
    // ------------------------------------------------------------------------------------
    static void rocketItems() throws IOException {
        // Tier accent colors
        int[][] tierColors = {
                {0xFFE2E8F0, 0xFF94A3B8, 0xFF475569},  // T1: steel gray
                {0xFFF59E0B, 0xFFD97724, 0xFF92400E},  // T2: celidium orange
                {0xFF22D3EE, 0xFF06B6D4, 0xFF155E75},  // T3: verdantite cyan
        };
        String[] types = {"cone", "fin", "tank", "engine", "plate", "thruster", "guidance", "heat_shield"};

        for (int tier = 1; tier <= 3; tier++) {
            int[] c = tierColors[tier - 1];
            for (String type : types) {
                BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
                drawComponent(img, type, c[0], c[1], c[2]);
                write(img, "textures/item/rocket_" + type + "_t" + tier + ".png");
            }
            // Assembled rocket item
            BufferedImage rocket = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
            drawRocketItem(rocket, c[0], c[1], c[2]);
            write(rocket, "textures/item/rocket_t" + tier + ".png");
        }
    }

    static void drawComponent(BufferedImage img, String type, int light, int mid, int dark) {
        int outline = 0xFF1E293B;
        switch (type) {
            case "cone" -> {
                for (int y = 4; y <= 26; y++) {
                    int half = (y - 4) / 2;
                    for (int dx = -half; dx <= half; dx++) {
                        int x = 15 + dx;
                        if (x >= 0 && x < S) img.setRGB(x, y, y < 12 ? light : mid);
                    }
                }
            }
            case "fin" -> {
                for (int y = 6; y <= 26; y++) {
                    int half = (y - 6) / 3;
                    for (int dx = -half; dx <= half; dx++) {
                        int x = 15 + dx;
                        if (x >= 0 && x < S) img.setRGB(x, y, mid);
                    }
                }
                for (int y = 6; y <= 26; y++) img.setRGB(15, y, light);
            }
            case "tank" -> {
                for (int y = 5; y <= 26; y++) {
                    for (int x = 10; x <= 20; x++) {
                        img.setRGB(x, y, (x == 10 || x == 20) ? dark : (y < 8 ? light : mid));
                    }
                }
                for (int x = 12; x <= 18; x++) img.setRGB(x, 5, dark);
            }
            case "engine" -> {
                for (int y = 8; y <= 24; y++) {
                    for (int x = 11; x <= 19; x++) {
                        img.setRGB(x, y, (y < 12) ? light : ((x == 11 || x == 19) ? dark : mid));
                    }
                }
                for (int x = 13; x <= 17; x++) img.setRGB(x, 24, dark);
                for (int x = 13; x <= 17; x++) img.setRGB(x, 25, dark);
            }
            case "plate" -> {
                for (int y = 8; y <= 24; y++) {
                    for (int x = 6; x <= 24; x++) {
                        img.setRGB(x, y, ((x + y) % 4 == 0) ? dark : mid);
                    }
                }
                for (int x = 6; x <= 24; x++) {
                    img.setRGB(x, 8, light);
                    img.setRGB(x, 24, dark);
                }
            }
            case "thruster" -> {
                for (int y = 6; y <= 26; y++) {
                    for (int x = 12; x <= 18; x++) {
                        img.setRGB(x, y, (y < 10) ? light : ((x == 12 || x == 18) ? dark : mid));
                    }
                }
                for (int x = 13; x <= 17; x++) img.setRGB(x, 26, 0xFF00E5FF);
            }
            case "guidance" -> {
                for (int y = 10; y <= 22; y++) {
                    for (int x = 8; x <= 22; x++) {
                        img.setRGB(x, y, (x == 8 || x == 22 || y == 10 || y == 22) ? dark : mid);
                    }
                }
                for (int x = 12; x <= 18; x++) img.setRGB(x, 15, light);
                for (int x = 12; x <= 18; x++) img.setRGB(x, 16, 0xFF00E5FF);
            }
            case "heat_shield" -> {
                for (int y = 6; y <= 26; y++) {
                    for (int x = 8; x <= 22; x++) {
                        img.setRGB(x, y, ((x + y) % 3 == 0) ? dark : mid);
                    }
                }
                for (int x = 8; x <= 22; x++) {
                    img.setRGB(x, 6, light);
                    img.setRGB(x, 26, dark);
                }
            }
        }
        outline(img, outline);
    }

    static void drawRocketItem(BufferedImage img, int light, int mid, int dark) {
        int outline = 0xFF1E293B;
        // Vertical rocket silhouette
        for (int y = 3; y <= 28; y++) {
            int half = (y < 8) ? (8 - y) / 2 : 4;
            for (int dx = -half; dx <= half; dx++) {
                int x = 15 + dx;
                if (x >= 0 && x < S) img.setRGB(x, y, (y < 8) ? light : ((y > 24) ? dark : mid));
            }
        }
        // Window band
        for (int x = 12; x <= 18; x++) {
            img.setRGB(x, 12, 0xFF00838F);
            img.setRGB(x, 13, 0xFF00E5FF);
        }
        // Fins
        for (int y = 18; y <= 26; y++) {
            img.setRGB(10, y, dark);
            img.setRGB(20, y, dark);
        }
        // Engine flame
        for (int x = 13; x <= 17; x++) {
            img.setRGB(x, 29, 0xFFFF8A2A);
            img.setRGB(x, 30, 0xFFFFD54F);
        }
        outline(img, outline);
    }

    // ------------------------------------------------------------------------------------
    //  Machine block textures (assembly table, launch pad) - 32x32
    // ------------------------------------------------------------------------------------
    static void machineBlocks() throws IOException {
        // Assembly table: metal workbench with cyan accents
        BufferedImage table = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        int[] metal = {0xFFE2E8F0, 0xFFCBD5E1, 0xFF94A3B8, 0xFF475569, 0xFF1E293B};
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                if (y < 6) {
                    table.setRGB(x, y, metal[0]);
                } else if (y < 10) {
                    table.setRGB(x, y, metal[1]);
                } else if (y < 26) {
                    table.setRGB(x, y, metal[(x / 4 + y / 4) % 2 == 0 ? 2 : 3]);
                } else {
                    table.setRGB(x, y, metal[4]);
                }
            }
        }
        // Table surface detail: blueprint grid
        for (int y = 2; y < 6; y++) {
            for (int x = 0; x < S; x += 4) {
                table.setRGB(x, y, 0xFF00E5FF);
            }
        }
        // Cyan status light
        table.setRGB(26, 3, 0xFF00E5FF);
        table.setRGB(27, 3, 0xFF00E5FF);
        table.setRGB(26, 4, 0xFF00E5FF);
        table.setRGB(27, 4, 0xFF00E5FF);
        write(table, "textures/block/assembly_table.png");

        // Launch pad: dark metal plate with cyan ring
        BufferedImage pad = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                boolean ring = (x == 0 || y == 0 || x == S - 1 || y == S - 1);
                boolean inner = (x >= 6 && x <= 25 && y >= 6 && y <= 25);
                pad.setRGB(x, y, ring ? 0xFF1E293B : (inner ? 0xFF475569 : 0xFF334155));
            }
        }
        // Cyan corner lights
        pad.setRGB(2, 2, 0xFF00E5FF);
        pad.setRGB(29, 2, 0xFF00E5FF);
        pad.setRGB(2, 29, 0xFF00E5FF);
        pad.setRGB(29, 29, 0xFF00E5FF);
        write(pad, "textures/block/launch_pad.png");

        // Launch pad base: plain dark plate
        BufferedImage base = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                boolean edge = (x == 0 || y == 0 || x == S - 1 || y == S - 1);
                base.setRGB(x, y, edge ? 0xFF1E293B : 0xFF334155);
            }
        }
        write(base, "textures/block/launch_pad_base.png");
    }

    // ------------------------------------------------------------------------------------
    //  Assembly Table GUI (256x256 sheet, 176x166 panel at top-left)
    // ------------------------------------------------------------------------------------
    static void assemblyTableGui() throws IOException {
        BufferedImage img = new BufferedImage(256, 256, BufferedImage.TYPE_INT_ARGB);
        int panel = 0xFFC6C6C6;
        int panelDark = 0xFF8B8B8B;
        int panelLight = 0xFFFFFFFF;
        int slotBg = 0xFF8B8B8B;
        int slotBorder = 0xFF373737;
        int accent = 0xFF00E5FF;
        int accentDark = 0xFF00838F;

        // Panel background (176x166)
        for (int y = 0; y < 166; y++) {
            for (int x = 0; x < 176; x++) {
                boolean edge = x < 4 || y < 4 || x >= 172 || y >= 162;
                boolean corner = (x < 4 && y < 4) || (x >= 172 && y < 4) || (x < 4 && y >= 162) || (x >= 172 && y >= 162);
                int c = corner ? panelDark : (edge ? panelDark : ((x + y) % 2 == 0 ? panel : panelLight));
                img.setRGB(x, y, c);
            }
        }
        // Title bar accent
        for (int x = 4; x < 172; x++) {
            img.setRGB(x, 4, accentDark);
        }
        // Component slots: 2 rows x 4 cols at (26,18) + col*18 / row*18, each 16x16
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 4; col++) {
                int sx = 26 + col * 18 - 1;
                int sy = 18 + row * 18 - 1;
                drawSlot(img, sx, sy, slotBg, slotBorder);
            }
        }
        // Result slot at (134,27)
        drawSlot(img, 133, 26, slotBg, accent);
        // Rocket icon placeholder in result area (simple silhouette)
        for (int y = 30; y <= 40; y++) {
            int span = (y - 30) / 2 + 2;
            for (int dx = -span; dx <= span; dx++) {
                int x = 141 + dx;
                if (x >= 134 && x <= 150) img.setRGB(x, y, 0xFF373737);
            }
        }
        // Inventory area label separator
        for (int x = 8; x < 168; x++) {
            img.setRGB(x, 70, panelDark);
        }

        write(img, "textures/gui/assembly_table.png");
    }

    static void drawSlot(BufferedImage img, int x0, int y0, int bg, int border) {
        for (int y = 0; y < 18; y++) {
            for (int x = 0; x < 18; x++) {
                boolean edge = x == 0 || y == 0 || x == 17 || y == 17;
                img.setRGB(x0 + x, y0 + y, edge ? border : bg);
            }
        }
    }

    // ------------------------------------------------------------------------------------
    //  Mod icon (128x128): ringed alien planet with atmosphere glow over a starfield
    // ------------------------------------------------------------------------------------
    static void icon() throws IOException {
        int s = 128;
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(0x57A2);

        // deep space gradient
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                double t = (double) y / s;
                img.setRGB(x, y, lerp(0xFF070B1E, 0xFF1B0F33, t));
            }
        }
        // nebula wisps
        for (int i = 0; i < 40; i++) {
            int nx = rnd.nextInt(s), ny = rnd.nextInt(s);
            int len = 8 + rnd.nextInt(20);
            int dx = rnd.nextBoolean() ? 1 : -1;
            int dy = rnd.nextBoolean() ? 1 : -1;
            int col = rnd.nextBoolean() ? 0xFF2A1B4A : 0xFF0E3A4A;
            for (int k = 0; k < len; k++) {
                int x = nx + dx * k, y = ny + dy * k;
                if (x < 0 || y < 0 || x >= s || y >= s) break;
                img.setRGB(x, y, col);
            }
        }
        // stars: varied sizes and colors
        for (int i = 0; i < 160; i++) {
            int x = rnd.nextInt(s), y = rnd.nextInt(s);
            int c = switch (rnd.nextInt(6)) {
                case 0 -> 0xFFFFE9B0;
                case 1 -> 0xFFB0E9FF;
                case 2 -> 0xFFFFB0C8;
                default -> 0xFFFFFFFF;
            };
            img.setRGB(x, y, c);
            if (rnd.nextInt(4) == 0) {
                img.setRGB(Math.min(s - 1, x + 1), y, c);
                img.setRGB(Math.max(0, x - 1), y, c);
                img.setRGB(x, Math.min(s - 1, y + 1), c);
                img.setRGB(x, Math.max(0, y - 1), c);
            }
        }

        double cx = 64, cy = 66, r = 30;
        int[] planet = {0xFF2B0F4A, 0xFF4A1F7A, 0xFF6B3BB0, 0xFF3FA9C9, 0xFF7BF0E6, 0xFFD8FFF6};

        // atmosphere glow (behind planet)
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                double d = Math.sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy));
                if (d > r + 10 || d < r) continue;
                double a = 1 - (d - r) / 10.0;
                int c = img.getRGB(x, y);
                img.setRGB(x, y, blend(c, 0xFF7BF0E6, (int) (a * 90)));
            }
        }

        // back half of ring
        drawRing(img, cx, cy, r, true);
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                double dx = (x - cx) / r, dy = (y - cy) / r;
                double d2 = dx * dx + dy * dy;
                if (d2 > 1) continue;
                double nz = Math.sqrt(1 - d2);
                double light = Math.max(0, -0.55 * dx - 0.55 * dy + 0.63 * nz);
                double bands = 0.15 * Math.sin((dy * 6 + valueNoise(x / 10.0, y / 10.0, 5) * 2.5));
                int idx = clamp((int) ((light + bands) * planet.length), 0, planet.length - 1);
                img.setRGB(x, y, planet[idx]);
            }
        }
        // planet highlight
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                double dx = (x - cx) / r, dy = (y - cy) / r;
                double d2 = dx * dx + dy * dy;
                if (d2 > 1) continue;
                double light = Math.max(0, -0.55 * dx - 0.55 * dy + 0.63 * Math.sqrt(1 - d2));
                if (light > 0.85) {
                    img.setRGB(x, y, blend(img.getRGB(x, y), 0xFFFFFFFF, 60));
                }
            }
        }
        drawRing(img, cx, cy, r, false);

        // small moon
        int mx = 100, my = 30, mr = 6;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                double d = Math.sqrt((x - mx) * (x - mx) + (y - my) * (y - my));
                if (d > mr) continue;
                double light = Math.max(0, -0.5 * (x - mx) / mr - 0.5 * (y - my) / mr + 0.7);
                img.setRGB(x, y, lerp(0xFF6B7280, 0xFFE5E7EB, light));
            }
        }

        write(img, "icon.png");
    }

    static void drawRing(BufferedImage img, double cx, double cy, double r, boolean back) {
        for (double a = 0; a < Math.PI * 2; a += 0.002) {
            for (double w = 1.45; w <= 1.85; w += 0.05) {
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
    //  New alien minerals: celidium (amber), verdantite (toxic emerald), astralite (cyan)
    //  Each has: ore block (cube_all), raw item chunk, refined ingot bar
    // ------------------------------------------------------------------------------------
    static void newMinerals() throws IOException {
        mineral("celidium", 0xFFD97724, 0xFFFBBF77, 0xFF7C2D12, 42);
        mineral("verdantite", 0xFF22C55E, 0xFF86EFAC, 0xFF14532D, 77);
        mineral("astralite", 0xFF06B6D4, 0xFFA5F3FC, 0xFF164E63, 13);
    }

    static void mineral(String name, int main, int light, int dark, int seed) throws IOException {
        BufferedImage ore = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        int[] stone = {0xFF141024, 0xFF1C1730, 0xFF241D3C, 0xFF2C2448, 0xFF352B55};
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double n = 0.55 * valueNoise(x / 6.0, y / 6.0, seed) + 0.3 * valueNoise(x / 3.0, y / 3.0, seed + 1)
                        + 0.15 * valueNoise(x / 1.5, y / 1.5, seed + 2);
                ore.setRGB(x, y, stone[clamp((int) (n * stone.length), 0, stone.length - 1)]);
            }
        }
        Random rnd = new Random(seed * 97L);
        int[][] blobs = {{8, 8}, {22, 10}, {14, 22}, {26, 24}};
        for (int[] b : blobs) {
            int size = 2 + rnd.nextInt(2);
            for (int dy = -size; dy <= size; dy++) {
                for (int dx = -size; dx <= size; dx++) {
                    if (Math.abs(dx) + Math.abs(dy) > size) continue;
                    int x = b[0] + dx, y = b[1] + dy;
                    if (!inside(x, y)) continue;
                    boolean edge = Math.abs(dx) + Math.abs(dy) == size;
                    ore.setRGB(x, y, edge ? dark : (rnd.nextInt(4) == 0 ? light : main));
                }
            }
        }
        write(ore, "textures/block/" + name + "_ore.png");

        drawRawChunk(name, main, light, dark, seed);

        BufferedImage ingot = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        for (int y = 12; y <= 20; y++) {
            int inset = Math.max(0, 4 - Math.abs(y - 16));
            for (int x = 5 + inset; x <= 26 - inset; x++) {
                double n = valueNoise(x / 5.0, y / 2.0, seed + 7);
                int c;
                if (y <= 14) c = light;
                else if (y >= 19 || x >= 24) c = dark;
                else c = n < 0.5 ? main : light;
                ingot.setRGB(x, y, c);
            }
        }
        outline(ingot, 0xFF1A1E26);
        write(ingot, "textures/item/" + name + "_ingot.png");
    }

    /** Round-ish raw mineral chunk item sprite (shared by the refined minerals). */
    static void drawRawChunk(String name, int main, int light, int dark, int seed) throws IOException {
        BufferedImage raw = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double dx = (x - 15.5) / 9.5, dy = (y - 15.5) / 9.5;
                double wob = 0.18 * valueNoise(x / 3.0, y / 3.0, seed + 5);
                if (dx * dx + dy * dy * 1.15 > 1 + wob) continue;
                double n = valueNoise(x / 4.0, y / 4.0, seed + 6);
                int c = n < 0.35 ? dark : (n < 0.62 ? main : light);
                if (dx * dx + dy * dy * 1.15 > 0.82) c = dark;
                raw.setRGB(x, y, c);
            }
        }
        outline(raw, 0xFF1A1E26);
        write(raw, "textures/item/raw_" + name + ".png");
    }

    // ------------------------------------------------------------------------------------
    //  Alien ore raw chunk: violet crystal matrix with magenta bioluminescent glow.
    //  alien_ore has no refined ingot, so only the raw item sprite is generated here.
    // ------------------------------------------------------------------------------------
    static void rawAlien() throws IOException {
        drawRawChunk("alien", 0xFF7C4DFF, 0xFFDB72FF, 0xFF241D3C, 61);
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

    /** Lightens or darkens an ARGB color by delta (positive = lighter). */
    static int shade(int c, int delta) {
        int r = clamp(((c >> 16) & 255) + delta, 0, 255);
        int g = clamp(((c >> 8) & 255) + delta, 0, 255);
        int b = clamp((c & 255) + delta, 0, 255);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Blends color c toward target by amount (0-255). */
    static int blend(int c, int target, int amount) {
        int r = ((c >> 16) & 255) + ((((target >> 16) & 255) - ((c >> 16) & 255)) * amount) / 255;
        int g = ((c >> 8) & 255) + ((((target >> 8) & 255) - ((c >> 8) & 255)) * amount) / 255;
        int b = (c & 255) + (((target & 255) - (c & 255)) * amount) / 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
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
        // wrap at 8 cells so the 32px block texture tiles seamlessly
        x = Math.floorMod(x, 8);
        y = Math.floorMod(y, 8);
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
        return x >= 0 && y >= 0 && x < S && y < S;
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
