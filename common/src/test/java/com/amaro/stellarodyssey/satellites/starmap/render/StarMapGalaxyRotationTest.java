package com.amaro.stellarodyssey.satellites.starmap.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-JVM coverage for the procedural spiral-galaxy background (S3-A1).
 * {@link StarMapSkyRenderer} exposes its galaxy spec as public constants and its
 * projector {@link StarMapSkyRenderer#project(double, double, double, int, int,
 * double, double, float, float, float)} is Minecraft-free, so the invariants can
 * be tested headlessly.
 */
@DisplayName("StarMap Procedural Galaxy Tests (S3-A1)")
class StarMapGalaxyRotationTest {

    @Test
    @DisplayName("Every projected galaxy star sits inside the galactic disk with in-range alpha")
    void galaxyStarsStayInsideDisk() {
        float cx = 400.0F;
        float cy = 180.0F;
        float scale = 2.0F;
        float maxRadius = (StarMapSkyRenderer.GALAXY_CORE_RADIUS + 150.0F) * scale;
        for (int i = 0; i < StarMapSkyRenderer.GALAXY_STAR_COUNT; i++) {
            StarMapSkyRenderer.GalaxyStarPoint pt = StarMapSkyRenderer.galaxyPoint(i, 0L, cx, cy, scale);
            double dx = pt.x() - cx;
            double dy = (pt.y() - cy) / 0.42F; // undo disk flattening
            double radius = Math.sqrt(dx * dx + dy * dy);
            assertTrue(radius <= maxRadius + 1.0E-6, "star " + i + " escaped the disk: " + radius);
            assertTrue(pt.alpha() >= 24 && pt.alpha() <= 210, "alpha out of range at star " + i);
        }
    }

    @Test
    @DisplayName("galaxyPoint rotates over time: stars sweep around the core")
    void galaxyRotates() {
        float cx = 400.0F;
        float cy = 180.0F;
        float scale = 2.0F;
        StarMapSkyRenderer.GalaxyStarPoint t0 = StarMapSkyRenderer.galaxyPoint(5, 0L, cx, cy, scale);
        StarMapSkyRenderer.GalaxyStarPoint t1 = StarMapSkyRenderer.galaxyPoint(5, 1200L, cx, cy, scale);
        boolean moved = Math.abs(t0.x() - t1.x()) > 0.5 || Math.abs(t0.y() - t1.y()) > 0.5;
        assertTrue(moved, "galaxy must visibly rotate");
    }

    @Test
    @DisplayName("galaxyPoint is fully deterministic and out-of-range indices wrap safely")
    void galaxyPointDeterministicAndSafe() {
        StarMapSkyRenderer.GalaxyStarPoint a = StarMapSkyRenderer.galaxyPoint(7, 99L, 300.0F, 150.0F, 1.5F);
        StarMapSkyRenderer.GalaxyStarPoint b = StarMapSkyRenderer.galaxyPoint(7, 99L, 300.0F, 150.0F, 1.5F);
        assertEquals(a, b, "no randomness is allowed in the galaxy projection");
        // floorMod wraps negative indices to the same star as its positive twin.
        StarMapSkyRenderer.GalaxyStarPoint wrapped = StarMapSkyRenderer.galaxyPoint(
                -1, 99L, 300.0F, 150.0F, 1.5F);
        StarMapSkyRenderer.GalaxyStarPoint twin = StarMapSkyRenderer.galaxyPoint(
                StarMapSkyRenderer.GALAXY_STAR_COUNT - 1, 99L, 300.0F, 150.0F, 1.5F);
        assertEquals(twin, wrapped, "negative index must wrap to the last star");
    }

    @Test
    @DisplayName("The galaxy spec is a multi-arm spiral with a populated disk")
    void galaxySpecInvariants() {
        assertTrue(StarMapSkyRenderer.GALAXY_ARMS >= 2, "a spiral galaxy needs >= 2 arms");
        assertTrue(StarMapSkyRenderer.GALAXY_STAR_COUNT > 0, "the disk must be populated");
        assertTrue(StarMapSkyRenderer.GALAXY_SPIN > 0.0F, "spiral arms must wind");
        assertTrue(StarMapSkyRenderer.GALAXY_CORE_RADIUS > 0.0F, "a galactic bulge must exist");
    }

    @Test
    @DisplayName("project() applies yaw rotation deterministically: stars sweep around the core")
    void yawRotationSweepsStars() {
        int w = 800;
        int h = 600;
        StarMapSkyRenderer.ProjectedPoint t0 = StarMapSkyRenderer.project(100.0, 0.0, 0.0,
                w, h, 0.0, 0.0, 1.0F, 25.0F, 0.0F);
        StarMapSkyRenderer.ProjectedPoint t1 = StarMapSkyRenderer.project(100.0, 0.0, 0.0,
                w, h, 0.0, 0.0, 1.0F, 25.0F, 90.0F);
        // A 90-degree yaw rotates the (100, 0) star's projected offset by a quarter turn.
        assertTrue(t0.visible() && t1.visible(), "mid-disk stars must stay in viewport");
        assertTrue(Math.abs(t0.screenX() - t1.screenX()) > 1.0 || Math.abs(t0.screenY() - t1.screenY()) > 1.0,
                "yaw must visibly sweep the star");
    }

    @Test
    @DisplayName("Orbital coordinates interpolate smoothly between whole ticks")
    void orbitalCoordinatesInterpolateBetweenTicks() {
        ICelestialBodyStub body = new ICelestialBodyStub();
        StarMapSkyRenderer.SectorCoordinates first = StarMapSkyRenderer.getCoordinates(body, 10.0);
        StarMapSkyRenderer.SectorCoordinates halfway = StarMapSkyRenderer.getCoordinates(body, 10.5);
        StarMapSkyRenderer.SectorCoordinates next = StarMapSkyRenderer.getCoordinates(body, 11.0);

        assertEquals(first.sectorCode(), halfway.sectorCode(), "sector identity must not vary with animation time");
        assertEquals(first.sectorCode(), next.sectorCode(), "sector identity must not vary with animation time");
        assertTrue(Math.abs(halfway.x() - first.x()) < Math.abs(next.x() - first.x())
                        || Math.abs(halfway.z() - first.z()) < Math.abs(next.z() - first.z()),
                "sub-tick coordinates should interpolate rather than jump to the next tick");
    }

    @Test
    @DisplayName("The projector is fully deterministic: equal inputs yield equal outputs")
    void projectorIsDeterministic() {
        StarMapSkyRenderer.ProjectedPoint a = StarMapSkyRenderer.project(
                12.5, -3.0, 77.0, 800, 600, 10.0, -5.0, 1.5F, 25.0F, 33.0F);
        StarMapSkyRenderer.ProjectedPoint b = StarMapSkyRenderer.project(
                12.5, -3.0, 77.0, 800, 600, 10.0, -5.0, 1.5F, 25.0F, 33.0F);
        assertEquals(a, b, "no randomness is allowed in the projector");
    }

    @Test
    @DisplayName("Zoom scales the projected radius monotonically around the viewport center")
    void zoomScalesRadius() {
        int w = 800;
        int h = 600;
        double wx = 100.0;
        double wy = 0.0;
        double wz = 0.0;
        StarMapSkyRenderer.ProjectedPoint zoomedOut = StarMapSkyRenderer.project(
                wx, wy, wz, w, h, 0.0, 0.0, 0.5F, 25.0F, 0.0F);
        StarMapSkyRenderer.ProjectedPoint zoomedIn = StarMapSkyRenderer.project(
                wx, wy, wz, w, h, 0.0, 0.0, 2.0F, 25.0F, 0.0F);
        double radiusOut = Math.abs(zoomedOut.screenX() - w / 2.0);
        double radiusIn = Math.abs(zoomedIn.screenX() - w / 2.0);
        assertTrue(radiusIn > radiusOut, "zoom-in must project farther from the core");
    }

    @Test
    @DisplayName("Sector coordinates are deterministic per body and carry a sector code")
    void sectorCoordinatesAreDeterministic() {
        ICelestialBodyStub body = new ICelestialBodyStub();
        StarMapSkyRenderer.SectorCoordinates a = StarMapSkyRenderer.getCoordinates(body);
        StarMapSkyRenderer.SectorCoordinates b = StarMapSkyRenderer.getCoordinates(body);
        assertEquals(a, b, "sector coordinates must be deterministic");
        assertTrue(a.sectorCode() != null && !a.sectorCode().isEmpty());
        // Null-safety: a null body falls back to the home sector without crashing.
        StarMapSkyRenderer.SectorCoordinates home = StarMapSkyRenderer.getCoordinates(null);
        assertEquals("SEC-00-SOL", home.sectorCode());
    }

    /** Minimal concrete body for headless coordinate derivation. */
    private static final class ICelestialBodyStub implements com.amaro.stellarodyssey.api.celestial.ICelestialBody {
        @Override
        public net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimensionKey() {
            return net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.Identifier.parse("stellarodyssey:test_world"));
        }

        @Override
        public double gravityMultiplier() {
            return 1.0;
        }

        @Override
        public float atmosphericPressure() {
            return 1.0F;
        }

        @Override
        public boolean hasBreathableAtmosphere() {
            return true;
        }

        @Override
        public float solarRadiation() {
            return 1.0F;
        }

        @Override
        public String starSystemName() {
            return "Test System";
        }
    }
}
